package com.example.githubscreenshotmailer.screenshotmailer.service;

import com.example.githubscreenshotmailer.screenshotmailer.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendScreenshot(String to, String subject, String body, File attachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            if (attachment != null) {
                helper.addAttachment(attachment.getName(), new FileSystemResource(attachment));
            }

            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new EmailSendException("SMTP send error", ex);
        }
    }
}
