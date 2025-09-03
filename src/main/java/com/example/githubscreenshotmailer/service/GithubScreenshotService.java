package com.example.githubscreenshotmailer.service;

import com.example.githubscreenshotmailer.config.GithubAutomationProperties;
import com.example.githubscreenshotmailer.exception.ApiException;
import com.example.githubscreenshotmailer.exception.ScreenshotCaptureException;
import com.example.githubscreenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.model.dto.request.ScreenshotRequest;
import com.example.githubscreenshotmailer.model.entity.ScreenshotRecordEntity;
import com.example.githubscreenshotmailer.model.enums.ScreenshotStatus;
import com.example.githubscreenshotmailer.model.mapper.ScreenshotRecordEntityToScreenshotRecordMapper;
import com.example.githubscreenshotmailer.repository.ScreenshotRecordRepository;
import com.example.githubscreenshotmailer.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubScreenshotService {

    private final SeleniumAutomationService seleniumAutomationService;
    private final MailService mailService;
    private final ScreenshotRecordRepository repository;
    private final GithubAutomationProperties props;

    private static final ScreenshotRecordEntityToScreenshotRecordMapper ENTITY_TO_DOMAIN =
            ScreenshotRecordEntityToScreenshotRecordMapper.initialize();

    /**
     * Orchestrates: capture → email → persist. Returns DOMAIN record.
     */
    public ScreenshotRecord process(ScreenshotRequest req) {
        Path baseDir = Path.of(props.getScreenshotDir()).toAbsolutePath();

        Path dailyDir = ensureDailyDir(baseDir);
        String fileName = FileUtil.suggestPngName(req.githubUsername());
        Path target = dailyDir.resolve(fileName);

        try {
            // 1) Capture
            Path saved = seleniumAutomationService.captureProfileScreenshot(
                    req.githubUsername(), target, req.withLogin());

            long size = Files.size(saved);
            LocalDateTime now = LocalDateTime.now();

            // 2) Email
            mailService.sendScreenshot(
                    req.recipientEmail(),
                    "[GitHub] Profile screenshot: " + req.githubUsername(),
                    "Attached is the requested GitHub profile screenshot for user: " + req.githubUsername(),
                    saved.toFile()
            );

            // 3) Persist SUCCESS
            ScreenshotRecordEntity entity = ScreenshotRecordEntity.builder()
                    .githubUsername(req.githubUsername())
                    .recipientEmail(req.recipientEmail())
                    .fileName(fileName)
                    .filePath(saved.toString())
                    .fileSizeBytes(size)
                    .sentAt(now)
                    .status(ScreenshotStatus.SUCCESS)
                    .build();

            ScreenshotRecordEntity persisted = repository.save(entity);

            // 4) Map Entity → Domain and return
            return ENTITY_TO_DOMAIN.map(persisted);

        } catch (ApiException ex) {
            persistFailure(req, fileName, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            persistFailure(req, fileName, ex.getMessage());
            throw new ScreenshotCaptureException("unexpected error", ex);
        }
    }

    private Path ensureDailyDir(Path baseDir) {
        try {
            return FileUtil.ensureDailyDir(baseDir);
        } catch (Exception e) {
            throw new ScreenshotCaptureException("cannot create screenshot directory", e);
        }
    }

    private void persistFailure(ScreenshotRequest req, String fileName, String error) {
        try {
            ScreenshotRecordEntity failed = ScreenshotRecordEntity.builder()
                    .githubUsername(req.githubUsername())
                    .recipientEmail(req.recipientEmail())
                    .fileName(fileName != null ? fileName : "N/A")
                    .filePath("N/A")
                    .fileSizeBytes(0)
                    .sentAt(LocalDateTime.now())
                    .status(ScreenshotStatus.FAILED)
                    .errorMessage(error)
                    .build();
            repository.save(failed);
        } catch (Exception persistEx) {
            log.error("Failed to persist FAILED ScreenshotRecordEntity: {}", persistEx.getMessage(), persistEx);
        }
    }
}
