package com.nextup.core.domain.stadium

import com.nextup.common.exception.BookingTransferInvalidStateException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

@DisplayName("BookingTransfer")
class BookingTransferTest {
    private fun createOpenTransfer(
        bookingId: Long = 1L,
        sellerTeamId: Long = 10L,
        transferPrice: BigDecimal? = BigDecimal("50000"),
        message: String? = "긴급 양도합니다",
        expiresAt: Instant = Instant.now().plusSeconds(3600),
    ): BookingTransfer =
        BookingTransfer.create(
            bookingId = bookingId,
            sellerTeamId = sellerTeamId,
            transferPrice = transferPrice,
            message = message,
            expiresAt = expiresAt,
        )

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should create transfer successfully`() {
            // when
            val transfer = createOpenTransfer()

            // then
            assertThat(transfer.bookingId).isEqualTo(1L)
            assertThat(transfer.sellerTeamId).isEqualTo(10L)
            assertThat(transfer.transferPrice).isEqualByComparingTo(BigDecimal("50000"))
            assertThat(transfer.message).isEqualTo("긴급 양도합니다")
            assertThat(transfer.status).isEqualTo(TransferStatus.OPEN)
            assertThat(transfer.buyerTeamId).isNull()
            assertThat(transfer.acceptedAt).isNull()
        }

        @Test
        fun `should create transfer without price`() {
            // when
            val transfer = createOpenTransfer(transferPrice = null)

            // then
            assertThat(transfer.transferPrice).isNull()
            assertThat(transfer.status).isEqualTo(TransferStatus.OPEN)
        }

        @Test
        fun `should throw exception when booking ID is not positive`() {
            assertThatThrownBy {
                createOpenTransfer(bookingId = 0L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Booking ID must be positive")
        }

        @Test
        fun `should throw exception when seller team ID is not positive`() {
            assertThatThrownBy {
                createOpenTransfer(sellerTeamId = -1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Seller team ID must be positive")
        }

        @Test
        fun `should throw exception when transfer price is negative`() {
            assertThatThrownBy {
                createOpenTransfer(transferPrice = BigDecimal("-1000"))
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Transfer price must be non-negative")
        }

        @Test
        fun `should throw exception when expires at is in the past`() {
            assertThatThrownBy {
                createOpenTransfer(expiresAt = Instant.now().minusSeconds(60))
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Expiry time must be in the future")
        }
    }

    @Nested
    @DisplayName("accept")
    inner class Accept {
        @Test
        fun `should accept open transfer successfully`() {
            // given
            val transfer = createOpenTransfer()

            // when
            transfer.accept(buyerTeamId = 20L)

            // then
            assertThat(transfer.status).isEqualTo(TransferStatus.ACCEPTED)
            assertThat(transfer.buyerTeamId).isEqualTo(20L)
            assertThat(transfer.acceptedAt).isNotNull()
        }

        @Test
        fun `should throw exception when accepting already accepted transfer`() {
            // given
            val transfer = createOpenTransfer()
            transfer.accept(buyerTeamId = 20L)

            // when & then
            assertThatThrownBy {
                transfer.accept(buyerTeamId = 30L)
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be accepted")
        }

        @Test
        fun `should throw exception when accepting cancelled transfer`() {
            // given
            val transfer = createOpenTransfer()
            transfer.cancel()

            // when & then
            assertThatThrownBy {
                transfer.accept(buyerTeamId = 20L)
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be accepted")
        }

        @Test
        fun `should throw exception when accepting expired transfer`() {
            // given
            val transfer =
                createOpenTransfer(expiresAt = Instant.now().plusSeconds(1))
            Thread.sleep(1100)

            // when & then
            assertThatThrownBy {
                transfer.accept(buyerTeamId = 20L)
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("expired")
        }

        @Test
        fun `should throw exception when buyer team is same as seller team`() {
            // given
            val transfer = createOpenTransfer(sellerTeamId = 10L)

            // when & then
            assertThatThrownBy {
                transfer.accept(buyerTeamId = 10L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Buyer team cannot be the same as seller team")
        }

        @Test
        fun `should throw exception when buyer team ID is not positive`() {
            // given
            val transfer = createOpenTransfer()

            // when & then
            assertThatThrownBy {
                transfer.accept(buyerTeamId = 0L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Buyer team ID must be positive")
        }
    }

    @Nested
    @DisplayName("cancel")
    inner class Cancel {
        @Test
        fun `should cancel open transfer successfully`() {
            // given
            val transfer = createOpenTransfer()

            // when
            transfer.cancel()

            // then
            assertThat(transfer.status).isEqualTo(TransferStatus.CANCELLED)
        }

        @Test
        fun `should throw exception when cancelling accepted transfer`() {
            // given
            val transfer = createOpenTransfer()
            transfer.accept(buyerTeamId = 20L)

            // when & then
            assertThatThrownBy {
                transfer.cancel()
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be cancelled")
        }

        @Test
        fun `should throw exception when cancelling already cancelled transfer`() {
            // given
            val transfer = createOpenTransfer()
            transfer.cancel()

            // when & then
            assertThatThrownBy {
                transfer.cancel()
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be cancelled")
        }
    }

    @Nested
    @DisplayName("isExpired")
    inner class IsExpired {
        @Test
        fun `should return false when transfer is not expired`() {
            // given
            val transfer = createOpenTransfer(expiresAt = Instant.now().plusSeconds(3600))

            // when & then
            assertThat(transfer.isExpired()).isFalse()
        }

        @Test
        fun `should return true when transfer is expired`() {
            // given
            val transfer =
                createOpenTransfer(expiresAt = Instant.now().plusSeconds(1))
            Thread.sleep(1100)

            // when & then
            assertThat(transfer.isExpired()).isTrue()
        }
    }

    @Nested
    @DisplayName("expire")
    inner class Expire {
        @Test
        fun `should expire open transfer successfully`() {
            // given
            val transfer = createOpenTransfer()

            // when
            transfer.expire()

            // then
            assertThat(transfer.status).isEqualTo(TransferStatus.EXPIRED)
        }

        @Test
        fun `should throw exception when expiring accepted transfer`() {
            // given
            val transfer = createOpenTransfer()
            transfer.accept(buyerTeamId = 20L)

            // when & then
            assertThatThrownBy {
                transfer.expire()
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("cannot be expired")
        }
    }
}
