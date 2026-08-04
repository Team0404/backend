package com.sparta.company.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 이 서비스는 Gateway가 이미 JWT를 검증한 뒤 X-User-Id/Username/Role 헤더로
 * 신원을 전달해주는 구조를 신뢰한다 (AuthHeaders 참고).
 *
 * 그래서 이 서비스 자체는 로그인 폼/세션/CSRF 같은 Spring Security의 기본 웹 보안 기능이
 * 필요 없고, 실제 인가(권한 체크)는 CompanyService/ProductService에서
 * AuthUser.role 값을 보고 수동으로 판단한다.
 *
 * spring-boot-starter-security 의존성만 추가하고 이 설정을 두지 않으면,
 * Spring Security가 기본값으로 모든 요청에 로그인을 요구하며 막아버리므로 반드시 필요하다.
 *
 * ⚠️ 만약 팀 컨벤션이 @PreAuthorize 기반 인가로 바뀌면, 이 필터체인에서
 *    커스텀 필터로 X-User-Role 헤더를 읽어 Authentication을 SecurityContext에 채워 넣는
 *    방식으로 확장해야 한다 (지금은 그렇게까지 하지 않고 서비스 계층에서 직접 체크).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
