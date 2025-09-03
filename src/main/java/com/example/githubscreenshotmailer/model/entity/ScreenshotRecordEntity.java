package com.example.githubscreenshotmailer.model.entity;

import com.example.githubscreenshotmailer.model.enums.ScreenshotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "screenshot_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenshotRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;

    @Column(nullable = false)
    private String githubUsername;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private long fileSizeBytes;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScreenshotStatus status;

    @Column(length = 2000)
    private String errorMessage;


}
