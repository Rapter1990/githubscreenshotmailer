package com.example.githubscreenshotmailer.model;

import com.example.githubscreenshotmailer.model.enums.ScreenshotStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
