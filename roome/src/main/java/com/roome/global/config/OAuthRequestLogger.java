package com.roome.global.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuthRequestLogger implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getRequestURI().contains("/oauth2/authorization")) {
            log.info("[Incoming OAuth request]: {}", req.getRequestURL());
        }
        chain.doFilter(request, response);
    }
}
