package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // API Key 기반 요청 제한 필터
    private final ApiKeyRateLimitFilter apiKeyRateLimitFilter;

    // 생성자 주입
    public SecurityConfig(ApiKeyRateLimitFilter apiKeyRateLimitFilter) {
        this.apiKeyRateLimitFilter = apiKeyRateLimitFilter;
    }

    // SecurityFilterChain 설정 (CSRF, 권한, 보안 헤더, 커스텀 필터 순서 지정)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 기본 활성화
            .csrf(Customizer.withDefaults())
            // URL 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll() // API 요청은 인증 없이 접근 허용
                .anyRequest().authenticated()           // 나머지는 인증 필요
            )
            // 보안 헤더 설정
            .headers(headers -> headers
                // CSP(Content Security Policy) 설정: 기본적으로 자기 자신 도메인만 허용
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                // XSS 공격 방지 헤더 추가
                .xssProtection(Customizer.withDefaults())
                // iframe 등의 clickjacking 방지, 같은 출처만 허용
                .frameOptions().sameOrigin()
            )
            // API Key 인증 필터 적용
            .addFilterBefore(apiKeyRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
