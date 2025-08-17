package com.roome.global.filter;

import com.roome.domain.apiUsage.service.ApiUsageCountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
        String userId = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "anonymous";

        apiUsageCountService.incrementCount(Long.parseLong(userId), uri);

        filterChain.doFilter(request, response);
    }
}
