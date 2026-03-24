package com.nextup.core.domain.stadium

import com.nextup.common.exception.BookingTransferInvalidStateException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BookingTransfer")
class BookingTransferTest {
    private fun createPendingTransfer(
        bookingId: Long = 1L,
        fromTeamId: Long = 10L,
        toTeamId: Long = 20L,
        message: String? = "양도합니다",
    ): BookingTransfer =
        BookingTransfer.create(
            bookingId = bookingId,
            fromTeamId = fromTeamId,
            toTeamId = toTeamId,
            message = message,
        )

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should create transfer successfully`() {
            // when
            val transfer = createPendingTransfer()

            // then
            assertThat(transfer.bookingId).isEqualTo(1L)
            assertThat(transfer.fromTeamId).isEqualTo(10L)
            assertThat(transfer.toTeamId).isEqualTo(20L)
            assertThat(transfer.message).isEqualTo("양도합니다")
            assertThat(transfer.status).isEqualTo(TransferStatus.PENDING)
        }

        @Test
        fun `should create transfer without message`() {
            // when
            val transfer = createPendingTransfer(message = null)

            // then
            assertThat(transfer.message).isNull()
            assertThat(transfer.status).isEqualTo(TransferStatus.PENDING)
        }

        @Test
        fun `should throw exception when booking ID is not positive`() {
            assertThatThrownBy {
                createPendingTransfer(bookingId = 0L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Booking ID must be positive")
        }

        @Test
        fun `should throw exception when from team ID is not positive`() {
            assertThatThrownBy {
                createPendingTransfer(fromTeamId = -1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("From team ID must be positive")
        }

        @Test
        fun `should throw exception when to team ID is not positive`() {
            assertThatThrownBy {
                createPendingTransfer(toTeamId = 0L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("To team ID must be positive")
        }

        @Test
        fun `should throw exception when from and to team are the same`() {
            assertThatThrownBy {
                createPendingTransfer(fromTeamId = 10L, toTeamId = 10L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Cannot transfer to the same team")
        }
    }

    @Nested
    @DisplayName("accept")
    inner class Accept {
        @Test
        fun `should accept pending transfer successfully`() {
            // given
            val transfer = createPendingTransfer()

            // when
            transfer.accept()

            // then
            assertThat(transfer.status).isEqualTo(TransferStatus.ACCEPTED)
        }

        @Test
        fun `should throw exception when accepting already accepted transfer`() {
            // given
            val transfer = createPendingTransfer()
            transfer.accept()

            // when & then
            assertThatThrownBy {
                transfer.accept()
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be accepted")
        }

        @Test
        fun `should throw exception when accepting rejected transfer`() {
            // given
            val transfer = createPendingTransfer()
            transfer.reject()

            // when & then
            assertThatThrownBy {
                transfer.accept()
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be accepted")
        }
    }

    @Nested
    @DisplayName("reject")
    inner class Reject {
        @Test
        fun `should reject pending transfer successfully`() {
            // given
            val transfer = createPendingTransfer()

            // when
            transfer.reject()

            // then
            assertThat(transfer.status).isEqualTo(TransferStatus.REJECTED)
        }

        @Test
        fun `should throw exception when rejecting accepted transfer`() {
            // given
            val transfer = createPendingTransfer()
            transfer.accept()

            // when & then
            assertThatThrownBy {
                transfer.reject()
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be rejected")
        }

        @Test
        fun `should throw exception when rejecting already rejected transfer`() {
            // given
            val transfer = createPendingTransfer()
            transfer.reject()

            // when & then
            assertThatThrownBy {
                transfer.reject()
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be rejected")
        }
    }
}
