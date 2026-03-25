package com.nextup.core.domain.team

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TeamMemberRole 열거형 테스트")
class TeamMemberRoleTest {
    @Nested
    @DisplayName("권한 확인")
    inner class PermissionChecks {
        @Test
        fun `should OWNER have approve join permission`() {
            assertThat(TeamMemberRole.OWNER.canApproveJoin()).isTrue()
        }

        @Test
        fun `should MANAGER have approve join permission`() {
            assertThat(TeamMemberRole.MANAGER.canApproveJoin()).isTrue()
        }

        @Test
        fun `should MEMBER not have approve join permission`() {
            assertThat(TeamMemberRole.MEMBER.canApproveJoin()).isFalse()
        }

        @Test
        fun `should GUEST not have approve join permission`() {
            assertThat(TeamMemberRole.GUEST.canApproveJoin()).isFalse()
        }

        @Test
        fun `should only OWNER have kick permission`() {
            assertThat(TeamMemberRole.OWNER.canKickMember()).isTrue()
            assertThat(TeamMemberRole.MANAGER.canKickMember()).isFalse()
            assertThat(TeamMemberRole.MEMBER.canKickMember()).isFalse()
            assertThat(TeamMemberRole.GUEST.canKickMember()).isFalse()
        }

        @Test
        fun `should only OWNER have change role permission`() {
            assertThat(TeamMemberRole.OWNER.canChangeRole()).isTrue()
            assertThat(TeamMemberRole.MANAGER.canChangeRole()).isFalse()
            assertThat(TeamMemberRole.MEMBER.canChangeRole()).isFalse()
            assertThat(TeamMemberRole.GUEST.canChangeRole()).isFalse()
        }

        @Test
        fun `should GUEST not be able to vote in poll`() {
            assertThat(TeamMemberRole.GUEST.canVoteInPoll()).isFalse()
            assertThat(TeamMemberRole.MEMBER.canVoteInPoll()).isTrue()
            assertThat(TeamMemberRole.MANAGER.canVoteInPoll()).isTrue()
            assertThat(TeamMemberRole.OWNER.canVoteInPoll()).isTrue()
        }
    }

    @Nested
    @DisplayName("레벨 비교")
    inner class LevelComparison {
        @Test
        fun `should OWNER be higher than MANAGER`() {
            assertThat(TeamMemberRole.OWNER.isHigherThan(TeamMemberRole.MANAGER)).isTrue()
        }

        @Test
        fun `should OWNER be higher than MEMBER`() {
            assertThat(TeamMemberRole.OWNER.isHigherThan(TeamMemberRole.MEMBER)).isTrue()
        }

        @Test
        fun `should MANAGER be higher than MEMBER`() {
            assertThat(TeamMemberRole.MANAGER.isHigherThan(TeamMemberRole.MEMBER)).isTrue()
        }

        @Test
        fun `should MEMBER not be higher than MANAGER`() {
            assertThat(TeamMemberRole.MEMBER.isHigherThan(TeamMemberRole.MANAGER)).isFalse()
        }

        @Test
        fun `should same role not be higher`() {
            assertThat(TeamMemberRole.OWNER.isHigherThan(TeamMemberRole.OWNER)).isFalse()
        }

        @Test
        fun `should isHigherOrEqual work correctly`() {
            assertThat(TeamMemberRole.OWNER.isHigherOrEqual(TeamMemberRole.OWNER)).isTrue()
            assertThat(TeamMemberRole.OWNER.isHigherOrEqual(TeamMemberRole.MANAGER)).isTrue()
            assertThat(TeamMemberRole.MANAGER.isHigherOrEqual(TeamMemberRole.MANAGER)).isTrue()
            assertThat(TeamMemberRole.MEMBER.isHigherOrEqual(TeamMemberRole.MANAGER)).isFalse()
            assertThat(TeamMemberRole.MEMBER.isHigherThan(TeamMemberRole.GUEST)).isTrue()
            assertThat(TeamMemberRole.GUEST.isHigherOrEqual(TeamMemberRole.GUEST)).isTrue()
            assertThat(TeamMemberRole.GUEST.isHigherOrEqual(TeamMemberRole.MEMBER)).isFalse()
        }
    }

    @Nested
    @DisplayName("속성 확인")
    inner class Properties {
        @Test
        fun `should have correct display names`() {
            assertThat(TeamMemberRole.OWNER.displayName).isEqualTo("감독")
            assertThat(TeamMemberRole.MANAGER.displayName).isEqualTo("운영진")
            assertThat(TeamMemberRole.MEMBER.displayName).isEqualTo("일반 회원")
            assertThat(TeamMemberRole.GUEST.displayName).isEqualTo("게스트")
        }

        @Test
        fun `should have correct levels`() {
            assertThat(TeamMemberRole.OWNER.level).isEqualTo(100)
            assertThat(TeamMemberRole.MANAGER.level).isEqualTo(50)
            assertThat(TeamMemberRole.MEMBER.level).isEqualTo(10)
            assertThat(TeamMemberRole.GUEST.level).isEqualTo(1)
        }
    }
}
