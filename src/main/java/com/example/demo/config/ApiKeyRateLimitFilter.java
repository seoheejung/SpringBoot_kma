package com.example.demo.config;

import com.example.demo.constants.HttpStatusCodeConstants;
import com.example.demo.domain.ApiKey;
import com.example.demo.repository.ApiKeyRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiKeyRateLimitFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyRateLimitFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    // API Key별 요청 제한 Bucket 캐시
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    private Bucket createNewBucket(ApiKey apiKey) {
        int limit = apiKey.getLimitPerMinute();
        Refill refill = Refill.intervally(limit, Duration.ofMinutes(1));
        Bandwidth bandwidth = Bandwidth.classic(limit, refill);
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private Bucket resolveBucket(ApiKey apiKey) {
        return bucketCache.computeIfAbsent(apiKey.getApiKey(), k -> createNewBucket(apiKey));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String apiKeyHeader = request.getHeader("X-API-KEY");

        // 1️⃣ API Key 없으면 401 Unauthorized
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            response.sendError(HttpStatusCodeConstants.AUTHENTICATION_FAILURE, "API Key missing");
            return;
        }

        // 2️⃣ DB 조회 후 유효성 체크
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByApiKey(apiKeyHeader);
        if (apiKeyOpt.isEmpty() || !apiKeyOpt.get().getActive()) {
            response.sendError(HttpStatusCodeConstants.AUTHENTICATION_FAILURE, "Invalid or inactive API Key");
            return;
        }

        ApiKey apiKey = apiKeyOpt.get();

        // 3️⃣ Rate Limit 체크
        Bucket bucket = resolveBucket(apiKey);
        if (!bucket.tryConsume(1)) {
            response.sendError(HttpStatusCodeConstants.TOO_MANY_REQUESTS, "Rate limit exceeded for this API Key");
            return;
        }

        // 4️⃣ SecurityContext에 인증 등록
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(apiKey.getOwner(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 5️⃣ 다음 필터 진행
        filterChain.doFilter(request, response);
    }
}
