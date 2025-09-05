package com.example.githubscreenshotmailer.screenshotmailer.service;

import com.example.githubscreenshotmailer.base.AbstractBaseServiceTest;
import com.example.githubscreenshotmailer.screenshotmailer.exception.EmailSendException;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MailServiceTest extends AbstractBaseServiceTest {

    @InjectMocks
    MailService mailService;

    @Mock
    JavaMailSender mailSender;

    @TempDir
    Path tempDir;

    @Test
    void sendScreenshot_withAttachment_sendsMail() throws Exception {


        // Given
        MimeMessage realMessage = new MimeMessage((Session) null);

        Path filePath = tempDir.resolve("shot.png");
        Files.writeString(filePath, "dummy-bytes");
        File attachment = filePath.toFile();

        // When
        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        // Then
        mailService.sendScreenshot("user@example.com", "Subject", "Body", attachment);


        // Verify
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(same(realMessage));
        verifyNoMoreInteractions(mailSender);

    }

    @Test
    void sendScreenshot_withoutAttachment_sendsMail() {

        // Given
        MimeMessage realMessage = new MimeMessage((Session) null);

        // When
        when(mailSender.createMimeMessage()).thenReturn(realMessage);

        // Then
        mailService.sendScreenshot("user@example.com", "No Attachment", "Body", null);

        // Verify
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(same(realMessage));
        verifyNoMoreInteractions(mailSender);

    }

    @Test
    void sendScreenshot_invalidTo_throwsEmailSendException() {

        // Given
        MimeMessage failing = new MimeMessage((Session) null) {
            @Override
            public void setRecipients(jakarta.mail.Message.RecipientType type,
                                      Address[] addresses) throws MessagingException {
                throw new MessagingException("forced recipients failure");
            }
            @Override
            public void setRecipients(jakarta.mail.Message.RecipientType type,
                                      String addresses) throws MessagingException {
                throw new MessagingException("forced recipients failure");
            }
        };

        // When
        when(mailSender.createMimeMessage()).thenReturn(failing);

        // Then
        EmailSendException ex = assertThrows(
                EmailSendException.class,
                () -> mailService.sendScreenshot("user@example.com", "Subject", "Body", null) // address value doesn't matter
        );

        assertTrue(ex.getMessage().contains("SMTP send error"));

        // Verify
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
        verifyNoMoreInteractions(mailSender);

    }

}