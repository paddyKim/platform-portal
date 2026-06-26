package com.paddykim.platform.portal.argocd;

import java.util.List;

public interface ArgoCdApplicationCatalogClient {

    List<ArgoCdApplicationSummary> listApplications();

    ArgoCdApplicationSummary createApplication(ArgoCdApplicationCreateRequest request);
}
