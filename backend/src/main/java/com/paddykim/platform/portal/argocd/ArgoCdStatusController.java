package com.paddykim.platform.portal.argocd;

import com.paddykim.platform.portal.catalog.ApplicationNotFoundException;
import com.paddykim.platform.portal.catalog.CatalogController.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications/{applicationId}/environments/{environment}/status")
public class ArgoCdStatusController {

    private final ArgoCdStatusService argoCdStatusService;

    public ArgoCdStatusController(ArgoCdStatusService argoCdStatusService) {
        this.argoCdStatusService = argoCdStatusService;
    }

    @GetMapping
    public ArgoCdStatusResponse getStatus(
            @PathVariable Long applicationId,
            @PathVariable String environment
    ) {
        return argoCdStatusService.getStatus(applicationId, environment);
    }

    @ExceptionHandler({ApplicationNotFoundException.class, EnvironmentNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage());
    }
}
