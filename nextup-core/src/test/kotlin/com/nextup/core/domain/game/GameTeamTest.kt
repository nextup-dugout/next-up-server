package com.nextup.core.domain.game

import com.nextup.core.domain.team.Team
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GameTeam")
class GameTeamTest {

    private lateinit var game: Game
    private lateinit var team: Team
    private lateinit var gameTeam: GameTeam

    @BeforeEach
    fun setup() {
        game = mockk<Game>(relaxed = true)
        team = mockk<Team>(relaxed = true)
        gameTeam = GameTeam(
            game = game,
            team = team,
            homeAway = HomeAway.HOME
        )
    }

    @Nested
    @DisplayName("점수 추가")
    inner class AddScoreTest {

        @Test
        fun `점수를 추가할 수 있다`() {
            // when
            gameTeam.addScore(3)

            // then
            assertThat(gameTeam.totalScore).isEqualTo(3)
        }

        @Test
        fun `점수를 여러 번 추가하면 누적된다`() {
            // given
            gameTeam.addScore(2)
            gameTeam.addScore(3)

            // then
            assertThat(gameTeam.totalScore).isEqualTo(5)
        }

        @Test
        fun `음수 점수는 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                gameTeam.addScore(-1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("점수는 0 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("안타 추가")
    inner class AddHitTest {

        @Test
        fun `안타를 추가할 수 있다`() {
            // when
            gameTeam.addHit()

            // then
            assertThat(gameTeam.totalHits).isEqualTo(1)
        }

        @Test
        fun `안타를 여러 번 추가하면 누적된다`() {
            // given
            gameTeam.addHit()
            gameTeam.addHit()
            gameTeam.addHit()

            // then
            assertThat(gameTeam.totalHits).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("실책 추가")
    inner class AddErrorTest {

        @Test
        fun `실책을 추가할 수 있다`() {
            // when
            gameTeam.addError()

            // then
            assertThat(gameTeam.totalErrors).isEqualTo(1)
        }

        @Test
        fun `실책을 여러 번 추가하면 누적된다`() {
            // given
            gameTeam.addError()
            gameTeam.addError()

            // then
            assertThat(gameTeam.totalErrors).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("이닝별 점수 기록")
    inner class RecordInningScoreTest {

        @Test
        fun `이닝별 점수를 기록할 수 있다`() {
            // when
            gameTeam.recordInningScore(1, 2)

            // then
            assertThat(gameTeam.getInningScore(1)).isEqualTo(2)
        }

        @Test
        fun `여러 이닝의 점수를 기록할 수 있다`() {
            // when
            gameTeam.recordInningScore(1, 0)
            gameTeam.recordInningScore(2, 1)
            gameTeam.recordInningScore(3, 3)

            // then
            assertThat(gameTeam.getInningScore(1)).isEqualTo(0)
            assertThat(gameTeam.getInningScore(2)).isEqualTo(1)
            assertThat(gameTeam.getInningScore(3)).isEqualTo(3)
        }

        @Test
        fun `이닝 점수를 덮어쓸 수 있다`() {
            // given
            gameTeam.recordInningScore(2, 5)

            // when
            gameTeam.recordInningScore(2, 3)

            // then
            assertThat(gameTeam.getInningScore(2)).isEqualTo(3)
        }

        @Test
        fun `이닝은 1 이상이어야 한다`() {
            // when & then
            assertThatThrownBy {
                gameTeam.recordInningScore(0, 1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝은 1 이상이어야 합니다")
        }

        @Test
        fun `음수 점수는 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                gameTeam.recordInningScore(1, -1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("점수는 0 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("addRunInInning - 이닝별 득점 추가 (BoxScore)")
    inner class AddRunInInningTest {

        @Test
        fun `특정 이닝에 득점을 추가할 수 있다`() {
            // when
            gameTeam.addRunInInning(1, 2)

            // then
            assertThat(gameTeam.getInningScore(1)).isEqualTo(2)
            assertThat(gameTeam.totalScore).isEqualTo(2)
        }

        @Test
        fun `기본값은 1점이다`() {
            // when
            gameTeam.addRunInInning(2)

            // then
            assertThat(gameTeam.getInningScore(2)).isEqualTo(1)
            assertThat(gameTeam.totalScore).isEqualTo(1)
        }

        @Test
        fun `같은 이닝에 여러 번 득점하면 누적된다`() {
            // given
            gameTeam.addRunInInning(3, 1)
            gameTeam.addRunInInning(3, 2)

            // then
            assertThat(gameTeam.getInningScore(3)).isEqualTo(3)
            assertThat(gameTeam.totalScore).isEqualTo(3)
        }

        @Test
        fun `여러 이닝에 득점하면 총점에 모두 반영된다`() {
            // given
            gameTeam.addRunInInning(1, 2)
            gameTeam.addRunInInning(3, 1)
            gameTeam.addRunInInning(5, 3)

            // then
            assertThat(gameTeam.getInningScore(1)).isEqualTo(2)
            assertThat(gameTeam.getInningScore(2)).isEqualTo(0)
            assertThat(gameTeam.getInningScore(3)).isEqualTo(1)
            assertThat(gameTeam.getInningScore(5)).isEqualTo(3)
            assertThat(gameTeam.totalScore).isEqualTo(6)
        }

        @Test
        fun `연장전 이닝에도 득점을 추가할 수 있다`() {
            // when
            gameTeam.addRunInInning(10, 1)
            gameTeam.addRunInInning(11, 2)

            // then
            assertThat(gameTeam.getInningScore(10)).isEqualTo(1)
            assertThat(gameTeam.getInningScore(11)).isEqualTo(2)
            assertThat(gameTeam.totalScore).isEqualTo(3)
        }

        @Test
        fun `이닝은 1 이상이어야 한다`() {
            // when & then
            assertThatThrownBy {
                gameTeam.addRunInInning(0, 1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝은 1 이상이어야 합니다")
        }

        @Test
        fun `음수 점수는 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                gameTeam.addRunInInning(1, -1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("점수는 0 이상이어야 합니다")
        }

        @Test
        fun `0점도 추가할 수 있다`() {
            // when
            gameTeam.addRunInInning(1, 0)

            // then
            assertThat(gameTeam.getInningScore(1)).isEqualTo(0)
            assertThat(gameTeam.totalScore).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("이닝 점수 조회")
    inner class GetInningScoreTest {

        @Test
        fun `기록되지 않은 이닝은 0점이다`() {
            // when
            val score = gameTeam.getInningScore(5)

            // then
            assertThat(score).isEqualTo(0)
        }

        @Test
        fun `기록된 이닝 점수를 조회할 수 있다`() {
            // given
            gameTeam.recordInningScore(3, 2)

            // when
            val score = gameTeam.getInningScore(3)

            // then
            assertThat(score).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("경기 결과 업데이트")
    inner class UpdateResultTest {

        @Test
        fun `경기 결과를 업데이트할 수 있다`() {
            // when
            gameTeam.updateResult(GameResult.WIN)

            // then
            assertThat(gameTeam.result).isEqualTo(GameResult.WIN)
        }
    }

    @Nested
    @DisplayName("점수 업데이트")
    inner class UpdateScoreTest {

        @Test
        fun `전체 점수를 한 번에 업데이트할 수 있다`() {
            // when
            gameTeam.updateScore(totalScore = 5, totalHits = 8, totalErrors = 2)

            // then
            assertThat(gameTeam.totalScore).isEqualTo(5)
            assertThat(gameTeam.totalHits).isEqualTo(8)
            assertThat(gameTeam.totalErrors).isEqualTo(2)
        }

        @Test
        fun `음수 값은 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                gameTeam.updateScore(totalScore = -1, totalHits = 0, totalErrors = 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
