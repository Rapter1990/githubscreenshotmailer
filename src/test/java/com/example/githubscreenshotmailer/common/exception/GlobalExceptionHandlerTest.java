package com.example.githubscreenshotmailer.common.exception;

import com.example.githubscreenshotmailer.base.AbstractBaseServiceTest;
import com.example.githubscreenshotmailer.common.model.CustomError;
import com.example.githubscreenshotmailer.screenshotmailer.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest extends AbstractBaseServiceTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleMethodArgumentNotValid_returnsBadRequest_withSubErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        FieldError fe = new FieldError("obj", "age", "must be >= 18");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fe));

        ResponseEntity<Object> resp = globalExceptionHandler.handleMethodArgumentNotValid(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Validation failed")
                .subErrors(List.of(CustomError.CustomSubError.builder()
                        .field("age")
                        .message("must be >= 18")
                        .build()))
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleConstraintViolation_returnsBadRequest_withMappedViolations() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> cv = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);

        when(cv.getMessage()).thenReturn("must be positive");
        when(cv.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("dto.amount");
        when(cv.getInvalidValue()).thenReturn(-5);

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(cv));

        ResponseEntity<Object> resp = globalExceptionHandler.handlePathVariableErrors(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Constraint violation")
                .subErrors(List.of(CustomError.CustomSubError.builder()
                        .message("must be positive")
                        .field("amount")
                        .value("-5")
                        .type("Integer")
                        .build()))
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleTypeMismatch_returnsBadRequest_withSubError() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "limit", null, null);

        ResponseEntity<Object> resp = globalExceptionHandler.handleTypeMismatch(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Invalid parameter type")
                .subErrors(List.of(CustomError.CustomSubError.builder()
                        .field("limit")
                        .message("Type mismatch")
                        .value("abc")
                        .type("Integer")
                        .build()))
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleNoHandlerFound_returnsNotFound() throws Exception {

        NoHandlerFoundException ex = new NoHandlerFoundException(
                "GET", "/missing", new HttpHeaders());

        ResponseEntity<Object> resp = globalExceptionHandler.handleNoHandler(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.NOT_FOUND)
                .header(CustomError.Header.NOT_FOUND.getName())
                .message("Endpoint not found: /missing")
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleApiException_usesProvidedStatusAndHeader() {
        // pick a representative ApiException subclass you already have
        GithubLoginException ex = new GithubLoginException("auth failed");

        ResponseEntity<Object> resp = globalExceptionHandler.handleApiException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(ex.getStatus());

        CustomError expected = CustomError.builder()
                .httpStatus(ex.getStatus())
                .header(ex.getHeader().getName())
                .message("GitHub login failed: auth failed")
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleEmailSendException_mapsToServiceUnavailable() {
        // Given: constructor requires (reason, cause) and prefixes message with "Email sending failed: "
        EmailSendException ex = new EmailSendException("SMTP down", new RuntimeException("io"));

        // When
        ResponseEntity<Object> resp = globalExceptionHandler.handleEmailSendException(ex);

        // Then
        assertThat(resp.getStatusCode()).isEqualTo(ex.getStatus());

        CustomError expected = CustomError.builder()
                .httpStatus(EmailSendException.STATUS) // or ex.getStatus()
                .header(EmailSendException.HEADER.getName()) // or ex.getHeader().getName()
                .message("Email sending failed: SMTP down")  // matches exception message format
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }


    @Test
    void handleGithubLoginException_mapsToUnauthorized() {
        GithubLoginException ex = new GithubLoginException("bad 2FA");

        ResponseEntity<Object> resp = globalExceptionHandler.handleGithubLoginException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(ex.getStatus());

        CustomError expected = CustomError.builder()
                .httpStatus(ex.getStatus())
                .header(ex.getHeader().getName())
                .message("GitHub login failed: bad 2FA")
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleResourceNotFoundException_mapsToNotFound() {
        // Given: constructor requires (what, id) and formats "<what> not found with id: <id>"
        ResourceNotFoundException ex = new ResourceNotFoundException("thing", "not-found");

        // When
        ResponseEntity<Object> resp = globalExceptionHandler.handleResourceNotFoundException(ex);

        // Then
        assertThat(resp.getStatusCode()).isEqualTo(ex.getStatus());

        CustomError expected = CustomError.builder()
                .httpStatus(ResourceNotFoundException.STATUS) // or ex.getStatus()
                .header(ResourceNotFoundException.HEADER.getName()) // or ex.getHeader().getName()
                .message("thing not found with id: not-found")
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleScreenshotCaptureException_mapsToServerError() {
        ScreenshotCaptureException ex = new ScreenshotCaptureException("boom", new RuntimeException("x"));

        ResponseEntity<Object> resp = globalExceptionHandler.handleScreenshotCaptureException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(ex.getStatus());

        CustomError expected = CustomError.builder()
                .httpStatus(ex.getStatus())
                .header(ex.getHeader().getName())
                .message("Screenshot capture failed: boom")
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleRuntimeException_isInternalServerErrorWithMessage() {
        RuntimeException ex = new RuntimeException("unexpected");

        ResponseEntity<Object> resp = globalExceptionHandler.handleRuntimeException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CustomError.Header.API_ERROR.getName())
                .message("unexpected")
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void handleApiException_with5xxStatus_logsError_andBuildsBody() {
        // Given: an ApiException that returns a 5xx status
        ApiException ex = new ApiException("server exploded") {
            @Override
            public HttpStatus getStatus() { return HttpStatus.INTERNAL_SERVER_ERROR; }

            @Override
            public CustomError.Header getHeader() { return CustomError.Header.API_ERROR; }

        };

        // When
        ResponseEntity<Object> resp = globalExceptionHandler.handleApiException(ex);

        // Then
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CustomError.Header.API_ERROR.getName())
                .message("server exploded")
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    @Test
    void givenObjectError_whenHandleMethodArgumentNotValid_thenUsesObjectNameAsField() {
        // Given: BindingResult returns an ObjectError (NOT a FieldError)
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ObjectError objectError = new ObjectError("myObjectName", "object-level validation failed");
        when(bindingResult.getAllErrors()).thenReturn(List.of(objectError));

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Validation failed")
                .subErrors(List.of(
                        CustomError.CustomSubError.builder()
                                // <- should use error.getObjectName() here
                                .field("myObjectName")
                                .message("object-level validation failed")
                                .build()
                ))
                .build();

        // When
        ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        checkCustomError(expected, (CustomError) response.getBody());

    }

    @Test
    void givenConstraintViolation_withNullInvalidValue_setsNullValueAndType() {
        // Given
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> cv = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);

        when(cv.getMessage()).thenReturn("must not be null");
        when(cv.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("dto.name"); // will be trimmed to "name"
        when(cv.getInvalidValue()).thenReturn(null);  // <- triggers value/type = null

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(cv));

        // When
        ResponseEntity<Object> resp = globalExceptionHandler.handlePathVariableErrors(ex);

        // Then
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Constraint violation")
                .subErrors(List.of(
                        CustomError.CustomSubError.builder()
                                .message("must not be null")
                                .field("name")
                                .value(null) // explicitly null
                                .type(null)  // explicitly null
                                .build()
                ))
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());

    }

    @Test
    void handleTypeMismatch_whenRequiredTypeIsNull_setsTypeNull() {
        // Given: requiredType is null
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", /* requiredType */ null, "limit", null, null);

        // When
        ResponseEntity<Object> resp = globalExceptionHandler.handleTypeMismatch(ex);

        // Then
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        CustomError expected = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Invalid parameter type")
                .subErrors(List.of(
                        CustomError.CustomSubError.builder()
                                .field("limit")
                                .message("Type mismatch")
                                .value("abc")  // value from the exception
                                .type(null)    // <- branch under test
                                .build()
                ))
                .build();

        checkCustomError(expected, (CustomError) resp.getBody());
    }

    private void checkCustomError(CustomError expectedError, CustomError actualError) {

        assertThat(actualError).isNotNull();
        assertThat(actualError.getTime()).isNotNull();
        assertThat(actualError.getHeader()).isEqualTo(expectedError.getHeader());
        assertThat(actualError.getIsSuccess()).isEqualTo(expectedError.getIsSuccess());

        if (expectedError.getMessage() != null) {
            assertThat(actualError.getMessage()).isEqualTo(expectedError.getMessage());
        }

        if (expectedError.getSubErrors() != null) {
            assertThat(actualError.getSubErrors().size()).isEqualTo(expectedError.getSubErrors().size());
            if (!expectedError.getSubErrors().isEmpty()) {
                assertThat(actualError.getSubErrors().getFirst().getMessage()).isEqualTo(expectedError.getSubErrors().get(0).getMessage());
                assertThat(actualError.getSubErrors().getFirst().getField()).isEqualTo(expectedError.getSubErrors().get(0).getField());
                assertThat(actualError.getSubErrors().getFirst().getValue()).isEqualTo(expectedError.getSubErrors().get(0).getValue());
                assertThat(actualError.getSubErrors().getFirst().getType()).isEqualTo(expectedError.getSubErrors().get(0).getType());
            }
        }

    }

}