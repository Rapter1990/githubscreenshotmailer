package com.example.githubscreenshotmailer.common.exception;

import com.example.githubscreenshotmailer.common.model.CustomError;
import com.example.githubscreenshotmailer.screenshotmailer.exception.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- Validation: @Valid on request bodies ----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex) {
        List<CustomError.CustomSubError> subErrors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            String message = error.getDefaultMessage();
            subErrors.add(CustomError.CustomSubError.builder()
                    .field(fieldName)
                    .message(message)
                    .build());
        });

        return buildError(
                HttpStatus.BAD_REQUEST,
                CustomError.Header.VALIDATION_ERROR,
                "Validation failed",
                subErrors
        );
    }

    // ---------- Validation: @Validated on query/path params ----------
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handlePathVariableErrors(final ConstraintViolationException ex) {
        List<CustomError.CustomSubError> subErrors = new ArrayList<>();
        ex.getConstraintViolations().forEach(cv -> {
            Object invalid = cv.getInvalidValue();
            subErrors.add(CustomError.CustomSubError.builder()
                    .message(cv.getMessage())
                    .field(StringUtils.substringAfterLast(cv.getPropertyPath().toString(), "."))
                    .value(invalid != null ? invalid.toString() : null)
                    .type(invalid != null ? invalid.getClass().getSimpleName() : null)
                    .build());
        });

        return buildError(
                HttpStatus.BAD_REQUEST,
                CustomError.Header.VALIDATION_ERROR,
                "Constraint violation",
                subErrors
        );
    }

    // ---------- Type mismatch for query/path variables ----------
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        CustomError.CustomSubError sub = CustomError.CustomSubError.builder()
                .field(ex.getName())
                .message("Type mismatch")
                .value(ex.getValue())
                .type(ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : null)
                .build();

        return buildError(
                HttpStatus.BAD_REQUEST,
                CustomError.Header.VALIDATION_ERROR,
                "Invalid parameter type",
                List.of(sub)
        );
    }

    // ---------- 404 for unknown routes (if spring.mvc.throw-exception-if-no-handler-found=true) ----------
    @ExceptionHandler(NoHandlerFoundException.class)
    protected ResponseEntity<Object> handleNoHandler(final NoHandlerFoundException ex) {
        return buildError(
                HttpStatus.NOT_FOUND,
                CustomError.Header.NOT_FOUND,
                "Endpoint not found: " + ex.getRequestURL(),
                null
        );
    }

    // ---------- Your domain exceptions (all with STATUS & HEADER) ----------
    @ExceptionHandler(ApiException.class)
    protected ResponseEntity<Object> handleApiException(final ApiException ex) {
        // Server-side visibility
        if (ex.getStatus().is5xxServerError()) {
            log.error("API exception: {}", ex.getMessage(), ex);
        } else {
            log.warn("API exception: {}", ex.getMessage());
        }
        return buildError(ex.getStatus(), ex.getHeader(), ex.getMessage(), null);
    }

    // ---------- Catch-all ----------
    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleRuntimeException(final RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                CustomError.Header.API_ERROR,
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(EmailSendException.class)
    protected ResponseEntity<Object> handleEmailSendException(final EmailSendException ex) {
        // 503 — usually a server-side issue (SMTP down, etc.)
        log.error("Email send failure: {}", ex.getMessage(), ex);
        return buildError(ex.getStatus(), ex.getHeader(), ex.getMessage(), null);
    }

    @ExceptionHandler(GithubLoginException.class)
    protected ResponseEntity<Object> handleGithubLoginException(final GithubLoginException ex) {
        // 401 — auth problem; keep logs at WARN unless you need stacktraces
        log.warn("GitHub login error: {}", ex.getMessage());
        return buildError(ex.getStatus(), ex.getHeader(), ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFoundException(final ResourceNotFoundException ex) {
        // 404 — not found; WARN is fine
        log.warn("Resource not found: {}", ex.getMessage());
        return buildError(ex.getStatus(), ex.getHeader(), ex.getMessage(), null);
    }

    @ExceptionHandler(ScreenshotCaptureException.class)
    protected ResponseEntity<Object> handleScreenshotCaptureException(final ScreenshotCaptureException ex) {
        // 500 — capture pipeline failed
        log.error("Screenshot capture error: {}", ex.getMessage(), ex);
        return buildError(ex.getStatus(), ex.getHeader(), ex.getMessage(), null);
    }

    // ---------- Builder ----------
    private ResponseEntity<Object> buildError(HttpStatus status,
                                              CustomError.Header header,
                                              String message,
                                              List<CustomError.CustomSubError> subErrors) {

        CustomError body = CustomError.builder()
                .time(LocalDateTime.now())
                .httpStatus(status)
                .header(header.getName())
                .message(message)
                .subErrors(subErrors)
                .build();

        return new ResponseEntity<>(body, status);
    }

}
