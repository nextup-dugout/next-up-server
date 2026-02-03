package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Game 엔티티 테스트")
class GameTest {

    private lateinit var competition: Competition

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition = Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            status = CompetitionStatus.IN_PROGRESS
        )
    }

    private fun createGame(status: GameStatus = GameStatus.SCHEDULED): Game {
        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실야구장",
            status = status
        )
    }

    @Nested
    @DisplayName("경기 시작")
    inner class Start {

        @Test
        fun `예정된 경기를 시작할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when
            game.start()

            // then
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(game.currentInning).isEqualTo(1)
            assertThat(game.isTopInning).isTrue()
            assertThat(game.startedAt).isNotNull()
        }

        @Test
        fun `이미 진행 중인 경기는 시작할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when & then
            assertThatThrownBy { game.start() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("이닝 진행")
    inner class NextHalfInning {

        @Test
        fun `초에서 말로 진행한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS).apply {
                currentInning = 1
                isTopInning = true
            }

            // when
            game.nextHalfInning()

            // then
            assertThat(game.currentInning).isEqualTo(1)
            assertThat(game.isTopInning).isFalse()
        }

        @Test
        fun `말에서 다음 이닝 초로 진행한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS).apply {
                currentInning = 1
                isTopInning = false
            }

            // when
            game.nextHalfInning()

            // then
            assertThat(game.currentInning).isEqualTo(2)
            assertThat(game.isTopInning).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기는 이닝을 진행할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.nextHalfInning() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("경기 종료")
    inner class Finish {

        @Test
        fun `진행 중인 경기를 종료할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.finish()

            // then
            assertThat(game.status).isEqualTo(GameStatus.FINISHED)
            assertThat(game.endedAt).isNotNull()
        }

        @Test
        fun `예정된 경기는 종료할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.finish() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("콜드게임")
    inner class CallGame {

        @Test
        fun `진행 중인 경기를 콜드게임 처리할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.callGame("우천")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
            assertThat(game.endedAt).isNotNull()
            assertThat(game.note).contains("우천")
        }
    }

    @Nested
    @DisplayName("경기 취소")
    inner class Cancel {

        @Test
        fun `예정된 경기를 취소할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when
            game.cancel("우천 예보")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
            assertThat(game.note).contains("우천 예보")
        }

        @Test
        fun `진행 중인 경기는 취소할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when & then
            assertThatThrownBy { game.cancel() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("경기 연기")
    inner class Postpone {

        @Test
        fun `예정된 경기를 연기할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            val newSchedule = LocalDateTime.of(2025, 4, 20, 14, 0)

            // when
            game.postpone(newSchedule, "우천")

            // then
            assertThat(game.status).isEqualTo(GameStatus.POSTPONED)
            assertThat(game.scheduledAt).isEqualTo(newSchedule)
            assertThat(game.note).contains("우천")
        }
    }

    @Nested
    @DisplayName("몰수패")
    inner class Forfeit {

        @Test
        fun `예정된 경기를 몰수 처리할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when
            game.forfeit("상대팀 불참")

            // then
            assertThat(game.status).isEqualTo(GameStatus.FORFEITED)
            assertThat(game.note).contains("상대팀 불참")
        }
    }

    @Nested
    @DisplayName("일정 변경")
    inner class Reschedule {

        @Test
        fun `연기된 경기를 재스케줄하면 예정 상태로 변경된다`() {
            // given
            val game = createGame(status = GameStatus.POSTPONED)
            val newSchedule = LocalDateTime.of(2025, 5, 1, 14, 0)

            // when
            game.reschedule(newSchedule)

            // then
            assertThat(game.status).isEqualTo(GameStatus.SCHEDULED)
            assertThat(game.scheduledAt).isEqualTo(newSchedule)
        }
    }

    @Nested
    @DisplayName("이닝 표시")
    inner class InningDisplay {

        @Test
        fun `경기 전에는 '경기 전'을 반환한다`() {
            // given
            val game = createGame().apply { currentInning = 0 }

            // then
            assertThat(game.currentInningDisplay).isEqualTo("경기 전")
        }

        @Test
        fun `1회초는 '1회초'를 반환한다`() {
            // given
            val game = createGame().apply {
                currentInning = 1
                isTopInning = true
            }

            // then
            assertThat(game.currentInningDisplay).isEqualTo("1회초")
        }

        @Test
        fun `5회말은 '5회말'을 반환한다`() {
            // given
            val game = createGame().apply {
                currentInning = 5
                isTopInning = false
            }

            // then
            assertThat(game.currentInningDisplay).isEqualTo("5회말")
        }
    }

    @Nested
    @DisplayName("연장전 확인")
    inner class ExtraInning {

        @Test
        fun `9회까지는 연장전이 아니다`() {
            // given
            val game = createGame().apply {
                currentInning = 9
                totalInnings = 9
            }

            // then
            assertThat(game.isExtraInning).isFalse()
        }

        @Test
        fun `10회 이상은 연장전이다`() {
            // given
            val game = createGame().apply {
                currentInning = 10
                totalInnings = 9
            }

            // then
            assertThat(game.isExtraInning).isTrue()
        }
    }

    @Nested
    @DisplayName("아웃 기록")
    inner class RecordOut {

        @Test
        fun `진행 중인 경기에서 아웃을 기록할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            val isInningOver = game.recordOut()

            // then
            assertThat(isInningOver).isFalse()
        }

        @Test
        fun `3아웃이 되면 true를 반환한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.recordOut()
            game.recordOut()

            // when
            val isInningOver = game.recordOut()

            // then
            assertThat(isInningOver).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기에서는 아웃을 기록할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.recordOut() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("타자 진행")
    inner class AdvanceBatter {

        @Test
        fun `진행 중인 경기에서 타순을 진행할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.advanceBatter()

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 타순을 진행할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.FINISHED)

            // when & then
            assertThatThrownBy { game.advanceBatter() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("주자 설정")
    inner class SetRunner {

        @Test
        fun `진행 중인 경기에서 주자를 설정할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.setRunner(Base.FIRST, 123L)

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 주자를 설정할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.setRunner(Base.FIRST, 123L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("베이스 클리어")
    inner class ClearBases {

        @Test
        fun `진행 중인 경기에서 베이스를 클리어할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.setRunner(Base.FIRST, 123L)
            game.setRunner(Base.SECOND, 456L)

            // when
            game.clearBases()

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 베이스를 클리어할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.FINISHED)

            // when & then
            assertThatThrownBy { game.clearBases() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("볼카운트 리셋")
    inner class ResetCount {

        @Test
        fun `진행 중인 경기에서 볼카운트를 리셋할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.resetCount()

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 볼카운트를 리셋할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.resetCount() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("볼 추가")
    inner class AddBall {

        @Test
        fun `진행 중인 경기에서 볼을 추가할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            val isWalk = game.addBall()

            // then
            assertThat(isWalk).isFalse()
        }

        @Test
        fun `4볼이 되면 true를 반환한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.addBall()
            game.addBall()
            game.addBall()

            // when
            val isWalk = game.addBall()

            // then
            assertThat(isWalk).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기에서는 볼을 추가할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.CANCELLED)

            // when & then
            assertThatThrownBy { game.addBall() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("스트라이크 추가")
    inner class AddStrike {

        @Test
        fun `진행 중인 경기에서 스트라이크를 추가할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            val isStrikeout = game.addStrike()

            // then
            assertThat(isStrikeout).isFalse()
        }

        @Test
        fun `3스트라이크가 되면 true를 반환한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.addStrike()
            game.addStrike()

            // when
            val isStrikeout = game.addStrike()

            // then
            assertThat(isStrikeout).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기에서는 스트라이크를 추가할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.POSTPONED)

            // when & then
            assertThatThrownBy { game.addStrike() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }
}
