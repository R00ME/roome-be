package com.roome.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private static final String[] ALLOWED_ORIGINS = {
//			"http://localhost:5173",
			"https://roome.io.kr"
	};

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(ALLOWED_ORIGINS)
				.allowedMethods("*")
				.allowedHeaders("*")
				.exposedHeaders("Authorization", "Location", "Set-Cookie")
				.allowCredentials(true);
	}
}
