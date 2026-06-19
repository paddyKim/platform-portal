package com.paddykim.platform.portal.argocd;

public interface ArgoCdApplicationStatusClient {

    ArgoCdApplicationSnapshot getApplicationStatus(String applicationName);
}
