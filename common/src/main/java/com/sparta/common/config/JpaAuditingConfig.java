package com.sparta.common.config;

import com.sparta.common.constant.AuthHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Auditing 공통 설정.
 *
 * scanBasePackages 에 "com.sparta" 가 포함된 서비스에서 자동으로 활성화되어
 * BaseEntity 의 created_by / updated_by 를 채운다.
 * 감사자는 게이트웨이가 전달한 {@code X-User-Id} 헤더(사용자 PK)에서 가져오며,
 * 요청 컨텍스트가 없거나 값이 유효하지 않으면 비워 둔다(컬럼 null).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.empty();
            }
            String userId = attributes.getRequest().getHeader(AuthHeaders.USER_ID);
            if (userId == null || userId.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(UUID.fromString(userId));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        };
    }
}
