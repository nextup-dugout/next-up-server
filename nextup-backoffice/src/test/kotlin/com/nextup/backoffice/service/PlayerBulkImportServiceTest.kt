package com.nextup.backoffice.service

import com.nextup.backoffice.dto.player.PlayerImportItem
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.port.repository.PlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PlayerBulkImportService 테스트")
class PlayerBulkImportServiceTest {
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var service: PlayerBulkImportService

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        service = PlayerBulkImportService(playerRepository)
    }

    @Nested
    @DisplayName("importPlayers")
    inner class ImportPlayers {
        @Test
        @DisplayName("유효한 선수 목록을 일괄 등록한다")
        fun `should import all valid players successfully`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        birthDate = LocalDate.of(1990, 5, 15),
                        height = 180,
                        weight = 80,
                        throwingHand = ThrowingHand.RIGHT,
                        battingHand = BattingHand.LEFT,
                        debutYear = 2015,
                    ),
                    PlayerImportItem(
                        name = "김철수",
                        primaryPosition = Position.CATCHER,
                    ),
                )

            every { playerRepository.save(any()) } answers {
                val player = firstArg<Player>()
                player
            }

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.totalRequested).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(result.importedPlayers).hasSize(2)
            assertThat(result.failures).isEmpty()
            verify(exactly = 2) { playerRepository.save(any()) }
        }

        @Test
        @DisplayName("빈 이름은 검증 실패한다")
        fun `should fail validation for blank name`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "",
                        primaryPosition = Position.SHORTSTOP,
                    ),
                )

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.totalRequested).isEqualTo(1)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].rowIndex).isEqualTo(0)
            assertThat(result.failures[0].reason).contains("이름")
        }

        @Test
        @DisplayName("유효하지 않은 키 범위는 검증 실패한다")
        fun `should fail validation for invalid height`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        height = 50,
                    ),
                )

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.totalRequested).isEqualTo(1)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).contains("키")
        }

        @Test
        @DisplayName("유효하지 않은 몸무게 범위는 검증 실패한다")
        fun `should fail validation for invalid weight`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        weight = 10,
                    ),
                )

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.totalRequested).isEqualTo(1)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).contains("몸무게")
        }

        @Test
        @DisplayName("유효하지 않은 데뷔 연도는 검증 실패한다")
        fun `should fail validation for invalid debut year`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        debutYear = 1800,
                    ),
                )

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.totalRequested).isEqualTo(1)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).contains("데뷔 연도")
        }

        @Test
        @DisplayName("일부 실패해도 성공한 항목은 저장된다")
        fun `should save successful items even when some fail`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                    ),
                    PlayerImportItem(
                        name = "",
                        primaryPosition = Position.CATCHER,
                    ),
                    PlayerImportItem(
                        name = "박지성",
                        primaryPosition = Position.CENTER_FIELD,
                    ),
                )

            every { playerRepository.save(any()) } answers {
                val player = firstArg<Player>()
                player
            }

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.totalRequested).isEqualTo(3)
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].rowIndex).isEqualTo(1)
            verify(exactly = 2) { playerRepository.save(any()) }
        }

        @Test
        @DisplayName("저장소 예외 시 실패로 처리된다")
        fun `should handle repository exceptions as failures`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                    ),
                )

            every { playerRepository.save(any()) } throws RuntimeException("DB 오류")

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.totalRequested).isEqualTo(1)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).isEqualTo("DB 오류")
        }

        @Test
        @DisplayName("빈 목록 요청 시 빈 결과를 반환한다")
        fun `should return empty result for empty list`() {
            // when
            val result = service.importPlayers(emptyList())

            // then
            assertThat(result.totalRequested).isEqualTo(0)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(result.importedPlayers).isEmpty()
            assertThat(result.failures).isEmpty()
        }

        @Test
        @DisplayName("예외 메시지가 null이면 기본 오류 메시지를 사용한다")
        fun `should use default error message when exception message is null`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                    ),
                )

            every { playerRepository.save(any()) } throws RuntimeException()

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).isEqualTo("알 수 없는 오류")
        }

        @Test
        @DisplayName("키 상한 초과 시 검증 실패한다")
        fun `should fail validation for height above maximum`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        height = 300,
                    ),
                )

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).contains("키")
        }

        @Test
        @DisplayName("몸무게 상한 초과 시 검증 실패한다")
        fun `should fail validation for weight above maximum`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        weight = 250,
                    ),
                )

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).contains("몸무게")
        }

        @Test
        @DisplayName("데뷔 연도 상한 초과 시 검증 실패한다")
        fun `should fail validation for debut year above maximum`() {
            // given
            val items =
                listOf(
                    PlayerImportItem(
                        name = "홍길동",
                        primaryPosition = Position.SHORTSTOP,
                        debutYear = 2200,
                    ),
                )

            // when
            val result = service.importPlayers(items)

            // then
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures[0].reason).contains("데뷔 연도")
        }
    }
}
