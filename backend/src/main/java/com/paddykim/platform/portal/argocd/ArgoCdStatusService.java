package com.paddykim.platform.portal.argocd;

import com.paddykim.platform.portal.catalog.Application;
import com.paddykim.platform.portal.catalog.ApplicationEnvironment;
import com.paddykim.platform.portal.catalog.ApplicationNotFoundException;
import com.paddykim.platform.portal.catalog.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArgoCdStatusService {

    private final ApplicationRepository applicationRepository;
    private final ArgoCdApplicationStatusClient argoCdApplicationStatusClient;

    public ArgoCdStatusService(
            ApplicationRepository applicationRepository,
            ArgoCdApplicationStatusClient argoCdApplicationStatusClient
    ) {
        this.applicationRepository = applicationRepository;
        this.argoCdApplicationStatusClient = argoCdApplicationStatusClient;
    }

    @Transactional(readOnly = true)
    public ArgoCdStatusResponse getStatus(Long applicationId, String environmentName) {
        Application application = applicationRepository.findWithEnvironmentsById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        ApplicationEnvironment environment = application.getEnvironments().stream()
                .filter(candidate -> candidate.getEnvironment().equalsIgnoreCase(environmentName))
                .findFirst()
                .orElseThrow(() -> new EnvironmentNotFoundException(applicationId, environmentName));

        try {
            ArgoCdApplicationSnapshot snapshot = argoCdApplicationStatusClient.getApplicationStatus(
                    environment.getArgocdApplicationName()
            );
            return ArgoCdStatusResponse.available(
                    application.getId(),
                    application.getName(),
                    environment.getEnvironment(),
                    environment.getArgocdApplicationName(),
                    snapshot
            );
        } catch (ArgoCdStatusUnavailableException exception) {
            return ArgoCdStatusResponse.unavailable(
                    application.getId(),
                    application.getName(),
                    environment.getEnvironment(),
                    environment.getArgocdApplicationName(),
                    exception.getMessage()
            );
        }
    }
}
