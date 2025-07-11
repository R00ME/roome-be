package com.roome.global.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.roome.domain.mycd.dto.MyCdListResponse;
import com.roome.domain.mycd.dto.MyCdResponse;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@EnableCaching
@Slf4j
@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host:localhost}") // 기본값 설정
	private String host;

	@Value("${spring.data.redis.port:6379}")
	private int port;

	@Value("${spring.data.redis.password:}") // 비밀번호가 없을 수도 있으므로 기본값 ""
	private String password;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(host);
		config.setPort(port);
		config.setPassword(password);

		LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
		factory.afterPropertiesSet(); // 강제 초기화

		// Redis 연결 확인 (Ping 테스트)
		try {
			String result = factory.getConnection().ping();
			log.info("Redis 연결 성공! 응답: {}", result);
		} catch (Exception e) {
			log.error("Redis 연결 실패: {}", e.getMessage(), e);
		}

		return factory;
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return new StringRedisTemplate(redisConnectionFactory);
	}

	// TempCode 전용 RedisTemplate
	@Bean(name = "tempCodeRedisTemplate")
	public RedisTemplate<String, String> tempCodeRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}

	@Bean(name = "refreshTokenRedisTemplate")
	public RedisTemplate<String, String> refreshTokenRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// key/value 직렬화 방식
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());

		return template;
	}

	@Bean(name = "blacklistRedisTemplate")
	public RedisTemplate<String, String> blacklistRedisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}

	// 랭킹 시스템 전용 RedisTemplate (String 직렬화)
	@Bean("rankingRedisTemplate")
	public RedisTemplate<String, String> rankingRedisTemplate(
			RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// String 직렬화 설정
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new StringRedisSerializer());

		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// ObjectMapper 설정 (LocalDate 변환 가능하도록 설정)
		ObjectMapper redisObjectMapper = new ObjectMapper();
		redisObjectMapper.registerModule(new JavaTimeModule()); // LocalDate 지원 추가
		redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// Jackson 기반 JSON 직렬화 설정
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(
				redisObjectMapper);

		template.setKeySerializer(new StringRedisSerializer()); // Key: String
		template.setValueSerializer(serializer); // Value: JSON 직렬화
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(serializer);

		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public RedisTemplate<String, MyCdResponse> myCdRedisTemplate(
			RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, MyCdResponse> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// ObjectMapper 생성 및 JavaTimeModule 등록 (LocalDate 직렬화 문제 해결)
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule()); // LocalDate 지원
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// ObjectMapper를 적용한 Jackson2JsonRedisSerializer 생성
		Jackson2JsonRedisSerializer<MyCdResponse> serializer = new Jackson2JsonRedisSerializer<>(
				objectMapper, MyCdResponse.class);

		template.setKeySerializer(new StringRedisSerializer()); // Key: String
		template.setValueSerializer(serializer); // Value: JSON 직렬화
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(serializer);

		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());  // LocalDate 변환 지원
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

		// setObjectMapper 대신 생성자에서 설정
		Jackson2JsonRedisSerializer<MyCdListResponse> serializer =
				new Jackson2JsonRedisSerializer<>(objectMapper, MyCdListResponse.class);

		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofHours(1)) // 1시간 캐싱 유지
				.serializeKeysWith(
						RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(
						RedisSerializationContext.SerializationPair.fromSerializer(serializer));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(config)
				.build();
	}

	// Redis 메시지 수신자 컨테이너
	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		return container;
	}

	@Bean
	public KeyGenerator simpleKeyGenerator() {
		return new SimpleKeyGenerator();
	}

	@PreDestroy
	public void cleanUp() {
		if (redisConnectionFactory() instanceof LettuceConnectionFactory lettuceFactory) {
			lettuceFactory.destroy();
			log.info("Redis 연결 종료 완료!");
		}
	}
}
