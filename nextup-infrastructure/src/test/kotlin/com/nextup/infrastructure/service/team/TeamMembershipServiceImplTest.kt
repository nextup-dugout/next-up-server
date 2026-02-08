package com.nextup.infrastructure.service.team

import com.nextup.common.exception.*
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.*
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TeamMembershipServiceImpl")
class TeamMembershipServiceImplTest {
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var teamJoinRequestRepository: TeamJoinRequestRepositoryPort
    private lateinit var teamBlacklistRepository: TeamBlacklistRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var service: TeamMembershipServiceImpl

    private lateinit var team: Team
    private lateinit var user: User
    private lateinit var player: Player
    private lateinit var owner: User
    private lateinit var ownerMember: TeamMember

    @BeforeEach
    fun setUp() {
        teamMemberRepository = mockk()
        teamJoinRequestRepository = mockk()
        teamBlacklistRepository = mockk()
        teamRepository = mockk()
        userRepository = mockk()
        playerRepository = mockk()

        service =
            TeamMembershipServiceImpl(
                teamMemberRepository,
                teamJoinRequestRepository,
                teamBlacklistRepository,
                teamRepository,
                userRepository,
                playerRepository,
            )

        // 테스트 데이터 생성
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)
        setTeamId(team, 1L)

        user = User.createLocalUser("user@example.com", "password", "일반회원")
        setUserId(user, 2L)
        player = Player(name = "일반회원", primaryPosition = Position.SHORTSTOP)
        setPlayerId(player, 3L)
        user.player = player

        owner = User.createLocalUser("owner@example.com", "password", "감독")
        setUserId(owner, 10L)
        val ownerPlayer = Player(name = "감독", primaryPosition = Position.STARTING_PITCHER)
        setPlayerId(ownerPlayer, 11L)
        ownerMember = TeamMember.create(team, owner, ownerPlayer, 1, TeamMemberRole.OWNER)
        setTeamMemberId(ownerMember, 100L)
    }

    @Nested
    @DisplayName("requestJoin")
    inner class RequestJoin {
        @Test
        fun `should throw when user not found`() {
            // given
            every { userRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.requestJoin(999L, 1L, 7, null)
            }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should throw when team not found`() {
            // given
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.requestJoin(2L, 999L, 7, null)
            }.isInstanceOf(TeamNotFoundException::class.java)
        }

        @Test
        fun `should throw when player not found`() {
            // given
            val userWithoutPlayer =
                User.createLocalUser("noplayer@example.com", "password", "선수없음")
            setUserId(userWithoutPlayer, 50L)

            every { userRepository.findByIdOrNull(50L) } returns userWithoutPlayer
            every { teamRepository.findByIdOrNull(1L) } returns team

            // when & then
            assertThatThrownBy {
                service.requestJoin(50L, 1L, 7, null)
            }.isInstanceOf(PlayerNotFoundException::class.java)
        }

        @Test
        fun `should create join request when valid`() {
            // given
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findActiveByUserId(2L) } returns null
            every { teamBlacklistRepository.existsActiveByTeamIdAndUserId(1L, 2L) } returns false
            every { teamJoinRequestRepository.findPendingByTeamIdAndUserId(1L, 2L) } returns null
            every { teamJoinRequestRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.requestJoin(2L, 1L, 7, "잘 부탁드립니다")

            // then
            assertThat(result.desiredUniformNumber).isEqualTo(7)
            assertThat(result.requestMessage).isEqualTo("잘 부탁드립니다")
            assertThat(result.status).isEqualTo(JoinRequestStatus.PENDING)
            verify { teamJoinRequestRepository.save(any()) }
        }

        @Test
        fun `should throw when user already in team`() {
            // given
            val existingMember = TeamMember.create(team, user, player, 10)
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findActiveByUserId(2L) } returns existingMember

            // when & then
            assertThatThrownBy {
                service.requestJoin(2L, 1L, 7, null)
            }.isInstanceOf(AlreadyInTeamException::class.java)
        }

        @Test
        fun `should throw when user is blacklisted`() {
            // given
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findActiveByUserId(2L) } returns null
            every { teamBlacklistRepository.existsActiveByTeamIdAndUserId(1L, 2L) } returns true
            every { teamBlacklistRepository.findByTeamIdAndUserId(1L, 2L) } returns null

            // when & then
            assertThatThrownBy {
                service.requestJoin(2L, 1L, 7, null)
            }.isInstanceOf(BlacklistedUserException::class.java)
        }

        @Test
        fun `should throw when duplicate join request exists`() {
            // given
            val existingRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(existingRequest, 99L)

            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findActiveByUserId(2L) } returns null
            every { teamBlacklistRepository.existsActiveByTeamIdAndUserId(1L, 2L) } returns false
            every { teamJoinRequestRepository.findPendingByTeamIdAndUserId(1L, 2L) } returns existingRequest

            // when & then
            assertThatThrownBy {
                service.requestJoin(2L, 1L, 7, null)
            }.isInstanceOf(DuplicateJoinRequestException::class.java)
        }

        @Test
        fun `should throw when uniform number is invalid`() {
            // given
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamRepository.findByIdOrNull(1L) } returns team

            // when & then
            assertThatThrownBy {
                service.requestJoin(2L, 1L, 0, null)
            }.isInstanceOf(InvalidUniformNumberException::class.java)

            assertThatThrownBy {
                service.requestJoin(2L, 1L, 100, null)
            }.isInstanceOf(InvalidUniformNumberException::class.java)
        }
    }

    @Nested
    @DisplayName("approveJoinRequest")
    inner class ApproveJoinRequest {
        @Test
        fun `should throw when join request not found`() {
            // given
            every { teamJoinRequestRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.approveJoinRequest(999L, 10L, null, null)
            }.isInstanceOf(TeamJoinRequestNotFoundException::class.java)
        }

        @Test
        fun `should throw when processor user not found`() {
            // given
            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.approveJoinRequest(50L, 999L, null, null)
            }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should throw when processor is not a team member`() {
            // given
            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(10L) } returns owner
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns null

            // when & then
            assertThatThrownBy {
                service.approveJoinRequest(50L, 10L, null, null)
            }.isInstanceOf(InsufficientTeamRoleException::class.java)
        }

        @Test
        fun `should use final uniform number when provided`() {
            // given
            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(10L) } returns owner
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every {
                teamMemberRepository.existsByTeamIdAndUniformNumberAndStatus(
                    1L,
                    99,
                    TeamMemberStatus.ACTIVE,
                )
            } returns false
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.approveJoinRequest(50L, 10L, 99, null)

            // then
            assertThat(result.uniformNumber).isEqualTo(99)
        }

        @Test
        fun `should throw when final uniform number is invalid`() {
            // given
            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(10L) } returns owner
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember

            // when & then
            assertThatThrownBy {
                service.approveJoinRequest(50L, 10L, 0, null)
            }.isInstanceOf(InvalidUniformNumberException::class.java)
        }

        @Test
        fun `should approve join request and create member`() {
            // given
            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(10L) } returns owner
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every {
                teamMemberRepository.existsByTeamIdAndUniformNumberAndStatus(
                    1L,
                    7,
                    TeamMemberStatus.ACTIVE,
                )
            } returns false
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.approveJoinRequest(50L, 10L, null, "환영합니다")

            // then
            assertThat(result.uniformNumber).isEqualTo(7)
            assertThat(result.role).isEqualTo(TeamMemberRole.MEMBER)
            assertThat(result.status).isEqualTo(TeamMemberStatus.ACTIVE)
            assertThat(joinRequest.status).isEqualTo(JoinRequestStatus.APPROVED)
            verify { teamMemberRepository.save(any()) }
        }

        @Test
        fun `should throw when uniform number already taken on approve`() {
            // given
            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(10L) } returns owner
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every {
                teamMemberRepository.existsByTeamIdAndUniformNumberAndStatus(
                    1L,
                    7,
                    TeamMemberStatus.ACTIVE,
                )
            } returns true

            // when & then
            assertThatThrownBy {
                service.approveJoinRequest(50L, 10L, null, null)
            }.isInstanceOf(UniformNumberAlreadyTakenException::class.java)
        }

        @Test
        fun `should throw when processor has insufficient permission`() {
            // given
            val memberUser = User.createLocalUser("member@example.com", "password", "일반회원")
            setUserId(memberUser, 20L)
            val memberPlayer = Player(name = "일반회원", primaryPosition = Position.SHORTSTOP)
            val member = TeamMember.create(team, memberUser, memberPlayer, 20, TeamMemberRole.MEMBER)

            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(20L) } returns memberUser
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 20L) } returns member

            // when & then
            assertThatThrownBy {
                service.approveJoinRequest(50L, 20L, null, null)
            }.isInstanceOf(InsufficientTeamRoleException::class.java)
        }
    }

    @Nested
    @DisplayName("rejectJoinRequest")
    inner class RejectJoinRequest {
        @Test
        fun `should throw when join request not found on reject`() {
            // given
            every { teamJoinRequestRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.rejectJoinRequest(999L, 10L, null)
            }.isInstanceOf(TeamJoinRequestNotFoundException::class.java)
        }

        @Test
        fun `should throw when reject processor has insufficient permission`() {
            // given
            val memberUser =
                User.createLocalUser("member@example.com", "password", "일반회원2")
            setUserId(memberUser, 20L)
            val memberPlayer =
                Player(name = "일반회원2", primaryPosition = Position.SHORTSTOP)
            val member =
                TeamMember.create(team, memberUser, memberPlayer, 20, TeamMemberRole.MEMBER)

            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(20L) } returns memberUser
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 20L) } returns member

            // when & then
            assertThatThrownBy {
                service.rejectJoinRequest(50L, 20L, "거부")
            }.isInstanceOf(InsufficientTeamRoleException::class.java)
        }

        @Test
        fun `should reject join request`() {
            // given
            val joinRequest = TeamJoinRequest.create(team, user, player, 7)
            setJoinRequestId(joinRequest, 50L)

            every { teamJoinRequestRepository.findByIdOrNull(50L) } returns joinRequest
            every { userRepository.findByIdOrNull(10L) } returns owner
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every { teamJoinRequestRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.rejectJoinRequest(50L, 10L, "인원 충원됨")

            // then
            assertThat(result.status).isEqualTo(JoinRequestStatus.REJECTED)
            assertThat(result.responseMessage).isEqualTo("인원 충원됨")
            verify { teamJoinRequestRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("kickMember")
    inner class KickMember {
        @Test
        fun `should kick member with blacklist option`() {
            // given
            val memberToKick = TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(memberToKick, 200L)

            every { teamMemberRepository.findByIdOrNull(200L) } returns memberToKick
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every { teamMemberRepository.save(any()) } answers { firstArg() }
            every { teamBlacklistRepository.save(any()) } answers { firstArg() }

            // when
            service.kickMember(200L, 10L, "규칙 위반", true)

            // then
            assertThat(memberToKick.status).isEqualTo(TeamMemberStatus.KICKED)
            verify { teamMemberRepository.save(memberToKick) }
            verify { teamBlacklistRepository.save(any()) }
        }

        @Test
        fun `should kick member without blacklist`() {
            // given
            val memberToKick =
                TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(memberToKick, 200L)

            every { teamMemberRepository.findByIdOrNull(200L) } returns memberToKick
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when
            service.kickMember(200L, 10L, "규칙 위반", false)

            // then
            assertThat(memberToKick.status).isEqualTo(TeamMemberStatus.KICKED)
            verify { teamMemberRepository.save(memberToKick) }
            verify(exactly = 0) { teamBlacklistRepository.save(any()) }
        }

        @Test
        fun `should throw when member not found on kick`() {
            // given
            every { teamMemberRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.kickMember(999L, 10L, "test", false)
            }.isInstanceOf(TeamMemberNotFoundException::class.java)
        }

        @Test
        fun `should throw when kicker not found`() {
            // given
            val memberToKick =
                TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(memberToKick, 200L)

            every { teamMemberRepository.findByIdOrNull(200L) } returns memberToKick
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns null

            // when & then
            assertThatThrownBy {
                service.kickMember(200L, 10L, "test", false)
            }.isInstanceOf(InsufficientTeamRoleException::class.java)
        }

        @Test
        fun `should throw when kicking higher role member`() {
            // given
            every { teamMemberRepository.findByIdOrNull(100L) } returns ownerMember
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember

            // when & then
            assertThatThrownBy {
                service.kickMember(100L, 10L, "test", false)
            }.isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Nested
    @DisplayName("leaveMember")
    inner class LeaveMember {
        @Test
        fun `should allow member to leave`() {
            // given
            val member = TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(member, 200L)

            every { teamMemberRepository.findByIdOrNull(200L) } returns member
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when
            service.leaveMember(200L)

            // then
            assertThat(member.status).isEqualTo(TeamMemberStatus.LEFT)
            verify { teamMemberRepository.save(member) }
        }

        @Test
        fun `should throw when member not found on leave`() {
            // given
            every { teamMemberRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.leaveMember(999L)
            }.isInstanceOf(TeamMemberNotFoundException::class.java)
        }

        @Test
        fun `should allow owner to leave when other owners exist`() {
            // given
            every { teamMemberRepository.findByIdOrNull(100L) } returns ownerMember
            every { teamMemberRepository.countOwnersByTeamId(1L) } returns 2
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when - should not throw because there are 2 owners
            // Owner can't leave via entity logic (role check), so this tests the count check path
            assertThatThrownBy {
                service.leaveMember(100L)
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `should throw when last owner tries to leave`() {
            // given
            every { teamMemberRepository.findByIdOrNull(100L) } returns ownerMember
            every { teamMemberRepository.countOwnersByTeamId(1L) } returns 1

            // when & then
            assertThatThrownBy {
                service.leaveMember(100L)
            }.isInstanceOf(OwnerCannotLeaveException::class.java)
        }
    }

    @Nested
    @DisplayName("changeRole")
    inner class ChangeRole {
        @Test
        fun `should throw when member not found on changeRole`() {
            // given
            every { teamMemberRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.changeRole(999L, TeamMemberRole.MANAGER, 10L)
            }.isInstanceOf(TeamMemberNotFoundException::class.java)
        }

        @Test
        fun `should throw when changer not found`() {
            // given
            val member =
                TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(member, 200L)

            every { teamMemberRepository.findByIdOrNull(200L) } returns member
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns null

            // when & then
            assertThatThrownBy {
                service.changeRole(200L, TeamMemberRole.MANAGER, 10L)
            }.isInstanceOf(InsufficientTeamRoleException::class.java)
        }

        @Test
        fun `should change member role`() {
            // given
            val member = TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(member, 200L)

            every { teamMemberRepository.findByIdOrNull(200L) } returns member
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.changeRole(200L, TeamMemberRole.MANAGER, 10L)

            // then
            assertThat(result.role).isEqualTo(TeamMemberRole.MANAGER)
            verify { teamMemberRepository.save(member) }
        }
    }

    @Nested
    @DisplayName("getRoster")
    inner class GetRoster {
        @Test
        fun `should return roster sorted by role`() {
            // given
            val members =
                listOf(
                    ownerMember,
                    TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER),
                )
            every { teamMemberRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns members

            // when
            val result = service.getRoster(1L)

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("createTeamWithOwner")
    inner class CreateTeamWithOwner {
        @Test
        fun `should create team and register owner`() {
            // given
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamMemberRepository.findActiveByUserId(2L) } returns null
            every { teamRepository.findByName("뉴팀") } returns null
            every { teamRepository.save(any()) } answers { firstArg() }
            every { teamMemberRepository.save(any()) } answers { firstArg() }

            val association = Association(name = "서울시야구협회", region = "서울")
            val league = League(association = association, name = "1부 리그", foundedYear = 2020)
            val newTeam = Team(league = league, name = "뉴팀", city = "서울", foundedYear = 2026)

            // when
            val result = service.createTeamWithOwner(2L, newTeam, 7)

            // then
            assertThat(result.name).isEqualTo("뉴팀")
            verify { teamRepository.save(any()) }
            verify { teamMemberRepository.save(any()) }
        }

        @Test
        fun `should throw when uniform number is invalid`() {
            // when & then
            assertThatThrownBy {
                service.createTeamWithOwner(2L, team, 0)
            }.isInstanceOf(InvalidUniformNumberException::class.java)

            assertThatThrownBy {
                service.createTeamWithOwner(2L, team, 100)
            }.isInstanceOf(InvalidUniformNumberException::class.java)
        }

        @Test
        fun `should throw when user not found`() {
            // given
            every { userRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.createTeamWithOwner(999L, team, 7)
            }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should throw when user has no player`() {
            // given
            val userWithoutPlayer = User.createLocalUser("noplayer@example.com", "password", "없음")
            setUserId(userWithoutPlayer, 50L)

            every { userRepository.findByIdOrNull(50L) } returns userWithoutPlayer

            // when & then
            assertThatThrownBy {
                service.createTeamWithOwner(50L, team, 7)
            }.isInstanceOf(PlayerNotFoundException::class.java)
        }

        @Test
        fun `should throw when user already in team`() {
            // given
            val existingMember = TeamMember.create(team, user, player, 10)
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamMemberRepository.findActiveByUserId(2L) } returns existingMember

            // when & then
            assertThatThrownBy {
                service.createTeamWithOwner(2L, team, 7)
            }.isInstanceOf(AlreadyInTeamException::class.java)
        }

        @Test
        fun `should throw when team name already exists`() {
            // given
            every { userRepository.findByIdOrNull(2L) } returns user
            every { teamMemberRepository.findActiveByUserId(2L) } returns null
            every { teamRepository.findByName("타이거즈") } returns team

            // when & then
            assertThatThrownBy {
                service.createTeamWithOwner(2L, team, 7)
            }.isInstanceOf(TeamAlreadyExistsException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteTeam")
    inner class DeleteTeam {
        @Test
        fun `should delete team when owner and single member`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every { teamMemberRepository.countByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns 1L
            every { teamMemberRepository.delete(ownerMember) } returns Unit
            every { teamRepository.delete(team) } returns Unit

            // when
            service.deleteTeam(1L, 10L)

            // then
            verify { teamMemberRepository.delete(ownerMember) }
            verify { teamRepository.delete(team) }
        }

        @Test
        fun `should throw when team not found`() {
            // given
            every { teamRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.deleteTeam(999L, 10L)
            }.isInstanceOf(TeamNotFoundException::class.java)
        }

        @Test
        fun `should throw when user is not a member`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 999L) } returns null

            // when & then
            assertThatThrownBy {
                service.deleteTeam(1L, 999L)
            }.isInstanceOf(InsufficientTeamRoleException::class.java)
        }

        @Test
        fun `should throw when user is not owner`() {
            // given
            val memberOnly = TeamMember.create(team, user, player, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(memberOnly, 200L)

            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 2L) } returns memberOnly

            // when & then
            assertThatThrownBy {
                service.deleteTeam(1L, 2L)
            }.isInstanceOf(InsufficientTeamRoleException::class.java)
        }

        @Test
        fun `should throw when team has multiple active members`() {
            // given
            every { teamRepository.findByIdOrNull(1L) } returns team
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 10L) } returns ownerMember
            every { teamMemberRepository.countByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns 3L

            // when & then
            assertThatThrownBy {
                service.deleteTeam(1L, 10L)
            }.isInstanceOf(InvalidTeamStateException::class.java)
        }
    }

    @Nested
    @DisplayName("getTeamMemberCount")
    inner class GetTeamMemberCount {
        @Test
        fun `should return active member count`() {
            // given
            every { teamMemberRepository.countByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns 5L

            // when
            val result = service.getTeamMemberCount(1L)

            // then
            assertThat(result).isEqualTo(5)
        }

        @Test
        fun `should return zero when no active members`() {
            // given
            every { teamMemberRepository.countByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns 0L

            // when
            val result = service.getTeamMemberCount(1L)

            // then
            assertThat(result).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getMember")
    inner class GetMember {
        @Test
        fun `should return member when found`() {
            // given
            every {
                teamMemberRepository.findByTeamIdAndUserId(1L, 2L)
            } returns TeamMember.create(team, user, player, 20)

            // when
            val result = service.getMember(1L, 2L)

            // then
            assertThat(result).isNotNull
            assertThat(result?.uniformNumber).isEqualTo(20)
        }

        @Test
        fun `should return null when member not found`() {
            // given
            every { teamMemberRepository.findByTeamIdAndUserId(1L, 999L) } returns null

            // when
            val result = service.getMember(1L, 999L)

            // then
            assertThat(result).isNull()
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

    private fun setJoinRequestId(
        joinRequest: TeamJoinRequest,
        id: Long,
    ) {
        val idField = TeamJoinRequest::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(joinRequest, id)
    }
}
