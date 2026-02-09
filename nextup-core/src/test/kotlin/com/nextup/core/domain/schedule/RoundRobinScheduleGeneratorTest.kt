package com.nextup.core.domain.schedule

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class RoundRobinScheduleGeneratorTest {
    private val generator = RoundRobinScheduleGenerator()

    @Test
    fun `2개 팀 - 1라운드, 1경기`() {
        // given
        val teamIds = listOf(1L, 2L)

        // when
        val matches = generator.generate(teamIds)

        // then
        assertThat(matches).hasSize(1)
        assertThat(matches[0].round).isEqualTo(1)

        // 모든 팀이 한 번씩 대결
        val allTeams = matches.flatMap { listOf(it.homeTeamId, it.awayTeamId) }
        assertThat(allTeams).containsExactlyInAnyOrder(1L, 2L)
    }

    @Test
    fun `3개 팀(홀수) - 3라운드, 3경기`() {
        // given
        val teamIds = listOf(1L, 2L, 3L)

        // when
        val matches = generator.generate(teamIds)

        // then
        assertThat(matches).hasSize(3) // 3C2 = 3 matches

        // 3라운드
        assertThat(matches.map { it.round }.distinct()).containsExactlyInAnyOrder(1, 2, 3)

        // 각 팀이 정확히 2번씩 경기 (모든 상대와 1번씩)
        val teamMatchCounts = mutableMapOf<Long, Int>()
        matches.forEach { match ->
            teamMatchCounts[match.homeTeamId] = teamMatchCounts.getOrDefault(match.homeTeamId, 0) + 1
            teamMatchCounts[match.awayTeamId] = teamMatchCounts.getOrDefault(match.awayTeamId, 0) + 1
        }
        assertThat(teamMatchCounts.values).allMatch { it == 2 }

        // 모든 팀 조합이 정확히 한 번씩 경기
        assertAllTeamsPlayOnce(teamIds, matches)
    }

    @Test
    fun `4개 팀 - 3라운드, 6경기`() {
        // given
        val teamIds = listOf(1L, 2L, 3L, 4L)

        // when
        val matches = generator.generate(teamIds)

        // then
        assertThat(matches).hasSize(6) // 4C2 = 6 matches

        // 3라운드
        assertThat(matches.map { it.round }.distinct()).containsExactlyInAnyOrder(1, 2, 3)

        // 각 라운드마다 2경기씩
        matches.groupBy { it.round }.forEach { (_, roundMatches) ->
            assertThat(roundMatches).hasSize(2)
        }

        // 각 팀이 정확히 3번씩 경기 (모든 상대와 1번씩)
        val teamMatchCounts = mutableMapOf<Long, Int>()
        matches.forEach { match ->
            teamMatchCounts[match.homeTeamId] = teamMatchCounts.getOrDefault(match.homeTeamId, 0) + 1
            teamMatchCounts[match.awayTeamId] = teamMatchCounts.getOrDefault(match.awayTeamId, 0) + 1
        }
        assertThat(teamMatchCounts.values).allMatch { it == 3 }

        // 모든 팀 조합이 정확히 한 번씩 경기
        assertAllTeamsPlayOnce(teamIds, matches)
    }

    @Test
    fun `6개 팀 - 5라운드, 15경기`() {
        // given
        val teamIds = listOf(1L, 2L, 3L, 4L, 5L, 6L)

        // when
        val matches = generator.generate(teamIds)

        // then
        assertThat(matches).hasSize(15) // 6C2 = 15 matches

        // 5라운드
        assertThat(matches.map { it.round }.distinct()).containsExactlyInAnyOrder(1, 2, 3, 4, 5)

        // 각 라운드마다 3경기씩
        matches.groupBy { it.round }.forEach { (_, roundMatches) ->
            assertThat(roundMatches).hasSize(3)
        }

        // 각 팀이 정확히 5번씩 경기 (모든 상대와 1번씩)
        val teamMatchCounts = mutableMapOf<Long, Int>()
        matches.forEach { match ->
            teamMatchCounts[match.homeTeamId] = teamMatchCounts.getOrDefault(match.homeTeamId, 0) + 1
            teamMatchCounts[match.awayTeamId] = teamMatchCounts.getOrDefault(match.awayTeamId, 0) + 1
        }
        assertThat(teamMatchCounts.values).allMatch { it == 5 }

        // 모든 팀 조합이 정확히 한 번씩 경기
        assertAllTeamsPlayOnce(teamIds, matches)
    }

    @Test
    fun `더블 라운드 로빈 - 경기 수 2배`() {
        // given
        val teamIds = listOf(1L, 2L, 3L, 4L)

        // when
        val singleRound = generator.generate(teamIds, doubleRoundRobin = false)
        val doubleRound = generator.generate(teamIds, doubleRoundRobin = true)

        // then
        assertThat(doubleRound).hasSize(singleRound.size * 2)

        // 각 팀이 정확히 6번씩 경기 (모든 상대와 2번씩: 홈1, 원정1)
        val teamMatchCounts = mutableMapOf<Long, Int>()
        doubleRound.forEach { match ->
            teamMatchCounts[match.homeTeamId] = teamMatchCounts.getOrDefault(match.homeTeamId, 0) + 1
            teamMatchCounts[match.awayTeamId] = teamMatchCounts.getOrDefault(match.awayTeamId, 0) + 1
        }
        assertThat(teamMatchCounts.values).allMatch { it == 6 }
    }

    @Test
    fun `더블 라운드 로빈 - 홈과 원정 교대`() {
        // given
        val teamIds = listOf(1L, 2L)

        // when
        val matches = generator.generate(teamIds, doubleRoundRobin = true)

        // then
        assertThat(matches).hasSize(2)

        // 첫 번째 경기와 두 번째 경기가 홈/원정 교대
        val firstMatch = matches[0]
        val secondMatch = matches[1]

        assertThat(firstMatch.homeTeamId).isEqualTo(secondMatch.awayTeamId)
        assertThat(firstMatch.awayTeamId).isEqualTo(secondMatch.homeTeamId)
    }

    @Test
    fun `모든 팀이 서로 한 번씩 경기`() {
        // given
        val teamIds = listOf(1L, 2L, 3L, 4L, 5L)

        // when
        val matches = generator.generate(teamIds)

        // then
        assertAllTeamsPlayOnce(teamIds, matches)
    }

    @Test
    fun `각 라운드에서 팀은 중복 출전하지 않음`() {
        // given
        val teamIds = listOf(1L, 2L, 3L, 4L, 5L, 6L)

        // when
        val matches = generator.generate(teamIds)

        // then
        matches.groupBy { it.round }.forEach { (round, roundMatches) ->
            val teamsInRound = roundMatches.flatMap { listOf(it.homeTeamId, it.awayTeamId) }
            // 각 라운드에서 팀은 정확히 한 번씩만 출전
            assertThat(teamsInRound).hasSize(teamsInRound.distinct().size)
                .withFailMessage("라운드 $round 에서 팀이 중복 출전했습니다")
        }
    }

    @Test
    fun `중복 팀 ID - 예외 발생`() {
        // given
        val teamIds = listOf(1L, 2L, 1L, 3L) // 1L 중복

        // when & then
        assertThatThrownBy {
            generator.generate(teamIds)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("중복된 팀이 있습니다")
    }

    @Test
    fun `1개 팀 - 예외 발생`() {
        // given
        val teamIds = listOf(1L)

        // when & then
        assertThatThrownBy {
            generator.generate(teamIds)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("최소 2개 팀이 필요합니다")
    }

    @Test
    fun `빈 팀 목록 - 예외 발생`() {
        // given
        val teamIds = emptyList<Long>()

        // when & then
        assertThatThrownBy {
            generator.generate(teamIds)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("최소 2개 팀이 필요합니다")
    }

    /**
     * 모든 팀이 서로 정확히 한 번씩 경기했는지 검증
     */
    private fun assertAllTeamsPlayOnce(
        teamIds: List<Long>,
        matches: List<RoundRobinScheduleGenerator.MatchPair>,
    ) {
        val matchPairSet = mutableSetOf<Set<Long>>()

        matches.forEach { match ->
            val pair = setOf(match.homeTeamId, match.awayTeamId)
            assertThat(matchPairSet).doesNotContain(pair)
                .withFailMessage("팀 조합 $pair 가 중복 경기했습니다")
            matchPairSet.add(pair)
        }

        // 전체 경기 수 = nC2
        val expectedMatches = teamIds.size * (teamIds.size - 1) / 2
        assertThat(matches).hasSize(expectedMatches)
    }
}
