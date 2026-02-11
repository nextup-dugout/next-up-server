package com.nextup.scorer.config

import com.nextup.infrastructure.security.filter.RateLimitFilter
import com.nextup.infrastructure.security.handler.CustomAccessDeniedHandler
import com.nextup.infrastructure.security.handler.CustomAuthenticationEntryPoint
import com.nextup.infrastructure.security.jwt.JwtAuthenticationFilter
import com.nextup.infrastructure.security.jwt.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter

/**
 * Scorer Security 설정
 *
 * 기록원 전용 모듈로, SCORER 또는 ADMIN 역할이 필요합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/**", "/ws/**")
            }.headers { headers ->
                headers.frameOptions { it.deny() }
                headers.contentTypeOptions { }
                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                    hsts.maxAgeInSeconds(31536000)
                }
                headers.xssProtection { it.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK) }
            }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(customAuthenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Health check
                    .requestMatchers("/health", "/actuator/health").permitAll()

                    // Swagger/OpenAPI
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                    ).permitAll()

                    // WebSocket endpoints (for real-time scoreboard)
                    .requestMatchers("/ws/**").permitAll()

                    // Scorer API endpoints
                    .requestMatchers("/api/scorer/**").hasAnyRole("SCORER", "ADMIN")

                    // League management in scorer module
                    .requestMatchers("/api/leagues/**").hasAnyRole("SCORER", "ADMIN")

                    // All other endpoints require authentication with SCORER or ADMIN role
                    .anyRequest().hasAnyRole("SCORER", "ADMIN")
            }
            .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
