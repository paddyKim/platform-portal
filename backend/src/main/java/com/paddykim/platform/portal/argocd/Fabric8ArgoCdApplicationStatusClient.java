package com.paddykim.platform.portal.argocd;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Fabric8ArgoCdApplicationStatusClient implements ArgoCdApplicationStatusClient, ArgoCdApplicationCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(Fabric8ArgoCdApplicationStatusClient.class);

    private final String namespace;

    public Fabric8ArgoCdApplicationStatusClient(
            @Value("${platform.argocd.namespace:argocd}") String namespace
    ) {
        this.namespace = namespace;
    }

    @Override
    public ArgoCdApplicationSnapshot getApplicationStatus(String applicationName) {
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config()).build()) {
            GenericKubernetesResource application = client.genericKubernetesResources(applicationContext())
                    .inNamespace(namespace)
                    .withName(applicationName)
                    .get();

            if (application == null) {
                throw new ArgoCdStatusUnavailableException("ArgoCD application not found: " + applicationName);
            }

            return ArgoCdStatusMapper.from(application.getAdditionalProperties());
        } catch (ArgoCdStatusUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Unable to read ArgoCD application status for {}", applicationName, exception);
            throw new ArgoCdStatusUnavailableException("Unable to read ArgoCD application status", exception);
        }
    }

    @Override
    public List<ArgoCdApplicationSummary> listApplications() {
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config()).build()) {
            return client.genericKubernetesResources(applicationContext())
                    .inNamespace(namespace)
                    .list()
                    .getItems()
                    .stream()
                    .map(this::toSummary)
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Unable to list ArgoCD applications", exception);
            throw new ArgoCdStatusUnavailableException("Unable to list ArgoCD applications", exception);
        }
    }

    @Override
    public ArgoCdApplicationSummary createApplication(ArgoCdApplicationCreateRequest request) {
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config()).build()) {
            GenericKubernetesResource resource = new GenericKubernetesResource();
            resource.setApiVersion("argoproj.io/v1alpha1");
            resource.setKind("Application");
            resource.setMetadata(new ObjectMetaBuilder()
                    .withName(request.name())
                    .withNamespace(namespace)
                    .build());
            resource.setAdditionalProperty("spec", applicationSpec(request));

            GenericKubernetesResource created = client.genericKubernetesResources(applicationContext())
                    .inNamespace(namespace)
                    .resource(resource)
                    .create();

            return toSummary(created);
        } catch (RuntimeException exception) {
            log.warn("Unable to create ArgoCD application {}", request.name(), exception);
            throw new ArgoCdStatusUnavailableException("Unable to create ArgoCD application", exception);
        }
    }

    @Override
    public ArgoCdApplicationDetail getApplication(String applicationName) {
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config()).build()) {
            return toDetail(findApplication(client, applicationName));
        } catch (ArgoCdApplicationNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Unable to read ArgoCD application {}", applicationName, exception);
            throw new ArgoCdStatusUnavailableException("Unable to read ArgoCD application", exception);
        }
    }

    @Override
    public ArgoCdApplicationDetail syncApplication(
            String applicationName,
            ArgoCdApplicationSyncRequest request
    ) {
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config()).build()) {
            findApplication(client, applicationName);

            String patch = """
                    {
                      "operation": {
                        "sync": {
                          "prune": %s,
                          "dryRun": %s
                        }
                      }
                    }
                    """.formatted(request.prune(), request.dryRun());

            GenericKubernetesResource updated = client.genericKubernetesResources(applicationContext())
                    .inNamespace(namespace)
                    .withName(applicationName)
                    .patch(PatchContext.of(PatchType.JSON_MERGE), patch);

            return toDetail(updated);
        } catch (ArgoCdApplicationNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Unable to sync ArgoCD application {}", applicationName, exception);
            throw new ArgoCdStatusUnavailableException("Unable to sync ArgoCD application", exception);
        }
    }

    private Config config() {
        Config config = Config.autoConfigure(null);
        config.setConnectionTimeout(2_000);
        config.setRequestTimeout(3_000);
        config.setRequestRetryBackoffLimit(0);
        return config;
    }

    private ResourceDefinitionContext applicationContext() {
        return new ResourceDefinitionContext.Builder()
                .withGroup("argoproj.io")
                .withVersion("v1alpha1")
                .withPlural("applications")
                .withNamespaced(true)
                .build();
    }

    private Map<String, Object> applicationSpec(ArgoCdApplicationCreateRequest request) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("repoURL", request.sourceRepoUrl());
        source.put("path", request.sourcePath());
        source.put("targetRevision", request.targetRevision());

        Map<String, Object> destination = new LinkedHashMap<>();
        destination.put("server", request.destinationServer());
        destination.put("namespace", request.destinationNamespace());

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("project", request.project());
        spec.put("source", source);
        spec.put("destination", destination);

        if (request.automated()) {
            Map<String, Object> automated = new LinkedHashMap<>();
            automated.put("prune", request.prune());
            automated.put("selfHeal", request.selfHeal());
            spec.put("syncPolicy", Map.of("automated", automated));
        }

        return spec;
    }

    private GenericKubernetesResource findApplication(KubernetesClient client, String applicationName) {
        GenericKubernetesResource application = client.genericKubernetesResources(applicationContext())
                .inNamespace(namespace)
                .withName(applicationName)
                .get();

        if (application == null) {
            throw new ArgoCdApplicationNotFoundException(applicationName);
        }
        return application;
    }

    private ArgoCdApplicationDetail toDetail(GenericKubernetesResource application) {
        return ArgoCdApplicationDetailMapper.from(
                application.getMetadata().getName(),
                application.getMetadata().getNamespace(),
                application.getAdditionalProperties()
        );
    }

    @SuppressWarnings("unchecked")
    private ArgoCdApplicationSummary toSummary(GenericKubernetesResource application) {
        Map<String, Object> properties = application.getAdditionalProperties();
        Map<String, Object> spec = asMap(properties.get("spec"));
        Map<String, Object> source = asMap(spec.get("source"));
        Map<String, Object> destination = asMap(spec.get("destination"));
        ArgoCdApplicationSnapshot snapshot = ArgoCdStatusMapper.from(properties);

        return new ArgoCdApplicationSummary(
                application.getMetadata().getName(),
                application.getMetadata().getNamespace(),
                stringOrUnknown(spec.get("project")),
                stringOrUnknown(source.get("repoURL")),
                stringOrUnknown(source.get("path")),
                stringOrUnknown(source.get("targetRevision")),
                stringOrUnknown(destination.get("server")),
                stringOrUnknown(destination.get("namespace")),
                snapshot.syncStatus(),
                snapshot.healthStatus(),
                snapshot.operationPhase(),
                snapshot.reconciledAt()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String stringOrUnknown(Object value) {
        String result = value == null ? null : String.valueOf(value);
        return result == null || result.isBlank() ? "Unknown" : result;
    }
}
