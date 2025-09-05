package com.example.githubscreenshotmailer.screenshotmailer.service;

import com.example.githubscreenshotmailer.base.AbstractBaseServiceTest;
import com.example.githubscreenshotmailer.common.model.CustomPage;
import com.example.githubscreenshotmailer.common.model.dto.request.CustomPagingRequest;
import com.example.githubscreenshotmailer.screenshotmailer.config.GithubAutomationProperties;
import com.example.githubscreenshotmailer.screenshotmailer.exception.ApiException;
import com.example.githubscreenshotmailer.screenshotmailer.exception.EmailSendException;
import com.example.githubscreenshotmailer.screenshotmailer.exception.ScreenshotCaptureException;
import com.example.githubscreenshotmailer.screenshotmailer.model.ScreenshotRecord;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.request.ListScreenshotRecordRequest;
import com.example.githubscreenshotmailer.screenshotmailer.model.dto.request.ScreenshotRequest;
import com.example.githubscreenshotmailer.screenshotmailer.model.entity.ScreenshotRecordEntity;
import com.example.githubscreenshotmailer.screenshotmailer.model.enums.ScreenshotStatus;
import com.example.githubscreenshotmailer.screenshotmailer.model.mapper.ScreenshotRecordEntityToScreenshotRecordMapper;
import com.example.githubscreenshotmailer.screenshotmailer.repository.ScreenshotRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.data.domain.Page;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GithubScreenshotServiceTest extends AbstractBaseServiceTest {

    @InjectMocks
    private GithubScreenshotService service;

    @Mock
    private SeleniumAutomationService seleniumAutomationService;

    @Mock
    private MailService mailService;

    @Mock
    private ScreenshotRecordRepository repository;

    @Mock
    private GithubAutomationProperties props;

    @TempDir
    Path tempDir;

    private static final ScreenshotRecordEntityToScreenshotRecordMapper ENTITY_TO_DOMAIN =
            ScreenshotRecordEntityToScreenshotRecordMapper.initialize();

    @Test
    void process_success_captures_emails_and_persists_success_entity() throws Exception {

        // Given
        when(props.getScreenshotDir()).thenReturn(tempDir.toString());

        ScreenshotRequest req = mockReq("octocat", "to@example.com", true);

        Path saved = tempDir.resolve("saved.png");
        Files.createDirectories(saved.getParent());
        Files.writeString(saved, "png-bytes");
        long size = Files.size(saved);

        ScreenshotRecordEntity persisted = ScreenshotRecordEntity.builder()
                .githubUsername("octocat")
                .recipientEmail("to@example.com")
                .fileName("saved.png")
                .filePath(saved.toString())
                .fileSizeBytes(size)
                .sentAt(LocalDateTime.now())
                .status(ScreenshotStatus.SUCCESS)
                .build();

        ScreenshotRecord expected = ENTITY_TO_DOMAIN.map(persisted);

        // When
        when(seleniumAutomationService.captureProfileScreenshot(eq("octocat"), any(Path.class), eq(true)))
                .thenReturn(saved);
        when(repository.save(any(ScreenshotRecordEntity.class))).thenReturn(persisted);

        // Then
        ScreenshotRecord result = service.process(req);
        assertNotNull(result);
        assertEquals(expected.githubUsername(), result.githubUsername());
        assertEquals(expected.recipientEmail(), result.recipientEmail());
        assertEquals(expected.fileName(),       result.fileName());
        assertEquals(expected.path(),           result.path());
        assertEquals(expected.fileSize(),       result.fileSize());
        assertEquals(expected.status(),         result.status());
        assertNotNull(result.sentAt());

        // Verify
        verify(seleniumAutomationService, times(1))
                .captureProfileScreenshot(eq("octocat"), any(Path.class), eq(true));
        verify(mailService, times(1))
                .sendScreenshot(eq("to@example.com"), anyString(), anyString(), eq(saved.toFile()));
        verify(repository, times(1))
                .save(argThat(e ->
                        "octocat".equals(e.getGithubUsername())
                                && "to@example.com".equals(e.getRecipientEmail())
                                && saved.toString().equals(e.getFilePath())
                                && e.getFileSizeBytes() == size
                                && e.getStatus() == ScreenshotStatus.SUCCESS
                                && e.getSentAt() != null
                ));
        verifyNoMoreInteractions(repository, seleniumAutomationService, mailService);

    }

    @Test
    void process_mail_fails_with_ApiException_persists_failure_and_rethrows() throws Exception {

        // Given
        when(props.getScreenshotDir()).thenReturn(tempDir.toString());
        ScreenshotRequest req = mockReq("octocat", "to@example.com", false);

        Path saved = tempDir.resolve("saved-mail.png");
        Files.writeString(saved, "png");

        //  When
        when(seleniumAutomationService.captureProfileScreenshot(eq("octocat"),
                any(Path.class), eq(false)))
                .thenReturn(saved);

        // Then
        doThrow(new EmailSendException("SMTP send error", new RuntimeException("smtp")))
                .when(mailService).sendScreenshot(anyString(), anyString(), anyString(), any());
        ApiException ex = assertThrows(ApiException.class, () -> service.process(req));
        assertEquals("Email sending failed: SMTP send error", ex.getMessage());

        // Verify
        verify(repository, times(1)).save(argThat(e -> e.getStatus() == ScreenshotStatus.FAILED));
        verifyNoMoreInteractions(repository);

    }

    @Test
    void process_capture_throws_runtime_persists_failure_and_wraps_as_ScreenshotCaptureException() {

        // Given
        when(props.getScreenshotDir()).thenReturn(tempDir.toString());
        ScreenshotRequest req = mockReq("octocat", "to@example.com", true);

        when(seleniumAutomationService.captureProfileScreenshot(eq("octocat"), any(Path.class), eq(true)))
                .thenThrow(new RuntimeException("webdriver died"));

        // Then
        ScreenshotCaptureException ex =
                assertThrows(ScreenshotCaptureException.class, () -> service.process(req));
        assertEquals("Screenshot capture failed: unexpected error", ex.getMessage());
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("webdriver died"));

        // Verify
        verify(repository, times(1)).save(argThat(e -> e.getStatus() == ScreenshotStatus.FAILED));
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mailService);

    }

    @Test
    void process_wraps_directory_creation_failure_early() throws Exception {

        // Given
        Path baseFile = tempDir.resolve("not-a-dir.txt");
        Files.writeString(baseFile, "x");
        when(props.getScreenshotDir()).thenReturn(baseFile.toString());

        ScreenshotRequest req = mockReq("octocat", "to@example.com", true);

        // When & Then
        ScreenshotCaptureException ex =
                assertThrows(ScreenshotCaptureException.class, () -> service.process(req));
        assertEquals("Screenshot capture failed: cannot create screenshot directory", ex.getMessage());

        // Verify
        verifyNoInteractions(seleniumAutomationService, mailService, repository);

    }

    @Test
    void process_persistFailure_swallows_repository_error_and_preserves_original_exception() throws Exception {

        // Given
        when(props.getScreenshotDir()).thenReturn(tempDir.toString());
        ScreenshotRequest req = mockReq("octocat", "to@example.com", true);

        Path saved = tempDir.resolve("will-fail-save.png");
        Files.writeString(saved, "png");

        // When
        when(seleniumAutomationService.captureProfileScreenshot(eq("octocat"), any(Path.class), eq(true)))
                .thenReturn(saved);
        doThrow(new EmailSendException("SMTP send error", new RuntimeException("x")))
                .when(mailService).sendScreenshot(anyString(), anyString(), anyString(), any());
        when(repository.save(any(ScreenshotRecordEntity.class)))
                .thenThrow(new RuntimeException("db down"));

        // Then
        assertThrows(ApiException.class, () -> service.process(req));

        // Verify
        verify(repository, times(1)).save(any(ScreenshotRecordEntity.class));

    }

    @Test
    void getScreenshots_withNullRequest_returnsMappedPage() {

        // Given
        CustomPagingRequest paging = mock(CustomPagingRequest.class);
        when(paging.toPageable()).thenReturn(PageRequest.of(0, 10));

        ScreenshotRecordEntity e = ScreenshotRecordEntity.builder()
                .githubUsername("octo")
                .recipientEmail("to@ex.com")
                .fileName("a.png")
                .filePath("/p/a.png")
                .fileSizeBytes(42)
                .sentAt(LocalDateTime.now())
                .status(ScreenshotStatus.SUCCESS)
                .build();

        ScreenshotRecord expected = ENTITY_TO_DOMAIN.map(e);

        Page<ScreenshotRecordEntity> springPage =
                new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);

        // When
        when(repository.findAll(
                org.mockito.ArgumentMatchers.<Specification<ScreenshotRecordEntity>>isNull(),
                any(Pageable.class)
        )).thenReturn(springPage);

        // Then
        CustomPage<ScreenshotRecord> result = service.getScreenshots(null, paging);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertEquals(1L, result.getTotalElementCount());
        assertEquals(1, result.getTotalPageCount());

        ScreenshotRecord actual   = result.getContent().getFirst();
        assertEquals(expected.githubUsername(), actual.githubUsername());
        assertEquals(expected.recipientEmail(), actual.recipientEmail());
        assertEquals(expected.fileName(),       actual.fileName());
        assertEquals(expected.path(),           actual.path());
        assertEquals(expected.fileSize(),       actual.fileSize());
        assertEquals(expected.status(),         actual.status());
        assertNotNull(actual.sentAt());

        // Verify
        verify(repository, times(1)).findAll(
                org.mockito.ArgumentMatchers.<Specification<ScreenshotRecordEntity>>isNull(),
                any(Pageable.class)
        );

    }

    @Test
    void getScreenshots_withSpecFromRequest_returnsMappedPage() {

        // Given
        ListScreenshotRecordRequest req = mock(ListScreenshotRecordRequest.class);
        @SuppressWarnings("unchecked")
        Specification<ScreenshotRecordEntity> spec = (root, q, cb) -> null;
        when(req.toSpecification()).thenReturn(spec);

        CustomPagingRequest paging = mock(CustomPagingRequest.class);
        Page<ScreenshotRecordEntity> springPage =
                new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(paging.toPageable()).thenReturn(PageRequest.of(0, 5));

        // When
        when(repository.findAll(
                org.mockito.ArgumentMatchers.eq(spec),
                any(Pageable.class)
        )).thenReturn(springPage);

        // Then
        CustomPage<ScreenshotRecord> result = service.getScreenshots(req, paging);
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(1, result.getPageNumber());
        assertEquals(5, result.getPageSize());
        assertEquals(0L, result.getTotalElementCount());
        assertEquals(0, result.getTotalPageCount());

        // Verify
        verify(repository, times(1)).findAll(
                org.mockito.ArgumentMatchers.<Specification<ScreenshotRecordEntity>>eq(spec),
                any(Pageable.class)
        );

    }

    private ScreenshotRequest mockReq(String user, String email, boolean withLogin) {
        ScreenshotRequest req = mock(ScreenshotRequest.class);
        when(req.githubUsername()).thenReturn(user);
        when(req.recipientEmail()).thenReturn(email);
        when(req.withLogin()).thenReturn(withLogin);
        return req;
    }

}