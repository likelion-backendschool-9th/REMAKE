package com.remake.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.remake.share.domain.SharedRecipePhotoPolicy;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UploadProperties.class)
public class UploadConfig {

	@Bean
	SharedRecipePhotoPolicy sharedRecipePhotoPolicy() {
		return new SharedRecipePhotoPolicy(SharedRecipePhotoPolicy.MAX_FILE_BYTES);
	}
}
