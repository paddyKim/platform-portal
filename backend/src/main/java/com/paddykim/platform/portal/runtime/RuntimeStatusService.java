package com.paddykim.platform.portal.runtime;

import com.paddykim.platform.portal.argocd.EnvironmentNotFoundException;
import com.paddykim.platform.portal.catalog.Application;
import com.paddykim.platform.portal.catalog.ApplicationEnvironment;
import com.paddykim.platform.portal.catalog.ApplicationNotFoundException;
import com.paddykim.platform.portal.catalog.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeStatusService {

    private final ApplicationRepository applicationRepository;
    private final KubernetesRuntimeClient kubernetesRuntimeClient;

    public RuntimeStatusService(
            ApplicationRepository applicationRepository,
            KubernetesRuntimeClient kubernetesRuntimeClient
    ) {
        this.applicationRepository = applicationRepository;
        this.kubernetesRuntimeClient = kubernetesRuntimeClient;
    }

    @Transactional(readOnly = true)
    public RuntimeStatusResponse getRuntime(Long applicationId, String environmentName) {
        Application application = applicationRepository.findWithEnvironmentsById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        ApplicationEnvironment environment = application.getEnvironments().stream()
                .filter(candidate -> candidate.getEnvironment().equalsIgnoreCase(environmentName))
                .findFirst()
                .orElseThrow(() -> new EnvironmentNotFoundException(applicationId, environmentName));

        try {
            KubernetesRuntimeSnapshot snapshot = kubernetesRuntimeClient.getRuntime(
                    environment.getNamespace(),
                    environment.getComponents()
            );
            return RuntimeStatusResponse.available(
                    application.getId(),
                    application.getName(),
                    environment.getEnvironment(),
                    environment.getNamespace(),
                    snapshot
            );
        } catch (KubernetesRuntimeUnavailableException exception) {
            return RuntimeStatusResponse.unavailable(
                    application.getId(),
                    application.getName(),
                    environment.getEnvironment(),
                    environment.getNamespace(),
                    exception.getMessage()
            );
        }
    }
}
