package com.nextup.api.controller.game

import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.port.PdfGeneratorPort
import com.nextup.core.service.game.ScoresheetService
import com.nextup.core.service.game.dto.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class ScoresheetControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var scoresheetService: ScoresheetService
    private lateinit var pdfGenerator: PdfGeneratorPort

    @BeforeEach
    fun setUp() {
        scoresheetService = mockk()
        pdfGenerator = mockk()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(ScoresheetController(scoresheetService, pdfGenerator))
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `should return scoresheet data`() {
        // given
        val scoresheetDto =
            ScoresheetDto(
                gameInfo =
                    GameInfoDto(
                        gameId = 100L,
                        competitionName = "2024 시즌",
                        gameNumber = 1,
                        scheduledAt = LocalDateTime.of(2024, 3, 15, 14, 0),
                        startedAt = LocalDateTime.of(2024, 3, 15, 14, 10),
                        endedAt = LocalDateTime.of(2024, 3, 15, 17, 0),
                        location = "서울",
                        fieldName = "잠실구장",
                        status = "종료",
                        currentInning = "9회말",
                        totalInnings = 9,
                    ),
                teams =
                    TeamsScoresheetDto(
                        home =
                            TeamScoresheetInfoDto(
                                teamId = 1L,
                                teamName = "홈팀",
                                totalScore = 5,
                                totalHits = 10,
                                totalErrors = 1,
                                result = "승리",
                            ),
                        away =
                            TeamScoresheetInfoDto(
                                teamId = 2L,
                                teamName = "원정팀",
                                totalScore = 3,
                                totalHits = 8,
                                totalErrors = 2,
                                result = "패배",
                            ),
                    ),
                inningScores =
                    InningScoresDto(
                        innings = 9,
                        homeScores = listOf(0, 2, 0, 1, 0, 0, 2, 0, 0),
                        awayScores = listOf(1, 0, 2, 0, 0, 0, 0, 0, 0),
                    ),
                battingRecords =
                    BattingRecordsDto(
                        home =
                            listOf(
                                BatterScoresheetDto(
                                    playerId = 1L,
                                    name = "홈타자1",
                                    backNumber = 10,
                                    position = "1B",
                                    battingOrder = 1,
                                    plateAppearances = 4,
                                    atBats = 3,
                                    runs = 2,
                                    hits = 2,
                                    doubles = 1,
                                    triples = 0,
                                    homeRuns = 0,
                                    rbis = 1,
                                    walks = 1,
                                    strikeouts = 1,
                                    stolenBases = 0,
                                    avg = "0.667",
                                ),
                            ),
                        away = emptyList(),
                    ),
                pitchingRecords =
                    PitchingRecordsDto(
                        home =
                            listOf(
                                PitcherScoresheetDto(
                                    playerId = 10L,
                                    name = "홈투수1",
                                    backNumber = 1,
                                    isStartingPitcher = true,
                                    inningsPitched = "7.0",
                                    hitsAllowed = 6,
                                    runsAllowed = 3,
                                    earnedRuns = 2,
                                    walks = 2,
                                    strikeouts = 8,
                                    homeRunsAllowed = 1,
                                    decision = "W",
                                    era = "2.57",
                                ),
                            ),
                        away = emptyList(),
                    ),
                keyEvents =
                    listOf(
                        KeyEventDto(
                            inning = "1회초",
                            description = "원정팀 선제 득점",
                            timestamp = "14:15:30",
                        ),
                    ),
            )

        every { scoresheetService.getScoresheet(100L) } returns scoresheetDto

        // when & then
        mockMvc
            .perform(get("/api/v1/games/100/scoresheet"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.gameInfo.gameId").value(100))
            .andExpect(jsonPath("$.data.gameInfo.competitionName").value("2024 시즌"))
            .andExpect(jsonPath("$.data.teams.home.teamName").value("홈팀"))
            .andExpect(jsonPath("$.data.teams.away.teamName").value("원정팀"))
            .andExpect(jsonPath("$.data.teams.home.totalScore").value(5))
            .andExpect(jsonPath("$.data.teams.away.totalScore").value(3))
            .andExpect(jsonPath("$.data.inningScores.innings").value(9))
            .andExpect(jsonPath("$.data.battingRecords.home[0].name").value("홈타자1"))
            .andExpect(jsonPath("$.data.pitchingRecords.home[0].name").value("홈투수1"))
            .andExpect(jsonPath("$.data.keyEvents[0].description").value("원정팀 선제 득점"))
    }

    @Test
    fun `should return 404 when game not found`() {
        // given
        every { scoresheetService.getScoresheet(999L) } throws GameNotFoundException(999L)

        // when & then
        mockMvc
            .perform(get("/api/v1/games/999/scoresheet"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("GAME_NOT_FOUND"))
    }

    @Test
    fun `should download scoresheet as PDF`() {
        // given
        val scoresheetDto =
            ScoresheetDto(
                gameInfo =
                    GameInfoDto(
                        gameId = 100L,
                        competitionName = "2024 시즌",
                        gameNumber = 1,
                        scheduledAt = LocalDateTime.of(2024, 3, 15, 14, 0),
                        startedAt = null,
                        endedAt = null,
                        location = null,
                        fieldName = null,
                        status = "예정",
                        currentInning = "경기 전",
                        totalInnings = 9,
                    ),
                teams =
                    TeamsScoresheetDto(
                        home =
                            TeamScoresheetInfoDto(
                                teamId = 1L,
                                teamName = "홈팀",
                                totalScore = 0,
                                totalHits = 0,
                                totalErrors = 0,
                                result = "미정",
                            ),
                        away =
                            TeamScoresheetInfoDto(
                                teamId = 2L,
                                teamName = "원정팀",
                                totalScore = 0,
                                totalHits = 0,
                                totalErrors = 0,
                                result = "미정",
                            ),
                    ),
                inningScores = InningScoresDto(9, emptyList(), emptyList()),
                battingRecords = BattingRecordsDto(emptyList(), emptyList()),
                pitchingRecords = PitchingRecordsDto(emptyList(), emptyList()),
                keyEvents = emptyList(),
            )

        val pdfBytes = "PDF Content".toByteArray()

        every { scoresheetService.getScoresheet(100L) } returns scoresheetDto
        every { pdfGenerator.generateScoresheetPdf(scoresheetDto) } returns pdfBytes

        // when & then
        mockMvc
            .perform(get("/api/v1/games/100/scoresheet/pdf"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(
                header().string(
                    "Content-Disposition",
                    "form-data; name=\"attachment\"; filename=\"scoresheet_game_100.pdf\""
                )
            )
            .andExpect(content().bytes(pdfBytes))
    }

    @Test
    fun `should return 404 when downloading PDF for non-existent game`() {
        // given
        every { scoresheetService.getScoresheet(999L) } throws GameNotFoundException(999L)

        // when & then
        mockMvc
            .perform(get("/api/v1/games/999/scoresheet/pdf"))
            .andExpect(status().isNotFound)
    }
}
