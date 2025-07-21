package com.roome.global.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class TimeTraceLog {

    @Around("execution(* com.roome.domain..controller..*.*(..)) || execution(* com.roome.global.controller..*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("logType", "execution_time");
        logMap.put("timestamp", LocalDateTime.now().toString());
        logMap.put("classMethod", joinPoint.getSignature().toShortString());
        logMap.put("executionTimeMs", duration);

        log.info(asJson(logMap));

        return result;
    }

    private String asJson(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to convert log to JSON\"}";
        }
    }
}
