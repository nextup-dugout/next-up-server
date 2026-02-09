package com.nextup.infrastructure.service.game

import com.nextup.common.exception.*
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.AttendanceVote
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("AttendanceServiceImpl")
class AttendanceServiceImplTest {
    private lateinit var attendanceVoteRepository: AttendanceVoteRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var service: AttendanceServiceImpl

    private lateinit var game: Game
    private lateinit var member: TeamMember
    private lateinit var team: Team

    @BeforeEach
    fun setUp() {
        attendanceVoteRepository = mockk()
        teamMemberRepository = mockk()
        gameRepository = mockk()
        gameTeamRepository = mockk()

        service =
            AttendanceServiceImpl(
                attendanceVoteRepository,
                teamMemberRepository,
                gameRepository,
                gameTeamRepository,
            )

        // 테스트 데이터 생성
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)
        setTeamId(team, 1L)

        val competition = mockk<com.nextup.core.domain.competition.Competition>()
        game = Game(competition = competition, scheduledAt = LocalDateTime.now().plusDays(7))
        setGameId(game, 100L)

        val user = User.createLocalUser("member@example.com", "password", "회원")
        setUserId(user, 10L)
        val player = Player(name = "회원", primaryPosition = Position.SHORTSTOP)
        setPlayerId(player, 11L)
        member = TeamMember.create(team, user, player, 7, TeamMemberRole.MEMBER)
        setTeamMemberId(member, 200L)
    }

    @Nested
    @DisplayName("vote")
    inner class Vote {
        @Test
        fun `should vote for game`() {
            // given
            every { gameRepository.findByIdOrNull(100L) } returns game
            every { teamMemberRepository.findByIdOrNull(200L) } returns member
            every { attendanceVoteRepository.findByGameIdAndMemberId(100L, 200L) } returns null
            every { attendanceVoteRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.vote(100L, 200L, AttendanceStatus.ATTENDING, "참석합니다")

            // then
            assertThat(result.status).isEqualTo(AttendanceStatus.ATTENDING)
            assertThat(result.reason).isEqualTo("참석합니다")
            assertThat(result.hasResponded).isTrue()
            verify { attendanceVoteRepository.save(any()) }
        }

        @Test
        fun `should change existing vote`() {
            // given
            val existingVote = AttendanceVote.createForGame(game, member)
            existingVote.vote(AttendanceStatus.ATTENDING)

            every { gameRepository.findByIdOrNull(100L) } returns game
            every { teamMemberRepository.findByIdOrNull(200L) } returns member
            every { attendanceVoteRepository.findByGameIdAndMemberId(100L, 200L) } returns existingVote
            every { attendanceVoteRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.vote(100L, 200L, AttendanceStatus.ABSENT, "일정 변경")

            // then
            assertThat(result.status).isEqualTo(AttendanceStatus.ABSENT)
            assertThat(result.reason).isEqualTo("일정 변경")
            verify { attendanceVoteRepository.save(existingVote) }
        }

        @Test
        fun `should throw when member not in team`() {
            // given
            every { gameRepository.findByIdOrNull(100L) } returns game
            every { teamMemberRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.vote(100L, 999L, AttendanceStatus.ATTENDING, null)
            }.isInstanceOf(TeamMemberNotFoundException::class.java)
        }

        @Test
        fun `should throw when member cannot vote`() {
            // given
            val kickedMember = TeamMember.create(team, member.user, member.player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(kickedMember, 201L)
            val owner = TeamMember.create(team, member.user, member.player, 1, TeamMemberRole.OWNER)
            setTeamMemberId(owner, 1L)
            kickedMember.kick("test", owner)

            every { gameRepository.findByIdOrNull(100L) } returns game
            every { teamMemberRepository.findByIdOrNull(201L) } returns kickedMember

            // when & then
            assertThatThrownBy {
                service.vote(100L, 201L, AttendanceStatus.ATTENDING, null)
            }.isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `should throw when game already started`() {
            // given
            game.start()

            every { gameRepository.findByIdOrNull(100L) } returns game
            every { teamMemberRepository.findByIdOrNull(200L) } returns member

            // when & then
            assertThatThrownBy {
                service.vote(100L, 200L, AttendanceStatus.ATTENDING, null)
            }.isInstanceOf(VoteClosedException::class.java)
        }
    }

    @Nested
    @DisplayName("getVoteSummary")
    inner class GetVoteSummary {
        @Test
        fun `should get vote summary with counts`() {
            // given
            val votes =
                listOf(
                    AttendanceVote.createForGame(game, member).apply { vote(AttendanceStatus.ATTENDING) },
                    AttendanceVote.createForGame(game, member).apply { vote(AttendanceStatus.ATTENDING) },
                    AttendanceVote.createForGame(game, member).apply { vote(AttendanceStatus.ABSENT) },
                    AttendanceVote.createForGame(game, member),
                )

            every { gameRepository.findByIdOrNull(100L) } returns game
            every { attendanceVoteRepository.findByGameId(100L) } returns votes

            // when
            val result = service.getVoteSummary(100L)

            // then
            assertThat(result.gameId).isEqualTo(100L)
            assertThat(result.totalMembers).isEqualTo(4)
            assertThat(result.attending).isEqualTo(2)
            assertThat(result.absent).isEqualTo(1)
            assertThat(result.undecided).isEqualTo(1)
        }

        @Test
        fun `should throw when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.getVoteSummary(999L)
            }.isInstanceOf(GameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getNonVoters")
    inner class GetNonVoters {
        @Test
        fun `should get non-voters list`() {
            // given
            val member1 = TeamMember.create(team, member.user, member.player, 1, TeamMemberRole.MEMBER)
            val member2 = TeamMember.create(team, member.user, member.player, 2, TeamMemberRole.MEMBER)
            val member3 = TeamMember.create(team, member.user, member.player, 3, TeamMemberRole.MEMBER)

            val votes =
                listOf(
                    AttendanceVote.createForGame(game, member1).apply { vote(AttendanceStatus.ATTENDING) },
                    AttendanceVote.createForGame(game, member2), // UNDECIDED
                    AttendanceVote.createForGame(game, member3), // UNDECIDED
                )

            every { attendanceVoteRepository.findByGameId(100L) } returns votes

            // when
            val result = service.getNonVoters(100L)

            // then
            assertThat(result).hasSize(2)
            assertThat(result).allMatch { it.uniformNumber in listOf(2, 3) }
        }
    }

    @Nested
    @DisplayName("createVotesForGame")
    inner class CreateVotesForGame {
        @Test
        fun `should create votes for all active members`() {
            // given
            val gameTeam1 = mockk<com.nextup.core.domain.game.GameTeam>()
            val gameTeam2 = mockk<com.nextup.core.domain.game.GameTeam>()
            every { gameTeam1.team } returns team
            every { gameTeam2.team } returns team

            val members =
                listOf(
                    TeamMember.create(team, member.user, member.player, 1, TeamMemberRole.MEMBER),
                    TeamMember.create(team, member.user, member.player, 2, TeamMemberRole.MEMBER),
                )
            setTeamMemberId(members[0], 1L)
            setTeamMemberId(members[1], 2L)

            every { gameRepository.findByIdOrNull(100L) } returns game
            every { gameTeamRepository.findAllByGameId(100L) } returns listOf(gameTeam1, gameTeam2)
            every { teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns members
            every { attendanceVoteRepository.findByGameId(100L) } returns emptyList()
            every { attendanceVoteRepository.saveAll(any<List<AttendanceVote>>()) } answers { firstArg() }

            // when
            val result = service.createVotesForGame(100L)

            // then
            assertThat(result).hasSize(4) // 2 members x 2 teams
            assertThat(result).allMatch { it.status == AttendanceStatus.UNDECIDED }
            verify { attendanceVoteRepository.saveAll(any<List<AttendanceVote>>()) }
        }

        @Test
        fun `should throw when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.createVotesForGame(999L)
            }.isInstanceOf(GameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getVotesByGameId")
    inner class GetVotesByGameId {
        @Test
        fun `should return votes for game`() {
            // given
            val votes =
                listOf(
                    AttendanceVote.createForGame(game, member).apply { vote(AttendanceStatus.ATTENDING) },
                    AttendanceVote.createForGame(game, member).apply { vote(AttendanceStatus.ABSENT) },
                )
            every { attendanceVoteRepository.findByGameId(100L) } returns votes

            // when
            val result = service.getVotesByGameId(100L)

            // then
            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(votes)
            verify { attendanceVoteRepository.findByGameId(100L) }
        }
    }

    @Nested
    @DisplayName("verifyGameTeamMember")
    inner class VerifyGameTeamMember {
        @Test
        fun `should pass when user is a member of a game team`() {
            // given
            val user = User.createLocalUser("user@example.com", "password", "사용자")
            setUserId(user, 10L)
            val player = Player(name = "사용자", primaryPosition = Position.STARTING_PITCHER)
            val teamMember = TeamMember.create(team, user, player, 5, TeamMemberRole.MEMBER)

            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>()
            every { gameTeam.team } returns team

            every { gameTeamRepository.findAllByGameId(100L) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(1L) } returns listOf(teamMember)

            // when & then
            service.verifyGameTeamMember(100L, 10L)

            verify { gameTeamRepository.findAllByGameId(100L) }
            verify { teamMemberRepository.findByTeamId(1L) }
        }

        @Test
        fun `should throw when user is not a member of any game team`() {
            // given
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>()
            every { gameTeam.team } returns team

            every { gameTeamRepository.findAllByGameId(100L) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(1L) } returns emptyList()

            // when & then
            assertThatThrownBy {
                service.verifyGameTeamMember(100L, 999L)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage("You are not a member of either team in this game")
        }
    }

    @Nested
    @DisplayName("findMemberInGame")
    inner class FindMemberInGame {
        @Test
        fun `should return member when found in game team`() {
            // given
            val user = User.createLocalUser("user@example.com", "password", "사용자")
            setUserId(user, 10L)
            val player = Player(name = "사용자", primaryPosition = Position.STARTING_PITCHER)
            val teamMember = TeamMember.create(team, user, player, 5, TeamMemberRole.MEMBER)

            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>()
            every { gameTeam.team } returns team

            every { gameRepository.findByIdOrNull(100L) } returns game
            every { gameTeamRepository.findAllByGameId(100L) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(1L) } returns listOf(teamMember)

            // when
            val result = service.findMemberInGame(100L, 10L)

            // then
            assertThat(result).isEqualTo(teamMember)
            verify { gameRepository.findByIdOrNull(100L) }
            verify { gameTeamRepository.findAllByGameId(100L) }
            verify { teamMemberRepository.findByTeamId(1L) }
        }

        @Test
        fun `should throw GameNotFoundException when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.findMemberInGame(999L, 10L)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `should throw IllegalStateException when user not in any team`() {
            // given
            val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>()
            every { gameTeam.team } returns team

            every { gameRepository.findByIdOrNull(100L) } returns game
            every { gameTeamRepository.findAllByGameId(100L) } returns listOf(gameTeam)
            every { teamMemberRepository.findByTeamId(1L) } returns emptyList()

            // when & then
            assertThatThrownBy {
                service.findMemberInGame(100L, 999L)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage("You are not a member of either team in this game")
        }
    }

    @Nested
    @DisplayName("getGameScheduledAt")
    inner class GetGameScheduledAt {
        @Test
        fun `should return game scheduled time`() {
            // given
            val scheduledTime = LocalDateTime.of(2026, 3, 15, 14, 0)
            val futureGame =
                Game(
                    competition = mockk<com.nextup.core.domain.competition.Competition>(),
                    scheduledAt = scheduledTime,
                )
            setGameId(futureGame, 100L)

            every { gameRepository.findByIdOrNull(100L) } returns futureGame

            // when
            val result = service.getGameScheduledAt(100L)

            // then
            assertThat(result).isEqualTo(scheduledTime)
            verify { gameRepository.findByIdOrNull(100L) }
        }

        @Test
        fun `should throw GameNotFoundException when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.getGameScheduledAt(999L)
            }.isInstanceOf(GameNotFoundException::class.java)
        }
    }

    private fun setTeamId(
        team: Team,
        id: Long,
    ) {
        val idField = Team::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(team, id)
    }

    private fun setGameId(
        game: Game,
        id: Long,
    ) {
        val idField = Game::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(game, id)
    }

    private fun setUserId(
        user: User,
        id: Long,
    ) {
        val idField = User::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, id)
    }

    private fun setPlayerId(
        player: Player,
        id: Long,
    ) {
        val idField = Player::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(player, id)
    }

    private fun setTeamMemberId(
        teamMember: TeamMember,
        id: Long,
    ) {
        val idField = TeamMember::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(teamMember, id)
    }
}
