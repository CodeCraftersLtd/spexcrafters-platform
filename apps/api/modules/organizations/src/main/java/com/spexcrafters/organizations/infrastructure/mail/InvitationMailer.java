package com.spexcrafters.organizations.infrastructure.mail;

import com.spexcrafters.organizations.domain.OrganizationRole;
import com.spexcrafters.organizations.infrastructure.config.OrganizationsFrontendProperties;
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
 * Sends the organization invitation email (plain text + simple HTML alternative) with a
 * link to {@code {spexcrafters.web.base-url}/en/invitations/accept?token=...}. The raw
 * token appears exactly once — in this email; it is never logged and never stored.
 *
 * <p>Sent asynchronously so SMTP latency or outages never block or fail the invitation
 * request; on failure we log (without the token) and the inviter can revoke and re-invite.
 */
@Component
public class InvitationMailer {

    private static final Logger log = LoggerFactory.getLogger(InvitationMailer.class);

    private final JavaMailSender mailSender;
    private final OrganizationsFrontendProperties frontendProperties;
    private final String fromAddress;

    public InvitationMailer(JavaMailSender mailSender, OrganizationsFrontendProperties frontendProperties,
            @Value("${spexcrafters.mail.from:no-reply@spexcrafters.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.frontendProperties = frontendProperties;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendInvitationEmail(String recipientEmail, String organizationName,
            OrganizationRole role, String rawToken) {
        String link = frontendProperties.baseUrl() + "/en/invitations/accept?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("You have been invited to join " + organizationName + " on SpexCrafters");
            helper.setText(plainTextBody(organizationName, role, link), htmlBody(organizationName, role, link));
            mailSender.send(message);
            log.info("Invitation email sent to {}", recipientEmail);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send invitation email to {}; the inviter can revoke and re-invite",
                    recipientEmail, ex);
        }
    }

    private String plainTextBody(String organizationName, OrganizationRole role, String link) {
        return """
                Hello,

                You have been invited to join %s on SpexCrafters as %s. \
                Open the link below to accept the invitation:

                %s

                The link is valid for 7 days and can be used once, and only with an account \
                registered under this email address. If you were not expecting this invitation, \
                you can safely ignore this email.

                — The SpexCrafters team
                """.formatted(organizationName, role.name(), link);
    }

    private String htmlBody(String organizationName, OrganizationRole role, String link) {
        String safeName = escapeHtml(organizationName);
        return """
                <html>
                <body style="font-family: Arial, Helvetica, sans-serif; color: #1a1a1a;">
                  <p>Hello,</p>
                  <p>You have been invited to join <strong>%s</strong> on SpexCrafters as %s.</p>
                  <p><a href="%s" style="display: inline-block; padding: 10px 18px; \
                background-color: #14532d; color: #ffffff; text-decoration: none; \
                border-radius: 4px;">Accept invitation</a></p>
                  <p>Or open this link: <a href="%s">%s</a></p>
                  <p>The link is valid for 7 days and can be used once, and only with an account
                  registered under this email address. If you were not expecting this invitation,
                  you can safely ignore this email.</p>
                  <p>— The SpexCrafters team</p>
                </body>
                </html>
                """.formatted(safeName, role.name(), link, link, link);
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
