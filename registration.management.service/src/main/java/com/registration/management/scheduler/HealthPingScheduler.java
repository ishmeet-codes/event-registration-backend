package com.registration.management.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Scheduler that pings the health endpoint every three minutes.
 * This keeps the Render instance warm and provides a simple health check.
 */
@Component
public class HealthPingScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthPingScheduler.class);

    private final RestTemplate restTemplate;
    private final String healthUrl;

    public HealthPingScheduler(RestTemplate restTemplate,
                               @Value("${health.ping.url}") String healthUrl) {
        this.restTemplate = restTemplate;
        this.healthUrl = healthUrl;
    }

    // Runs at second 0 of every 3‑minute interval (cron: 0 */3 * * * *)
    @Scheduled(cron = "0 */3 * * * *")
    public void pingHealth() {
        try {
            var response = restTemplate.getForEntity(healthUrl, String.class);
            log.info("Health ping successful – status: {}", response.getStatusCode().value());
        } catch (Exception ex) {
            log.warn("Health ping failed: {}", ex.getMessage());
        }
    }
}
