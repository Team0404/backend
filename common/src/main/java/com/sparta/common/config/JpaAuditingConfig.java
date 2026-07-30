package com.sparta.common.config;

import com.sparta.common.constant.AuthHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * JPA Auditing 공통 설정.
 *
 * scanBasePackages 에 "com.sparta" 가 포함된 서비스에서 자동으로 활성화되어
 * BaseEntity 의 created_by / updated_by 를 채운다.
 * 감사자는 게이트웨이가 전달한 {@code X-Username} 헤더에서 가져오며, 없으면 "system".
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Optional.of("system");
            }
            String username = attributes.getRequest().getHeader(AuthHeaders.USERNAME);
            return Optional.ofNullable(username)
                    .filter(name -> !name.isBlank())
                    .or(() -> Optional.of("system"));
        };
    }
}
