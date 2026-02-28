package com.nextup.scorer.config

import com.nextup.infrastructure.security.filter.RateLimitFilter
import com.nextup.infrastructure.security.handler.CustomAccessDeniedHandler
import com.nextup.infrastructure.security.handler.CustomAuthenticationEntryPoint
import com.nextup.infrastructure.security.jwt.JwtAuthenticationFilter
import com.nextup.infrastructure.security.jwt.JwtProperties
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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
    @Value("\${springdoc.swagger-ui.enabled:true}")
    private val swaggerEnabled: Boolean,
    @Value("\${app.cors.allowed-origins:\${app.websocket.allowed-origins:http://localhost:3000}}")
    private val allowedOrigins: String,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .cors { it.configurationSource(corsConfigurationSource()) }
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
                    // Swagger/OpenAPI - 프로덕션 환경에서는 비활성화
                    .let { registry ->
                        if (swaggerEnabled) {
                            registry.requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                            ).permitAll()
                        } else {
                            registry
                        }
                    }
                    // WebSocket endpoints - HTTP 핸드셰이크 허용 (STOMP CONNECT에서 JWT 검증)
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

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
