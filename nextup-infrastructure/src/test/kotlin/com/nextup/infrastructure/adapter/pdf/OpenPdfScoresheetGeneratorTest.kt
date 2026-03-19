package com.nextup.infrastructure.adapter.pdf

import com.nextup.core.service.game.dto.BatterScoresheetDto
import com.nextup.core.service.game.dto.BattingRecordsDto
import com.nextup.core.service.game.dto.GameInfoDto
import com.nextup.core.service.game.dto.InningScoresDto
import com.nextup.core.service.game.dto.KeyEventDto
import com.nextup.core.service.game.dto.PitcherScoresheetDto
import com.nextup.core.service.game.dto.PitchingRecordsDto
import com.nextup.core.service.game.dto.ScoresheetDto
import com.nextup.core.service.game.dto.TeamScoresheetInfoDto
import com.nextup.core.service.game.dto.TeamsScoresheetDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OpenPdfScoresheetGeneratorTest {
    private val generator = OpenPdfScoresheetGenerator()

    @Test
    fun `PDF 바이트 배열이 유효한 PDF 헤더로 시작한다`() {
        val scoresheet = createTestScoresheet()

        val result = generator.generateScoresheetPdf(scoresheet)

        assertThat(result).isNotEmpty()
        val header = String(result.copyOfRange(0, 5))
        assertThat(header).isEqualTo("%PDF-")
    }

    @Test
    fun `빈 타격 및 투구 기록으로도 PDF가 정상 생성된다`() {
        val scoresheet =
            createTestScoresheet(
                batters = emptyList(),
                pitchers = emptyList(),
            )

        val result = generator.generateScoresheetPdf(scoresheet)

        assertThat(result).isNotEmpty()
        val header = String(result.copyOfRange(0, 5))
        assertThat(header).isEqualTo("%PDF-")
    }

    @Test
    fun `주요 이벤트가 없어도 PDF가 정상 생성된다`() {
        val scoresheet = createTestScoresheet(keyEvents = emptyList())

        val result = generator.generateScoresheetPdf(scoresheet)

        assertThat(result).isNotEmpty()
    }

    @Test
    fun `여러 타자와 투수 기록이 포함된 PDF가 정상 생성된다`() {
        val batters =
            listOf(
                createTestBatter("Player A", "SS", 1),
                createTestBatter("Player B", "CF", 2),
                createTestBatter("Player C", "1B", 3),
            )
        val pitchers =
            listOf(
                createTestPitcher("Pitcher X", "W"),
                createTestPitcher("Pitcher Y", null),
            )
        val scoresheet = createTestScoresheet(batters = batters, pitchers = pitchers)

        val result = generator.generateScoresheetPdf(scoresheet)

        assertThat(result).isNotEmpty()
        assertThat(result.size).isGreaterThan(100)
    }

    @Test
    fun `동일한 입력에 대해 동일한 PDF를 생성한다`() {
        val scoresheet = createTestScoresheet()

        val result1 = generator.generateScoresheetPdf(scoresheet)
        val result2 = generator.generateScoresheetPdf(scoresheet)

        assertThat(result1.size).isEqualTo(result2.size)
    }

    private fun createTestScoresheet(
        batters: List<BatterScoresheetDto> = listOf(createTestBatter("Test Player", "SS", 1)),
        pitchers: List<PitcherScoresheetDto> = listOf(createTestPitcher("Test Pitcher", "W")),
        keyEvents: List<KeyEventDto> = listOf(KeyEventDto("1T", "Strikeout", "12:00")),
    ): ScoresheetDto =
        ScoresheetDto(
            gameInfo =
                GameInfoDto(
                    gameId = 1L,
                    competitionName = "Test League",
                    gameNumber = 1,
                    scheduledAt = LocalDateTime.of(2026, 3, 10, 14, 0),
                    startedAt = LocalDateTime.of(2026, 3, 10, 14, 5),
                    endedAt = LocalDateTime.of(2026, 3, 10, 16, 30),
                    location = "Seoul Stadium",
                    fieldName = "Main Field",
                    status = "FINALIZED",
                    currentInning = "9B",
                    totalInnings = 9,
                ),
            teams =
                TeamsScoresheetDto(
                    home =
                        TeamScoresheetInfoDto(
                            teamId = 1L,
                            teamName = "Home Team",
                            logoUrl = null,
                            totalScore = 5,
                            totalHits = 8,
                            totalErrors = 1,
                            result = "WIN",
                        ),
                    away =
                        TeamScoresheetInfoDto(
                            teamId = 2L,
                            teamName = "Away Team",
                            logoUrl = null,
                            totalScore = 3,
                            totalHits = 6,
                            totalErrors = 2,
                            result = "LOSS",
                        ),
                ),
            inningScores =
                InningScoresDto(
                    innings = 9,
                    homeScores = listOf(0, 1, 0, 2, 0, 0, 1, 1, 0),
                    awayScores = listOf(1, 0, 0, 0, 2, 0, 0, 0, 0),
                ),
            battingRecords =
                BattingRecordsDto(
                    home = batters,
                    away = batters,
                ),
            pitchingRecords =
                PitchingRecordsDto(
                    home = pitchers,
                    away = pitchers,
                ),
            keyEvents = keyEvents,
        )

    private fun createTestBatter(
        name: String,
        position: String,
        order: Int,
    ): BatterScoresheetDto =
        BatterScoresheetDto(
            playerId = order.toLong(),
            name = name,
            backNumber = order * 10,
            position = position,
            battingOrder = order,
            plateAppearances = 4,
            atBats = 3,
            runs = 1,
            hits = 1,
            doubles = 0,
            triples = 0,
            homeRuns = 0,
            rbis = 1,
            walks = 1,
            strikeouts = 0,
            stolenBases = 0,
            avg = ".333",
        )

    private fun createTestPitcher(
        name: String,
        decision: String?,
    ): PitcherScoresheetDto =
        PitcherScoresheetDto(
            playerId = 10L,
            name = name,
            backNumber = 18,
            isStartingPitcher = decision == "W",
            inningsPitched = "6.0",
            hitsAllowed = 6,
            runsAllowed = 3,
            earnedRuns = 2,
            walks = 2,
            strikeouts = 5,
            homeRunsAllowed = 1,
            decision = decision,
            era = "3.00",
        )
}
