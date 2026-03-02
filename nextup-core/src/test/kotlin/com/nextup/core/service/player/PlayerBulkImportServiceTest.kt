package com.nextup.core.service.player

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PlayerBulkImportService - 데이터 모델")
class PlayerBulkImportServiceTest {

    @Nested
    @DisplayName("PlayerImportRow")
    inner class PlayerImportRowTest {

        @Test
        fun `모든 필드가 포함된 행을 생성할 수 있다`() {
            val row =
                PlayerImportRow(
                    rowNumber = 2,
                    name = "홍길동",
                    primaryPosition = "SHORTSTOP",
                    birthDate = "1990-01-15",
                    height = 180,
                    weight = 75,
                    throwingHand = "RIGHT",
                    battingHand = "LEFT",
                )

            assertThat(row.rowNumber).isEqualTo(2)
            assertThat(row.name).isEqualTo("홍길동")
            assertThat(row.primaryPosition).isEqualTo("SHORTSTOP")
            assertThat(row.birthDate).isEqualTo("1990-01-15")
            assertThat(row.height).isEqualTo(180)
            assertThat(row.weight).isEqualTo(75)
            assertThat(row.throwingHand).isEqualTo("RIGHT")
            assertThat(row.battingHand).isEqualTo("LEFT")
        }

        @Test
        fun `선택 필드가 null인 행을 생성할 수 있다`() {
            val row =
                PlayerImportRow(
                    rowNumber = 1,
                    name = "김철수",
                    primaryPosition = "CATCHER",
                    birthDate = null,
                    height = null,
                    weight = null,
                    throwingHand = null,
                    battingHand = null,
                )

            assertThat(row.birthDate).isNull()
            assertThat(row.height).isNull()
            assertThat(row.weight).isNull()
            assertThat(row.throwingHand).isNull()
            assertThat(row.battingHand).isNull()
        }

        @Test
        fun `동일한 데이터를 가진 행은 동등하다`() {
            val row1 =
                PlayerImportRow(
                    rowNumber = 1,
                    name = "홍길동",
                    primaryPosition = "SHORTSTOP",
                    birthDate = null,
                    height = null,
                    weight = null,
                    throwingHand = null,
                    battingHand = null,
                )
            val row2 = row1.copy()

            assertThat(row1).isEqualTo(row2)
            assertThat(row1.hashCode()).isEqualTo(row2.hashCode())
        }
    }

    @Nested
    @DisplayName("PlayerImportResult")
    inner class PlayerImportResultTest {

        @Test
        fun `성공 결과를 생성할 수 있다`() {
            val player = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP)
            val result =
                PlayerImportResult(
                    successCount = 1,
                    errorCount = 0,
                    importedPlayers = listOf(player),
                    errors = emptyList(),
                )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(0)
            assertThat(result.importedPlayers).hasSize(1)
            assertThat(result.errors).isEmpty()
        }

        @Test
        fun `혼합 결과를 생성할 수 있다`() {
            val player = Player(name = "김철수", primaryPosition = Position.CATCHER)
            val error = PlayerImportError(rowNumber = 3, reason = "유효하지 않은 포지션")
            val result =
                PlayerImportResult(
                    successCount = 1,
                    errorCount = 1,
                    importedPlayers = listOf(player),
                    errors = listOf(error),
                )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
            assertThat(result.importedPlayers).hasSize(1)
            assertThat(result.errors).hasSize(1)
        }
    }

    @Nested
    @DisplayName("PlayerImportError")
    inner class PlayerImportErrorTest {

        @Test
        fun `오류를 생성할 수 있다`() {
            val error =
                PlayerImportError(
                    rowNumber = 5,
                    reason = "유효하지 않은 포지션입니다",
                )

            assertThat(error.rowNumber).isEqualTo(5)
            assertThat(error.reason).isEqualTo("유효하지 않은 포지션입니다")
        }

        @Test
        fun `동일한 데이터를 가진 오류는 동등하다`() {
            val error1 = PlayerImportError(rowNumber = 2, reason = "이름 누락")
            val error2 = PlayerImportError(rowNumber = 2, reason = "이름 누락")

            assertThat(error1).isEqualTo(error2)
            assertThat(error1.hashCode()).isEqualTo(error2.hashCode())
            assertThat(error1.toString()).contains("rowNumber=2")
            assertThat(error1.toString()).contains("이름 누락")
        }
    }
}
