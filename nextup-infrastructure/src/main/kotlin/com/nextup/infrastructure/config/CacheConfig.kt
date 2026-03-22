package com.nextup.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * Spring Cache 설정
 *
 * Caffeine 기반 인메모리 캐시를 캐시별 TTL로 구성합니다.
 *
 * | 캐시명           | TTL   | 최대 크기 | 용도                           |
 * |-----------------|-------|----------|-------------------------------|
 * | standings       | 5분   | 200      | 대회 순위표                     |
 * | leaderboard     | 5분   | 500      | 개인 타이틀 리더보드              |
 * | teamStats       | 10분  | 500      | 팀 통계                        |
 * | competitionInfo | 30분  | 200      | 대회 기본 정보 (목록/상세)        |
 * | leagueInfo      | 30분  | 100      | 리그 기본 정보 (목록/상세)        |
 * | teamInfo        | 30분  | 300      | 팀 기본 정보 (팀+리그)            |
 * | completedBoxScore | 무한 | 500(LRU) | 완료 경기 박스스코어 (불변 데이터) |
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val STANDINGS_CACHE = "standings"
        const val LEADERBOARD_CACHE = "leaderboard"
        const val TEAM_STATS_CACHE = "teamStats"
        const val COMPETITION_INFO_CACHE = "competitionInfo"
        const val LEAGUE_INFO_CACHE = "leagueInfo"
        const val TEAM_INFO_CACHE = "teamInfo"
        const val COMPLETED_BOX_SCORE_CACHE = "completedBoxScore"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val caches =
            listOf(
                buildCache(STANDINGS_CACHE, 5, TimeUnit.MINUTES, 200),
                buildCache(LEADERBOARD_CACHE, 5, TimeUnit.MINUTES, 500),
                buildCache(TEAM_STATS_CACHE, 10, TimeUnit.MINUTES, 500),
                buildCache(COMPETITION_INFO_CACHE, 30, TimeUnit.MINUTES, 200),
                buildCache(LEAGUE_INFO_CACHE, 30, TimeUnit.MINUTES, 100),
                buildCache(TEAM_INFO_CACHE, 30, TimeUnit.MINUTES, 300),
                buildLruCache(COMPLETED_BOX_SCORE_CACHE, 500),
            )
        val manager = SimpleCacheManager()
        manager.setCaches(caches)
        return manager
    }

    private fun buildCache(
        name: String,
        ttl: Long,
        unit: TimeUnit,
        maxSize: Long,
    ): CaffeineCache =
        CaffeineCache(
            name,
            Caffeine.newBuilder()
                .expireAfterWrite(ttl, unit)
                .maximumSize(maxSize)
                .build(),
        )

    /**
     * TTL 없는 LRU 캐시를 생성합니다.
     *
     * 완료된 경기 박스스코어 등 불변 데이터에 적합합니다.
     * 캐시 크기 제한(maximumSize)만으로 메모리를 관리합니다.
     */
    private fun buildLruCache(
        name: String,
        maxSize: Long,
    ): CaffeineCache =
        CaffeineCache(
            name,
            Caffeine.newBuilder()
                .maximumSize(maxSize)
                .build(),
        )
}
