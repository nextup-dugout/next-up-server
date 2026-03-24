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
        fromTeamId: Long = 10L,
        toTeamId: Long = 20L,
        status: TransferStatus = TransferStatus.PENDING,
    ): BookingTransfer {
        val transfer =
            BookingTransfer.create(
                bookingId = bookingId,
                fromTeamId = fromTeamId,
                toTeamId = toTeamId,
                message = "양도합니다",
            )
        if (status == TransferStatus.ACCEPTED) transfer.accept()
        if (status == TransferStatus.REJECTED) transfer.reject()
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
            every { jpaRepository.findByStatus(TransferStatus.PENDING) } returns listOf(transfer1, transfer2)

            // when
            val result = adapter.findByStatus(TransferStatus.PENDING)

            // then
            assertThat(result).hasSize(2)
            assertThat(result).containsExactly(transfer1, transfer2)
        }

        @Test
        fun `should return empty list when no transfers with given status`() {
            // given
            every { jpaRepository.findByStatus(TransferStatus.REJECTED) } returns emptyList()

            // when
            val result = adapter.findByStatus(TransferStatus.REJECTED)

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
    @DisplayName("findByFromTeamId")
    inner class FindByFromTeamId {
        @Test
        fun `should return transfers for given from team`() {
            // given
            val transfer = createTransfer(fromTeamId = 10L)
            every { jpaRepository.findByFromTeamId(10L) } returns listOf(transfer)

            // when
            val result = adapter.findByFromTeamId(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].fromTeamId).isEqualTo(10L)
        }

        @Test
        fun `should return empty list when no transfers for from team`() {
            // given
            every { jpaRepository.findByFromTeamId(99L) } returns emptyList()

            // when
            val result = adapter.findByFromTeamId(99L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByToTeamId")
    inner class FindByToTeamId {
        @Test
        fun `should return transfers for given to team`() {
            // given
            val transfer = createTransfer(toTeamId = 20L)
            every { jpaRepository.findByToTeamId(20L) } returns listOf(transfer)

            // when
            val result = adapter.findByToTeamId(20L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].toTeamId).isEqualTo(20L)
        }

        @Test
        fun `should return empty list when no transfers for to team`() {
            // given
            every { jpaRepository.findByToTeamId(99L) } returns emptyList()

            // when
            val result = adapter.findByToTeamId(99L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("existsPendingTransferForBooking")
    inner class ExistsPendingTransferForBooking {
        @Test
        fun `should return true when pending transfer exists`() {
            // given
            every { jpaRepository.existsPendingTransferForBooking(1L) } returns true

            // when
            val result = adapter.existsPendingTransferForBooking(1L)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when no pending transfer exists`() {
            // given
            every { jpaRepository.existsPendingTransferForBooking(99L) } returns false

            // when
            val result = adapter.existsPendingTransferForBooking(99L)

            // then
            assertThat(result).isFalse()
        }
    }
}
