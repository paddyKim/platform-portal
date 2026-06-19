package com.paddykim.platform.portal.runtime;

import com.paddykim.platform.portal.argocd.EnvironmentNotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{applicationId}/environments/{environment}/runtime")
public class RuntimeStatusController {

    private final RuntimeStatusService runtimeStatusService;

    public RuntimeStatusController(RuntimeStatusService runtimeStatusService) {
        this.runtimeStatusService = runtimeStatusService;
    }

    @GetMapping
    public RuntimeStatusResponse getRuntime(
            @PathVariable Long applicationId,
            @PathVariable String environment
    ) {
        return runtimeStatusService.getRuntime(applicationId, environment);
    }

    @ExceptionHandler(EnvironmentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleEnvironmentNotFound(EnvironmentNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }
}
