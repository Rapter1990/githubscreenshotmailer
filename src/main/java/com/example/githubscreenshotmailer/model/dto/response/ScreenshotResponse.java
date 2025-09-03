package com.example.githubscreenshotmailer.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScreenshotResponse(
        String imageId,
        String githubUsername,
        String recipientEmail,
        String fileName,
        String path,
        long fileSize,
        LocalDateTime sentAt,
        String status
) {}
