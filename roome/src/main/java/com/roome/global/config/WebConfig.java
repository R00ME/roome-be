package com.roome.global.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private static final String ALLOW_ALL_PATH = "/**";
	private static final String ALLOWED_METHODS = "*";
	private static final String FRONTEND_LOCALHOST = "http://localhost:5173";
	private static final String FRONTEND_CLOUDFRONT = "https://desqb38rc2v50.cloudfront.net";

//    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;


	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping(ALLOW_ALL_PATH)
				.allowedMethods(ALLOWED_METHODS)
				.allowedOrigins(
						FRONTEND_LOCALHOST,
						FRONTEND_CLOUDFRONT
				)
				.allowCredentials(true);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
//        resolvers.add(authenticatedUserArgumentResolver);
		resolvers.add(null);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/oauth2/**") // 모든 API에 대해
						.allowedOrigins("http://localhost:5173") // 프론트 주소
						.allowedMethods("*") // GET, POST, PUT, DELETE 등 허용
						.allowedHeaders("*") // 모든 헤더 허용
						.exposedHeaders("Authorization") // 👉 이거 추가해서 Authorization 헤더 클라이언트가 읽을 수 있게 함
						.allowCredentials(true)
						.exposedHeaders("Location"); // (Optional) 쿠키 전달 허용
			}
		};
	}

//    @Bean
//    public WebClient.Builder webClientBuilder() {
//        return WebClient.builder()
//                .codecs(configurer -> configurer
//                        .defaultCodecs()
//                        .maxInMemorySize(16 * 1024 * 1024))
//                .clientConnector(new ReactorClientHttpConnector(
//                        HttpClient.create()
//                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
//                                .responseTimeout(Duration.ofSeconds(5))
//                ));
//    }
}
