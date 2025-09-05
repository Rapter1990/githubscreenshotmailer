package com.example.githubscreenshotmailer.screenshotmailer.config;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailConfigTest {

    @Test
    void javaMailSender_isConfiguredWithProps_andSmtpDefaults() {
        // Given
        MailConfigProperties props = mock(MailConfigProperties.class);
        when(props.getHost()).thenReturn("smtp.example.com");
        when(props.getPort()).thenReturn(587);
        when(props.getUsername()).thenReturn("user@example.com");
        when(props.getPassword()).thenReturn("secret");

        MailConfig config = new MailConfig(props);

        // When
        JavaMailSender sender = config.javaMailSender();

        // Then
        assertThat(sender).isInstanceOf(JavaMailSenderImpl.class);
        JavaMailSenderImpl impl = (JavaMailSenderImpl) sender;

        assertThat(impl.getHost()).isEqualTo("smtp.example.com");
        assertThat(impl.getPort()).isEqualTo(587);
        assertThat(impl.getUsername()).isEqualTo("user@example.com");
        assertThat(impl.getPassword()).isEqualTo("secret");

        Properties p = impl.getJavaMailProperties();
        assertThat(p.getProperty("mail.transport.protocol")).isEqualTo("smtp");
        assertThat(p.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(p.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(p.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
        assertThat(p.getProperty("mail.smtp.ssl.protocols")).isEqualTo("TLSv1.2 TLSv1.3");
        assertThat(p.getProperty("mail.smtp.connectiontimeout")).isEqualTo("10000");
        assertThat(p.getProperty("mail.smtp.timeout")).isEqualTo("10000");
        assertThat(p.getProperty("mail.smtp.writetimeout")).isEqualTo("10000");
        assertThat(p.getProperty("mail.smtp.ssl.trust")).isEqualTo("smtp.example.com");

        // Also verify we actually read values from the properties bean
        verify(props, atLeastOnce()).getHost();
        verify(props, atLeastOnce()).getPort();
        verify(props, atLeastOnce()).getUsername();
        verify(props, atLeastOnce()).getPassword();
        verifyNoMoreInteractions(props);
    }

    @Test
    void javaMailSender_sslTrust_followsHost() {
        // Given
        MailConfigProperties props = mock(MailConfigProperties.class);
        when(props.getHost()).thenReturn("mail.company.internal");
        when(props.getPort()).thenReturn(465);
        when(props.getUsername()).thenReturn("svc@company");
        when(props.getPassword()).thenReturn("pw");

        MailConfig config = new MailConfig(props);

        // When
        JavaMailSenderImpl impl = (JavaMailSenderImpl) config.javaMailSender();

        // Then
        assertThat(impl.getHost()).isEqualTo("mail.company.internal");
        assertThat(impl.getPort()).isEqualTo(465);

        Properties p = impl.getJavaMailProperties();
        assertThat(p.getProperty("mail.smtp.ssl.trust")).isEqualTo("mail.company.internal");

    }

}