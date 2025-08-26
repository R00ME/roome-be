package com.roome.global.filter;

import com.roome.domain.apiUsage.service.ApiUsageCountService;
import com.roome.global.security.jwt.principal.CustomUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiCountFilter extends OncePerRequestFilter {

    private final ApiUsageCountService apiUsageCountService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.startsWith("/api/auth/") || uri.startsWith("/ws/") || uri.startsWith("/api/notification")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUser customUser) {
            Long userId = customUser.getId();
            apiUsageCountService.incrementCount(userId, uri);
        }

        filterChain.doFilter(request, response);
    }
}
