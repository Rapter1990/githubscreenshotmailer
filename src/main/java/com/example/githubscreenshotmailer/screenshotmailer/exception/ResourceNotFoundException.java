package com.example.githubscreenshotmailer.screenshotmailer.exception;

import com.example.githubscreenshotmailer.common.model.CustomError;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public static final HttpStatus STATUS = HttpStatus.NOT_FOUND; // 404
    public static final CustomError.Header HEADER = CustomError.Header.NOT_FOUND;

    public ResourceNotFoundException(String what, String id) {
        super("%s not found with id: %s".formatted(what, id));
    }

    @Override public HttpStatus getStatus() { return STATUS; }

    @Override public CustomError.Header getHeader() { return HEADER; }

}
