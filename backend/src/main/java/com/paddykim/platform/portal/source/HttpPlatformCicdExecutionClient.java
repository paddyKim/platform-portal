package com.paddykim.platform.portal.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class HttpPlatformCicdExecutionClient implements PlatformCicdExecutionClient {

    private final RestClient restClient;

    public HttpPlatformCicdExecutionClient(@Value("${portal.cicd.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public PlatformCicdExecutionResponse createExecution(PlatformCicdExecutionCreateRequest request) {
        try {
            return restClient.post()
                    .uri("/api/cicd/executions")
                    .body(request)
                    .retrieve()
                    .body(PlatformCicdExecutionResponse.class);
        } catch (RestClientException exception) {
            throw new SourceRepositoryValidationException(
                    "Unable to dispatch build profile run to platform-cicd: " + exception.getMessage()
            );
        }
    }
}
