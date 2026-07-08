package com.spexcrafters.config;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables {@code @Async} (used for email dispatch) with an executor that propagates the
 * MDC, so log lines emitted on async threads keep the request's correlation id.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("app-async-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setTaskDecorator(task -> {
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            return () -> {
                if (mdc != null) {
                    MDC.setContextMap(mdc);
                }
                try {
                    task.run();
                } finally {
                    MDC.clear();
                }
            };
        });
        return executor;
    }
}
