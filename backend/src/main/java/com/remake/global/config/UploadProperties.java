package com.remake.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "remake.upload")
public record UploadProperties(@NotBlank String rootDirectory) {
}
