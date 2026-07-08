package com.spexcrafters.identity.infrastructure.mail;

import com.spexcrafters.identity.infrastructure.config.FrontendProperties;
import com.spexcrafters.sharedkernel.util.LogSanitizer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends the account verification email (plain text + simple HTML alternative) with a link
 * to {@code {spexcrafters.web.base-url}/auth/verify-email?token=...}.
 *
 * <p>Sent asynchronously so SMTP latency or outages never block or fail the registration
 * request; on failure we log and rely on the resend-verification endpoint as the recovery
 * path. Dev SMTP is Mailpit (see infrastructure/docker/compose.yaml).
 */
@Component
public class VerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(VerificationMailer.class);

    private final JavaMailSender mailSender;
    private final FrontendProperties frontendProperties;
    private final String fromAddress;

    public VerificationMailer(JavaMailSender mailSender, FrontendProperties frontendProperties,
            @Value("${spexcrafters.mail.from:no-reply@spexcrafters.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.frontendProperties = frontendProperties;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendVerificationEmail(String recipientEmail, String displayName, String rawToken) {
        String link = frontendProperties.baseUrl() + "/auth/verify-email?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("Verify your SpexCrafters email address");
            helper.setText(plainTextBody(displayName, link), htmlBody(displayName, link));
            mailSender.send(message);
            log.info("Verification email sent to {}", LogSanitizer.maskEmail(recipientEmail));
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send verification email to {}; the user can request a resend",
                    LogSanitizer.maskEmail(recipientEmail), ex);
        }
    }

    private String plainTextBody(String displayName, String link) {
        return """
                Hello %s,

                Welcome to SpexCrafters. Please confirm your email address by opening the link below:

                %s

                The link is valid for 24 hours and can be used once. If you did not create a \
                SpexCrafters account, you can safely ignore this email.

                — The SpexCrafters team
                """.formatted(displayName, link);
    }

    private String htmlBody(String displayName, String link) {
        String safeName = escapeHtml(displayName);
        return """
                <html>
                <body style="font-family: Arial, Helvetica, sans-serif; color: #1a1a1a;">
                  <p>Hello %s,</p>
                  <p>Welcome to SpexCrafters. Please confirm your email address:</p>
                  <p><a href="%s" style="display: inline-block; padding: 10px 18px; \
                background-color: #14532d; color: #ffffff; text-decoration: none; \
                border-radius: 4px;">Verify email address</a></p>
                  <p>Or open this link: <a href="%s">%s</a></p>
                  <p>The link is valid for 24 hours and can be used once. If you did not create a
                  SpexCrafters account, you can safely ignore this email.</p>
                  <p>— The SpexCrafters team</p>
                </body>
                </html>
                """.formatted(safeName, link, link, link);
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
