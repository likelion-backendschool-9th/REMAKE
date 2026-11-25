package com.remake.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
		AuthProperties.class,
		OAuthProperties.class,
		GeminiProperties.class,
		TossPaymentsProperties.class})
public class RemakePropertiesConfig {
}
