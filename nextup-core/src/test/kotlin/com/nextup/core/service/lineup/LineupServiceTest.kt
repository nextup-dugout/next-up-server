package com.nextup.core.service.lineup

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
    private lateinit var attendanceVoteRepository: com.nextup.core.port.repository.AttendanceVoteRepositoryPort
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

        game = Game(competition = competition, scheduledAt = LocalDateTime.now().plusDays(1), location = "서울야구장")
        team = Team(league = league, name = "Tigers", city = "서울", foundedYear = 2020)
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
                    lineupService.addLineupEntry(1L, 1L, Position.SHORTSTOP, 1, 7, true)
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
                    lineupService.addLineupEntry(1L, 1L, Position.SHORTSTOP, 1, 7, true)
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

            val entries =
                listOf(
                    LineupEntryInput(
                        playerId = 1L,
                        position = Position.STARTING_PITCHER,
                        battingOrder = 1,
                        backNumber = 1,
                        isStarter = true,
                    ),
                    LineupEntryInput(
                        playerId = 2L,
                        position = Position.CATCHER,
                        battingOrder = 2,
                        backNumber = 2,
                        isStarter = true,
                    ),
                )

            // when
            val result = lineupService.setLineupEntries(1L, entries)

            // then
            assertThat(result).hasSize(2)
            verify { lineupEntryRepository.deleteAllBySubmissionId(any()) }
        }

        @Test
        fun `should throw exception when duplicate players`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { lineupEntryRepository.deleteAllBySubmissionId(any()) } returns Unit

            val entries =
                listOf(
                    LineupEntryInput(
                        playerId = 1L,
                        position = Position.STARTING_PITCHER,
                        battingOrder = 1,
                        backNumber = 1,
                        isStarter = true,
                    ),
                    LineupEntryInput(
                        playerId = 1L,
                        position = Position.CATCHER,
                        battingOrder = 2,
                        backNumber = 2,
                        isStarter = true,
                    ),
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.setLineupEntries(1L, entries)
                }
            assertThat(exception.message).contains("중복된 선수")
        }

        @Test
        fun `should throw exception when duplicate batting order`() {
            // given
            val submission = LineupSubmission.create(game, team, user)
            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { lineupEntryRepository.deleteAllBySubmissionId(any()) } returns Unit

            val entries =
                listOf(
                    LineupEntryInput(
                        playerId = 1L,
                        position = Position.STARTING_PITCHER,
                        battingOrder = 1,
                        backNumber = 1,
                        isStarter = true,
                    ),
                    LineupEntryInput(
                        playerId = 2L,
                        position = Position.CATCHER,
                        battingOrder = 1,
                        backNumber = 2,
                        isStarter = true,
                    ),
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    lineupService.setLineupEntries(1L, entries)
                }
            assertThat(exception.message).contains("중복된 타순")
        }
    }

    @Nested
    inner class SubmitLineupTest {
        @Test
        fun `should submit lineup successfully with 9 starters`() {
            // given
            val submission = createSubmissionWithEntries()
            val attendingVotes = createAttendingVotesForPlayers(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every {
                attendanceVoteRepository.findByGameIdAndStatus(
                    any(),
                    com.nextup.core.domain.game.AttendanceStatus.ATTENDING
                )
            } returns attendingVotes

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
                attendanceVoteRepository.findByGameIdAndStatus(
                    any(),
                    com.nextup.core.domain.game.AttendanceStatus.ATTENDING
                )
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
                attendanceVoteRepository.findByGameIdAndStatus(
                    any(),
                    com.nextup.core.domain.game.AttendanceStatus.ATTENDING
                )
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
                attendanceVoteRepository.findByGameIdAndStatus(
                    any(),
                    com.nextup.core.domain.game.AttendanceStatus.ATTENDING
                )
            } returns attendingVotes

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

        @Test
        fun `should publish LineupConfirmedEvent when lineup is confirmed`() {
            // given
            val submission = createSubmissionWithEntries().apply { submit() }
            val scorer =
                User.createLocalUser(email = "scorer@test.com", encodedPassword = "encoded", nickname = "기록원")

            every { lineupSubmissionRepository.findByIdOrNull(any()) } returns submission
            every { userRepository.findByIdOrNull(any()) } returns scorer

            val submissionSlot = slot<LineupSubmission>()
            every { lineupSubmissionRepository.save(capture(submissionSlot)) } answers { submissionSlot.captured }

            val eventSlot = slot<LineupConfirmedEvent>()
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            // when
            lineupService.confirmLineup(1L, 1L)

            // then
            verify(exactly = 1) { eventPublisher.publishEvent(any<LineupConfirmedEvent>()) }
            assertThat(eventSlot.captured).isNotNull
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

    // Helper methods
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

    private fun createMinimalLineup(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createMockEntry(submission, Position.STARTING_PITCHER, 1),
            createMockEntry(submission, Position.CATCHER, 2),
            createMockEntry(submission, Position.FIRST_BASE, 3),
            createMockEntry(submission, Position.SECOND_BASE, 4),
            createMockEntry(submission, Position.THIRD_BASE, 5),
            createMockEntry(submission, Position.SHORTSTOP, 6),
            createMockEntry(submission, Position.LEFT_FIELD, 7),
            createMockEntry(submission, Position.CENTER_FIELD, 8),
            createMockEntry(submission, Position.RIGHT_FIELD, 9),
        )

    private fun createMinimalLineupWithoutPitcher(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createMockEntry(submission, Position.CATCHER, 1),
            createMockEntry(submission, Position.FIRST_BASE, 2),
            createMockEntry(submission, Position.SECOND_BASE, 3),
            createMockEntry(submission, Position.THIRD_BASE, 4),
            createMockEntry(submission, Position.SHORTSTOP, 5),
            createMockEntry(submission, Position.LEFT_FIELD, 6),
            createMockEntry(submission, Position.CENTER_FIELD, 7),
            createMockEntry(submission, Position.RIGHT_FIELD, 8),
            createMockEntry(submission, Position.DESIGNATED_HITTER, 9),
        )

    private fun createMinimalLineupWithoutCatcher(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createMockEntry(submission, Position.STARTING_PITCHER, 1),
            createMockEntry(submission, Position.FIRST_BASE, 2),
            createMockEntry(submission, Position.SECOND_BASE, 3),
            createMockEntry(submission, Position.THIRD_BASE, 4),
            createMockEntry(submission, Position.SHORTSTOP, 5),
            createMockEntry(submission, Position.LEFT_FIELD, 6),
            createMockEntry(submission, Position.CENTER_FIELD, 7),
            createMockEntry(submission, Position.RIGHT_FIELD, 8),
            createMockEntry(submission, Position.DESIGNATED_HITTER, 9),
        )

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
    ): List<com.nextup.core.domain.game.GameParticipation> =
        playerIds.map { playerId ->
            mockk<com.nextup.core.domain.game.GameParticipation>().apply {
                every { member } returns
                    mockk<com.nextup.core.domain.team.TeamMember>().apply {
                        every { team } returns this@LineupServiceTest.team
                        every { player } returns
                            mockk<Player>().apply {
                                every { id } returns playerId
                            }
                    }
            }
        }
}
