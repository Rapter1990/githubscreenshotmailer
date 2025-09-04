package com.example.githubscreenshotmailer.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard API error payload used by GlobalExceptionHandler.
 */
@Getter
@Builder
public class CustomError {

    /** Server time when the error was created. */
    @Builder.Default
    private LocalDateTime time = LocalDateTime.now();

    /** HTTP status to send to the client. */
    private HttpStatus httpStatus;

    /** High-level error header/category (e.g., VALIDATION ERROR, NOT FOUND). */
    private String header;

    /** Human-readable message (nullable). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    /** Always false for error responses. */
    @Builder.Default
    private final Boolean isSuccess = false;

    /** Optional list of detailed sub-errors (e.g., validation problems). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CustomSubError> subErrors;

    /**
     * Represents a sub-error, usually used for validation problems.
     */
    @Getter
    @Builder
    public static class CustomSubError {

        /** Description of the sub-error. */
        private String message;

        /** Field/parameter name related to the error (if applicable). */
        private String field;

        /** Offending value (nullable). */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object value;

        /** Type of the offending value (nullable). */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String type;
    }

    /**
     * Enumeration of predefined error categories for consistent API responses.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Header {

        API_ERROR("API ERROR"),
        ALREADY_EXIST("ALREADY EXIST"),
        NOT_FOUND("NOT EXIST"),
        VALIDATION_ERROR("VALIDATION ERROR"),
        DATABASE_ERROR("DATABASE ERROR"),
        PROCESS_ERROR("PROCESS ERROR");

        private final String name;
    }
}
