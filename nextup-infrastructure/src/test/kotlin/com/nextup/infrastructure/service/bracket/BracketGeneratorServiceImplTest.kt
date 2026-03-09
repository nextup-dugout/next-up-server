package com.nextup.infrastructure.service.bracket

import com.nextup.common.exception.BracketEntryNotFoundException
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BracketEntryRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("BracketGeneratorServiceImpl")
class BracketGeneratorServiceImplTest {
    private lateinit var bracketEntryRepository: BracketEntryRepositoryPort
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var bracketGeneratorService: BracketGeneratorServiceImpl

    private lateinit var competition: Competition
    private lateinit var league: League
    private lateinit var association: Association
    private lateinit var teams: List<Team>

    @BeforeEach
    fun setUp() {
        bracketEntryRepository = mockk()
        competitionRepository = mockk()
        teamRepository = mockk()
        gameRepository = mockk()
        bracketGeneratorService =
            BracketGeneratorServiceImpl(
                bracketEntryRepository,
                competitionRepository,
                teamRepository,
                gameRepository,
            )

        // Test data setup
        association = Association(name = "서울시야구협회", id = 1L)
        league = League(association = association, name = "1부 리그", foundedYear = 2020, id = 1L)
        competition =
            Competition(
                league = league,
                name = "2025 춘계 토너먼트",
                year = 2025,
                season = 1,
                type = CompetitionType.TOURNAMENT,
                startDate = LocalDate.of(2025, 3, 1),
                id = 1L,
            )

        teams =
            listOf(
                Team(league = league, name = "팀1", city = "서울", foundedYear = 2020, id = 1L),
                Team(league = league, name = "팀2", city = "서울", foundedYear = 2020, id = 2L),
                Team(league = league, name = "팀3", city = "서울", foundedYear = 2020, id = 3L),
                Team(league = league, name = "팀4", city = "서울", foundedYear = 2020, id = 4L),
            )
    }

    @Nested
    @DisplayName("generateSingleElimination")
    inner class GenerateSingleElimination {
        @Test
        fun `대회가 존재하지 않으면 CompetitionNotFoundException 발생`() {
            // given
            val competitionId = 999L
            every { competitionRepository.findByIdOrNull(competitionId) } returns null

            // when & then
            assertThrows<CompetitionNotFoundException> {
                bracketGeneratorService.generateSingleElimination(competitionId, listOf(1L, 2L))
            }
        }

        @Test
        fun `팀 목록이 비어있으면 InvalidInputException 발생`() {
            // given
            every { competitionRepository.findByIdOrNull(1L) } returns competition

            // when & then
            val exception =
                assertThrows<InvalidInputException> {
                    bracketGeneratorService.generateSingleElimination(1L, emptyList())
                }
            assertThat(exception.code).isEqualTo("EMPTY_TEAM_LIST")
        }

        @Test
        fun `중복된 팀이 있으면 InvalidInputException 발생`() {
            // given
            every { competitionRepository.findByIdOrNull(1L) } returns competition

            // when & then
            val exception =
                assertThrows<InvalidInputException> {
                    bracketGeneratorService.generateSingleElimination(1L, listOf(1L, 1L, 2L))
                }
            assertThat(exception.code).isEqualTo("DUPLICATE_TEAMS")
        }

        @Test
        fun `2개 팀으로 단일 토너먼트 생성 시 결승전만 생성`() {
            // given
            val teamIds = listOf(1L, 2L)
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { bracketEntryRepository.deleteByCompetitionId(1L) } returns Unit
            every { teamRepository.findByIdOrNull(1L) } returns teams[0]
            every { teamRepository.findByIdOrNull(2L) } returns teams[1]

            val savedEntries = mutableListOf<BracketEntry>()
            every { bracketEntryRepository.saveAll(any<List<BracketEntry>>()) } answers {
                savedEntries.addAll(firstArg<List<BracketEntry>>())
                savedEntries
            }

            // when
            val result = bracketGeneratorService.generateSingleElimination(1L, teamIds)

            // then
            verify { bracketEntryRepository.deleteByCompetitionId(1L) }
            assertThat(result).hasSize(1)
            assertThat(result[0].roundNumber).isEqualTo(1)
            assertThat(result[0].team1).isEqualTo(teams[0])
            assertThat(result[0].team2).isEqualTo(teams[1])
        }

        @Test
        fun `4개 팀으로 단일 토너먼트 생성 시 준결승 2경기와 결승전 생성`() {
            // given
            val teamIds = listOf(1L, 2L, 3L, 4L)
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { bracketEntryRepository.deleteByCompetitionId(1L) } returns Unit
            teams.forEachIndexed { index, team ->
                every { teamRepository.findByIdOrNull(team.id) } returns teams[index]
            }

            val savedEntries = mutableListOf<BracketEntry>()
            every { bracketEntryRepository.saveAll(any<List<BracketEntry>>()) } answers {
                savedEntries.addAll(firstArg<List<BracketEntry>>())
                savedEntries
            }

            // when
            val result = bracketGeneratorService.generateSingleElimination(1L, teamIds)

            // then
            assertThat(result).hasSize(3) // 준결승 2경기 + 결승 1경기

            // 1라운드 (준결승)
            val round1 = result.filter { it.roundNumber == 1 }
            assertThat(round1).hasSize(2)

            // 2라운드 (결승)
            val round2 = result.filter { it.roundNumber == 2 }
            assertThat(round2).hasSize(1)
            assertThat(round2[0].team1).isNull()
            assertThat(round2[0].team2).isNull()
        }

        @Test
        fun `3개 팀으로 단일 토너먼트 생성 시 부전승 포함`() {
            // given
            val teamIds = listOf(1L, 2L, 3L)
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { bracketEntryRepository.deleteByCompetitionId(1L) } returns Unit
            every { teamRepository.findByIdOrNull(1L) } returns teams[0]
            every { teamRepository.findByIdOrNull(2L) } returns teams[1]
            every { teamRepository.findByIdOrNull(3L) } returns teams[2]

            val savedEntries = mutableListOf<BracketEntry>()
            every { bracketEntryRepository.saveAll(any<List<BracketEntry>>()) } answers {
                savedEntries.addAll(firstArg<List<BracketEntry>>())
                savedEntries
            }

            // when
            val result = bracketGeneratorService.generateSingleElimination(1L, teamIds)

            // then
            assertThat(result).hasSize(3)

            val round1 = result.filter { it.roundNumber == 1 }
            val byeMatches = round1.filter { it.team1 == null || it.team2 == null }
            assertThat(byeMatches).hasSize(1)
        }
    }

    @Nested
    @DisplayName("generateDoubleElimination")
    inner class GenerateDoubleElimination {
        @Test
        fun `4개 팀으로 더블 토너먼트 생성 시 Winners와 Losers 브라켓 모두 생성`() {
            // given
            val teamIds = listOf(1L, 2L, 3L, 4L)
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { bracketEntryRepository.deleteByCompetitionId(1L) } returns Unit
            teams.forEachIndexed { index, team ->
                every { teamRepository.findByIdOrNull(team.id) } returns teams[index]
            }

            val savedEntries = mutableListOf<BracketEntry>()
            every { bracketEntryRepository.saveAll(any<List<BracketEntry>>()) } answers {
                savedEntries.addAll(firstArg<List<BracketEntry>>())
                savedEntries
            }

            // when
            val result = bracketGeneratorService.generateDoubleElimination(1L, teamIds)

            // then
            val winnersBracket = result.filter { it.bracketType == "WINNERS" }
            val losersBracket = result.filter { it.bracketType == "LOSERS" }
            val finalBracket = result.filter { it.bracketType == "FINAL" }

            assertThat(winnersBracket).isNotEmpty
            assertThat(losersBracket).isNotEmpty
            assertThat(finalBracket).hasSize(1)
        }
    }

    @Nested
    @DisplayName("getBracket")
    inner class GetBracket {
        @Test
        fun `대회가 존재하지 않으면 CompetitionNotFoundException 발생`() {
            // given
            val competitionId = 999L
            every { competitionRepository.findByIdOrNull(competitionId) } returns null

            // when & then
            assertThrows<CompetitionNotFoundException> {
                bracketGeneratorService.getBracket(competitionId)
            }
        }

        @Test
        fun `대진표를 정상적으로 조회`() {
            // given
            val mockEntries =
                listOf(
                    BracketEntry(
                        competition = competition,
                        roundNumber = 1,
                        matchNumber = 1,
                        team1 = teams[0],
                        team2 = teams[1],
                        id = 1L,
                    ),
                )
            every { competitionRepository.findByIdOrNull(1L) } returns competition
            every { bracketEntryRepository.findByCompetitionId(1L) } returns mockEntries

            // when
            val result = bracketGeneratorService.getBracket(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].competition).isEqualTo(competition)
        }
    }

    @Nested
    @DisplayName("createGameForBracketEntry")
    inner class CreateGameForBracketEntry {
        @Test
        fun `대진표 엔트리가 존재하지 않으면 BracketEntryNotFoundException 발생`() {
            // given
            val bracketEntryId = 999L
            every { bracketEntryRepository.findByIdOrNull(bracketEntryId) } returns null

            // when & then
            assertThrows<BracketEntryNotFoundException> {
                bracketGeneratorService.createGameForBracketEntry(
                    bracketEntryId = bracketEntryId,
                    scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0),
                )
            }
        }

        @Test
        fun `팀이 결정되지 않은 슬롯에는 경기를 생성할 수 없다`() {
            // given
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 2,
                    matchNumber = 1,
                    team1 = null,
                    team2 = null,
                    id = 1L,
                )
            every { bracketEntryRepository.findByIdOrNull(1L) } returns bracketEntry

            // when & then
            val exception =
                assertThrows<InvalidInputException> {
                    bracketGeneratorService.createGameForBracketEntry(
                        bracketEntryId = 1L,
                        scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0),
                    )
                }
            assertThat(exception.code).isEqualTo("BRACKET_TEAMS_NOT_DECIDED")
        }

        @Test
        fun `이미 경기가 연결된 경우 InvalidInputException 발생`() {
            // given
            val existingGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = teams[0],
                    awayTeam = teams[1],
                    scheduledAt = LocalDateTime.of(2025, 3, 10, 14, 0),
                    id = 10L,
                )
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = teams[0],
                    team2 = teams[1],
                    game = existingGame,
                    id = 1L,
                )
            every { bracketEntryRepository.findByIdOrNull(1L) } returns bracketEntry

            // when & then
            val exception =
                assertThrows<InvalidInputException> {
                    bracketGeneratorService.createGameForBracketEntry(
                        bracketEntryId = 1L,
                        scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0),
                    )
                }
            assertThat(exception.code).isEqualTo("BRACKET_GAME_ALREADY_EXISTS")
        }

        @Test
        fun `경기를 정상적으로 생성하고 대진표 엔트리에 연결한다`() {
            // given
            val scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0)
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = teams[0],
                    team2 = teams[1],
                    id = 1L,
                )
            val savedGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = teams[0],
                    awayTeam = teams[1],
                    scheduledAt = scheduledAt,
                    id = 100L,
                )

            every { bracketEntryRepository.findByIdOrNull(1L) } returns bracketEntry
            every { gameRepository.save(any<Game>()) } returns savedGame
            every { bracketEntryRepository.save(any<BracketEntry>()) } answers { firstArg() }

            // when
            val result =
                bracketGeneratorService.createGameForBracketEntry(
                    bracketEntryId = 1L,
                    scheduledAt = scheduledAt,
                )

            // then
            assertThat(result.game).isEqualTo(savedGame)
            verify(exactly = 1) { gameRepository.save(any<Game>()) }
            verify(exactly = 1) { bracketEntryRepository.save(any<BracketEntry>()) }
        }

        @Test
        fun `경기 생성 시 location과 fieldName이 전달된다`() {
            // given
            val scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0)
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = teams[0],
                    team2 = teams[1],
                    id = 1L,
                )

            var capturedGame: Game? = null
            every { bracketEntryRepository.findByIdOrNull(1L) } returns bracketEntry
            every { gameRepository.save(any<Game>()) } answers {
                capturedGame = firstArg()
                firstArg()
            }
            every { bracketEntryRepository.save(any<BracketEntry>()) } answers { firstArg() }

            // when
            bracketGeneratorService.createGameForBracketEntry(
                bracketEntryId = 1L,
                scheduledAt = scheduledAt,
                location = "서울 잠실야구장",
                fieldName = "주경기장",
            )

            // then
            assertThat(capturedGame?.location).isEqualTo("서울 잠실야구장")
            assertThat(capturedGame?.fieldName).isEqualTo("주경기장")
        }
    }

    @Nested
    @DisplayName("advanceWinner")
    inner class AdvanceWinner {
        @Test
        fun `대진표 엔트리가 존재하지 않으면 BracketEntryNotFoundException 발생`() {
            // given
            val bracketEntryId = 999L
            every { bracketEntryRepository.findByIdOrNull(bracketEntryId) } returns null

            // when & then
            assertThrows<BracketEntryNotFoundException> {
                bracketGeneratorService.advanceWinner(bracketEntryId, 1L)
            }
        }

        @Test
        fun `팀이 존재하지 않으면 InvalidInputException 발생`() {
            // given
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = teams[0],
                    team2 = teams[1],
                    id = 1L,
                )
            every { bracketEntryRepository.findByIdOrNull(1L) } returns bracketEntry
            every { teamRepository.findByIdOrNull(999L) } returns null

            // when & then
            val exception =
                assertThrows<InvalidInputException> {
                    bracketGeneratorService.advanceWinner(1L, 999L)
                }
            assertThat(exception.code).isEqualTo("TEAM_NOT_FOUND")
        }

        @Test
        fun `승자를 정상적으로 기록`() {
            // given
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = teams[0],
                    team2 = teams[1],
                    id = 1L,
                )
            every { bracketEntryRepository.findByIdOrNull(1L) } returns bracketEntry
            every { teamRepository.findByIdOrNull(1L) } returns teams[0]
            every { bracketEntryRepository.save(any()) } answers { firstArg() }

            // when
            val result = bracketGeneratorService.advanceWinner(1L, 1L)

            // then
            assertThat(result.winner).isEqualTo(teams[0])
            verify { bracketEntryRepository.save(any()) }
        }
    }
}
