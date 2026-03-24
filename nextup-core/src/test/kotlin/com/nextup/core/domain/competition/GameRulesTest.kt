package com.nextup.core.domain.competition

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GameRules 값 객체 테스트")
class GameRulesTest {
    @Nested
    @DisplayName("기본값 검증")
    inner class DefaultValues {
        @Test
        fun `기본 GameRules는 9이닝 규칙을 따른다`() {
            val rules = GameRules()

            assertThat(rules.defaultInnings).isEqualTo(9)
            assertThat(rules.mercyRuleEnabled).isFalse()
            assertThat(rules.tiedGameResult).isEqualTo(TiedGameResult.DRAW)
            assertThat(rules.tiebreakerEnabled).isFalse()
            assertThat(rules.forfeitScore).isEqualTo(7)
            assertThat(rules.starterWinQualificationOuts).isEqualTo(15)
            assertThat(rules.qualificationPAMultiplier).isEqualTo(3.1)
            assertThat(rules.qualificationIPMultiplier).isEqualTo(1.0)
            assertThat(rules.timeLimitMinutes).isNull()
            assertThat(rules.maxExtraInnings).isNull()
            assertThat(rules.minBattingOrderCount).isEqualTo(9)
        }
    }

    @Nested
    @DisplayName("기본 이닝 검증")
    inner class DefaultInningsValidation {
        @Test
        fun `3이닝도 유효하다`() {
            val rules = GameRules(defaultInnings = 3, doubleheaderInnings = 3)

            assertThat(rules.defaultInnings).isEqualTo(3)
        }

        @Test
        fun `12이닝도 유효하다`() {
            val rules = GameRules(defaultInnings = 12)

            assertThat(rules.defaultInnings).isEqualTo(12)
        }

        @Test
        fun `7이닝 단축 경기를 설정할 수 있다`() {
            val rules = GameRules(defaultInnings = 7)

            assertThat(rules.defaultInnings).isEqualTo(7)
        }

        @Test
        fun `2이닝은 유효하지 않다`() {
            assertThatThrownBy { GameRules(defaultInnings = 2) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("기본 이닝은 3~12 사이여야 합니다")
        }

        @Test
        fun `더블헤더 이닝 기본값은 7이다`() {
            val rules = GameRules()
            assertThat(rules.doubleheaderInnings).isEqualTo(7)
        }

        @Test
        fun `더블헤더 이닝을 커스텀 설정할 수 있다`() {
            val rules = GameRules(doubleheaderInnings = 5)
            assertThat(rules.doubleheaderInnings).isEqualTo(5)
        }

        @Test
        fun `더블헤더 이닝이 기본 이닝보다 크면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(defaultInnings = 7, doubleheaderInnings = 9)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("더블헤더 이닝은 3 이상 기본 이닝")
        }

        @Test
        fun `더블헤더 이닝이 3 미만이면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(doubleheaderInnings = 2)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("더블헤더 이닝은 3 이상")
        }

        @Test
        fun `13이닝은 유효하지 않다`() {
            assertThatThrownBy { GameRules(defaultInnings = 13) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("기본 이닝은 3~12 사이여야 합니다")
        }
    }

    @Nested
    @DisplayName("몰수승 점수 검증")
    inner class ForfeitScoreValidation {
        @Test
        fun `몰수승 점수를 사용자 정의할 수 있다`() {
            val rules = GameRules(forfeitScore = 9)

            assertThat(rules.forfeitScore).isEqualTo(9)
        }

        @Test
        fun `몰수승 점수가 0이면 예외가 발생한다`() {
            assertThatThrownBy { GameRules(forfeitScore = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("몰수승 점수는 양수여야 합니다")
        }

        @Test
        fun `몰수승 점수가 음수이면 예외가 발생한다`() {
            assertThatThrownBy { GameRules(forfeitScore = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("몰수승 점수는 양수여야 합니다")
        }
    }

    @Nested
    @DisplayName("콜드게임(머시 룰) 검증")
    inner class MercyRuleValidation {
        @Test
        fun `콜드게임을 활성화할 수 있다`() {
            val rules =
                GameRules(
                    mercyRuleEnabled = true,
                    mercyRunDifference = 10,
                    mercyMinimumInning = 5,
                )

            assertThat(rules.mercyRuleEnabled).isTrue()
            assertThat(rules.mercyRunDifference).isEqualTo(10)
            assertThat(rules.mercyMinimumInning).isEqualTo(5)
        }

        @Test
        fun `콜드게임 활성화 시 득점 차이가 없으면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(
                    mercyRuleEnabled = true,
                    mercyRunDifference = null,
                    mercyMinimumInning = 5,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("득점 차이를 설정해야 합니다")
        }

        @Test
        fun `콜드게임 활성화 시 최소 이닝이 없으면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(
                    mercyRuleEnabled = true,
                    mercyRunDifference = 10,
                    mercyMinimumInning = null,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 이닝을 설정해야 합니다")
        }

        @Test
        fun `콜드게임 득점 차이가 0이면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(
                    mercyRuleEnabled = true,
                    mercyRunDifference = 0,
                    mercyMinimumInning = 5,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("득점 차이는 양수여야 합니다")
        }

        @Test
        fun `콜드게임 최소 이닝이 기본 이닝보다 크면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(
                    defaultInnings = 7,
                    mercyRuleEnabled = true,
                    mercyRunDifference = 10,
                    mercyMinimumInning = 8,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("콜드게임 최소 이닝은 1 이상 기본 이닝")
        }

        @Test
        fun `콜드게임 비활성화 시 관련 값이 null이어도 유효하다`() {
            val rules =
                GameRules(
                    mercyRuleEnabled = false,
                    mercyRunDifference = null,
                    mercyMinimumInning = null,
                )

            assertThat(rules.mercyRuleEnabled).isFalse()
        }
    }

    @Nested
    @DisplayName("선발 승리 자격 검증")
    inner class StarterWinQualificationValidation {
        @Test
        fun `선발 승리 자격 아웃 수를 사용자 정의할 수 있다`() {
            // 5이닝(15아웃) 대신 4이닝(12아웃)으로 설정
            val rules = GameRules(starterWinQualificationOuts = 12)

            assertThat(rules.starterWinQualificationOuts).isEqualTo(12)
        }

        @Test
        fun `선발 승리 자격 아웃 수가 0이면 예외가 발생한다`() {
            assertThatThrownBy { GameRules(starterWinQualificationOuts = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("선발 승리 자격 아웃 수는 양수여야 합니다")
        }
    }

    @Nested
    @DisplayName("규정 배수 검증")
    inner class QualificationMultiplierValidation {
        @Test
        fun `규정 타석 배수를 사용자 정의할 수 있다`() {
            val rules = GameRules(qualificationPAMultiplier = 2.7)

            assertThat(rules.qualificationPAMultiplier).isEqualTo(2.7)
        }

        @Test
        fun `규정 타석 배수가 0이면 예외가 발생한다`() {
            assertThatThrownBy { GameRules(qualificationPAMultiplier = 0.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("규정 타석 배수는 양수여야 합니다")
        }

        @Test
        fun `규정 이닝 배수가 0이면 예외가 발생한다`() {
            assertThatThrownBy { GameRules(qualificationIPMultiplier = 0.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("규정 이닝 배수는 양수여야 합니다")
        }
    }

    @Nested
    @DisplayName("연장 이닝 설정")
    inner class MaxExtraInningsValidation {
        @Test
        fun `최대 연장 이닝을 설정할 수 있다`() {
            val rules = GameRules(maxExtraInnings = 2)

            assertThat(rules.maxExtraInnings).isEqualTo(2)
        }

        @Test
        fun `최대 연장 이닝이 0이면 예외가 발생한다`() {
            assertThatThrownBy { GameRules(maxExtraInnings = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최대 연장 이닝 수는 양수여야 합니다")
        }

        @Test
        fun `최대 연장 이닝을 null로 설정하면 무제한이다`() {
            val rules = GameRules(maxExtraInnings = null)

            assertThat(rules.maxExtraInnings).isNull()
        }
    }

    @Nested
    @DisplayName("시간 제한 설정")
    inner class TimeLimitValidation {
        @Test
        fun `시간 제한을 설정할 수 있다`() {
            val rules = GameRules(timeLimitMinutes = 90)

            assertThat(rules.timeLimitMinutes).isEqualTo(90)
        }

        @Test
        fun `시간 제한이 0이면 예외가 발생한다`() {
            assertThatThrownBy { GameRules(timeLimitMinutes = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("시간 제한은 양수여야 합니다")
        }

        @Test
        fun `시간 제한을 null로 설정하면 무제한이다`() {
            val rules = GameRules(timeLimitMinutes = null)

            assertThat(rules.timeLimitMinutes).isNull()
        }
    }

    @Nested
    @DisplayName("동점 처리 설정")
    inner class TiedGameResultSetting {
        @Test
        fun `무승부로 처리할 수 있다`() {
            val rules = GameRules(tiedGameResult = TiedGameResult.DRAW)

            assertThat(rules.tiedGameResult).isEqualTo(TiedGameResult.DRAW)
        }

        @Test
        fun `타이브레이커로 처리할 수 있다`() {
            val rules = GameRules(tiedGameResult = TiedGameResult.TIEBREAKER)

            assertThat(rules.tiedGameResult).isEqualTo(TiedGameResult.TIEBREAKER)
        }

        @Test
        fun `재경기로 처리할 수 있다`() {
            val rules = GameRules(tiedGameResult = TiedGameResult.RESCHEDULE)

            assertThat(rules.tiedGameResult).isEqualTo(TiedGameResult.RESCHEDULE)
        }
    }

    @Nested
    @DisplayName("사회인 야구 대표 규칙 설정")
    inner class CommonAmateurBaseballRules {
        @Test
        fun `7이닝 단축 콜드게임 규칙을 설정할 수 있다`() {
            val rules =
                GameRules(
                    defaultInnings = 7,
                    mercyRuleEnabled = true,
                    mercyRunDifference = 10,
                    mercyMinimumInning = 5,
                    forfeitScore = 9,
                )

            assertThat(rules.defaultInnings).isEqualTo(7)
            assertThat(rules.mercyRuleEnabled).isTrue()
            assertThat(rules.mercyRunDifference).isEqualTo(10)
            assertThat(rules.mercyMinimumInning).isEqualTo(5)
            assertThat(rules.forfeitScore).isEqualTo(9)
        }

        @Test
        fun `시간 제한 있는 경기 규칙을 설정할 수 있다`() {
            val rules =
                GameRules(
                    defaultInnings = 7,
                    timeLimitMinutes = 100,
                    maxExtraInnings = 1,
                )

            assertThat(rules.timeLimitMinutes).isEqualTo(100)
            assertThat(rules.maxExtraInnings).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("최소 타순 인원 검증")
    inner class MinBattingOrderCountValidation {
        @Test
        fun `기본값은 9이다`() {
            val rules = GameRules()
            assertThat(rules.minBattingOrderCount).isEqualTo(9)
        }

        @Test
        fun `8인 경기를 설정할 수 있다`() {
            val rules = GameRules(minBattingOrderCount = 8)
            assertThat(rules.minBattingOrderCount).isEqualTo(8)
        }

        @Test
        fun `9인 경기를 명시적으로 설정할 수 있다`() {
            val rules = GameRules(minBattingOrderCount = 9)
            assertThat(rules.minBattingOrderCount).isEqualTo(9)
        }

        @Test
        fun `7인 경기는 유효하지 않다`() {
            assertThatThrownBy { GameRules(minBattingOrderCount = 7) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 타순 인원은 8~9 사이여야 합니다")
        }

        @Test
        fun `10인 경기는 유효하지 않다`() {
            assertThatThrownBy { GameRules(minBattingOrderCount = 10) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 타순 인원은 8~9 사이여야 합니다")
        }
    }

    @Nested
    @DisplayName("순위 타이브레이커 설정")
    inner class StandingsTiebreakerValidation {
        @Test
        fun `기본 타이브레이커 순서는 상대전적-득실점차-다득점이다`() {
            val rules = GameRules()

            assertThat(rules.standingsTiebreakerOrder)
                .isEqualTo("HEAD_TO_HEAD,RUN_DIFFERENTIAL,RUNS_SCORED")
        }

        @Test
        fun `기본 타이브레이커 순서를 파싱할 수 있다`() {
            val rules = GameRules()
            val criteria = rules.parseTiebreakerOrder()

            assertThat(criteria).containsExactly(
                TiebreakerCriterion.HEAD_TO_HEAD,
                TiebreakerCriterion.RUN_DIFFERENTIAL,
                TiebreakerCriterion.RUNS_SCORED,
            )
        }

        @Test
        fun `커스텀 타이브레이커 순서를 설정할 수 있다`() {
            val rules =
                GameRules(standingsTiebreakerOrder = "RUN_DIFFERENTIAL,RUNS_SCORED,HEAD_TO_HEAD")
            val criteria = rules.parseTiebreakerOrder()

            assertThat(criteria).containsExactly(
                TiebreakerCriterion.RUN_DIFFERENTIAL,
                TiebreakerCriterion.RUNS_SCORED,
                TiebreakerCriterion.HEAD_TO_HEAD,
            )
        }

        @Test
        fun `단일 기준만 설정할 수 있다`() {
            val rules = GameRules(standingsTiebreakerOrder = "HEAD_TO_HEAD")
            val criteria = rules.parseTiebreakerOrder()

            assertThat(criteria).containsExactly(TiebreakerCriterion.HEAD_TO_HEAD)
        }

        @Test
        fun `빈 타이브레이커 순서는 예외가 발생한다`() {
            assertThatThrownBy { GameRules(standingsTiebreakerOrder = "") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("비어있을 수 없습니다")
        }

        @Test
        fun `중복된 기준이 있으면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(standingsTiebreakerOrder = "HEAD_TO_HEAD,HEAD_TO_HEAD")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("중복된 기준")
        }

        @Test
        fun `유효하지 않은 기준이면 예외가 발생한다`() {
            assertThatThrownBy {
                GameRules(standingsTiebreakerOrder = "INVALID_CRITERION")
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
