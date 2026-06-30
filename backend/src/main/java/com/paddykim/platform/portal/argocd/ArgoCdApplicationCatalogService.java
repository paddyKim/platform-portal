package com.paddykim.platform.portal.argocd;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ArgoCdApplicationCatalogService {

    private final ArgoCdApplicationCatalogClient argoCdApplicationCatalogClient;

    public ArgoCdApplicationCatalogService(ArgoCdApplicationCatalogClient argoCdApplicationCatalogClient) {
        this.argoCdApplicationCatalogClient = argoCdApplicationCatalogClient;
    }

    public List<ArgoCdApplicationSummary> listApplications() {
        return argoCdApplicationCatalogClient.listApplications();
    }

    public ArgoCdApplicationSummary createApplication(ArgoCdApplicationCreateRequest request) {
        return argoCdApplicationCatalogClient.createApplication(request);
    }

    public ArgoCdApplicationDetail getApplication(String applicationName) {
        return argoCdApplicationCatalogClient.getApplication(applicationName);
    }

    public ArgoCdApplicationDetail syncApplication(
            String applicationName,
            ArgoCdApplicationSyncRequest request
    ) {
        return argoCdApplicationCatalogClient.syncApplication(applicationName, request);
    }
}
