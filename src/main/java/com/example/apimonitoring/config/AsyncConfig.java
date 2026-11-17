package com.example.apimonitoring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables @Async — used to send notification emails in the background.
 * Spring Boot provides the thread pool ("applicationTaskExecutor", configurable with spring.task.execution.*).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
