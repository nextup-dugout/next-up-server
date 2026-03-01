package com.nextup.infrastructure.service.player

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.service.player.PlayerImportRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PlayerBulkImportServiceImpl")
class PlayerBulkImportServiceImplTest {

    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var service: PlayerBulkImportServiceImpl

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        service = PlayerBulkImportServiceImpl(playerRepository)
    }

    @Nested
    @DisplayName("importPlayers")
    inner class ImportPlayers {

        @Test
        fun `유효한 선수 데이터를 임포트할 수 있다`() {
            // given
            val rows =
                listOf(
                    PlayerImportRow(
                        rowNumber = 2,
                        name = "홍길동",
                        primaryPosition = "SHORTSTOP",
                        birthDate = "1990-01-15",
                        height = 180,
                        weight = 75,
                        throwingHand = "RIGHT",
                        battingHand = "RIGHT",
                    ),
                )
            val savedPlayer = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP)
            val playerSlot = slot<Player>()
            every { playerRepository.save(capture(playerSlot)) } returns savedPlayer

            // when
            val result = service.importPlayers(rows)

            // then
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(0)
            assertThat(result.errors).isEmpty()
            assertThat(result.importedPlayers).hasSize(1)

            val captured = playerSlot.captured
            assertThat(captured.name).isEqualTo("홍길동")
            assertThat(captured.primaryPosition).isEqualTo(Position.SHORTSTOP)
            assertThat(captured.height).isEqualTo(180)
            assertThat(captured.weight).isEqualTo(75)
        }

        @Test
        fun `선택 필드가 없는 선수도 임포트할 수 있다`() {
            // given
            val rows =
                listOf(
                    PlayerImportRow(
                        rowNumber = 2,
                        name = "김철수",
                        primaryPosition = "STARTING_PITCHER",
                        birthDate = null,
                        height = null,
                        weight = null,
                        throwingHand = null,
                        battingHand = null,
                    ),
                )
            val savedPlayer = Player(name = "김철수", primaryPosition = Position.STARTING_PITCHER)
            every { playerRepository.save(any()) } returns savedPlayer

            // when
            val result = service.importPlayers(rows)

            // then
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(0)
        }

        @Test
        fun `유효하지 않은 포지션은 오류로 처리하고 나머지 행은 계속 임포트한다`() {
            // given
            val rows =
                listOf(
                    PlayerImportRow(
                        rowNumber = 2,
                        name = "오류선수",
                        primaryPosition = "INVALID_POS",
                        birthDate = null,
                        height = null,
                        weight = null,
                        throwingHand = null,
                        battingHand = null,
                    ),
                    PlayerImportRow(
                        rowNumber = 3,
                        name = "정상선수",
                        primaryPosition = "CATCHER",
                        birthDate = null,
                        height = null,
                        weight = null,
                        throwingHand = null,
                        battingHand = null,
                    ),
                )
            val savedPlayer = Player(name = "정상선수", primaryPosition = Position.CATCHER)
            every { playerRepository.save(any()) } returns savedPlayer

            // when
            val result = service.importPlayers(rows)

            // then
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
            assertThat(result.errors[0].rowNumber).isEqualTo(2)
            assertThat(result.errors[0].reason).contains("INVALID_POS")
            verify(exactly = 1) { playerRepository.save(any()) }
        }

        @Test
        fun `유효하지 않은 생년월일 형식은 오류로 처리한다`() {
            // given
            val rows =
                listOf(
                    PlayerImportRow(
                        rowNumber = 2,
                        name = "홍길동",
                        primaryPosition = "SHORTSTOP",
                        birthDate = "90-01-15",
                        height = null,
                        weight = null,
                        throwingHand = null,
                        battingHand = null,
                    ),
                )

            // when
            val result = service.importPlayers(rows)

            // then
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.errorCount).isEqualTo(1)
            assertThat(result.errors[0].rowNumber).isEqualTo(2)
        }

        @Test
        fun `빈 이름은 오류로 처리한다`() {
            // given
            val rows =
                listOf(
                    PlayerImportRow(
                        rowNumber = 2,
                        name = "",
                        primaryPosition = "SHORTSTOP",
                        birthDate = null,
                        height = null,
                        weight = null,
                        throwingHand = null,
                        battingHand = null,
                    ),
                )

            // when
            val result = service.importPlayers(rows)

            // then
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.errorCount).isEqualTo(1)
        }

        @Test
        fun `빈 목록을 임포트하면 결과가 비어 있다`() {
            // when
            val result = service.importPlayers(emptyList())

            // then
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.errorCount).isEqualTo(0)
            assertThat(result.importedPlayers).isEmpty()
            assertThat(result.errors).isEmpty()
        }
    }
}
