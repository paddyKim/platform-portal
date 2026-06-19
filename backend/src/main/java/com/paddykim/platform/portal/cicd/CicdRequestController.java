package com.paddykim.platform.portal.cicd;

import com.paddykim.platform.portal.catalog.ApplicationNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/cicd/requests")
public class CicdRequestController {

    private final CicdRequestService cicdRequestService;

    public CicdRequestController(CicdRequestService cicdRequestService) {
        this.cicdRequestService = cicdRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CicdRequestResponse createRequest(@Valid @RequestBody CicdRequestCreateRequest request) {
        return cicdRequestService.createRequest(request);
    }

    @GetMapping
    public List<CicdRequestResponse> listRequests() {
        return cicdRequestService.listRequests();
    }

    @GetMapping("/{id}")
    public CicdRequestResponse getRequest(@PathVariable Long id) {
        return cicdRequestService.getRequest(id);
    }

    @ExceptionHandler(CicdRequestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleRequestNotFound(CicdRequestNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleApplicationNotFound(ApplicationNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler({CicdRequestValidationException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(Exception exception) {
        return Map.of("message", exception.getMessage());
    }
}
