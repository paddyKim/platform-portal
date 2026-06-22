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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/source-repositories")
public class SourceRepositoryController {

    private final SourceRepositoryService sourceRepositoryService;

    public SourceRepositoryController(SourceRepositoryService sourceRepositoryService) {
        this.sourceRepositoryService = sourceRepositoryService;
    }

    @GetMapping
    public List<SourceRepositoryResponse> listRepositories() {
        return sourceRepositoryService.listRepositories();
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

    @ExceptionHandler(SourceRepositoryValidationException.class)
    ResponseEntity<ErrorResponse> handleValidation(SourceRepositoryValidationException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SourceRepositoryNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(SourceRepositoryNotFoundException exception) {
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
}
