package com.remake.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "remake.toss")
public record TossPaymentsProperties(
		boolean mockEnabled,
		String secretKey,
		String billingKeySecret,
		String baseUrl) {
}
