package com.spexcrafters.support;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * In-memory {@link JavaMailSender} for integration tests: records every message instead
 * of talking SMTP. Simpler and faster than GreenMail, and it lets tests read the raw
 * verification token out of the email body (the database only stores the token hash).
 */
public class RecordingMailSender implements JavaMailSender {

    private final Session session = Session.getInstance(new Properties());
    private final List<MimeMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public MimeMessage createMimeMessage() {
        return new MimeMessage(session);
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) {
        try {
            return new MimeMessage(session, contentStream);
        } catch (MessagingException ex) {
            throw new MailParseException(ex);
        }
    }

    @Override
    public void send(MimeMessage mimeMessage) {
        sent.add(mimeMessage);
    }

    @Override
    public void send(MimeMessage... mimeMessages) {
        sent.addAll(Arrays.asList(mimeMessages));
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) {
        throw new UnsupportedOperationException("The application only sends MIME messages");
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) {
        throw new UnsupportedOperationException("The application only sends MIME messages");
    }

    /** Latest message addressed to {@code email} (case-insensitive), if any. */
    public Optional<MimeMessage> latestMessageTo(String email) {
        for (int i = sent.size() - 1; i >= 0; i--) {
            if (isAddressedTo(sent.get(i), email)) {
                return Optional.of(sent.get(i));
            }
        }
        return Optional.empty();
    }

    /** All body text (plain and HTML parts concatenated) of a recorded message. */
    public String bodyText(MimeMessage message) {
        try {
            return extractText(message.getContent());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read recorded email body", ex);
        }
    }

    public void reset() {
        sent.clear();
    }

    private static boolean isAddressedTo(MimeMessage message, String email) {
        try {
            Address[] recipients = message.getAllRecipients();
            if (recipients == null) {
                return false;
            }
            return Arrays.stream(recipients)
                    .anyMatch(address -> address.toString().toLowerCase(Locale.ROOT)
                            .contains(email.toLowerCase(Locale.ROOT)));
        } catch (MessagingException ex) {
            throw new IllegalStateException("Could not read recorded email recipients", ex);
        }
    }

    private static String extractText(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof MimeMultipart multipart) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                builder.append(extractText(part.getContent())).append('\n');
            }
            return builder.toString();
        }
        return "";
    }
}
