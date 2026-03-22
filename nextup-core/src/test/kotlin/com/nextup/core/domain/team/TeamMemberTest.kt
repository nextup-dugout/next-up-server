package com.nextup.core.domain.team

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("TeamMember 엔티티 테스트")
class TeamMemberTest {
    private lateinit var team: Team
    private lateinit var ownerUser: User
    private lateinit var memberUser: User
    private lateinit var ownerPlayer: Player
    private lateinit var memberPlayer: Player
    private lateinit var owner: TeamMember
    private lateinit var member: TeamMember

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)

        ownerUser = User.createLocalUser("owner@example.com", "password", "감독님")
        memberUser = User.createLocalUser("member@example.com", "password", "일반회원")

        ownerPlayer = Player(name = "감독님", primaryPosition = Position.STARTING_PITCHER)
        memberPlayer = Player(name = "일반회원", primaryPosition = Position.SHORTSTOP)

        owner = TeamMember.create(team, ownerUser, ownerPlayer, 1, TeamMemberRole.OWNER)
        member = TeamMember.create(team, memberUser, memberPlayer, 7, TeamMemberRole.MEMBER)

        // ID 설정 (테스트용)
        setTeamMemberId(owner, 1L)
        setTeamMemberId(member, 2L)
    }

    @Nested
    @DisplayName("팀 멤버 생성")
    inner class Create {
        @Test
        fun `should create team member with default values`() {
            // when
            val newMember =
                TeamMember.create(
                    team = team,
                    user = memberUser,
                    player = memberPlayer,
                    uniformNumber = 10,
                )

            // then
            assertThat(newMember.team).isEqualTo(team)
            assertThat(newMember.user).isEqualTo(memberUser)
            assertThat(newMember.player).isEqualTo(memberPlayer)
            assertThat(newMember.uniformNumber).isEqualTo(10)
            assertThat(newMember.role).isEqualTo(TeamMemberRole.MEMBER)
            assertThat(newMember.status).isEqualTo(TeamMemberStatus.ACTIVE)
            assertThat(newMember.isActive).isTrue()
        }

        @Test
        fun `should throw exception when uniform number is out of range`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                TeamMember.create(team, memberUser, memberPlayer, 0)
            }

            assertThrows<IllegalArgumentException> {
                TeamMember.create(team, memberUser, memberPlayer, 100)
            }
        }
    }

    @Nested
    @DisplayName("역할 변경")
    inner class ChangeRole {
        @Test
        fun `should change role when changed by owner`() {
            // when
            member.changeRole(TeamMemberRole.MANAGER, owner)

            // then
            assertThat(member.role).isEqualTo(TeamMemberRole.MANAGER)
        }

        @Test
        fun `should throw when non-owner tries to change role`() {
            // given
            val anotherMember = TeamMember.create(team, memberUser, memberPlayer, 20, TeamMemberRole.MEMBER)
            setTeamMemberId(anotherMember, 3L)

            // when & then
            assertThrows<IllegalStateException> {
                member.changeRole(TeamMemberRole.MANAGER, anotherMember)
            }
        }

        @Test
        fun `should throw when trying to change own role`() {
            // when & then
            assertThrows<IllegalStateException> {
                owner.changeRole(TeamMemberRole.MEMBER, owner)
            }
        }

        @Test
        fun `should throw when trying to change role to OWNER`() {
            // when & then
            assertThrows<IllegalStateException> {
                member.changeRole(TeamMemberRole.OWNER, owner)
            }
        }
    }

    @Nested
    @DisplayName("강퇴")
    inner class Kick {
        @Test
        fun `should kick member when kicker has owner role`() {
            // when
            member.kick("규칙 위반", owner)

            // then
            assertThat(member.status).isEqualTo(TeamMemberStatus.KICKED)
            assertThat(member.leftAt).isNotNull()
            assertThat(member.memo).isEqualTo("규칙 위반")
        }

        @Test
        fun `should throw when non-owner tries to kick`() {
            // given
            val manager = TeamMember.create(team, memberUser, memberPlayer, 30, TeamMemberRole.MANAGER)
            setTeamMemberId(manager, 4L)

            // when & then
            assertThrows<IllegalStateException> {
                member.kick("test", manager)
            }
        }

        @Test
        fun `should throw when kicking higher or equal role member`() {
            // when & then
            assertThrows<IllegalStateException> {
                owner.kick("test", owner)
            }
        }

        @Test
        fun `should throw when trying to kick self`() {
            // when & then
            assertThrows<IllegalStateException> {
                owner.kick("test", owner)
            }
        }
    }

    @Nested
    @DisplayName("L-10: 강제 강퇴")
    inner class ForceKick {
        @Test
        fun `일반 멤버를 강제 강퇴하면 wasOwner가 false이다`() {
            // when
            val wasOwner = member.forceKick("관리자 강퇴")

            // then
            assertThat(wasOwner).isFalse()
            assertThat(member.status).isEqualTo(TeamMemberStatus.KICKED)
            assertThat(member.leftAt).isNotNull()
            assertThat(member.memo).isEqualTo("관리자 강퇴")
        }

        @Test
        fun `OWNER를 강제 강퇴하면 wasOwner가 true이다`() {
            // when
            val wasOwner = owner.forceKick("관리자에 의한 OWNER 강퇴")

            // then
            assertThat(wasOwner).isTrue()
            assertThat(owner.status).isEqualTo(TeamMemberStatus.KICKED)
            assertThat(owner.leftAt).isNotNull()
            assertThat(owner.memo).isEqualTo("관리자에 의한 OWNER 강퇴")
        }

        @Test
        fun `MANAGER를 강제 강퇴하면 wasOwner가 false이다`() {
            // given
            val managerUser = User.createLocalUser("manager@example.com", "password", "매니저")
            val managerPlayer = Player(name = "매니저", primaryPosition = Position.CATCHER)
            val manager = TeamMember.create(team, managerUser, managerPlayer, 5, TeamMemberRole.MANAGER)
            setTeamMemberId(manager, 3L)

            // when
            val wasOwner = manager.forceKick("관리자 강퇴")

            // then
            assertThat(wasOwner).isFalse()
            assertThat(manager.status).isEqualTo(TeamMemberStatus.KICKED)
        }

        @Test
        fun `사유 없이 강제 강퇴할 수 있다`() {
            // when
            val wasOwner = member.forceKick()

            // then
            assertThat(wasOwner).isFalse()
            assertThat(member.status).isEqualTo(TeamMemberStatus.KICKED)
            assertThat(member.memo).isNull()
        }
    }

    @Nested
    @DisplayName("탈퇴")
    inner class Leave {
        @Test
        fun `should leave team when member is not owner`() {
            // when
            member.leave()

            // then
            assertThat(member.status).isEqualTo(TeamMemberStatus.LEFT)
            assertThat(member.leftAt).isNotNull()
        }

        @Test
        fun `should throw when owner leaves without another owner`() {
            // when & then
            assertThrows<IllegalStateException> {
                owner.leave()
            }
        }

        @Test
        fun `should throw when non-active member tries to leave`() {
            // given
            member.suspend("test", owner)

            // when & then
            assertThrows<IllegalStateException> {
                member.leave()
            }
        }
    }

    @Nested
    @DisplayName("활동 정지 및 재개")
    inner class SuspendAndResume {
        @Test
        fun `should suspend member`() {
            // when
            member.suspend("회비 미납", owner)

            // then
            assertThat(member.status).isEqualTo(TeamMemberStatus.SUSPENDED)
            assertThat(member.memo).isEqualTo("회비 미납")
        }

        @Test
        fun `should resume suspended member`() {
            // given
            member.suspend("test", owner)

            // when
            member.resume(owner)

            // then
            assertThat(member.status).isEqualTo(TeamMemberStatus.ACTIVE)
        }

        @Test
        fun `should throw when suspending owner`() {
            // when & then
            assertThrows<IllegalStateException> {
                owner.suspend("test", owner)
            }
        }

        @Test
        fun `should throw when resuming non-suspended member`() {
            // when & then
            assertThrows<IllegalStateException> {
                member.resume(owner)
            }
        }

        @Test
        fun `should throw when non-owner tries to suspend`() {
            // when & then
            assertThrows<IllegalStateException> {
                member.suspend("test", member)
            }
        }
    }

    @Nested
    @DisplayName("OWNER 역할 이양")
    inner class TransferOwnership {
        @Test
        fun `should transfer ownership to another active member`() {
            // when
            owner.transferOwnership(member, owner)

            // then
            assertThat(owner.role).isEqualTo(TeamMemberRole.MANAGER)
            assertThat(member.role).isEqualTo(TeamMemberRole.OWNER)
        }

        @Test
        fun `should throw when non-owner tries to transfer`() {
            // when & then
            assertThrows<IllegalStateException> {
                member.transferOwnership(owner, member)
            }
        }

        @Test
        fun `should throw when requester is not self`() {
            // when & then
            assertThrows<IllegalStateException> {
                owner.transferOwnership(member, member)
            }
        }

        @Test
        fun `should throw when target is not active`() {
            // given
            member.suspend("test", owner)

            // when & then
            assertThrows<IllegalStateException> {
                owner.transferOwnership(member, owner)
            }
        }
    }

    @Nested
    @DisplayName("역할 확인")
    inner class RoleChecks {
        @Test
        fun `should check isOwner correctly`() {
            assertThat(owner.isOwner()).isTrue()
            assertThat(member.isOwner()).isFalse()
        }

        @Test
        fun `should check isManager correctly`() {
            val manager = TeamMember.create(team, memberUser, memberPlayer, 20, TeamMemberRole.MANAGER)
            assertThat(manager.isManager()).isTrue()
            assertThat(owner.isManager()).isFalse()
            assertThat(member.isManager()).isFalse()
        }

        @Test
        fun `should check canManageMembers correctly`() {
            val manager = TeamMember.create(team, memberUser, memberPlayer, 20, TeamMemberRole.MANAGER)
            assertThat(owner.canManageMembers()).isTrue()
            assertThat(manager.canManageMembers()).isTrue()
            assertThat(member.canManageMembers()).isFalse()
        }
    }

    @Nested
    @DisplayName("권한 확인")
    inner class Permissions {
        @Test
        fun `should check if member can vote`() {
            // then
            assertThat(member.canVote).isTrue()
        }

        @Test
        fun `should allow suspended member to vote`() {
            // given
            member.suspend("test", owner)

            // then
            assertThat(member.canVote).isTrue()
        }

        @Test
        fun `should not allow kicked member to vote`() {
            // given
            member.kick("test", owner)

            // then
            assertThat(member.canVote).isFalse()
        }

        @Test
        fun `should check if member can participate in game`() {
            // then
            assertThat(member.canParticipateInGame).isTrue()

            // given
            member.suspend("test", owner)

            // then
            assertThat(member.canParticipateInGame).isFalse()
        }

        @Test
        fun `should check if member can participate in election`() {
            assertThat(member.canParticipateInElection).isTrue()
            assertThat(owner.canParticipateInElection).isTrue()
        }

        @Test
        fun `should check if member can be in lineup`() {
            assertThat(member.canBeInLineup).isTrue()
            assertThat(owner.canBeInLineup).isTrue()
        }

        @Test
        fun `should not allow suspended member in lineup`() {
            // given
            member.suspend("test", owner)

            // then
            assertThat(member.canBeInLineup).isFalse()
        }
    }

    @Nested
    @DisplayName("GUEST 역할")
    inner class GuestRole {
        private lateinit var guestUser: User
        private lateinit var guestPlayer: Player
        private lateinit var guest: TeamMember

        @BeforeEach
        fun setUpGuest() {
            guestUser = User.createLocalUser("guest@example.com", "password", "게스트")
            guestPlayer = Player(name = "게스트", primaryPosition = Position.DESIGNATED_HITTER)
            guest = TeamMember.create(team, guestUser, guestPlayer, 99, TeamMemberRole.GUEST)
            setTeamMemberId(guest, 10L)
        }

        @Test
        fun `should create guest member`() {
            assertThat(guest.role).isEqualTo(TeamMemberRole.GUEST)
            assertThat(guest.status).isEqualTo(TeamMemberStatus.ACTIVE)
            assertThat(guest.isActive).isTrue()
        }

        @Test
        fun `GUEST should not be able to vote`() {
            assertThat(guest.canVote).isFalse()
        }

        @Test
        fun `GUEST should not be able to participate in election`() {
            assertThat(guest.canParticipateInElection).isFalse()
        }

        @Test
        fun `GUEST should be able to participate in game`() {
            assertThat(guest.canParticipateInGame).isTrue()
        }

        @Test
        fun `GUEST should be able to be in lineup`() {
            assertThat(guest.canBeInLineup).isTrue()
        }

        @Test
        fun `GUEST should not be able to manage members`() {
            assertThat(guest.canManageMembers()).isFalse()
        }

        @Test
        fun `GUEST should have lowest level`() {
            assertThat(TeamMemberRole.GUEST.level).isEqualTo(1)
            assertThat(TeamMemberRole.MEMBER.isHigherThan(TeamMemberRole.GUEST)).isTrue()
            assertThat(TeamMemberRole.GUEST.isHigherThan(TeamMemberRole.MEMBER)).isFalse()
        }

        @Test
        fun `GUEST should be kickable by owner`() {
            guest.kick("게스트 기간 종료", owner)
            assertThat(guest.status).isEqualTo(TeamMemberStatus.KICKED)
        }

        @Test
        fun `GUEST should be able to leave`() {
            guest.leave()
            assertThat(guest.status).isEqualTo(TeamMemberStatus.LEFT)
        }
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
