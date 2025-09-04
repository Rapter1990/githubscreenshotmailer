package com.example.githubscreenshotmailer.screenshotmailer.exception;

import com.example.githubscreenshotmailer.common.model.CustomError;
import org.springframework.http.HttpStatus;

public class ScreenshotCaptureException extends ApiException {

    public static final HttpStatus STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public static final CustomError.Header HEADER = CustomError.Header.PROCESS_ERROR;

    public ScreenshotCaptureException(String reason, Throwable cause) {
        super("Screenshot capture failed: " + reason);
        initCause(cause);
    }

    @Override
    public HttpStatus getStatus() { return STATUS; }

    @Override
    public CustomError.Header getHeader() { return HEADER; }

}
