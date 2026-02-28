package com.nextup.api.config

import com.nextup.infrastructure.security.filter.RateLimitFilter
import com.nextup.infrastructure.security.handler.CustomAccessDeniedHandler
import com.nextup.infrastructure.security.handler.CustomAuthenticationEntryPoint
import com.nextup.infrastructure.security.jwt.JwtAuthenticationFilter
import com.nextup.infrastructure.security.jwt.JwtProperties
import com.nextup.infrastructure.security.oauth2.CustomOAuth2UserService
import com.nextup.infrastructure.security.oauth2.OAuth2AuthenticationFailureHandler
import com.nextup.infrastructure.security.oauth2.OAuth2AuthenticationSuccessHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 설정
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
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,
    @Value("\${springdoc.swagger-ui.enabled:true}")
    private val swaggerEnabled: Boolean,
    @Value("\${app.cors.allowed-origins:http://localhost:3000}")
    private val allowedOrigins: String,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/**")
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
            }.authorizeHttpRequests { auth ->
                auth
                    // Public auth endpoints (explicit paths instead of wildcard)
                    .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/oauth2/token",
                    ).permitAll()
                    // Authenticated auth endpoints
                    .requestMatchers(
                        "/api/auth/me",
                        "/api/auth/logout",
                        "/api/auth/logout-all",
                    ).authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/associations/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/stats/**")
                    .permitAll()
                    // OAuth2 endpoints
                    .requestMatchers("/oauth2/**")
                    .permitAll()
                    .requestMatchers("/login/oauth2/**")
                    .permitAll()
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
                    // Actuator endpoints
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    // Admin endpoints
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    // Scorer endpoints (for game recording)
                    .requestMatchers("/api/games/*/records/**")
                    .hasAnyRole("SCORER", "ADMIN")
                    // All other endpoints require authentication
                    .anyRequest()
                    .authenticated()
            }.oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { it.userService(customOAuth2UserService) }
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler)
            }.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

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
