package com.paddykim.platform.portal.argocd;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Fabric8ArgoCdApplicationStatusClient implements ArgoCdApplicationStatusClient {

    private static final Logger log = LoggerFactory.getLogger(Fabric8ArgoCdApplicationStatusClient.class);

    private final String namespace;

    public Fabric8ArgoCdApplicationStatusClient(
            @Value("${platform.argocd.namespace:argocd}") String namespace
    ) {
        this.namespace = namespace;
    }

    @Override
    public ArgoCdApplicationSnapshot getApplicationStatus(String applicationName) {
        Config config = Config.autoConfigure(null);
        config.setConnectionTimeout(2_000);
        config.setRequestTimeout(3_000);
        config.setRequestRetryBackoffLimit(0);

        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build()) {
            ResourceDefinitionContext applicationContext = new ResourceDefinitionContext.Builder()
                    .withGroup("argoproj.io")
                    .withVersion("v1alpha1")
                    .withPlural("applications")
                    .withNamespaced(true)
                    .build();

            GenericKubernetesResource application = client.genericKubernetesResources(applicationContext)
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
}
