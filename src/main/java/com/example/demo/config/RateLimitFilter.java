package com.example.demo.config;

import com.example.demo.constants.HttpStatusCodeConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@WebFilter("/*") // 모든 요청에 대해 적용
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60; // 1분당 최대 요청 수
    // 클라이언트 IP 별 요청 수를 기록 (동시성 안전)
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();


    @Override
    public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        
        // 클라이언트 식별: IP 기준
        String clientIp = request.getRemoteAddr();
        requestCounts.putIfAbsent(clientIp, new AtomicInteger(0));

        // 요청 카운트 증가
        int requests = requestCounts.get(clientIp).incrementAndGet();

        // 제한 초과 시 응답 반환
        if (requests > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(HttpStatusCodeConstants.TOO_MANY_REQUESTS);
            response.getWriter().write("Too many requests - Rate limit exceeded");
            return;
        }

        // 제한 미만이면 다음 필터 또는 컨트롤러로 요청 전달
        chain.doFilter(req, res);
    }
}
