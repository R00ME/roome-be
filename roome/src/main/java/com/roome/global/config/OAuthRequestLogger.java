package com.roome.global.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthRequestLogger implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getRequestURI().contains("/oauth2/authorization")) {
            System.out.println("[Incoming OAuth request]: " + req.getRequestURL());
        }
        chain.doFilter(request, response);
    }
}
