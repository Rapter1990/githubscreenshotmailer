package com.example.githubscreenshotmailer.exception;

import org.springframework.http.HttpStatus;

public class ScreenshotCaptureException extends ApiException {

    public static final HttpStatus STATUS = HttpStatus.INTERNAL_SERVER_ERROR; // 500
    public static final CustomError.Header HEADER = CustomError.Header.PROCESS_ERROR;

    public ScreenshotCaptureException(String reason, Throwable cause) {
        super("Screenshot capture failed: " + reason);
        initCause(cause);
    }

    public ScreenshotCaptureException(String reason) {
        super("Screenshot capture failed: " + reason);
    }

    @Override public HttpStatus getStatus() { return STATUS; }

    @Override public CustomError.Header getHeader() { return HEADER; }

}
