package com.nextup.core.domain.election

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit

class EmergencyElectionTest {
    @Test
    fun `긴급 선거를 생성할 수 있다`() {
        // given
        val teamId = 1L
        val triggeredByMemberId = 10L
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)

        // when
        val election =
            Election.createEmergency(
                teamId = teamId,
                triggeredByMemberId = triggeredByMemberId,
                title = "비상대책위원회 긴급 선거",
                description = "구단주 부재로 인한 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )

        // then
        assertThat(election.teamId).isEqualTo(teamId)
        assertThat(election.electionType).isEqualTo(ElectionType.EMERGENCY)
        assertThat(election.status).isEqualTo(ElectionStatus.IN_PROGRESS)
        assertThat(election.triggeredByMemberId).isEqualTo(triggeredByMemberId)
        assertThat(election.regularElectionDeadline).isNotNull
        assertThat(election.actingOwnerMemberId).isNull()
        assertThat(election.actingOwnerPermissions).isNull()
    }

    @Test
    fun `긴급 선거는 생성 즉시 IN_PROGRESS 상태이다`() {
        // given
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)

        // when
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )

        // then
        assertThat(election.status).isEqualTo(ElectionStatus.IN_PROGRESS)
    }

    @Test
    fun `긴급 선거의 정규 선거 마감 기한은 발동 후 14일이다`() {
        // given
        val before = Instant.now()
        val startAt = before
        val endAt = startAt.plus(14, ChronoUnit.DAYS)

        // when
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )
        val after = Instant.now()

        // then
        val deadline = election.regularElectionDeadline!!
        val expectedMin = before.plus(Election.REGULAR_ELECTION_DEADLINE_DAYS, ChronoUnit.DAYS)
        val expectedMax = after.plus(Election.REGULAR_ELECTION_DEADLINE_DAYS, ChronoUnit.DAYS)
        assertThat(deadline).isAfterOrEqualTo(expectedMin)
        assertThat(deadline).isBeforeOrEqualTo(expectedMax)
    }

    @Test
    fun `긴급 선거 생성 시 제목이 공백이면 예외가 발생한다`() {
        // given
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)

        // when & then
        assertThrows<IllegalArgumentException> {
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "",
                startAt = startAt,
                endAt = endAt,
            )
        }
    }

    @Test
    fun `긴급 선거 생성 시 종료 시간이 시작 시간보다 이전이면 예외가 발생한다`() {
        // given
        val startAt = Instant.now().plus(7, ChronoUnit.DAYS)
        val endAt = startAt.minus(1, ChronoUnit.DAYS)

        // when & then
        assertThrows<IllegalArgumentException> {
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )
        }
    }

    @Test
    fun `isEmergency는 EMERGENCY 타입일 때 true를 반환한다`() {
        // given
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )

        // when & then
        assertThat(election.isEmergency()).isTrue()
    }

    @Test
    fun `isEmergency는 일반 선거일 때 false를 반환한다`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )

        // when & then
        assertThat(election.isEmergency()).isFalse()
    }

    @Test
    fun `임시 구단주를 지정할 수 있다`() {
        // given
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )
        val actingOwnerMemberId = 20L

        // when
        election.designateActingOwner(actingOwnerMemberId)

        // then
        assertThat(election.actingOwnerMemberId).isEqualTo(actingOwnerMemberId)
        assertThat(election.actingOwnerPermissions).isNotNull
        assertThat(election.actingOwnerPermissions!!.canManageLineup).isTrue()
        assertThat(election.actingOwnerPermissions!!.canManageSchedule).isTrue()
        assertThat(election.actingOwnerPermissions!!.canKickMember).isFalse()
        assertThat(election.actingOwnerPermissions!!.canDissolveTeam).isFalse()
        assertThat(election.actingOwnerPermissions!!.canTransferOwnership).isFalse()
    }

    @Test
    fun `임시 구단주 지정 후 hasActingOwner는 true를 반환한다`() {
        // given
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )

        assertThat(election.hasActingOwner()).isFalse()

        // when
        election.designateActingOwner(20L)

        // then
        assertThat(election.hasActingOwner()).isTrue()
    }

    @Test
    fun `일반 선거에 임시 구단주를 지정하면 예외가 발생한다`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            election.designateActingOwner(20L)
        }
    }

    @Test
    fun `이미 임시 구단주가 지정된 선거에 다시 지정하면 예외가 발생한다`() {
        // given
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )
        election.designateActingOwner(20L)

        // when & then
        assertThrows<IllegalArgumentException> {
            election.designateActingOwner(30L)
        }
    }

    @Test
    fun `취소된 긴급 선거에 임시 구단주를 지정하면 예외가 발생한다`() {
        // given
        val startAt = Instant.now()
        val endAt = startAt.plus(14, ChronoUnit.DAYS)
        val election =
            Election.createEmergency(
                teamId = 1L,
                triggeredByMemberId = 10L,
                title = "비상대책위원회 긴급 선거",
                startAt = startAt,
                endAt = endAt,
            )
        election.cancel()

        // when & then
        assertThrows<IllegalArgumentException> {
            election.designateActingOwner(20L)
        }
    }

    @Test
    fun `ActingOwnerPermissions 기본값은 라인업_일정 관리 가능 나머지 불가이다`() {
        // when
        val permissions = ActingOwnerPermissions.default()

        // then
        assertThat(permissions.canManageLineup).isTrue()
        assertThat(permissions.canManageSchedule).isTrue()
        assertThat(permissions.canKickMember).isFalse()
        assertThat(permissions.canDissolveTeam).isFalse()
        assertThat(permissions.canTransferOwnership).isFalse()
    }
}
