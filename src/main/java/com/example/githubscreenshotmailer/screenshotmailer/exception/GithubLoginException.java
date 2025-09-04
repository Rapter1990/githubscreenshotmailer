package com.example.githubscreenshotmailer.screenshotmailer.exception;

import com.example.githubscreenshotmailer.common.model.CustomError;
import org.springframework.http.HttpStatus;

public class GithubLoginException extends ApiException {

    public static final HttpStatus STATUS = HttpStatus.UNAUTHORIZED;

    public static final CustomError.Header HEADER = CustomError.Header.PROCESS_ERROR;

    public GithubLoginException(String reason) {
        super("GitHub login failed: " + reason);
    }

    @Override
    public HttpStatus getStatus() { return STATUS; }

    @Override
    public CustomError.Header getHeader() { return HEADER; }

}
