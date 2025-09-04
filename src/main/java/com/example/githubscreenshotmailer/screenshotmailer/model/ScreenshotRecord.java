package com.example.githubscreenshotmailer.screenshotmailer.model;

import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;

import java.time.LocalDateTime;

public record ScreenshotRecord(
        String imageId,
        String githubUsername,
        String recipientEmail,
        String fileName,
        String path,
        long fileSize,
        LocalDateTime sentAt,
        ScreenshotStatus status
) {}
