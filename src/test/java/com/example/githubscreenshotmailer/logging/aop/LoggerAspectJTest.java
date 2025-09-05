package com.example.githubscreenshotmailer.logging.aop;

import com.example.githubscreenshotmailer.base.AbstractBaseServiceTest;

import com.example.githubscreenshotmailer.logging.model.entity.LogEntity;
import com.example.githubscreenshotmailer.logging.service.LogService;
import com.example.githubscreenshotmailer.screenshotmailer.exception.EmailSendException;
import com.example.githubscreenshotmailer.screenshotmailer.exception.GithubLoginException;
import com.example.githubscreenshotmailer.screenshotmailer.exception.ResourceNotFoundException;
import com.example.githubscreenshotmailer.screenshotmailer.exception.ScreenshotCaptureException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ch.qos.logback.classic.Level;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoggerAspectJTest extends AbstractBaseServiceTest {

    @InjectMocks
    private LoggerAspectJ loggerAspectJ;

    @Mock
    private LogService logService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    public void setUp() {
        // Initialize mocks and set request attributes
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(servletRequestAttributes.getResponse()).thenReturn(httpServletResponse);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);

        // Mock JoinPoint signature
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(signature.getDeclaringTypeName()).thenReturn("LoggerAspectJ");
        when(signature.getDeclaringType()).thenReturn(LoggerAspectJ.class);
    }

    @Test
    void logAfterThrowing_buildsCorrectLogEntity_andSaves() {

        // Given: a domain ApiException (maps to its own status via getStatus())
        GithubLoginException ex = new GithubLoginException("invalid credentials");

        // Request/JoinPoint context
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/github"));
        when(httpServletRequest.getMethod()).thenReturn("PATCH");
        when(signature.getName()).thenReturn("loginAction");

        // When
        loggerAspectJ.logAfterThrowing(joinPoint, ex);

        // Then: a LogEntity is persisted with values derived by the aspect
        verify(logService).saveLogToDatabase(argThat(log ->
                log.getEndpoint().equals("http://localhost/api/github") &&
                        log.getMethod().equals("PATCH") &&
                        log.getOperation().equals("loginAction") &&
                        log.getMessage().equals(ex.getMessage()) &&
                        log.getResponse().equals(ex.getMessage()) &&
                        log.getErrorType().equals(GithubLoginException.class.getName()) &&
                        // status is derived via getHttpStatusFromException(ex) -> ex.getStatus().name()
                        log.getStatus() == ex.getStatus()
        ));

    }

    @Test
    public void testLogAfterReturning() throws IOException {

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletResponse.getStatus()).thenReturn(HttpStatus.OK.value());
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getSignature()).thenReturn(signature);

        // Then
        loggerAspectJ.logAfterReturning(joinPoint, "test response");

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterReturning_WithJsonNode() throws IOException {

        // Given
        JsonNode jsonNode = new ObjectMapper().createObjectNode().put("key", "value");

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletResponse.getStatus()).thenReturn(HttpStatus.OK.value());

        // Then
        loggerAspectJ.logAfterReturning(joinPoint, jsonNode);

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterReturning_NoResponseAttributes() throws IOException {

        // Given
        RequestContextHolder.resetRequestAttributes();

        // When
        loggerAspectJ.logAfterReturning(mock(JoinPoint.class), "test response");

        // Then
        verify(logService, never()).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterReturning_SaveLogThrowsException() throws IOException {

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletResponse.getStatus()).thenReturn(HttpStatus.OK.value());
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        doThrow(new RuntimeException("Database error")).when(logService).saveLogToDatabase(any(LogEntity.class));

        // Then
        assertDoesNotThrow(() -> loggerAspectJ.logAfterReturning(joinPoint, "test response"));

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    void testLogAfterThrowing_whenRequestAttributesAreNull_thenLogError() {

        // Given
        RequestContextHolder.resetRequestAttributes();
        Exception ex = new RuntimeException("Some error");

        // When
        loggerAspectJ.logAfterThrowing(joinPoint, ex);

        // Then
        Optional<String> logMessage = logTracker.checkMessage(Level.ERROR, "logAfterThrowing | Request Attributes are null!");
        assertTrue(logMessage.isPresent(), "Expected error log message not found.");
        assertEquals(logMessage.get(), "logAfterThrowing | Request Attributes are null!");

    }

    @Test
    void testGetHttpStatusFromException_AllCases() {
        // Given
        BindingResult dummyBinding = mock(BindingResult.class);

        // Cases according to LoggerAspectJ#getHttpStatusFromException:
        // - ApiException -> ex.getStatus().name()
        // - MethodArgumentNotValidException, ConstraintViolationException -> BAD_REQUEST
        // - Default -> INTERNAL_SERVER_ERROR
        var cases = Map.ofEntries(
                Map.entry(new MethodArgumentNotValidException(null, dummyBinding),
                        HttpStatus.BAD_REQUEST.name()),
                Map.entry(new ConstraintViolationException(Collections.emptySet()),
                        HttpStatus.BAD_REQUEST.name()),
                Map.entry(new GithubLoginException("bad auth"),
                        new GithubLoginException("x").getStatus().name()),
                Map.entry(new EmailSendException("SMTP down", new RuntimeException()),
                        new EmailSendException("r", null).getStatus().name()),
                Map.entry(new ResourceNotFoundException("Thing", "42"),
                        new ResourceNotFoundException("X", "Y").getStatus().name()),
                Map.entry(new ScreenshotCaptureException("boom", new RuntimeException()),
                        new ScreenshotCaptureException("x", null).getStatus().name()),
                Map.entry(new RuntimeException("oops"),
                        HttpStatus.INTERNAL_SERVER_ERROR.name()),
                Map.entry(new Exception("unknown"),
                        HttpStatus.INTERNAL_SERVER_ERROR.name())
        );

        // When / Then
        cases.forEach((exc, expected) -> {
            String actual = (String) org.springframework.test.util.ReflectionTestUtils
                    .invokeMethod(loggerAspectJ, "getHttpStatusFromException", exc);
            assertEquals(expected, actual, "Mismatch for: " + exc.getClass().getSimpleName());
        });
    }


}