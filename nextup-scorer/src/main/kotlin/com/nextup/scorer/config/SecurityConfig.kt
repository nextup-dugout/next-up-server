package com.nextup.scorer.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // 헬스체크는 인증 없이 접근 가능
                auth.requestMatchers("/health", "/actuator/health").permitAll()

                // WebSocket 엔드포인트
                auth.requestMatchers("/ws/**").permitAll()

                // TODO: JWT 인증 구현 후 SCORER 역할 필수로 변경
                // 현재는 개발 편의를 위해 모든 요청 허용
                auth.anyRequest().permitAll()

                // 추후 적용할 권한 설정:
                // auth.requestMatchers("/api/scorer/**").hasRole("SCORER")
                // auth.requestMatchers("/ws/scoreboard/**").hasRole("SCORER")
                // auth.anyRequest().authenticated()
            }

        return http.build()
    }
}
