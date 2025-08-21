package com.roome.domain.apiUsage.service;

import com.roome.domain.apiUsage.repository.UserApiUsageRepository;
import com.roome.domain.apiUsage.entity.UserApiUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApiUsageService {

    @Qualifier("apiCountRedisTemplate")
    private final RedisTemplate<String, Long> apiCountRedisTemplate;
    private final UserApiUsageRepository userApiUsageRepository;

    @Transactional
    public void flushCountsToDb() {
        // 1. Redis 키 스캔 (api_count:*)
        Set<String> keys = apiCountRedisTemplate.keys("api_count:user:*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        for (String key : keys) {
            Long count = apiCountRedisTemplate.opsForValue().get(key);
            if (count == null || count == 0) {
                // 카운트가 없거나 0이면 처리하지 않음
                apiCountRedisTemplate.delete(key);
                continue;
            }

            String[] parts = key.split(":");
            if (parts.length < 4) {
                // 키 형식이 올바르지 않으면 다음 키로 넘어감
                continue;
            }
            Long userId = Long.parseLong(parts[2]);
            String apiUri = parts[3];
            String domain = resolveDomain(apiUri);

            // 2. userId, apiUri, 오늘 날짜로 기존 레코드 찾기
            Optional<UserApiUsage> existingUsage = userApiUsageRepository.findByUserIdAndDomainAndApiUriAndDate(userId, domain, apiUri, today);

            if (existingUsage.isPresent()) {
                // 3-1. 기존 레코드가 있으면 카운트만 업데이트
                UserApiUsage usage = existingUsage.get();
                usage.plusCount(count); // UserApiUsage 엔티티에 추가할 메소드
                userApiUsageRepository.save(usage);
            } else {
                // 3-2. 기존 레코드가 없으면 새로 생성
                userApiUsageRepository.save(
                        UserApiUsage.builder()
                                .userId(userId)
                                .domain(domain)
                                .apiUri(apiUri)
                                .date(today)
                                .count(count)
                                .build()
                );
            }

            // 4. DB로 이관이 완료된 Redis 키는 삭제
            apiCountRedisTemplate.delete(key);
        }
    }

    private String resolveDomain(String uri) {
        String[] parts = uri.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "etc";
    }
}
