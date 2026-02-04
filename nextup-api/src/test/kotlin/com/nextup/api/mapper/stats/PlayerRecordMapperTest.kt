package com.nextup.api.mapper.stats

import com.nextup.core.service.stats.dto.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("PlayerRecordMapper 테스트")
class PlayerRecordMapperTest {
    @Test
    fun `PlayerRecordDto를 PlayerRecordResponse로 변환한다`() {
        // given
        val dto =
            PlayerRecordDto(
                playerId = 1L,
                playerName = "홍길동",
                scope = RecordScope.CAREER,
                type = RecordType.ALL,
                year = null,
                competitionId = null,
                competitionName = null,
                battingStats =
                    BattingStatsDto(
                        gamesPlayed = 100,
                        plateAppearances = 450,
                        atBats = 400,
                        hits = 120,
                        doubles = 25,
                        triples = 3,
                        homeRuns = 15,
                        runs = 60,
                        runsBattedIn = 55,
                        walks = 40,
                        strikeouts = 80,
                        stolenBases = 10,
                        battingAverage = BigDecimal("0.300"),
                        onBasePercentage = BigDecimal("0.378"),
                        sluggingPercentage = BigDecimal("0.485"),
                        ops = BigDecimal("0.863"),
                    ),
                pitchingStats = null,
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.playerId).isEqualTo(1L)
        assertThat(response.playerName).isEqualTo("홍길동")
        assertThat(response.scope).isEqualTo(RecordScope.CAREER)
        assertThat(response.type).isEqualTo(RecordType.ALL)
        assertThat(response.battingStats).isNotNull
        assertThat(response.pitchingStats).isNull()
    }

    @Test
    fun `BattingStatsDto를 BattingStatsResponse로 변환한다`() {
        // given
        val dto =
            BattingStatsDto(
                gamesPlayed = 50,
                plateAppearances = 220,
                atBats = 200,
                hits = 60,
                doubles = 12,
                triples = 2,
                homeRuns = 8,
                runs = 30,
                runsBattedIn = 28,
                walks = 15,
                strikeouts = 40,
                stolenBases = 5,
                battingAverage = BigDecimal("0.300"),
                onBasePercentage = BigDecimal("0.364"),
                sluggingPercentage = BigDecimal("0.480"),
                ops = BigDecimal("0.844"),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gamesPlayed).isEqualTo(50)
        assertThat(response.plateAppearances).isEqualTo(220)
        assertThat(response.atBats).isEqualTo(200)
        assertThat(response.hits).isEqualTo(60)
        assertThat(response.doubles).isEqualTo(12)
        assertThat(response.triples).isEqualTo(2)
        assertThat(response.homeRuns).isEqualTo(8)
        assertThat(response.runs).isEqualTo(30)
        assertThat(response.runsBattedIn).isEqualTo(28)
        assertThat(response.walks).isEqualTo(15)
        assertThat(response.strikeouts).isEqualTo(40)
        assertThat(response.stolenBases).isEqualTo(5)
        assertThat(response.battingAverage).isEqualTo(BigDecimal("0.300"))
        assertThat(response.onBasePercentage).isEqualTo(BigDecimal("0.364"))
        assertThat(response.sluggingPercentage).isEqualTo(BigDecimal("0.480"))
        assertThat(response.ops).isEqualTo(BigDecimal("0.844"))
    }

    @Test
    fun `PitchingStatsDto를 PitchingStatsResponse로 변환한다`() {
        // given
        val dto =
            PitchingStatsDto(
                gamesPlayed = 30,
                gamesStarted = 25,
                inningsPitched = "160.2",
                wins = 10,
                losses = 5,
                saves = 0,
                holds = 0,
                earnedRuns = 45,
                hitsAllowed = 140,
                walksAllowed = 40,
                strikeouts = 130,
                homeRunsAllowed = 12,
                era = BigDecimal("2.52"),
                whip = BigDecimal("1.12"),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gamesPlayed).isEqualTo(30)
        assertThat(response.gamesStarted).isEqualTo(25)
        assertThat(response.inningsPitched).isEqualTo("160.2")
        assertThat(response.wins).isEqualTo(10)
        assertThat(response.losses).isEqualTo(5)
        assertThat(response.saves).isEqualTo(0)
        assertThat(response.holds).isEqualTo(0)
        assertThat(response.earnedRuns).isEqualTo(45)
        assertThat(response.hitsAllowed).isEqualTo(140)
        assertThat(response.walksAllowed).isEqualTo(40)
        assertThat(response.strikeouts).isEqualTo(130)
        assertThat(response.homeRunsAllowed).isEqualTo(12)
        assertThat(response.era).isEqualTo(BigDecimal("2.52"))
        assertThat(response.whip).isEqualTo(BigDecimal("1.12"))
    }

    @Test
    fun `투수 기록만 있는 PlayerRecordDto를 변환한다`() {
        // given
        val dto =
            PlayerRecordDto(
                playerId = 2L,
                playerName = "김투수",
                scope = RecordScope.SEASON,
                type = RecordType.PITCHING,
                year = 2026,
                competitionId = null,
                competitionName = null,
                battingStats = null,
                pitchingStats =
                    PitchingStatsDto(
                        gamesPlayed = 20,
                        gamesStarted = 18,
                        inningsPitched = "120.0",
                        wins = 8,
                        losses = 4,
                        saves = 0,
                        holds = 0,
                        earnedRuns = 35,
                        hitsAllowed = 100,
                        walksAllowed = 30,
                        strikeouts = 100,
                        homeRunsAllowed = 8,
                        era = BigDecimal("2.63"),
                        whip = BigDecimal("1.08"),
                    ),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.playerId).isEqualTo(2L)
        assertThat(response.scope).isEqualTo(RecordScope.SEASON)
        assertThat(response.type).isEqualTo(RecordType.PITCHING)
        assertThat(response.year).isEqualTo(2026)
        assertThat(response.battingStats).isNull()
        assertThat(response.pitchingStats).isNotNull
    }

    @Test
    fun `대회 범위의 PlayerRecordDto를 변환한다`() {
        // given
        val dto =
            PlayerRecordDto(
                playerId = 3L,
                playerName = "박선수",
                scope = RecordScope.COMPETITION,
                type = RecordType.BATTING,
                year = null,
                competitionId = 10L,
                competitionName = "봄리그",
                battingStats =
                    BattingStatsDto(
                        gamesPlayed = 15,
                        plateAppearances = 65,
                        atBats = 60,
                        hits = 20,
                        doubles = 4,
                        triples = 1,
                        homeRuns = 3,
                        runs = 10,
                        runsBattedIn = 12,
                        walks = 4,
                        strikeouts = 10,
                        stolenBases = 2,
                        battingAverage = BigDecimal("0.333"),
                        onBasePercentage = BigDecimal("0.385"),
                        sluggingPercentage = BigDecimal("0.550"),
                        ops = BigDecimal("0.935"),
                    ),
                pitchingStats = null,
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.scope).isEqualTo(RecordScope.COMPETITION)
        assertThat(response.competitionId).isEqualTo(10L)
        assertThat(response.competitionName).isEqualTo("봄리그")
    }
}
