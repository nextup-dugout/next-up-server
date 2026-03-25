package com.nextup.core.service.lineup

import com.nextup.common.exception.LineupExchangeNotAuthorizedException
import com.nextup.common.exception.LineupNotExchangedException
import com.nextup.common.exception.NoCatcherInLineupException
import com.nextup.common.exception.NonAttendingPlayerInLineupException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.event.LineupConfirmedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

class LineupServiceTest {
    private lateinit var lineupSubmissionRepository: LineupSubmissionRepositoryPort
    private lateinit var lineupEntryRepository: LineupEntryRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var attendancePollRepository: com.nextup.core.port.attendance.AttendancePollRepositoryPort
    private lateinit var attendanceVoteRepository: com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var lineupService: LineupService

    private lateinit var game: Game
    private lateinit var team: Team
    private lateinit var user: User
    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        lineupSubmissionRepository = mockk()
        lineupEntryRepository = mockk()
        gameRepository = mockk()
        teamRepository = mockk()
        playerRepository = mockk()
        userRepository = mockk()
        attendancePollRepository = mockk()
        attendanceVoteRepository = mockk()
        eventPublisher = mockk(relaxed = true)

        lineupService =
            LineupService(
                lineupSubmissionRepository = lineupSubmissionRepository,
                lineupEntryRepository = lineupEntryRepository,
                gameRepository = gameRepository,
                teamRepository = teamRepository,
                playerRepository = playerRepository,
                userRepository = userRepository,
                attendancePollRepository = attendancePollRepository,
                attendanceVoteRepository = attendanceVoteRepository,
                eventPublisher = eventPublisher,
            )

        // Setup test data
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        val competition =
            Competition(
                league = league,
                name = "2024 시즌",
                year = 2024,
                startDate = LocalDate.now().minusDays(30),
                endDate = LocalDate.now().plusDays(30),
            )

        team = Team(league = league, name = "Tigers", city = "서울", foundedYear = 2020)
        val awayTeam = Team(league = league, name = "Eagles", city = "부산", foundedYear = 2020)
        game =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.now().plusDays(1),
                location = "서울야구장",
            )
        user = User.createLocalUser(email = "test@test.com", encodedPassword = "encoded", nickname = "테스트")
        player =
            mockk<Player>().apply {
                every { id } returns 1L
                every { name } returns "홍길동"
            }
    }

    @Nested
    inner class CreateLineupSubmissionTest {
        @Test
        fun `should create lineup submission successfully`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns game
            every { teamRepository.findByIdOrNull(any()) } returns team
            every { userRepository.findByIdOrNull(any()) } returns user
            every { lineupSubmissionRepository.findByGameIdAndTeamId(any(), any()) } returns null

            val submissionSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(submissionSlot)) } answers { submissionSlot.captured }

            // when
            val result = lineupService.createLineupSubmission(1L, 1L, 1L)

            // then
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.DRAFT)
            verify { lineupSubmissionRepository.save(any()) }
        }

        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.createLineupSubmission(1L, 1L, 1L)
                }
            assertThat(exception.message).contains("경기 ID")
        }

        @Test
        fun `should throw exception when team not found`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns game
            every { teamRepository.findByIdOrNull(any()) } returns null

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.createLineupSubmission(1L, 1L, 1L)
                }
            assertThat(exception.message).contains("팀 ID")
        }

        @Test
        fun `should throw exception when user not found`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns game
            every { teamRepository.findByIdOrNull(any()) } returns team
            every { userRepository.findByIdOrNull(any()) } returns null

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.createLineupSubmission(1L, 1L, 1L)
                }
            assertThat(exception.message).contains("사용자 ID")
        }

        @Test
        fun `should throw exception when lineup already exists`() {
            // given
            val existingSubmission = LineupSubmission.create(game, team, user)
            every { gameRepository.findByIdOrNull(any()) } returns game
            every { teamRepository.findByIdOrNull(any()) } returns team
            every { userRepository.findByIdOrNull(any()) } returns user
            every { lineupSubmissionRepository.findByGameIdAndTeamId(any(), any()) } returns existingSubmission

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.createLineupSubmission(1L, 1L, 1L)
                }
            assertThat(exception.message).contains("이미 해당 경기/팀의 라인업이 존재합니다")
        }
    }

    @Nested
    inner class GetLineupSubmissionTest {
        @Test
        fun `should get lineup submission by id`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByIdOrNull(1L) } returns submission

            // when
            val result = lineupService.getLineupSubmission(1L)

            // then
            assertThat(result).isEqualTo(submission)
        }

        @Test
        fun `should throw exception when submission not found`() {
            // given
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns null

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.getLineupSubmission(1L)
                }
            assertThat(exception.message).contains("라인업 제출 ID")
        }
    }

    @Nested
    inner class AddLineupEntryTest {
        @Test
        fun `should add lineup entry successfully`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { playerRepository.findByIdOrNull(any()) } returns player
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(any(), any()) } returns null
            every { lineupEntryRepository.findBySubmissionIdAndBattingOrder(any(), any()) } returns null

            val entrySlot = slot<LineupEntry>()
            every { lineupEntryRepository.save(capture(entrySlot)) } answers { entrySlot.captured }

            // when
            val result =
                lineupService.addLineupEntry(
                    submissionId = 1L,
                    playerId = 1L,
                    position = Position.SHORTSTOP,
                    battingOrder = 1,
                    backNumber = 7,
                    isStarter = true,
                )

            // then
            assertThat(result.position).isEqualTo(Position.SHORTSTOP)
            assertThat(result.battingOrder).isEqualTo(1)
            verify { lineupEntryRepository.save(any()) }
        }

        @Test
        fun `should throw exception when player already in lineup`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            val existingEntry = mockk<LineupEntry>()
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { playerRepository.findByIdOrNull(any()) } returns player
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(any(), any()) } returns existingEntry

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.addLineupEntry(
                        submissionId = 1L,
                        playerId = 1L,
                        position = Position.SHORTSTOP,
                        battingOrder = 1,
                        backNumber = 7,
                    )
                }
            assertThat(exception.message).contains("이미 라인업에 등록된 선수입니다")
        }

        @Test
        fun `should throw exception when batting order already taken`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            val existingEntry = mockk<LineupEntry>()
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { playerRepository.findByIdOrNull(any()) } returns player
            every { lineupEntryRepository.findBySubmissionIdAndPlayerId(any(), any()) } returns null
            every { lineupEntryRepository.findBySubmissionIdAndBattingOrder(any(), any()) } returns existingEntry

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.addLineupEntry(
                        submissionId = 1L,
                        playerId = 1L,
                        position = Position.SHORTSTOP,
                        battingOrder = 1,
                        backNumber = 7,
                    )
                }
            assertThat(exception.message).contains("이미 해당 타순에 선수가 등록되어 있습니다")
        }
    }

    @Nested
    inner class SetLineupEntriesTest {
        @Test
        fun `should set lineup entries successfully`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { lineupEntryRepository.deleteAllBySubmissionId(any()) } returns Unit
            every { playerRepository.findByIdOrNull(any()) } returns player

            val entrySlot = slot<LineupEntry>()
            every { lineupEntryRepository.save(capture(entrySlot)) } answers { entrySlot.captured }

            val inputs =
                listOf(
                    LineupEntryInput(
                        playerId = 1L,
                        position = Position.SHORTSTOP,
                        battingOrder = 1,
                        backNumber = 7,
                    ),
                )

            // when
            val result = lineupService.setLineupEntries(1L, inputs)

            // then
            assertThat(result).hasSize(1)
            verify { lineupEntryRepository.deleteAllBySubmissionId(any()) }
        }

        @Test
        fun `should throw exception when duplicate player in entries`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { lineupEntryRepository.deleteAllBySubmissionId(any()) } returns Unit

            val inputs =
                listOf(
                    LineupEntryInput(playerId = 1L, position = Position.SHORTSTOP, battingOrder = 1, backNumber = 7),
                    LineupEntryInput(playerId = 1L, position = Position.FIRST_BASE, battingOrder = 2, backNumber = 8),
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.setLineupEntries(1L, inputs)
                }
            assertThat(exception.message).contains("중복된 선수")
        }

        @Test
        fun `should throw exception when duplicate batting order in entries`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { lineupEntryRepository.deleteAllBySubmissionId(any()) } returns Unit

            val inputs =
                listOf(
                    LineupEntryInput(playerId = 1L, position = Position.SHORTSTOP, battingOrder = 1, backNumber = 7),
                    LineupEntryInput(playerId = 2L, position = Position.FIRST_BASE, battingOrder = 1, backNumber = 8),
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.setLineupEntries(1L, inputs)
                }
            assertThat(exception.message).contains("중복된 타순")
        }
    }

    @Nested
    inner class SubmitLineupTest {
        @Test
        fun `should submit lineup successfully`() {
            // given
            val submission = createSubmissionWithEntries()
            val attendingVotes =
                createAttendingVotesForPlayers(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every {
                attendancePollRepository.findByGameIdAndTeamId(any(), any())
            } returns mockk<com.nextup.core.domain.attendance.AttendancePoll>().apply { every { id } returns 999L }
            every {
                attendanceVoteRepository.findByPollId(999L)
            } returns attendingVotes
            // Only one team submitted — no exchange yet
            every { lineupSubmissionRepository.findAllByGameId(any()) } returns listOf(submission)

            val submissionSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(submissionSlot)) } answers { submissionSlot.captured }

            // when
            val result = lineupService.submitLineup(1L)

            // then
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        }

        @Test
        fun `should throw exception when less than 9 starters`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            addEntriesToSubmission(
                submission,
                listOf(Position.STARTING_PITCHER, Position.CATCHER),
            )

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.submitLineup(1L)
                }
            assertThat(exception.message).contains("선발 라인업은 최소 9명이 필요합니다")
        }

        @Test
        fun `should throw exception when no catcher`() {
            // given
            val submission = createSubmissionWithEntriesNoCatcher()
            val attendingVotes = createAttendingVotesForPlayers(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every {
                attendancePollRepository.findByGameIdAndTeamId(any(), any())
            } returns mockk<com.nextup.core.domain.attendance.AttendancePoll>().apply { every { id } returns 999L }
            every {
                attendanceVoteRepository.findByPollId(999L)
            } returns attendingVotes

            // when & then
            assertThrows<NoCatcherInLineupException> {
                lineupService.submitLineup(1L)
            }
        }

        @Test
        fun `should throw exception when non-attending player in lineup`() {
            // given
            val submission = createSubmissionWithEntries()
            val attendingVotes = createAttendingVotesForPlayers(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L))

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every {
                attendancePollRepository.findByGameIdAndTeamId(any(), any())
            } returns mockk<com.nextup.core.domain.attendance.AttendancePoll>().apply { every { id } returns 999L }
            every {
                attendanceVoteRepository.findByPollId(999L)
            } returns attendingVotes

            // when & then - player 9 is not attending
            assertThrows<NonAttendingPlayerInLineupException> {
                lineupService.submitLineup(1L)
            }
        }

        @Test
        fun `should submit successfully when all players are attending`() {
            // given
            val submission = createSubmissionWithEntries()
            val attendingVotes = createAttendingVotesForPlayers(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every {
                attendancePollRepository.findByGameIdAndTeamId(any(), any())
            } returns mockk<com.nextup.core.domain.attendance.AttendancePoll>().apply { every { id } returns 999L }
            every {
                attendanceVoteRepository.findByPollId(999L)
            } returns attendingVotes
            // Only one team submitted — no exchange yet
            every { lineupSubmissionRepository.findAllByGameId(any()) } returns listOf(submission)

            val submissionSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(submissionSlot)) } answers { submissionSlot.captured }

            // when
            val result = lineupService.submitLineup(1L)

            // then
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        }
    }

    @Nested
    inner class ConfirmLineupTest {
        @Test
        fun `should confirm lineup successfully`() {
            // given
            val submission = createSubmissionWithEntries().apply { submit() }
            val scorer = User.createLocalUser(email = "scorer@test.com", encodedPassword = "encoded", nickname = "기록원")

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { userRepository.findByIdOrNull(any()) } returns scorer

            val submissionSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(submissionSlot)) } answers { submissionSlot.captured }

            // when
            val result = lineupService.confirmLineup(1L, 1L)

            // then
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.CONFIRMED)
            verify { eventPublisher.publishEvent(any<LineupConfirmedEvent>()) }
        }

        @Test
        fun `should throw exception when scorer not found`() {
            // given
            val submission = createSubmissionWithEntries().apply { submit() }

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { userRepository.findByIdOrNull(any()) } returns null

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.confirmLineup(1L, 1L)
                }
            assertThat(exception.message).contains("기록원 ID")
        }
    }

    @Nested
    inner class RejectLineupTest {
        @Test
        fun `should reject lineup successfully`() {
            // given
            val submission = createSubmissionWithEntries().apply { submit() }
            val scorer = User.createLocalUser(email = "scorer@test.com", encodedPassword = "encoded", nickname = "기록원")

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { userRepository.findByIdOrNull(any()) } returns scorer

            val submissionSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(submissionSlot)) } answers { submissionSlot.captured }

            // when
            val result = lineupService.rejectLineup(1L, 1L, "선수 등록번호 확인 필요")

            // then
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.REJECTED)
            assertThat(result.rejectionReason).isEqualTo("선수 등록번호 확인 필요")
        }
    }

    @Nested
    inner class GetLineupSubmissionsByGameTest {
        @Test
        fun `should get all lineup submissions by game`() {
            // given
            val submissions =
                listOf(
                    LineupSubmission.create(game, team, user),
                )
            every { lineupSubmissionRepository.findAllByGameId(any()) } returns submissions

            // when
            val result = lineupService.getLineupSubmissionsByGame(1L)

            // then
            assertThat(result).hasSize(1)
        }
    }

    @Nested
    inner class GetLineupSubmissionsByTeamTest {
        @Test
        fun `should get all lineup submissions by team`() {
            // given
            val submissions =
                listOf(
                    LineupSubmission.create(game, team, user),
                )
            every { lineupSubmissionRepository.findAllByTeamId(any()) } returns submissions

            // when
            val result = lineupService.getLineupSubmissionsByTeam(1L)

            // then
            assertThat(result).hasSize(1)
        }
    }

    @Nested
    inner class GetSubmittedLineupsByGameTest {
        @Test
        fun `should get submitted lineups by game`() {
            // given
            val submission = createSubmissionWithEntries().apply { submit() }
            every {
                lineupSubmissionRepository.findAllByGameIdAndStatus(any(), LineupSubmissionStatus.SUBMITTED)
            } returns
                listOf(submission)

            // when
            val result = lineupService.getSubmittedLineupsByGame(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        }
    }

    @Nested
    inner class GetLineupEntriesTest {
        @Test
        fun `should get lineup entries by submission id`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            val entries = listOf(createMockEntry(submission, Position.STARTING_PITCHER, 1))
            every { lineupEntryRepository.findAllBySubmissionId(any()) } returns entries

            // when
            val result = lineupService.getLineupEntries(1L)

            // then
            assertThat(result).hasSize(1)
        }
    }

    @Nested
    inner class GetLineupSubmissionByGameAndTeamTest {
        @Test
        fun `should get lineup submission by game and team`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByGameIdAndTeamId(1L, 1L) } returns submission

            // when
            val result = lineupService.getLineupSubmissionByGameAndTeam(1L, 1L)

            // then
            assertThat(result).isNotNull
        }

        @Test
        fun `should return null when no submission found`() {
            // given
            every { lineupSubmissionRepository.findByGameIdAndTeamId(1L, 1L) } returns null

            // when
            val result = lineupService.getLineupSubmissionByGameAndTeam(1L, 1L)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    inner class ExchangePendingTest {
        // awayTeam has a distinct id (2L) so findAllByGameId can differentiate home vs away
        private fun createAwayTeam(): com.nextup.core.domain.team.Team {
            val league =
                com.nextup.core.domain.league.League(
                    association =
                        com.nextup.core.domain.association.Association(
                            name = "서울시야구협회",
                            abbreviation = "서야협",
                            region = "서울",
                        ),
                    name = "서울시 리그",
                    foundedYear = 2020,
                )
            return com.nextup.core.domain.team.Team(
                league = league,
                name = "Lions",
                city = "부산",
                foundedYear = 2021,
                id = 2L,
            )
        }

        @Test
        fun `should mark both lineups as exchange pending when both teams submit`() {
            // given
            val awayTeam = createAwayTeam()
            val homeSubmission = createSubmissionWithEntries()
            val awaySubmission =
                LineupSubmission.create(game, awayTeam, user).also { sub ->
                    addEntriesToSubmission(sub, ninePositions())
                }

            // homeSubmission is already SUBMITTED before away submits
            homeSubmission.submit()

            // Create attending votes that reference awayTeam (id=2L) so the team filter passes
            val awayAttendingVotes =
                (1L..9L).map { playerId ->
                    mockk<com.nextup.core.domain.attendance.AttendanceVote>().apply {
                        every { voteType } returns com.nextup.core.domain.attendance.VoteType.ATTEND
                        every { player } returns
                            mockk<Player>().apply {
                                every { id } returns playerId
                            }
                    }
                }

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns awaySubmission
            every {
                attendancePollRepository.findByGameIdAndTeamId(any(), any())
            } returns mockk<com.nextup.core.domain.attendance.AttendancePoll>().apply { every { id } returns 999L }
            every {
                attendanceVoteRepository.findByPollId(999L)
            } returns awayAttendingVotes
            // Both submissions present — exchange pending should trigger
            every { lineupSubmissionRepository.findAllByGameId(any()) } returns
                listOf(homeSubmission, awaySubmission)

            val savedSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            lineupService.submitLineup(awaySubmission.id)

            // then: both should now be EXCHANGE_PENDING (waiting for manager approval)
            assertThat(homeSubmission.status).isEqualTo(LineupSubmissionStatus.EXCHANGE_PENDING)
            assertThat(awaySubmission.status).isEqualTo(LineupSubmissionStatus.EXCHANGE_PENDING)
        }

        @Test
        fun `should not mark exchange pending when only one team has submitted`() {
            // given
            val homeSubmission = createSubmissionWithEntries()
            val attendingVotes =
                createAttendingVotesForPlayers(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns homeSubmission
            every {
                attendancePollRepository.findByGameIdAndTeamId(any(), any())
            } returns mockk<com.nextup.core.domain.attendance.AttendancePoll>().apply { every { id } returns 999L }
            every {
                attendanceVoteRepository.findByPollId(999L)
            } returns attendingVotes
            // Only one submission in the game — opponent hasn't submitted yet
            every { lineupSubmissionRepository.findAllByGameId(any()) } returns listOf(homeSubmission)

            val savedSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            // when
            lineupService.submitLineup(homeSubmission.id)

            // then: still SUBMITTED since only one team submitted
            assertThat(homeSubmission.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        }
    }

    @Nested
    inner class ApproveLineupExchangeTest {
        private val homeTeamId = 1L
        private val awayTeamId = 2L

        private fun createHomeTeamWithId(): com.nextup.core.domain.team.Team {
            val league =
                com.nextup.core.domain.league.League(
                    association =
                        com.nextup.core.domain.association.Association(
                            name = "서울시야구협회",
                            abbreviation = "서야협",
                            region = "서울",
                        ),
                    name = "서울시 리그",
                    foundedYear = 2020,
                )
            return com.nextup.core.domain.team.Team(
                league = league,
                name = "Tigers",
                city = "서울",
                foundedYear = 2020,
                id = homeTeamId,
            )
        }

        private fun createAwayTeamWithId(): com.nextup.core.domain.team.Team {
            val league =
                com.nextup.core.domain.league.League(
                    association =
                        com.nextup.core.domain.association.Association(
                            name = "서울시야구협회",
                            abbreviation = "서야협",
                            region = "서울",
                        ),
                    name = "서울시 리그",
                    foundedYear = 2020,
                )
            return com.nextup.core.domain.team.Team(
                league = league,
                name = "Lions",
                city = "부산",
                foundedYear = 2021,
                id = awayTeamId,
            )
        }

        private fun createExchangePendingSubmission(teamForSub: com.nextup.core.domain.team.Team): LineupSubmission {
            val sub = LineupSubmission.create(game, teamForSub, user)
            addEntriesToSubmission(sub, ninePositions())
            sub.submit()
            sub.markExchangePending()
            return sub
        }

        @Test
        fun `should approve opponent lineup exchange and transition to EXCHANGED`() {
            // given
            val homeTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            val homeSubmission = createExchangePendingSubmission(homeTeam)
            val awaySubmission = createExchangePendingSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(homeSubmission, awaySubmission)

            val savedSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            // when: home team manager approves opponent (away) lineup
            val result = lineupService.approveLineupExchange(gameId = 1L, approvingTeamId = homeTeamId)

            // then: opponent (away) lineup is now EXCHANGED
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.EXCHANGED)
            assertThat(awaySubmission.status).isEqualTo(LineupSubmissionStatus.EXCHANGED)
        }

        @Test
        fun `should throw LineupExchangeNotAuthorizedException when opponent lineup is not exchange pending`() {
            // given
            val homeTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            val homeSubmission = createExchangePendingSubmission(homeTeam)
            // Opponent is only SUBMITTED, not EXCHANGE_PENDING
            val awaySubmission =
                LineupSubmission.create(game, awayTeam, user).also { sub ->
                    addEntriesToSubmission(sub, ninePositions())
                    sub.submit()
                }

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(homeSubmission, awaySubmission)

            // when & then
            assertThrows<LineupExchangeNotAuthorizedException> {
                lineupService.approveLineupExchange(gameId = 1L, approvingTeamId = homeTeamId)
            }
        }

        @Test
        fun `should approve opponent and skip my lineup transition when my lineup is not EXCHANGE_PENDING`() {
            // given: home is SUBMITTED (not EXCHANGE_PENDING), away is EXCHANGE_PENDING
            // Home team approves the away lineup — home's own lineup is not yet pending
            val homeTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            val homeSubmission =
                LineupSubmission.create(game, homeTeam, user).also { sub ->
                    addEntriesToSubmission(sub, ninePositions())
                    sub.submit()
                }
            val awaySubmission = createExchangePendingSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(homeSubmission, awaySubmission)

            val savedSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            // when: home team approves away lineup; home is SUBMITTED so mySubmission.canApproveExchange() = false
            val result = lineupService.approveLineupExchange(gameId = 1L, approvingTeamId = homeTeamId)

            // then: opponent (away) is approved; my (home) lineup stays SUBMITTED
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.EXCHANGED)
            assertThat(homeSubmission.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        }

        @Test
        fun `should throw IllegalArgumentException when my team lineup not found`() {
            // given
            val awayTeam = createAwayTeamWithId()
            val awaySubmission = createExchangePendingSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns listOf(awaySubmission)

            // when & then: requesting team (homeTeamId=1) has no submission
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.approveLineupExchange(gameId = 1L, approvingTeamId = homeTeamId)
                }
            assertThat(exception.message).contains("라인업을 찾을 수 없습니다")
        }

        @Test
        fun `should throw IllegalArgumentException when opponent lineup not found`() {
            // given
            val homeTeam = createHomeTeamWithId()
            val homeSubmission = createExchangePendingSubmission(homeTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns listOf(homeSubmission)

            // when & then: no opponent submission
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.approveLineupExchange(gameId = 1L, approvingTeamId = homeTeamId)
                }
            assertThat(exception.message).contains("상대팀 라인업을 찾을 수 없습니다")
        }
    }

    @Nested
    inner class RejectLineupExchangeTest {
        private val homeTeamId = 1L
        private val awayTeamId = 2L

        private fun createHomeTeamWithId(): com.nextup.core.domain.team.Team {
            val league =
                com.nextup.core.domain.league.League(
                    association =
                        com.nextup.core.domain.association.Association(
                            name = "서울시야구협회",
                            abbreviation = "서야협",
                            region = "서울",
                        ),
                    name = "서울시 리그",
                    foundedYear = 2020,
                )
            return com.nextup.core.domain.team.Team(
                league = league,
                name = "Tigers",
                city = "서울",
                foundedYear = 2020,
                id = homeTeamId,
            )
        }

        private fun createAwayTeamWithId(): com.nextup.core.domain.team.Team {
            val league =
                com.nextup.core.domain.league.League(
                    association =
                        com.nextup.core.domain.association.Association(
                            name = "서울시야구협회",
                            abbreviation = "서야협",
                            region = "서울",
                        ),
                    name = "서울시 리그",
                    foundedYear = 2020,
                )
            return com.nextup.core.domain.team.Team(
                league = league,
                name = "Lions",
                city = "부산",
                foundedYear = 2021,
                id = awayTeamId,
            )
        }

        private fun createExchangePendingSubmission(teamForSub: com.nextup.core.domain.team.Team): LineupSubmission {
            val sub = LineupSubmission.create(game, teamForSub, user)
            addEntriesToSubmission(sub, ninePositions())
            sub.submit()
            sub.markExchangePending()
            return sub
        }

        @Test
        fun `should reject opponent lineup exchange and revert my lineup to SUBMITTED`() {
            // given
            val homeTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()
            val manager =
                User.createLocalUser(
                    email = "manager@test.com",
                    encodedPassword = "encoded",
                    nickname = "감독",
                )

            val homeSubmission = createExchangePendingSubmission(homeTeam)
            val awaySubmission = createExchangePendingSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(homeSubmission, awaySubmission)
            every { userRepository.findByIdOrNull(any()) } returns manager

            val savedSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            // when: home team manager rejects opponent (away) lineup
            val result =
                lineupService.rejectLineupExchange(
                    gameId = 1L,
                    rejectingTeamId = homeTeamId,
                    rejectingUserId = 1L,
                    reason = "선수 등록번호 불일치",
                )

            // then: away lineup is EXCHANGE_REJECTED, home lineup reverts to SUBMITTED
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.EXCHANGE_REJECTED)
            assertThat(awaySubmission.status).isEqualTo(LineupSubmissionStatus.EXCHANGE_REJECTED)
            assertThat(awaySubmission.exchangeRejectionReason).isEqualTo("선수 등록번호 불일치")
            assertThat(homeSubmission.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        }

        @Test
        fun `should throw LineupExchangeNotAuthorizedException when opponent lineup is not exchange pending`() {
            // given
            val homeTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            val homeSubmission = createExchangePendingSubmission(homeTeam)
            // Opponent is only SUBMITTED, not EXCHANGE_PENDING
            val awaySubmission =
                LineupSubmission.create(game, awayTeam, user).also { sub ->
                    addEntriesToSubmission(sub, ninePositions())
                    sub.submit()
                }

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(homeSubmission, awaySubmission)
            every { userRepository.findByIdOrNull(any()) } returns user

            // when & then
            assertThrows<LineupExchangeNotAuthorizedException> {
                lineupService.rejectLineupExchange(
                    gameId = 1L,
                    rejectingTeamId = homeTeamId,
                    rejectingUserId = 1L,
                    reason = "사유",
                )
            }
        }

        @Test
        fun `should not revert my lineup when it is not EXCHANGE_PENDING during reject`() {
            // given: home lineup is SUBMITTED (not EXCHANGE_PENDING), away is EXCHANGE_PENDING
            val homeTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()
            val manager = User.createLocalUser(email = "mgr@test.com", encodedPassword = "encoded", nickname = "감독")

            val homeSubmission =
                LineupSubmission.create(game, homeTeam, user).also { sub ->
                    addEntriesToSubmission(sub, ninePositions())
                    sub.submit()
                }
            val awaySubmission = createExchangePendingSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(homeSubmission, awaySubmission)
            every { userRepository.findByIdOrNull(any()) } returns manager

            val savedSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            // when: home team rejects away lineup — home is SUBMITTED so no revert needed
            val result =
                lineupService.rejectLineupExchange(
                    gameId = 1L,
                    rejectingTeamId = homeTeamId,
                    rejectingUserId = 1L,
                    reason = "선수 오류",
                )

            // then: away becomes EXCHANGE_REJECTED; home stays SUBMITTED (unchanged)
            assertThat(result.status).isEqualTo(LineupSubmissionStatus.EXCHANGE_REJECTED)
            assertThat(homeSubmission.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        }

        @Test
        fun `should throw IllegalArgumentException when rejectingTeam lineup not found`() {
            // given: only away submission exists, home (rejectingTeam) has none
            val awayTeam = createAwayTeamWithId()
            val awaySubmission = createExchangePendingSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns listOf(awaySubmission)

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.rejectLineupExchange(
                        gameId = 1L,
                        rejectingTeamId = homeTeamId,
                        rejectingUserId = 1L,
                        reason = "사유",
                    )
                }
            assertThat(exception.message).contains("라인업을 찾을 수 없습니다")
        }

        @Test
        fun `should throw IllegalArgumentException when opponent lineup not found during reject`() {
            // given: only home submission exists, no opponent
            val homeTeam = createHomeTeamWithId()
            val homeSubmission = createExchangePendingSubmission(homeTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns listOf(homeSubmission)

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.rejectLineupExchange(
                        gameId = 1L,
                        rejectingTeamId = homeTeamId,
                        rejectingUserId = 1L,
                        reason = "사유",
                    )
                }
            assertThat(exception.message).contains("상대팀 라인업을 찾을 수 없습니다")
        }

        @Test
        fun `should throw IllegalArgumentException when rejecting user not found`() {
            // given
            val homeTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            val homeSubmission = createExchangePendingSubmission(homeTeam)
            val awaySubmission = createExchangePendingSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(homeSubmission, awaySubmission)
            every { userRepository.findByIdOrNull(any()) } returns null

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.rejectLineupExchange(
                        gameId = 1L,
                        rejectingTeamId = homeTeamId,
                        rejectingUserId = 999L,
                        reason = "사유",
                    )
                }
            assertThat(exception.message).contains("사용자 ID")
        }
    }

    @Nested
    inner class GetOpponentLineupTest {
        private val homeTeamId = 1L
        private val awayTeamId = 2L

        private fun createHomeTeamWithId(): com.nextup.core.domain.team.Team {
            val league =
                com.nextup.core.domain.league.League(
                    association =
                        com.nextup.core.domain.association.Association(
                            name = "서울시야구협회",
                            abbreviation = "서야협",
                            region = "서울",
                        ),
                    name = "서울시 리그",
                    foundedYear = 2020,
                )
            return com.nextup.core.domain.team.Team(
                league = league,
                name = "Tigers",
                city = "서울",
                foundedYear = 2020,
                id = homeTeamId,
            )
        }

        private fun createAwayTeamWithId(): com.nextup.core.domain.team.Team {
            val league =
                com.nextup.core.domain.league.League(
                    association =
                        com.nextup.core.domain.association.Association(
                            name = "서울시야구협회",
                            abbreviation = "서야협",
                            region = "서울",
                        ),
                    name = "서울시 리그",
                    foundedYear = 2020,
                )
            return com.nextup.core.domain.team.Team(
                league = league,
                name = "Lions",
                city = "부산",
                foundedYear = 2021,
                id = awayTeamId,
            )
        }

        private fun createExchangedSubmission(teamForSub: com.nextup.core.domain.team.Team): LineupSubmission {
            val sub = LineupSubmission.create(game, teamForSub, user)
            addEntriesToSubmission(sub, ninePositions())
            sub.submit()
            sub.markExchangePending()
            sub.approveExchange()
            return sub
        }

        private fun createSubmittedSubmission(teamForSub: com.nextup.core.domain.team.Team): LineupSubmission {
            val sub = LineupSubmission.create(game, teamForSub, user)
            addEntriesToSubmission(sub, ninePositions())
            sub.submit()
            return sub
        }

        @Test
        fun `should return opponent lineup when both are exchanged`() {
            // given
            val myTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            val mySubmission = createExchangedSubmission(myTeam)
            val opponentSubmission = createExchangedSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(mySubmission, opponentSubmission)

            // when
            val result = lineupService.getOpponentLineup(gameId = 1L, myTeamId = homeTeamId)

            // then
            assertThat(result).isEqualTo(opponentSubmission)
            assertThat(result.team.name).isEqualTo("Lions")
        }

        @Test
        fun `should throw LineupNotExchangedException when opponent has not submitted`() {
            // given
            val myTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            val mySubmission = createExchangedSubmission(myTeam)
            // Opponent is only SUBMITTED, not EXCHANGED
            val opponentSubmission = createSubmittedSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(mySubmission, opponentSubmission)

            // when & then
            assertThrows<LineupNotExchangedException> {
                lineupService.getOpponentLineup(gameId = 1L, myTeamId = homeTeamId)
            }
        }

        @Test
        fun `should throw LineupNotExchangedException when my lineup is not yet exchanged`() {
            // given
            val myTeam = createHomeTeamWithId()
            val awayTeam = createAwayTeamWithId()

            // My submission is only SUBMITTED, not EXCHANGED
            val mySubmission = createSubmittedSubmission(myTeam)
            val opponentSubmission = createExchangedSubmission(awayTeam)

            every { lineupSubmissionRepository.findAllByGameId(1L) } returns
                listOf(mySubmission, opponentSubmission)

            // when & then
            assertThrows<LineupNotExchangedException> {
                lineupService.getOpponentLineup(gameId = 1L, myTeamId = homeTeamId)
            }
        }

        @Test
        fun `should throw IllegalArgumentException when my lineup does not exist`() {
            // given
            every { lineupSubmissionRepository.findAllByGameId(1L) } returns emptyList()

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.getOpponentLineup(gameId = 1L, myTeamId = 999L)
                }
            assertThat(exception.message).contains("라인업을 찾을 수 없습니다")
        }

        @Test
        fun `should throw IllegalArgumentException when opponent lineup does not exist`() {
            // given
            val myTeam = createHomeTeamWithId()
            val mySubmission = createExchangedSubmission(myTeam)
            every { lineupSubmissionRepository.findAllByGameId(1L) } returns listOf(mySubmission)

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.getOpponentLineup(gameId = 1L, myTeamId = homeTeamId)
                }
            assertThat(exception.message).contains("상대팀 라인업을 찾을 수 없습니다")
        }
    }

    // Helper methods
    private fun ninePositions(): List<Position> =
        listOf(
            Position.STARTING_PITCHER,
            Position.CATCHER,
            Position.FIRST_BASE,
            Position.SECOND_BASE,
            Position.THIRD_BASE,
            Position.SHORTSTOP,
            Position.LEFT_FIELD,
            Position.CENTER_FIELD,
            Position.RIGHT_FIELD,
        )

    private fun createMockEntry(
        submission: LineupSubmission,
        position: Position,
        battingOrder: Int,
    ): LineupEntry {
        val mockPlayer =
            mockk<Player>().apply {
                every { id } returns battingOrder.toLong()
                every { name } returns "선수$battingOrder"
            }
        return LineupEntry(
            submission = submission,
            player = mockPlayer,
            position = position,
            battingOrder = battingOrder,
            backNumber = battingOrder,
            isStarter = true,
        )
    }

    private fun createSubmissionWithEntries(): LineupSubmission {
        val submission = LineupSubmission.create(game, team, user)
        addEntriesToSubmission(
            submission,
            listOf(
                Position.STARTING_PITCHER,
                Position.CATCHER,
                Position.FIRST_BASE,
                Position.SECOND_BASE,
                Position.THIRD_BASE,
                Position.SHORTSTOP,
                Position.LEFT_FIELD,
                Position.CENTER_FIELD,
                Position.RIGHT_FIELD,
            ),
        )
        return submission
    }

    private fun createSubmissionWithEntriesNoCatcher(): LineupSubmission {
        val submission = LineupSubmission.create(game, team, user)
        addEntriesToSubmission(
            submission,
            listOf(
                Position.STARTING_PITCHER,
                Position.FIRST_BASE,
                Position.SECOND_BASE,
                Position.THIRD_BASE,
                Position.SHORTSTOP,
                Position.LEFT_FIELD,
                Position.CENTER_FIELD,
                Position.RIGHT_FIELD,
                Position.DESIGNATED_HITTER,
            ),
        )
        return submission
    }

    private fun addEntriesToSubmission(
        submission: LineupSubmission,
        positions: List<Position>,
    ) {
        positions.forEachIndexed { index, position ->
            val mockPlayer =
                mockk<Player>().apply {
                    every { id } returns (index + 1).toLong()
                    every { name } returns "선수${index + 1}"
                }
            submission.addEntry(
                LineupEntry(
                    submission = submission,
                    player = mockPlayer,
                    position = position,
                    battingOrder = index + 1,
                    backNumber = index + 1,
                    isStarter = true,
                ),
            )
        }
    }

    private fun createAttendingVotesForPlayers(
        playerIds: List<Long>,
    ): List<com.nextup.core.domain.attendance.AttendanceVote> =
        playerIds.map { playerId ->
            mockk<com.nextup.core.domain.attendance.AttendanceVote>().apply {
                every { voteType } returns com.nextup.core.domain.attendance.VoteType.ATTEND
                every { player } returns
                    mockk<Player>().apply {
                        every { id } returns playerId
                    }
            }
        }
}
