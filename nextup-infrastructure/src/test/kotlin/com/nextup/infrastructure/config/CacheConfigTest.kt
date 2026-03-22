package com.nextup.infrastructure.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import org.springframework.cache.support.SimpleCacheManager

@DisplayName("CacheConfig")
class CacheConfigTest {

    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setUp() {
        val config = CacheConfig()
        val manager = config.cacheManager() as SimpleCacheManager
        manager.afterPropertiesSet()
        cacheManager = manager
    }

    @Nested
    @DisplayName("cacheManager()")
    inner class CacheManagerTest {

        @Test
        @DisplayName("모든 캐시 이름이 등록되어 있어야 한다")
        fun shouldRegisterAllCacheNames() {
            assertThat(cacheManager.cacheNames).containsExactlyInAnyOrder(
                CacheConfig.STANDINGS_CACHE,
                CacheConfig.LEADERBOARD_CACHE,
                CacheConfig.TEAM_STATS_CACHE,
                CacheConfig.COMPETITION_INFO_CACHE,
                CacheConfig.LEAGUE_INFO_CACHE,
                CacheConfig.TEAM_INFO_CACHE,
                CacheConfig.COMPLETED_BOX_SCORE_CACHE,
            )
        }

        @Test
        @DisplayName("standings 캐시가 존재해야 한다")
        fun shouldHaveStandingsCache() {
            assertThat(cacheManager.getCache(CacheConfig.STANDINGS_CACHE)).isNotNull
        }

        @Test
        @DisplayName("leaderboard 캐시가 존재해야 한다")
        fun shouldHaveLeaderboardCache() {
            assertThat(cacheManager.getCache(CacheConfig.LEADERBOARD_CACHE)).isNotNull
        }

        @Test
        @DisplayName("teamStats 캐시가 존재해야 한다")
        fun shouldHaveTeamStatsCache() {
            assertThat(cacheManager.getCache(CacheConfig.TEAM_STATS_CACHE)).isNotNull
        }

        @Test
        @DisplayName("competitionInfo 캐시가 존재해야 한다")
        fun shouldHaveCompetitionInfoCache() {
            assertThat(cacheManager.getCache(CacheConfig.COMPETITION_INFO_CACHE)).isNotNull
        }

        @Test
        @DisplayName("leagueInfo 캐시가 존재해야 한다")
        fun shouldHaveLeagueInfoCache() {
            assertThat(cacheManager.getCache(CacheConfig.LEAGUE_INFO_CACHE)).isNotNull
        }

        @Test
        @DisplayName("teamInfo 캐시가 존재해야 한다")
        fun shouldHaveTeamInfoCache() {
            assertThat(cacheManager.getCache(CacheConfig.TEAM_INFO_CACHE)).isNotNull
        }

        @Test
        @DisplayName("completedBoxScore 캐시가 존재해야 한다")
        fun shouldHaveCompletedBoxScoreCache() {
            assertThat(cacheManager.getCache(CacheConfig.COMPLETED_BOX_SCORE_CACHE)).isNotNull
        }

        @Test
        @DisplayName("캐시에 값을 넣고 꺼낼 수 있어야 한다")
        fun shouldPutAndGetCacheValue() {
            val cache = cacheManager.getCache(CacheConfig.STANDINGS_CACHE)!!

            cache.put("key1", "value1")
            assertThat(cache.get("key1")?.get()).isEqualTo("value1")
        }

        @Test
        @DisplayName("캐시 evict 후 값이 제거되어야 한다")
        fun shouldEvictCacheValue() {
            val cache = cacheManager.getCache(CacheConfig.STANDINGS_CACHE)!!

            cache.put("key1", "value1")
            cache.evict("key1")
            assertThat(cache.get("key1")).isNull()
        }

        @Test
        @DisplayName("캐시 clear 후 모든 값이 제거되어야 한다")
        fun shouldClearAllValues() {
            val cache = cacheManager.getCache(CacheConfig.LEADERBOARD_CACHE)!!

            cache.put("key1", "value1")
            cache.put("key2", "value2")
            cache.clear()
            assertThat(cache.get("key1")).isNull()
            assertThat(cache.get("key2")).isNull()
        }
    }
}
