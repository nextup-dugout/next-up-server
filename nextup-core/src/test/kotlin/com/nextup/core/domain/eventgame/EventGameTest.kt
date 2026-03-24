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

    private fun createGameReadyToStart(): EventGame {
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
        return game
    }

    private fun createGameInProgress(): EventGame {
        val game = createGameReadyToStart()
        game.start()
        return game
    }

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
        fun `모든 필드를 포함하여 생성`() {
            val game =
                EventGame.create(
                    organizerId = 1L,
                    title = "주말 픽업 게임",
                    description = "누구나 환영",
                    scheduledAt = LocalDateTime.now().plusDays(7),
                    location = "잠실 야구장",
                    fieldName = "A구장",
                    maxParticipants = 18,
                    innings = 5,
                    teamAName = "레드팀",
                    teamBName = "블루팀",
                )

            assertThat(game.description).isEqualTo("누구나 환영")
            assertThat(game.location).isEqualTo("잠실 야구장")
            assertThat(game.fieldName).isEqualTo("A구장")
            assertThat(game.maxParticipants).isEqualTo(18)
            assertThat(game.innings).isEqualTo(5)
            assertThat(game.teamAName).isEqualTo("레드팀")
            assertThat(game.teamBName).isEqualTo("블루팀")
        }

        @Test
        fun `최소 참가 인원이 2명 미만이면 예외가 발생한다`() {
            assertThatThrownBy { createEventGame(maxParticipants = 1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("2명 이상")
        }

        @Test
        fun `이닝이 범위를 벗어나면 예외가 발생한다 - 10이닝`() {
            assertThatThrownBy { createEventGame(innings = 10) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("1~9")
        }

        @Test
        fun `이닝이 범위를 벗어나면 예외가 발생한다 - 0이닝`() {
            assertThatThrownBy { createEventGame(innings = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("1~9")
        }

        @Test
        fun `빈 제목으로 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                EventGame.create(
                    organizerId = 1L,
                    title = "",
                    scheduledAt = LocalDateTime.now().plusDays(7),
                    maxParticipants = 20,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("제목")
        }

        @Test
        fun `공백 제목으로 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                EventGame.create(
                    organizerId = 1L,
                    title = "   ",
                    scheduledAt = LocalDateTime.now().plusDays(7),
                    maxParticipants = 20,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("제목")
        }

        @Test
        fun `이닝 경계값 1과 9는 정상 생성`() {
            val game1 = createEventGame(innings = 1)
            assertThat(game1.innings).isEqualTo(1)

            val game9 = createEventGame(innings = 9)
            assertThat(game9.innings).isEqualTo(9)
        }

        @Test
        fun `최소 참가 인원 2명은 정상 생성`() {
            val game = createEventGame(maxParticipants = 2)
            assertThat(game.maxParticipants).isEqualTo(2)
        }

        @Test
        fun `초기 상태 검증`() {
            val game = createEventGame()

            assertThat(game.teamAScore).isNull()
            assertThat(game.teamBScore).isNull()
            assertThat(game.startedAt).isNull()
            assertThat(game.endedAt).isNull()
            assertThat(game.cancelReason).isNull()
            assertThat(game.participants).isEmpty()
            assertThat(game.activeParticipantCount).isEqualTo(0)
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
        fun `CANCELLED 상태에서 마감하면 예외`() {
            val game = createEventGame()
            game.cancel("사유")

            assertThatThrownBy { game.closeRecruitment() }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `경기 취소 성공 - RECRUITING 상태`() {
            val game = createEventGame()
            game.cancel("우천 취소")
            assertThat(game.status).isEqualTo(EventGameStatus.CANCELLED)
            assertThat(game.cancelReason).isEqualTo("우천 취소")
        }

        @Test
        fun `경기 취소 성공 - CLOSED 상태`() {
            val game = createEventGame()
            game.closeRecruitment()
            game.cancel("인원 부족")
            assertThat(game.status).isEqualTo(EventGameStatus.CANCELLED)
            assertThat(game.cancelReason).isEqualTo("인원 부족")
        }

        @Test
        fun `경기 취소 성공 - TEAM_ASSIGNED 상태`() {
            val game = createGameReadyToStart()
            game.cancel("구장 사용 불가")
            assertThat(game.status).isEqualTo(EventGameStatus.CANCELLED)
        }

        @Test
        fun `IN_PROGRESS 상태에서 취소하면 예외`() {
            val game = createGameInProgress()

            assertThatThrownBy { game.cancel("취소 사유") }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `FINISHED 상태에서 취소하면 예외`() {
            val game = createGameInProgress()
            game.finish(5, 3)

            assertThatThrownBy { game.cancel("취소 사유") }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `이미 취소된 게임에서 취소하면 예외`() {
            val game = createEventGame()
            game.cancel("첫 번째 취소")

            assertThatThrownBy { game.cancel("두 번째 취소") }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `취소 사유가 비어있으면 예외`() {
            val game = createEventGame()
            assertThatThrownBy { game.cancel("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `취소 사유가 공백이면 예외`() {
            val game = createEventGame()
            assertThatThrownBy { game.cancel("   ") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `경기 시작 성공`() {
            val game = createGameReadyToStart()
            game.start()

            assertThat(game.status).isEqualTo(EventGameStatus.IN_PROGRESS)
            assertThat(game.startedAt).isNotNull()
        }

        @Test
        fun `RECRUITING 상태에서 시작하면 예외`() {
            val game = createEventGame()

            assertThatThrownBy { game.start() }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `CLOSED 상태에서 시작하면 예외`() {
            val game = createEventGame()
            game.closeRecruitment()

            assertThatThrownBy { game.start() }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `경기 종료 성공`() {
            val game = createGameInProgress()

            game.finish(5, 3)
            assertThat(game.status).isEqualTo(EventGameStatus.FINISHED)
            assertThat(game.teamAScore).isEqualTo(5)
            assertThat(game.teamBScore).isEqualTo(3)
            assertThat(game.endedAt).isNotNull()
        }

        @Test
        fun `RECRUITING 상태에서 종료하면 예외`() {
            val game = createEventGame()

            assertThatThrownBy { game.finish(5, 3) }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `TEAM_ASSIGNED 상태에서 종료하면 예외`() {
            val game = createGameReadyToStart()

            assertThatThrownBy { game.finish(5, 3) }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `음수 점수로 종료하면 예외 - teamAScore`() {
            val game = createGameInProgress()

            assertThatThrownBy { game.finish(-1, 3) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("0 이상")
        }

        @Test
        fun `음수 점수로 종료하면 예외 - teamBScore`() {
            val game = createGameInProgress()

            assertThatThrownBy { game.finish(3, -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("0 이상")
        }

        @Test
        fun `RECRUITING 상태에서 팀 배정 완료하면 예외`() {
            val game = createEventGame()

            assertThatThrownBy { game.completeTeamAssignment() }
                .isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `IN_PROGRESS 상태에서 팀 배정 완료하면 예외`() {
            val game = createGameInProgress()

            assertThatThrownBy { game.completeTeamAssignment() }
                .isInstanceOf(InvalidStateException::class.java)
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

        @Test
        fun `CANCELLED 상태에서 참가 불가`() {
            val game = createEventGame()
            game.cancel("취소")

            assertThatThrownBy {
                game.addParticipant(EventGameParticipant.create(game, 10L))
            }.isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `여러 참가자 추가 후 activeParticipantCount 검증`() {
            val game = createEventGame()
            game.addParticipant(EventGameParticipant.create(game, 10L))
            game.addParticipant(EventGameParticipant.create(game, 20L))
            game.addParticipant(EventGameParticipant.create(game, 30L))

            assertThat(game.activeParticipantCount).isEqualTo(3)
            assertThat(game.participants).hasSize(3)
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

        @Test
        fun `취소된 참가자는 팀 배정 검증에서 제외`() {
            val game = createEventGame()
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            val p3 = EventGameParticipant.create(game, 30L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            game.addParticipant(p3)
            p1.confirm()
            p2.confirm()
            p3.confirm()
            p3.cancel() // p3 취소
            game.closeRecruitment()

            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)

            // p3은 취소되었으므로 팀 배정 불필요
            game.completeTeamAssignment()
            assertThat(game.status).isEqualTo(EventGameStatus.TEAM_ASSIGNED)
        }
    }

    @Nested
    @DisplayName("전체 워크플로우")
    inner class FullWorkflow {
        @Test
        fun `RECRUITING - CLOSED - TEAM_ASSIGNED - IN_PROGRESS - FINISHED 전체 흐름`() {
            val game = createEventGame()
            assertThat(game.status).isEqualTo(EventGameStatus.RECRUITING)

            // 참가자 추가
            val p1 = EventGameParticipant.create(game, 10L)
            val p2 = EventGameParticipant.create(game, 20L)
            game.addParticipant(p1)
            game.addParticipant(p2)
            p1.confirm()
            p2.confirm()

            // 팀 배정
            p1.assignTeam(TeamAssignment.TEAM_A)
            p2.assignTeam(TeamAssignment.TEAM_B)

            // 모집 마감
            game.closeRecruitment()
            assertThat(game.status).isEqualTo(EventGameStatus.CLOSED)

            // 팀 배정 완료
            game.completeTeamAssignment()
            assertThat(game.status).isEqualTo(EventGameStatus.TEAM_ASSIGNED)

            // 경기 시작
            game.start()
            assertThat(game.status).isEqualTo(EventGameStatus.IN_PROGRESS)
            assertThat(game.startedAt).isNotNull()

            // 경기 종료
            game.finish(7, 2)
            assertThat(game.status).isEqualTo(EventGameStatus.FINISHED)
            assertThat(game.teamAScore).isEqualTo(7)
            assertThat(game.teamBScore).isEqualTo(2)
            assertThat(game.endedAt).isNotNull()
        }

        @Test
        fun `RECRUITING - CANCELLED 흐름`() {
            val game = createEventGame()
            game.cancel("인원 부족")
            assertThat(game.status).isEqualTo(EventGameStatus.CANCELLED)
            assertThat(game.cancelReason).isEqualTo("인원 부족")
        }
    }
}
