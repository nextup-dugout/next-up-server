package com.nextup.infrastructure.security.filter

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val buckets =
        Caffeine
            .newBuilder()
            .maximumSize(MAX_BUCKET_SIZE)
            .expireAfterAccess(BUCKET_EXPIRE_MINUTES)
            .build<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val clientIp = getClientIp(request)
        val isAuthEndpoint = request.requestURI.startsWith("/api/auth")

        val bucket =
            buckets.get(clientKey(clientIp, isAuthEndpoint)) {
                createBucket(isAuthEndpoint)
            }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write(
                """{"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please try again later."}}""",
            )
        }
    }

    private fun createBucket(isAuthEndpoint: Boolean): Bucket {
        val limit =
            if (isAuthEndpoint) {
                Bandwidth
                    .builder()
                    .capacity(10)
                    .refillGreedy(10, Duration.ofMinutes(1))
                    .build()
            } else {
                Bandwidth
                    .builder()
                    .capacity(100)
                    .refillGreedy(100, Duration.ofMinutes(1))
                    .build()
            }
        return Bucket.builder().addLimit(limit).build()
    }

    internal fun getClientIp(request: HttpServletRequest): String = request.remoteAddr

    private fun clientKey(
        ip: String,
        isAuth: Boolean,
    ): String = if (isAuth) "auth:$ip" else "api:$ip"

    companion object {
        private const val MAX_BUCKET_SIZE = 100_000L
        private val BUCKET_EXPIRE_MINUTES = Duration.ofMinutes(5)
    }
}
