package com.nextup.backoffice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Backoffice Security 설정 (임시 - 모든 요청 허용)
 *
 * TODO: JWT 인증 및 권한 관리 구현 필요
 * 관리자 전용 모듈로, 기본적으로 인증이 필요합니다.
 * 역할별 권한: ADMIN, ASSOCIATION_ADMIN, LEAGUE_ADMIN, TEAM_MANAGER
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .build()
    }
}
