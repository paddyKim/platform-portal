package com.paddykim.platform.portal.command;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/application-commands")
public class ApplicationCommandController {

    private final ApplicationCommandService applicationCommandService;

    public ApplicationCommandController(ApplicationCommandService applicationCommandService) {
        this.applicationCommandService = applicationCommandService;
    }

    @PostMapping("/interpret")
    public ApplicationCommandResponse interpret(@Valid @RequestBody ApplicationCommandRequest request) {
        return applicationCommandService.interpret(request.command());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationError(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("Invalid command request");

        return new ErrorResponse(message);
    }

    public record ErrorResponse(String message) {
    }
}
