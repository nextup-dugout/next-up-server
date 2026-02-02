package com.nextup.backoffice.config

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

/**
 * Backoffice Security 설정
 *
 * 관리자 전용 모듈로, 기본적으로 ADMIN 역할이 필요합니다.
 * 역할별 권한: ADMIN, ASSOCIATION_ADMIN, LEAGUE_ADMIN, TEAM_MANAGER
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(customAuthenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Health check
                    .requestMatchers("/actuator/health").permitAll()

                    // Swagger/OpenAPI
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                    ).permitAll()

                    // All other endpoints require ADMIN role
                    .anyRequest().hasRole("ADMIN")
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
