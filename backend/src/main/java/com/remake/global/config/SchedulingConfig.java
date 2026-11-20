package com.remake.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "remake.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
