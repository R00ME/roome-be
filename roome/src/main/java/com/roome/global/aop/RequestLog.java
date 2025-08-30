package com.roome.global.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
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
public class RequestLog {

    @Pointcut(
            "execution(* com.roome.domain..controller..*.*(..))")
    private void cut() {
    }

    @Before("cut()")
    public void logBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("logType", "request_info");
        logMap.put("timestamp", LocalDateTime.now().toString());
        logMap.put("classMethod", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        logMap.put("httpMethod", request.getMethod());
        logMap.put("requestURI", request.getRequestURI());
        logMap.put("requestURL", request.getRequestURL().toString());
        logMap.put("ipAddress", getClientIp(request));
        logMap.put("username", getUsername());

        log.info(asJson(logMap));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return xfHeader == null ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }

    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "anonymous";
    }

    private String asJson(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to convert log to JSON\"}";
        }
    }
}
