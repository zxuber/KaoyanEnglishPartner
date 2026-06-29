package com.kaoyan.peipao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startAt = System.currentTimeMillis();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            log.info("[请求] start method={} uri={} query={} clientIp={} userAgent={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    resolveClientIp(request),
                    request.getHeader("User-Agent"));
            filterChain.doFilter(request, response);
        } finally {
            long costMs = System.currentTimeMillis() - startAt;
            log.info("[请求] end method={} uri={} status={} costMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    costMs);
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
