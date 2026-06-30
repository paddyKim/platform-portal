package com.paddykim.platform.portal.argocd;

import com.paddykim.platform.portal.catalog.CatalogController.ErrorResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/argocd/applications")
public class ArgoCdApplicationCatalogController {

    private final ArgoCdApplicationCatalogService argoCdApplicationCatalogService;

    public ArgoCdApplicationCatalogController(ArgoCdApplicationCatalogService argoCdApplicationCatalogService) {
        this.argoCdApplicationCatalogService = argoCdApplicationCatalogService;
    }

    @GetMapping
    public List<ArgoCdApplicationSummary> listApplications() {
        return argoCdApplicationCatalogService.listApplications();
    }

    @GetMapping("/{applicationName}")
    public ArgoCdApplicationDetail getApplication(@PathVariable String applicationName) {
        return argoCdApplicationCatalogService.getApplication(applicationName);
    }

    @PostMapping
    public ResponseEntity<ArgoCdApplicationSummary> createApplication(
            @Valid @RequestBody ArgoCdApplicationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(argoCdApplicationCatalogService.createApplication(request));
    }

    @PostMapping("/{applicationName}/sync")
    public ArgoCdApplicationDetail syncApplication(
            @PathVariable String applicationName,
            @Valid @RequestBody ArgoCdApplicationSyncRequest request
    ) {
        return argoCdApplicationCatalogService.syncApplication(applicationName, request);
    }

    @ExceptionHandler(ArgoCdApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleApplicationNotFound(ArgoCdApplicationNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(ArgoCdStatusUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleArgoCdUnavailable(ArgoCdStatusUnavailableException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("Invalid ArgoCD application request");

        return new ErrorResponse(message);
    }
}
