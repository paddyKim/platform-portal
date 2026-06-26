package com.paddykim.platform.portal.catalog;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<CatalogApplicationResponse> listApplications() {
        return catalogService.listApplications();
    }

    @GetMapping("/{id}")
    public CatalogApplicationResponse getApplication(@PathVariable Long id) {
        return catalogService.getApplication(id);
    }

    @PostMapping
    public ResponseEntity<CatalogApplicationResponse> createApplication(
            @Valid @RequestBody ApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createApplication(request));
    }

    @PutMapping("/{applicationId}")
    public CatalogApplicationResponse updateApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return catalogService.updateApplication(applicationId, request);
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long applicationId) {
        catalogService.deleteApplication(applicationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{applicationId}/environments")
    public ResponseEntity<CatalogApplicationResponse> createEnvironment(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationEnvironmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createEnvironment(applicationId, request));
    }

    @PutMapping("/{applicationId}/environments/{environmentId}")
    public CatalogApplicationResponse updateEnvironment(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId,
            @Valid @RequestBody ApplicationEnvironmentRequest request
    ) {
        return catalogService.updateEnvironment(applicationId, environmentId, request);
    }

    @DeleteMapping("/{applicationId}/environments/{environmentId}")
    public CatalogApplicationResponse deleteEnvironment(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId
    ) {
        return catalogService.deleteEnvironment(applicationId, environmentId);
    }

    @PostMapping("/{applicationId}/environments/{environmentId}/components")
    public ResponseEntity<CatalogApplicationResponse> createComponent(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId,
            @Valid @RequestBody ApplicationComponentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createComponent(applicationId, environmentId, request));
    }

    @PutMapping("/{applicationId}/environments/{environmentId}/components/{componentId}")
    public CatalogApplicationResponse updateComponent(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId,
            @PathVariable Long componentId,
            @Valid @RequestBody ApplicationComponentRequest request
    ) {
        return catalogService.updateComponent(applicationId, environmentId, componentId, request);
    }

    @DeleteMapping("/{applicationId}/environments/{environmentId}/components/{componentId}")
    public CatalogApplicationResponse deleteComponent(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId,
            @PathVariable Long componentId
    ) {
        return catalogService.deleteComponent(applicationId, environmentId, componentId);
    }

    @PostMapping("/{applicationId}/environments/{environmentId}/components/{componentId}/manifest-mapping")
    public ResponseEntity<CatalogApplicationResponse> createManifestMapping(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId,
            @PathVariable Long componentId,
            @Valid @RequestBody ApplicationManifestMappingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.upsertManifestMapping(applicationId, environmentId, componentId, request));
    }

    @PutMapping("/{applicationId}/environments/{environmentId}/components/{componentId}/manifest-mapping")
    public CatalogApplicationResponse updateManifestMapping(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId,
            @PathVariable Long componentId,
            @Valid @RequestBody ApplicationManifestMappingRequest request
    ) {
        return catalogService.upsertManifestMapping(applicationId, environmentId, componentId, request);
    }

    @DeleteMapping("/{applicationId}/environments/{environmentId}/components/{componentId}/manifest-mapping")
    public CatalogApplicationResponse deleteManifestMapping(
            @PathVariable Long applicationId,
            @PathVariable Long environmentId,
            @PathVariable Long componentId
    ) {
        return catalogService.deleteManifestMapping(applicationId, environmentId, componentId);
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleApplicationNotFound(ApplicationNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(CatalogResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCatalogResourceNotFound(CatalogResourceNotFoundException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(CatalogValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCatalogValidation(CatalogValidationException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("Invalid application request");

        return new ErrorResponse(message);
    }

    public record ErrorResponse(String message) {
    }
}
