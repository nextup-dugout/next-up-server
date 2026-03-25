package com.nextup.core.domain.eventgame

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("EventGameStatus 테스트")
class EventGameStatusTest {
    @Nested
    @DisplayName("canClose()")
    inner class CanClose {
        @Test
        fun `RECRUITING 상태에서만 마감 가능`() {
            assertThat(EventGameStatus.RECRUITING.canClose()).isTrue()
        }

        @ParameterizedTest
        @EnumSource(EventGameStatus::class, names = ["RECRUITING"], mode = EnumSource.Mode.EXCLUDE)
        fun `RECRUITING 외 상태에서는 마감 불가`(status: EventGameStatus) {
            assertThat(status.canClose()).isFalse()
        }
    }

    @Nested
    @DisplayName("canAssignTeams()")
    inner class CanAssignTeams {
        @Test
        fun `CLOSED 상태에서만 팀 배정 가능`() {
            assertThat(EventGameStatus.CLOSED.canAssignTeams()).isTrue()
        }

        @ParameterizedTest
        @EnumSource(EventGameStatus::class, names = ["CLOSED"], mode = EnumSource.Mode.EXCLUDE)
        fun `CLOSED 외 상태에서는 팀 배정 불가`(status: EventGameStatus) {
            assertThat(status.canAssignTeams()).isFalse()
        }
    }

    @Nested
    @DisplayName("canStart()")
    inner class CanStart {
        @Test
        fun `TEAM_ASSIGNED 상태에서만 시작 가능`() {
            assertThat(EventGameStatus.TEAM_ASSIGNED.canStart()).isTrue()
        }

        @ParameterizedTest
        @EnumSource(EventGameStatus::class, names = ["TEAM_ASSIGNED"], mode = EnumSource.Mode.EXCLUDE)
        fun `TEAM_ASSIGNED 외 상태에서는 시작 불가`(status: EventGameStatus) {
            assertThat(status.canStart()).isFalse()
        }
    }

    @Nested
    @DisplayName("canFinish()")
    inner class CanFinish {
        @Test
        fun `IN_PROGRESS 상태에서만 종료 가능`() {
            assertThat(EventGameStatus.IN_PROGRESS.canFinish()).isTrue()
        }

        @ParameterizedTest
        @EnumSource(EventGameStatus::class, names = ["IN_PROGRESS"], mode = EnumSource.Mode.EXCLUDE)
        fun `IN_PROGRESS 외 상태에서는 종료 불가`(status: EventGameStatus) {
            assertThat(status.canFinish()).isFalse()
        }
    }

    @Nested
    @DisplayName("canCancel()")
    inner class CanCancel {
        @Test
        fun `RECRUITING 상태에서 취소 가능`() {
            assertThat(EventGameStatus.RECRUITING.canCancel()).isTrue()
        }

        @Test
        fun `CLOSED 상태에서 취소 가능`() {
            assertThat(EventGameStatus.CLOSED.canCancel()).isTrue()
        }

        @Test
        fun `TEAM_ASSIGNED 상태에서 취소 가능`() {
            assertThat(EventGameStatus.TEAM_ASSIGNED.canCancel()).isTrue()
        }

        @Test
        fun `IN_PROGRESS 상태에서 취소 불가`() {
            assertThat(EventGameStatus.IN_PROGRESS.canCancel()).isFalse()
        }

        @Test
        fun `FINISHED 상태에서 취소 불가`() {
            assertThat(EventGameStatus.FINISHED.canCancel()).isFalse()
        }

        @Test
        fun `CANCELLED 상태에서 취소 불가`() {
            assertThat(EventGameStatus.CANCELLED.canCancel()).isFalse()
        }
    }

    @Nested
    @DisplayName("canJoin()")
    inner class CanJoin {
        @Test
        fun `RECRUITING 상태에서만 참가 가능`() {
            assertThat(EventGameStatus.RECRUITING.canJoin()).isTrue()
        }

        @ParameterizedTest
        @EnumSource(EventGameStatus::class, names = ["RECRUITING"], mode = EnumSource.Mode.EXCLUDE)
        fun `RECRUITING 외 상태에서는 참가 불가`(status: EventGameStatus) {
            assertThat(status.canJoin()).isFalse()
        }
    }

    @Nested
    @DisplayName("isCompleted()")
    inner class IsCompleted {
        @Test
        fun `FINISHED 상태는 완료`() {
            assertThat(EventGameStatus.FINISHED.isCompleted()).isTrue()
        }

        @Test
        fun `CANCELLED 상태는 완료`() {
            assertThat(EventGameStatus.CANCELLED.isCompleted()).isTrue()
        }

        @Test
        fun `RECRUITING 상태는 미완료`() {
            assertThat(EventGameStatus.RECRUITING.isCompleted()).isFalse()
        }

        @Test
        fun `CLOSED 상태는 미완료`() {
            assertThat(EventGameStatus.CLOSED.isCompleted()).isFalse()
        }

        @Test
        fun `TEAM_ASSIGNED 상태는 미완료`() {
            assertThat(EventGameStatus.TEAM_ASSIGNED.isCompleted()).isFalse()
        }

        @Test
        fun `IN_PROGRESS 상태는 미완료`() {
            assertThat(EventGameStatus.IN_PROGRESS.isCompleted()).isFalse()
        }
    }

    @Nested
    @DisplayName("displayName")
    inner class DisplayNameTest {
        @Test
        fun `각 상태의 displayName 검증`() {
            assertThat(EventGameStatus.RECRUITING.displayName).isEqualTo("모집 중")
            assertThat(EventGameStatus.CLOSED.displayName).isEqualTo("모집 마감")
            assertThat(EventGameStatus.TEAM_ASSIGNED.displayName).isEqualTo("팀 배정 완료")
            assertThat(EventGameStatus.IN_PROGRESS.displayName).isEqualTo("진행 중")
            assertThat(EventGameStatus.FINISHED.displayName).isEqualTo("종료")
            assertThat(EventGameStatus.CANCELLED.displayName).isEqualTo("취소")
        }
    }
}
