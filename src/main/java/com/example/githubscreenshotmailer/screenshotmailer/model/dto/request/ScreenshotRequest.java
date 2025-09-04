package com.example.githubscreenshotmailer.screenshotmailer.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ScreenshotRequest(
        @NotBlank String githubUsername,
        @Email @NotBlank String recipientEmail,
        boolean withLogin
) {}
