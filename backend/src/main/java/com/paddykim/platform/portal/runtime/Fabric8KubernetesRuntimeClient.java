package com.paddykim.platform.portal.runtime;

import com.paddykim.platform.portal.catalog.ApplicationComponent;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Fabric8KubernetesRuntimeClient implements KubernetesRuntimeClient {

    private static final Logger log = LoggerFactory.getLogger(Fabric8KubernetesRuntimeClient.class);
    private static final int RECENT_EVENT_LIMIT = 5;

    @Override
    public KubernetesRuntimeSnapshot getRuntime(String namespace, List<ApplicationComponent> components) {
        Config config = Config.autoConfigure(null);
        config.setConnectionTimeout(2_000);
        config.setRequestTimeout(3_000);
        config.setRequestRetryBackoffLimit(0);

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build()) {
            List<Event> namespaceEvents = client.v1().events()
                    .inNamespace(namespace)
                    .list()
                    .getItems();

            List<RuntimeComponentSnapshot> runtimeComponents = components.stream()
                    .map(component -> readComponent(client, namespace, namespaceEvents, component))
                    .toList();

            return new KubernetesRuntimeSnapshot(runtimeComponents);
        } catch (RuntimeException exception) {
            log.warn("Unable to read Kubernetes runtime status for namespace {}", namespace, exception);
            throw new KubernetesRuntimeUnavailableException("Unable to read Kubernetes runtime status", exception);
        }
    }

    private RuntimeComponentSnapshot readComponent(
            KubernetesClient client,
            String namespace,
            List<Event> namespaceEvents,
            ApplicationComponent component
    ) {
        Deployment deployment = client.apps().deployments()
                .inNamespace(namespace)
                .withName(component.getDeploymentName())
                .get();
        Service service = client.services()
                .inNamespace(namespace)
                .withName(component.getServiceName())
                .get();

        if (deployment == null) {
            return missingComponent(component, service);
        }

        Map<String, String> selector = deployment.getSpec().getSelector().getMatchLabels();
        List<Pod> pods = client.pods()
                .inNamespace(namespace)
                .withLabels(selector)
                .list()
                .getItems();

        List<RuntimePodSnapshot> podSnapshots = pods.stream()
                .sorted(Comparator.comparing(Pod::getMetadata, Comparator.comparing(metadata -> metadata.getName())))
                .map(Fabric8KubernetesRuntimeClient::toPodSnapshot)
                .toList();
        List<RuntimeEventSnapshot> eventSnapshots = namespaceEvents.stream()
                .filter(event -> referencesComponent(event, deployment, pods, service))
                .sorted(Comparator.comparing(Fabric8KubernetesRuntimeClient::eventTimestamp).reversed())
                .limit(RECENT_EVENT_LIMIT)
                .map(Fabric8KubernetesRuntimeClient::toEventSnapshot)
                .toList();

        int desiredReplicas = valueOrZero(deployment.getStatus().getReplicas());
        int readyReplicas = valueOrZero(deployment.getStatus().getReadyReplicas());
        int availableReplicas = valueOrZero(deployment.getStatus().getAvailableReplicas());
        int updatedReplicas = valueOrZero(deployment.getStatus().getUpdatedReplicas());
        int warningEvents = (int) eventSnapshots.stream()
                .filter(event -> "Warning".equalsIgnoreCase(event.type()))
                .count();
        int restartCount = podSnapshots.stream().mapToInt(RuntimePodSnapshot::restartCount).sum();

        String status = componentStatus(deployment, service, desiredReplicas, readyReplicas, availableReplicas, warningEvents, restartCount);

        return new RuntimeComponentSnapshot(
                component.getId(),
                component.getName(),
                component.getKind(),
                status,
                component.getDeploymentName(),
                desiredReplicas,
                readyReplicas,
                availableReplicas,
                updatedReplicas,
                component.getServiceName(),
                service == null ? "Missing" : service.getSpec().getType(),
                service == null ? "Missing" : service.getSpec().getClusterIP(),
                servicePorts(service),
                deploymentImages(deployment),
                podSnapshots,
                eventSnapshots,
                statusMessage(status, service, warningEvents, restartCount)
        );
    }

    private static RuntimeComponentSnapshot missingComponent(ApplicationComponent component, Service service) {
        return new RuntimeComponentSnapshot(
                component.getId(),
                component.getName(),
                component.getKind(),
                "MISSING",
                component.getDeploymentName(),
                0,
                0,
                0,
                0,
                component.getServiceName(),
                service == null ? "Missing" : service.getSpec().getType(),
                service == null ? "Missing" : service.getSpec().getClusterIP(),
                servicePorts(service),
                List.of(),
                List.of(),
                List.of(),
                "Deployment not found"
        );
    }

    private static RuntimePodSnapshot toPodSnapshot(Pod pod) {
        List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses() == null
                ? List.of()
                : pod.getStatus().getContainerStatuses();
        int totalContainers = containerStatuses.size();
        int readyContainers = (int) containerStatuses.stream()
                .filter(status -> Boolean.TRUE.equals(status.getReady()))
                .count();
        int restartCount = containerStatuses.stream()
                .map(ContainerStatus::getRestartCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return new RuntimePodSnapshot(
                pod.getMetadata().getName(),
                pod.getStatus().getPhase(),
                readyContainers,
                totalContainers,
                restartCount,
                pod.getStatus().getPodIP(),
                pod.getSpec().getNodeName(),
                pod.getStatus().getStartTime()
        );
    }

    private static RuntimeEventSnapshot toEventSnapshot(Event event) {
        ObjectReference reference = event.getInvolvedObject();
        return new RuntimeEventSnapshot(
                stringOrUnknown(event.getType()),
                stringOrUnknown(event.getReason()),
                reference == null ? "Unknown" : stringOrUnknown(reference.getKind()),
                reference == null ? "Unknown" : stringOrUnknown(reference.getName()),
                stringOrUnknown(event.getMessage()),
                eventTimestamp(event).toString()
        );
    }

    private static boolean referencesComponent(Event event, Deployment deployment, List<Pod> pods, Service service) {
        ObjectReference reference = event.getInvolvedObject();
        if (reference == null) {
            return false;
        }

        String kind = reference.getKind();
        String name = reference.getName();
        if ("Deployment".equals(kind) && deployment.getMetadata().getName().equals(name)) {
            return true;
        }
        if ("Service".equals(kind) && service != null && service.getMetadata().getName().equals(name)) {
            return true;
        }
        return "Pod".equals(kind) && pods.stream().anyMatch(pod -> pod.getMetadata().getName().equals(name));
    }

    private static Instant eventTimestamp(Event event) {
        if (event.getLastTimestamp() != null) {
            return Instant.parse(event.getLastTimestamp());
        }
        if (event.getFirstTimestamp() != null) {
            return Instant.parse(event.getFirstTimestamp());
        }
        if (event.getMetadata().getCreationTimestamp() != null) {
            return Instant.parse(event.getMetadata().getCreationTimestamp());
        }
        return Instant.EPOCH;
    }

    private static List<String> deploymentImages(Deployment deployment) {
        if (deployment.getSpec().getTemplate().getSpec().getContainers() == null) {
            return List.of();
        }

        return deployment.getSpec().getTemplate().getSpec().getContainers().stream()
                .map(container -> container.getImage())
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<String> servicePorts(Service service) {
        if (service == null || service.getSpec().getPorts() == null) {
            return List.of();
        }

        return service.getSpec().getPorts().stream()
                .map(Fabric8KubernetesRuntimeClient::formatPort)
                .toList();
    }

    private static String formatPort(ServicePort port) {
        String name = port.getName() == null ? "tcp" : port.getName();
        return "%s:%s/%s".formatted(name, port.getPort(), port.getProtocol());
    }

    private static String componentStatus(
            Deployment deployment,
            Service service,
            int desiredReplicas,
            int readyReplicas,
            int availableReplicas,
            int warningEvents,
            int restartCount
    ) {
        if (service == null) {
            return "MISSING";
        }
        if (warningEvents > 0 || restartCount > 0) {
            return "WARNING";
        }
        if (desiredReplicas == readyReplicas && desiredReplicas == availableReplicas) {
            return "READY";
        }
        if (deployment.getStatus().getObservedGeneration() == null) {
            return "UNKNOWN";
        }
        return "PROGRESSING";
    }

    private static String statusMessage(String status, Service service, int warningEvents, int restartCount) {
        if ("MISSING".equals(status) && service == null) {
            return "Deployment or service not found";
        }
        if (warningEvents > 0) {
            return "Recent warning events detected";
        }
        if (restartCount > 0) {
            return "Pod restarts detected";
        }
        return null;
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static String stringOrUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
}
