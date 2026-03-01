package com.nextup.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * Spring Cache 설정
 *
 * Caffeine 기반 인메모리 캐시를 구성합니다.
 * - standings: 대회 순위표 캐시 (10분 TTL)
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val STANDINGS_CACHE = "standings"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager(STANDINGS_CACHE)
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(200),
        )
        return cacheManager
    }
}
