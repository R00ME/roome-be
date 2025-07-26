package com.roome.global.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class ResponseLog {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    // 요청 시작 시간 기록 (같이 사용되도록)
    @Before("execution(* com.roome.domain..controller..*.*(..))")
    public void before() {
        startTimeThreadLocal.set(System.currentTimeMillis());
    }

    @AfterReturning(
            pointcut = "execution(* com.roome.domain..controller..*.*(..))",
            returning = "response"
    )
    public void logAfterReturning(JoinPoint joinPoint, Object response) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();

        long duration = System.currentTimeMillis() - startTimeThreadLocal.get();
        startTimeThreadLocal.remove();

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("logType", "response_info");
        logMap.put("timestamp", LocalDateTime.now().toString());
        logMap.put("classMethod", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        logMap.put("httpMethod", request.getMethod());
        logMap.put("requestURI", request.getRequestURI());
        logMap.put("username", getUsername());
        logMap.put("executionTimeMs", duration);
        logMap.put("responseBody", getResponseJson(response));

        log.info(asJson(logMap));
    }

    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "anonymous";
    }

    private String getResponseJson(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "Unserializable Response";
        }
    }

    private String asJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to convert response log to JSON\"}";
        }
    }
}
