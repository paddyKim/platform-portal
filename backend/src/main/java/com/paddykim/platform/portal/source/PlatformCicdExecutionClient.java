package com.paddykim.platform.portal.source;

public interface PlatformCicdExecutionClient {

    PlatformCicdExecutionResponse createExecution(PlatformCicdExecutionCreateRequest request);
}
