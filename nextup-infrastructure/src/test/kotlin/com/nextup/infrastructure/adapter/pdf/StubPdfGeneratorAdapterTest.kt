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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

@DisplayName("StubPdfGeneratorAdapter 테스트")
class StubPdfGeneratorAdapterTest {
    private val adapter = StubPdfGeneratorAdapter()

    @Test
    @DisplayName("모든 섹션이 포함된 PDF를 생성한다")
    fun `should generate PDF with all sections`() {
        // given
        val scoresheet = createScoresheetDto()

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("공식 기록지 (Official Scoresheet)")
        assertThat(content).contains("경기 정보")
        assertThat(content).contains("경기 결과")
        assertThat(content).contains("이닝별 점수")
        assertThat(content).contains("타격 기록")
        assertThat(content).contains("투수 기록")
        assertThat(content).contains("주요 이벤트")
    }

    @Test
    @DisplayName("경기 정보 섹션을 포함한다")
    fun `should include game info`() {
        // given
        val scoresheet = createScoresheetDto()

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("대회: 2024 서울시 야구 리그")
        assertThat(content).contains("장소: 잠실야구장 (메인필드)")
        assertThat(content).contains("상태: FINISHED")
    }

    @Test
    @DisplayName("팀 점수를 포함한다")
    fun `should include team scores`() {
        // given
        val scoresheet = createScoresheetDto()

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("원정팀")
        assertThat(content).contains("5점")
        assertThat(content).contains("홈팀")
        assertThat(content).contains("3점")
        assertThat(content).contains("승리")
        assertThat(content).contains("패배")
    }

    @Test
    @DisplayName("이닝별 점수를 포함한다")
    fun `should include inning scores`() {
        // given
        val scoresheet = createScoresheetDto()

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("이닝별 점수")
        assertThat(content).contains("원정")
        assertThat(content).contains("홈")
        assertThat(content).contains("R   H   E")
    }

    @Test
    @DisplayName("양 팀의 타격 기록을 포함한다")
    fun `should include batting records for both teams`() {
        // given
        val scoresheet = createScoresheetDto()

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("타격 기록 - 원정팀")
        assertThat(content).contains("타격 기록 - 홈팀")
        assertThat(content).contains("김철수")
        assertThat(content).contains("박영희")
        assertThat(content).contains("타석  타수  득점  안타")
        assertThat(content).contains(".333")
    }

    @Test
    @DisplayName("양 팀의 투수 기록을 포함한다")
    fun `should include pitching records for both teams`() {
        // given
        val scoresheet = createScoresheetDto()

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("투수 기록 - 원정팀")
        assertThat(content).contains("투수 기록 - 홈팀")
        assertThat(content).contains("이승현")
        assertThat(content).contains("최민수")
        assertThat(content).contains("이닝  피안타  실점")
        assertThat(content).contains("승리")
        assertThat(content).contains("패배")
        assertThat(content).contains("2.50")
    }

    @Test
    @DisplayName("주요 이벤트를 포함한다")
    fun `should include key events`() {
        // given
        val scoresheet = createScoresheetDto()

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("주요 이벤트")
        assertThat(content).contains("김철수 홈런")
        assertThat(content).contains("박영희 2루타")
    }

    @Test
    @DisplayName("location과 fieldName이 null일 때 처리한다")
    fun `should handle null location and fieldName`() {
        // given
        val scoresheet =
            createScoresheetDto(
                location = null,
                fieldName = null,
            )

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("장소: 미정")
    }

    @Test
    @DisplayName("타격 및 투수 기록이 비어있을 때 처리한다")
    fun `should handle empty batting and pitching records`() {
        // given
        val scoresheet =
            createScoresheetDto(
                battingRecords =
                    BattingRecordsDto(
                        home = emptyList(),
                        away = emptyList(),
                    ),
                pitchingRecords =
                    PitchingRecordsDto(
                        home = emptyList(),
                        away = emptyList(),
                    ),
            )

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("타격 기록")
        assertThat(content).contains("투수 기록")
        // 선수 이름이 없어야 함
        assertThat(content).doesNotContain("김철수")
        assertThat(content).doesNotContain("이승현")
    }

    @Test
    @DisplayName("주요 이벤트가 비어있을 때 처리한다")
    fun `should handle empty key events`() {
        // given
        val scoresheet =
            createScoresheetDto(
                keyEvents = emptyList(),
            )

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        // 주요 이벤트 섹션이 없어야 함
        assertThat(content).doesNotContain("주요 이벤트")
    }

    @Test
    @DisplayName("투수 decision이 null일 때 처리한다")
    fun `should handle null pitcher decision`() {
        // given
        val pitchingRecords =
            PitchingRecordsDto(
                home =
                    listOf(
                        createPitcherDto(
                            name = "투수A",
                            decision = null,
                        ),
                    ),
                away =
                    listOf(
                        createPitcherDto(
                            name = "투수B",
                            decision = null,
                        ),
                    ),
            )
        val scoresheet = createScoresheetDto(pitchingRecords = pitchingRecords)

        // when
        val result = adapter.generateScoresheetPdf(scoresheet)

        // then
        val content = String(result, StandardCharsets.UTF_8)
        assertThat(content).contains("투수A")
        assertThat(content).contains("투수B")
        assertThat(content).contains("-") // decision null은 "-"로 표시
    }

    // Helper Methods

    private fun createScoresheetDto(
        location: String? = "잠실야구장",
        fieldName: String? = "메인필드",
        battingRecords: BattingRecordsDto = createBattingRecords(),
        pitchingRecords: PitchingRecordsDto = createPitchingRecords(),
        keyEvents: List<KeyEventDto> = createKeyEvents(),
    ): ScoresheetDto =
        ScoresheetDto(
            gameInfo =
                GameInfoDto(
                    gameId = 1L,
                    competitionName = "2024 서울시 야구 리그",
                    gameNumber = 101,
                    scheduledAt = LocalDateTime.of(2024, 5, 15, 14, 0),
                    startedAt = LocalDateTime.of(2024, 5, 15, 14, 5),
                    endedAt = LocalDateTime.of(2024, 5, 15, 17, 30),
                    location = location,
                    fieldName = fieldName,
                    status = "FINISHED",
                    currentInning = "9",
                    totalInnings = 9,
                ),
            teams =
                TeamsScoresheetDto(
                    home =
                        TeamScoresheetInfoDto(
                            teamId = 1L,
                            teamName = "홈팀",
                            totalScore = 3,
                            totalHits = 7,
                            totalErrors = 1,
                            result = "패배",
                        ),
                    away =
                        TeamScoresheetInfoDto(
                            teamId = 2L,
                            teamName = "원정팀",
                            totalScore = 5,
                            totalHits = 10,
                            totalErrors = 0,
                            result = "승리",
                        ),
                ),
            inningScores =
                InningScoresDto(
                    innings = 9,
                    homeScores = listOf(0, 1, 0, 0, 2, 0, 0, 0, 0),
                    awayScores = listOf(1, 0, 2, 0, 1, 0, 1, 0, 0),
                ),
            battingRecords = battingRecords,
            pitchingRecords = pitchingRecords,
            keyEvents = keyEvents,
        )

    private fun createBattingRecords(): BattingRecordsDto =
        BattingRecordsDto(
            home =
                listOf(
                    BatterScoresheetDto(
                        playerId = 101L,
                        name = "박영희",
                        backNumber = 7,
                        position = "CF",
                        battingOrder = 1,
                        plateAppearances = 4,
                        atBats = 3,
                        runs = 1,
                        hits = 2,
                        doubles = 1,
                        triples = 0,
                        homeRuns = 0,
                        rbis = 1,
                        walks = 1,
                        strikeouts = 0,
                        stolenBases = 1,
                        avg = ".333",
                    ),
                ),
            away =
                listOf(
                    BatterScoresheetDto(
                        playerId = 201L,
                        name = "김철수",
                        backNumber = 10,
                        position = "1B",
                        battingOrder = 3,
                        plateAppearances = 5,
                        atBats = 4,
                        runs = 2,
                        hits = 3,
                        doubles = 0,
                        triples = 0,
                        homeRuns = 1,
                        rbis = 3,
                        walks = 1,
                        strikeouts = 1,
                        stolenBases = 0,
                        avg = ".333",
                    ),
                ),
        )

    private fun createPitchingRecords(): PitchingRecordsDto =
        PitchingRecordsDto(
            home =
                listOf(
                    PitcherScoresheetDto(
                        playerId = 102L,
                        name = "최민수",
                        backNumber = 1,
                        isStartingPitcher = true,
                        inningsPitched = "6.0",
                        hitsAllowed = 8,
                        runsAllowed = 4,
                        earnedRuns = 4,
                        walks = 2,
                        strikeouts = 5,
                        homeRunsAllowed = 1,
                        decision = "패배",
                        era = "4.50",
                    ),
                ),
            away =
                listOf(
                    PitcherScoresheetDto(
                        playerId = 202L,
                        name = "이승현",
                        backNumber = 11,
                        isStartingPitcher = true,
                        inningsPitched = "7.0",
                        hitsAllowed = 6,
                        runsAllowed = 2,
                        earnedRuns = 2,
                        walks = 1,
                        strikeouts = 8,
                        homeRunsAllowed = 0,
                        decision = "승리",
                        era = "2.50",
                    ),
                ),
        )

    private fun createKeyEvents(): List<KeyEventDto> =
        listOf(
            KeyEventDto(
                inning = "1",
                description = "김철수 홈런",
                timestamp = "14:12",
            ),
            KeyEventDto(
                inning = "3",
                description = "박영희 2루타",
                timestamp = "14:45",
            ),
        )

    private fun createPitcherDto(
        name: String,
        decision: String?,
    ): PitcherScoresheetDto =
        PitcherScoresheetDto(
            playerId = 999L,
            name = name,
            backNumber = 99,
            isStartingPitcher = true,
            inningsPitched = "5.0",
            hitsAllowed = 5,
            runsAllowed = 2,
            earnedRuns = 2,
            walks = 1,
            strikeouts = 4,
            homeRunsAllowed = 0,
            decision = decision,
            era = "3.60",
        )
}
