package com.example.url_shortner.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Enables Spring's scheduler.
 *
 * <p><b>Why a dedicated config class</b> rather than putting {@code @EnableScheduling}
 * on {@code UrlShortenerApplication}: it keeps the entry point free of feature flags,
 * and — more usefully — it can be switched off wholesale with
 * {@code app.scheduling.enabled=false}. The integration tests set exactly that, so
 * the expiry sweep cannot fire mid-test and deactivate a fixture out from under an
 * assertion.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {

    /**
     * Explicit scheduler pool.
     *
     * <p>Spring's default is a single-threaded executor, so any new scheduled job
     * added later would silently queue behind the expiry sweep. Two threads gives
     * headroom without pretending this application needs a large pool.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduled-task-");
        // Let in-flight jobs finish on shutdown rather than being killed mid-transaction.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
