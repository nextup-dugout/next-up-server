package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.TransferStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.Instant

@DisplayName("BookingTransferRepositoryAdapter")
class BookingTransferRepositoryAdapterTest {
    private lateinit var jpaRepository: BookingTransferJpaRepository
    private lateinit var adapter: BookingTransferRepositoryAdapter

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = BookingTransferRepositoryAdapter(jpaRepository)
    }

    private fun createTransfer(
        bookingId: Long = 1L,
        sellerTeamId: Long = 10L,
        status: TransferStatus = TransferStatus.OPEN,
    ): BookingTransfer {
        val transfer =
            BookingTransfer.create(
                bookingId = bookingId,
                sellerTeamId = sellerTeamId,
                transferPrice = BigDecimal("50000"),
                message = "양도합니다",
                expiresAt = Instant.now().plusSeconds(3600),
            )
        if (status == TransferStatus.CANCELLED) transfer.cancel()
        if (status == TransferStatus.EXPIRED) transfer.expire()
        return transfer
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `should save and return transfer`() {
            // given
            val transfer = createTransfer()
            every { jpaRepository.save(transfer) } returns transfer

            // when
            val result = adapter.save(transfer)

            // then
            assertThat(result).isEqualTo(transfer)
            verify { jpaRepository.save(transfer) }
        }
    }

    @Nested
    @DisplayName("findByIdOrNull")
    inner class FindByIdOrNull {
        @Test
        fun `should return transfer when found`() {
            // given
            val transfer = createTransfer()
            every { jpaRepository.findByIdOrNull(1L) } returns transfer

            // when
            val result = adapter.findByIdOrNull(1L)

            // then
            assertThat(result).isEqualTo(transfer)
        }

        @Test
        fun `should return null when not found`() {
            // given
            every { jpaRepository.findByIdOrNull(99L) } returns null

            // when
            val result = adapter.findByIdOrNull(99L)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByStatus")
    inner class FindByStatus {
        @Test
        fun `should return transfers with given status`() {
            // given
            val transfer1 = createTransfer(bookingId = 1L)
            val transfer2 = createTransfer(bookingId = 2L)
            every { jpaRepository.findByStatus(TransferStatus.OPEN) } returns listOf(transfer1, transfer2)

            // when
            val result = adapter.findByStatus(TransferStatus.OPEN)

            // then
            assertThat(result).hasSize(2)
            assertThat(result).containsExactly(transfer1, transfer2)
        }

        @Test
        fun `should return empty list when no transfers with given status`() {
            // given
            every { jpaRepository.findByStatus(TransferStatus.CANCELLED) } returns emptyList()

            // when
            val result = adapter.findByStatus(TransferStatus.CANCELLED)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByBookingId")
    inner class FindByBookingId {
        @Test
        fun `should return transfers for given booking`() {
            // given
            val transfer = createTransfer(bookingId = 5L)
            every { jpaRepository.findByBookingId(5L) } returns listOf(transfer)

            // when
            val result = adapter.findByBookingId(5L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].bookingId).isEqualTo(5L)
        }

        @Test
        fun `should return empty list when no transfers for booking`() {
            // given
            every { jpaRepository.findByBookingId(99L) } returns emptyList()

            // when
            val result = adapter.findByBookingId(99L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findBySellerTeamId")
    inner class FindBySellerTeamId {
        @Test
        fun `should return transfers for given seller team`() {
            // given
            val transfer = createTransfer(sellerTeamId = 10L)
            every { jpaRepository.findBySellerTeamId(10L) } returns listOf(transfer)

            // when
            val result = adapter.findBySellerTeamId(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].sellerTeamId).isEqualTo(10L)
        }

        @Test
        fun `should return empty list when no transfers for seller team`() {
            // given
            every { jpaRepository.findBySellerTeamId(99L) } returns emptyList()

            // when
            val result = adapter.findBySellerTeamId(99L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("existsOpenTransferForBooking")
    inner class ExistsOpenTransferForBooking {
        @Test
        fun `should return true when open transfer exists`() {
            // given
            every { jpaRepository.existsOpenTransferForBooking(1L) } returns true

            // when
            val result = adapter.existsOpenTransferForBooking(1L)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when no open transfer exists`() {
            // given
            every { jpaRepository.existsOpenTransferForBooking(99L) } returns false

            // when
            val result = adapter.existsOpenTransferForBooking(99L)

            // then
            assertThat(result).isFalse()
        }
    }
}
