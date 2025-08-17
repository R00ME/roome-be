package com.roome.domain.apiUsage.scheduler;

import com.roome.domain.apiUsage.service.ApiUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiUsageScheduler {
    private final ApiUsageService apiUsageService;

    // 새벽 4시마다 실행
    @Scheduled(cron = "0 0 4 * * *")
    public void flushApiCounts() {
        apiUsageService.flushCountsToDb();
    }
}
