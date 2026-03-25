package com.nextup.core.domain.discipline

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PlayerBan 엔티티 테스트")
class PlayerBanTest {
    @Nested
    @DisplayName("create()")
    inner class Create {
        @Test
        @DisplayName("정상적으로 영구 제재를 생성할 수 있다")
        fun createBan() {
            val ban =
                PlayerBan.create(
                    playerId = 1L,
                    competitionId = 10L,
                    reason = "폭력 행위",
                    issuedBy = "관리자",
                )

            assertThat(ban.playerId).isEqualTo(1L)
            assertThat(ban.competitionId).isEqualTo(10L)
            assertThat(ban.reason).isEqualTo("폭력 행위")
            assertThat(ban.issuedBy).isEqualTo("관리자")
            assertThat(ban.issuedAt).isNotNull()
        }

        @Test
        @DisplayName("사유가 비어있으면 예외가 발생한다")
        fun failWhenReasonBlank() {
            assertThatThrownBy {
                PlayerBan.create(
                    playerId = 1L,
                    competitionId = 10L,
                    reason = "",
                    issuedBy = "관리자",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("제재 사유는 필수")
        }

        @Test
        @DisplayName("발급자가 비어있으면 예외가 발생한다")
        fun failWhenIssuedByBlank() {
            assertThatThrownBy {
                PlayerBan.create(
                    playerId = 1L,
                    competitionId = 10L,
                    reason = "폭력 행위",
                    issuedBy = "",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("제재 발급자는 필수")
        }

        @Test
        @DisplayName("공백만 있는 사유는 예외가 발생한다")
        fun failWhenReasonOnlyWhitespace() {
            assertThatThrownBy {
                PlayerBan.create(
                    playerId = 1L,
                    competitionId = 10L,
                    reason = "   ",
                    issuedBy = "관리자",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("공백만 있는 발급자는 예외가 발생한다")
        fun failWhenIssuedByOnlyWhitespace() {
            assertThatThrownBy {
                PlayerBan.create(
                    playerId = 1L,
                    competitionId = 10L,
                    reason = "폭력 행위",
                    issuedBy = "   ",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
