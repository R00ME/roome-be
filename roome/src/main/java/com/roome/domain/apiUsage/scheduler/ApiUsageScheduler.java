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
//    @Scheduled(cron = "0 0 4 * * *")
    @Scheduled(cron = "0 */3 * * * *") // 3분마다 실행 추후 새벽 4시 스케줄링으로 변경 예정
    public void flushApiCounts() {
        apiUsageService.flushCountsToDb();
    }
}
