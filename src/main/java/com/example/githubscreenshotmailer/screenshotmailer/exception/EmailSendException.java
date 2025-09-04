package com.example.githubscreenshotmailer.screenshotmailer.exception;

import com.example.githubscreenshotmailer.common.model.CustomError;
import org.springframework.http.HttpStatus;

public class EmailSendException extends ApiException {

    public static final HttpStatus STATUS = HttpStatus.SERVICE_UNAVAILABLE; // 503
    public static final CustomError.Header HEADER = CustomError.Header.API_ERROR;

    public EmailSendException(String reason, Throwable cause) {
        super("Email sending failed: " + reason);
        initCause(cause);
    }

    public EmailSendException(String reason) {
        super("Email sending failed: " + reason);
    }

    @Override public HttpStatus getStatus() { return STATUS; }

    @Override public CustomError.Header getHeader() { return HEADER; }

}
