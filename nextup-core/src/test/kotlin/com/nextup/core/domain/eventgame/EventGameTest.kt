package com.nextup.core.domain.eventgame

import com.nextup.common.exception.InvalidStateException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("EventGame 엔티티 테스트")
class EventGameTest {
    private fun createEventGame(
        maxParticipants: Int = 20,
        innings: Int = 7,
    ): EventGame =
        EventGame.create(
            organizerId = 1L,
            title = "주말 픽업 게임",
            description = "누구나 참가 가능",
            scheduledAt = LocalDateTime.now().plusDays(7),
            location = "잠실 야구장",
            maxParticipants = maxParticipants,
            innings = innings,
        )

    @Nested
    @DisplayName("create()")
    inner class Create {
        @Test
        fun `정상적으로 이벤트 게임을 생성한다`() {
            val game = createEventGame()

            assertThat(game.status).isEqualTo(EventGameStatus.RECRUITING)
            assertThat(game.title).isEqualTo("주말 픽업 게임")
            assertThat(game.maxParticipants).isEqualTo(20)
            assertThat(game.innings).isEqualTo(7)
            assertThat(game.teamAName).isEqualTo("Team A")
            assertThat(game.teamBName).isEqualTo("Team B")
        }

        @Test
        fun `최소 참가 인원이 2명 미만이면 예외가 발생한다`() {
            assertThatThrownBy { createEventGame(maxParticipants = 1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("2명 이상")
        }

        @Test
        fun `이닝이 범위를 벗어나면 예외가 발생한다`() {
            assertThatThrownBy { createEventGame(innings = 10) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("1~9")
        }
    }

    @Nested
    @DisplayName("상태 전이")
    inner class StatusTransition {
        @Test
        fun `모집 마감 성공`() {
            val game = createEventGame()
            game.closeRecruitment()
            assertThat(game.status).isEqualTo(EventGameStatus.CLOSED)
        }

        @Test
        fun `모집 중이 아닌 상태에서 마감하면 예외`() {
            val game = createEventGame()
            game.closeRecruitment()

            assertThatThrownBy { game.closeRecruitment() }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `경기 취소 성공`() {
            val game = createEventGame()
            game.cancel("우천 취소")
            assertThat(game.status).isEqualTo(EventGameStatus.CANCELLED)
            assertThat(game.cancelReason).isEqualTo("우천 취소")
        }

        @Test
        fun `취소 사유가 비어있으면 예외`() {
            val game = createEventGame()
            assertThatThrownBy { game.cancel("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `경기 종료 성공`() {
            val game = createEventGame()
            game.closeRecruitment()

            // 참가자 추가 및 팀 배정을 위해 먼저 RECRUITING 상태에서 참가
            val game2 = createEventGame()
            val p1 = EventGameParticipant.create(game2, 10L)
            val p2 = EventGameParticipant.create(game2, 20L)
            game2.addParticipant(p1)
            game2.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game2.closeRecruitment()
            game2.completeTeamAssignment()
            game2.start()

            assertThat(game2.status).isEqualTo(EventGameStatus.IN_PROGRESS)
            assertThat(game2.startedAt).isNotNull()

            game2.finish(5, 3)
            assertThat(game2.status).isEqualTo(EventGameStatus.FINISHED)
            assertThat(game2.teamAScore).isEqualTo(5)
            assertThat(game2.teamBScore).isEqualTo(3)
            assertThat(game2.endedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("참가자 관리")
    inner class ParticipantManagement {
        @Test
        fun `참가자 추가 성공`() {
            val game = createEventGame()
            val participant = EventGameParticipant.create(game, 10L, "참가합니다")
            game.addParticipant(participant)

            assertThat(game.activeParticipantCount).isEqualTo(1)
            assertThat(game.participants).hasSize(1)
        }

        @Test
        fun `정원이 가득 차면 추가 불가`() {
            val game = createEventGame(maxParticipants = 2)
            game.addParticipant(EventGameParticipant.create(game, 10L))
            game.addParticipant(EventGameParticipant.create(game, 20L))

            assertThatThrownBy {
                game.addParticipant(EventGameParticipant.create(game, 30L))
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("정원")
        }

        @Test
        fun `중복 참가 불가`() {
            val game = createEventGame()
            game.addParticipant(EventGameParticipant.create(game, 10L))

            assertThatThrownBy {
                game.addParticipant(EventGameParticipant.create(game, 10L))
            }.isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("이미 참가")
        }

        @Test
        fun `취소한 참가자는 정원에서 제외`() {
            val game = createEventGame(maxParticipants = 2)
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)

            p1.cancel()
            assertThat(game.activeParticipantCount).isEqualTo(1)
        }

        @Test
        fun `모집 중이 아닌 상태에서 참가 불가`() {
            val game = createEventGame()
            game.closeRecruitment()

            assertThatThrownBy {
                game.addParticipant(EventGameParticipant.create(game, 10L))
            }.isInstanceOf(InvalidStateException::class.java)
        }
    }

    @Nested
    @DisplayName("상태 전이 실패 케이스")
    inner class StatusTransitionFailure {
        @Test
        fun `종료된 게임은 취소할 수 없다`() {
            val game = createEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            game.start()
            game.finish(5, 3)

            assertThatThrownBy { game.cancel("취소") }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `모집 중인 상태에서 시작할 수 없다`() {
            val game = createEventGame()

            assertThatThrownBy { game.start() }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `모집 중인 상태에서 종료할 수 없다`() {
            val game = createEventGame()

            assertThatThrownBy { game.finish(1, 0) }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `음수 점수로 종료할 수 없다`() {
            val game = createEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)
            game.closeRecruitment()
            game.completeTeamAssignment()
            game.start()

            assertThatThrownBy { game.finish(-1, 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `제목이 비어있으면 생성 실패`() {
            assertThatThrownBy {
                EventGame.create(
                    organizerId = 1L,
                    title = "  ",
                    scheduledAt = LocalDateTime.now().plusDays(7),
                    maxParticipants = 20,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `모집 마감이 아닌 상태에서 팀 배정 완료 불가`() {
            val game = createEventGame()

            assertThatThrownBy { game.completeTeamAssignment() }
                .isInstanceOf(InvalidStateException::class.java)
        }
    }

    @Nested
    @DisplayName("EventGameStatus")
    inner class EventGameStatusTest {
        @Test
        fun `FINISHED 상태는 isCompleted true`() {
            assertThat(EventGameStatus.FINISHED.isCompleted()).isTrue()
        }

        @Test
        fun `CANCELLED 상태는 isCompleted true`() {
            assertThat(EventGameStatus.CANCELLED.isCompleted()).isTrue()
        }

        @Test
        fun `RECRUITING 상태는 isCompleted false`() {
            assertThat(EventGameStatus.RECRUITING.isCompleted()).isFalse()
        }

        @Test
        fun `IN_PROGRESS 상태는 isCompleted false`() {
            assertThat(EventGameStatus.IN_PROGRESS.isCompleted()).isFalse()
        }

        @Test
        fun `canJoin은 RECRUITING에서만 true`() {
            assertThat(EventGameStatus.RECRUITING.canJoin()).isTrue()
            assertThat(EventGameStatus.CLOSED.canJoin()).isFalse()
        }

        @Test
        fun `canFinish는 IN_PROGRESS에서만 true`() {
            assertThat(EventGameStatus.IN_PROGRESS.canFinish()).isTrue()
            assertThat(EventGameStatus.TEAM_ASSIGNED.canFinish()).isFalse()
        }

        @Test
        fun `canCancel은 RECRUITING, CLOSED, TEAM_ASSIGNED에서 true`() {
            assertThat(EventGameStatus.RECRUITING.canCancel()).isTrue()
            assertThat(EventGameStatus.CLOSED.canCancel()).isTrue()
            assertThat(EventGameStatus.TEAM_ASSIGNED.canCancel()).isTrue()
            assertThat(EventGameStatus.IN_PROGRESS.canCancel()).isFalse()
            assertThat(EventGameStatus.FINISHED.canCancel()).isFalse()
        }
    }

    @Nested
    @DisplayName("팀 배정")
    inner class TeamAssignmentTest {
        @Test
        fun `팀 배정 완료 시 모든 확정 참가자에게 팀이 배정되어야 한다`() {
            val game = createEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            game.closeRecruitment()

            // 하나만 배정
            p1.assignTeam(TeamAssignment.TEAM_A)

            assertThatThrownBy { game.completeTeamAssignment() }
                .isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("모든 확정 참가자")
        }

        @Test
        fun `모든 참가자에게 팀이 배정되면 팀 배정 완료 성공`() {
            val game = createEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()
            game.closeRecruitment()

            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)

            game.completeTeamAssignment()
            assertThat(game.status).isEqualTo(EventGameStatus.TEAM_ASSIGNED)
        }
    }
}
