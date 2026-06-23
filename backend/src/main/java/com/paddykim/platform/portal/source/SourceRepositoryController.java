package com.paddykim.platform.portal.source;

import com.paddykim.platform.portal.catalog.CatalogController.ErrorResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/source-repositories")
public class SourceRepositoryController {

    private final SourceRepositoryService sourceRepositoryService;
    private final BuildProfileService buildProfileService;
    private final SourceRepositoryCredentialService credentialService;

    public SourceRepositoryController(
            SourceRepositoryService sourceRepositoryService,
            BuildProfileService buildProfileService,
            SourceRepositoryCredentialService credentialService
    ) {
        this.sourceRepositoryService = sourceRepositoryService;
        this.buildProfileService = buildProfileService;
        this.credentialService = credentialService;
    }

    @GetMapping
    public List<SourceRepositoryResponse> listRepositories() {
        return sourceRepositoryService.listRepositories();
    }

    @GetMapping("/{id}")
    public SourceRepositoryResponse getRepository(@PathVariable Long id) {
        return sourceRepositoryService.getRepository(id);
    }

    @GetMapping("/credential-public-key")
    public CredentialPublicKeyResponse credentialPublicKey() {
        return new CredentialPublicKeyResponse(credentialService.networkPublicKey());
    }

    @PostMapping
    public ResponseEntity<SourceRepositoryResponse> createRepository(
            @Valid @RequestBody SourceRepositoryCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sourceRepositoryService.createRepository(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable Long id) {
        sourceRepositoryService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{repositoryId}/build-profiles")
    public List<BuildProfileResponse> listBuildProfiles(@PathVariable Long repositoryId) {
        return buildProfileService.listBuildProfiles(repositoryId);
    }

    @PostMapping("/{repositoryId}/build-profiles")
    public ResponseEntity<BuildProfileResponse> createBuildProfile(
            @PathVariable Long repositoryId,
            @Valid @RequestBody BuildProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildProfileService.createBuildProfile(repositoryId, request));
    }

    @GetMapping("/{repositoryId}/build-profiles/{profileId}")
    public BuildProfileResponse getBuildProfile(
            @PathVariable Long repositoryId,
            @PathVariable Long profileId
    ) {
        return buildProfileService.getBuildProfile(repositoryId, profileId);
    }

    @PutMapping("/{repositoryId}/build-profiles/{profileId}")
    public BuildProfileResponse updateBuildProfile(
            @PathVariable Long repositoryId,
            @PathVariable Long profileId,
            @Valid @RequestBody BuildProfileRequest request
    ) {
        return buildProfileService.updateBuildProfile(repositoryId, profileId, request);
    }

    @DeleteMapping("/{repositoryId}/build-profiles/{profileId}")
    public ResponseEntity<Void> deleteBuildProfile(
            @PathVariable Long repositoryId,
            @PathVariable Long profileId
    ) {
        buildProfileService.deleteBuildProfile(repositoryId, profileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{repositoryId}/build-profiles/{profileId}/run")
    public BuildProfileRunResponse runBuildProfile(
            @PathVariable Long repositoryId,
            @PathVariable Long profileId,
            @Valid @RequestBody BuildProfileRunRequest request
    ) {
        return buildProfileService.prepareBuildProfileRun(repositoryId, profileId, request);
    }

    @ExceptionHandler(SourceRepositoryValidationException.class)
    ResponseEntity<ErrorResponse> handleValidation(SourceRepositoryValidationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SourceRepositoryNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(SourceRepositoryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(BuildProfileNotFoundException.class)
    ResponseEntity<ErrorResponse> handleBuildProfileNotFound(BuildProfileNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("Invalid source repository request");

        return ResponseEntity.badRequest()
                .body(Map.of("message", message));
    }

    public record CredentialPublicKeyResponse(String publicKey) {
    }
}
