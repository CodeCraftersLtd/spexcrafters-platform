package com.spexcrafters.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Swaps the SMTP {@code JavaMailSender} for the recording stub in integration tests. */
@TestConfiguration
public class RecordingMailConfig {

    @Bean
    @Primary
    RecordingMailSender recordingMailSender() {
        return new RecordingMailSender();
    }
}
