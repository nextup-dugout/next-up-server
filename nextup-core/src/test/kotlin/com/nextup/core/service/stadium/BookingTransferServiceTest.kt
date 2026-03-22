package com.nextup.core.service.stadium

import com.nextup.common.exception.BookingNotFoundException
import com.nextup.common.exception.BookingTransferForbiddenException
import com.nextup.common.exception.BookingTransferInvalidStateException
import com.nextup.common.exception.BookingTransferNotFoundException
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.domain.stadium.TransferStatus
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.port.repository.BookingTransferRepositoryPort
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

@DisplayName("BookingTransferService")
class BookingTransferServiceTest {
    private lateinit var bookingTransferRepository: BookingTransferRepositoryPort
    private lateinit var bookingRepository: StadiumBookingRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var service: BookingTransferService

    @BeforeEach
    fun setUp() {
        bookingTransferRepository = mockk()
        bookingRepository = mockk()
        teamMemberRepository = mockk()
        service = BookingTransferService(bookingTransferRepository, bookingRepository, teamMemberRepository)
    }

    private fun createMockBooking(
        id: Long = 1L,
        teamId: Long = 10L,
        status: BookingStatus = BookingStatus.CONFIRMED,
    ): StadiumBooking {
        val slot = mockk<StadiumSlot>(relaxed = true)
        return StadiumBooking.create(slot = slot, teamId = teamId, bookedBy = 100L)
    }

    private fun createMockTransfer(
        id: Long = 1L,
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
        val idField = BookingTransfer::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(transfer, id)
        return transfer
    }

    @Nested
    @DisplayName("createTransfer")
    inner class CreateTransfer {
        @Test
        fun `should create transfer successfully`() {
            // given
            val booking = createMockBooking(id = 1L, teamId = 10L)
            val transfer = createMockTransfer()
            val mockMember = mockk<TeamMember>(relaxed = true)

            every { teamMemberRepository.findByTeamIdAndUserId(10L, 100L) } returns mockMember
            every { bookingRepository.findByIdOrNull(1L) } returns booking
            every { bookingTransferRepository.existsOpenTransferForBooking(1L) } returns false
            every { bookingTransferRepository.save(any()) } returns transfer

            // when
            val result =
                service.createTransfer(
                    bookingId = 1L,
                    teamId = 10L,
                    price = BigDecimal("50000"),
                    message = "양도합니다",
                    userId = 100L,
                )

            // then
            assertThat(result).isNotNull()
            verify { bookingTransferRepository.save(any()) }
        }

        @Test
        fun `should throw exception when user is not a team member`() {
            // given
            every { teamMemberRepository.findByTeamIdAndUserId(10L, 999L) } returns null

            // when & then
            assertThatThrownBy {
                service.createTransfer(
                    bookingId = 1L,
                    teamId = 10L,
                    price = null,
                    message = null,
                    userId = 999L,
                )
            }.isInstanceOf(BookingTransferForbiddenException::class.java)
        }

        @Test
        fun `should throw exception when booking not found`() {
            // given
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { teamMemberRepository.findByTeamIdAndUserId(10L, 100L) } returns mockMember
            every { bookingRepository.findByIdOrNull(99L) } returns null

            // when & then
            assertThatThrownBy {
                service.createTransfer(
                    bookingId = 99L,
                    teamId = 10L,
                    price = null,
                    message = null,
                    userId = 100L,
                )
            }.isInstanceOf(BookingNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when team does not own the booking`() {
            // given
            val booking = createMockBooking(teamId = 10L)
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { teamMemberRepository.findByTeamIdAndUserId(99L, 100L) } returns mockMember
            every { bookingRepository.findByIdOrNull(1L) } returns booking

            // when & then
            assertThatThrownBy {
                service.createTransfer(
                    bookingId = 1L,
                    teamId = 99L,
                    price = null,
                    message = null,
                    userId = 100L,
                )
            }.isInstanceOf(BookingTransferForbiddenException::class.java)
        }

        @Test
        fun `should throw exception when open transfer already exists`() {
            // given
            val booking = createMockBooking(teamId = 10L)
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { teamMemberRepository.findByTeamIdAndUserId(10L, 100L) } returns mockMember
            every { bookingRepository.findByIdOrNull(1L) } returns booking
            every { bookingTransferRepository.existsOpenTransferForBooking(1L) } returns true

            // when & then
            assertThatThrownBy {
                service.createTransfer(
                    bookingId = 1L,
                    teamId = 10L,
                    price = null,
                    message = null,
                    userId = 100L,
                )
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("open transfer already exists")
        }
    }

    @Nested
    @DisplayName("acceptTransfer")
    inner class AcceptTransfer {
        @Test
        fun `should accept transfer successfully`() {
            // given
            val transfer = createMockTransfer(sellerTeamId = 10L)
            val booking = createMockBooking(teamId = 10L)
            val mockMember = mockk<TeamMember>(relaxed = true)

            every { teamMemberRepository.findByTeamIdAndUserId(20L, 200L) } returns mockMember
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { bookingRepository.findByIdOrNull(transfer.bookingId) } returns booking
            every { bookingTransferRepository.save(any()) } returns transfer
            every { bookingRepository.save(any()) } returns booking

            // when
            val result = service.acceptTransfer(transferId = 1L, buyerTeamId = 20L, userId = 200L)

            // then
            assertThat(result.status).isEqualTo(TransferStatus.ACCEPTED)
            assertThat(result.buyerTeamId).isEqualTo(20L)
            verify { bookingTransferRepository.save(any()) }
            verify { bookingRepository.save(any()) }
        }

        @Test
        fun `should throw exception when user is not a buyer team member`() {
            // given
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 999L) } returns null

            // when & then
            assertThatThrownBy {
                service.acceptTransfer(transferId = 1L, buyerTeamId = 20L, userId = 999L)
            }.isInstanceOf(BookingTransferForbiddenException::class.java)
        }

        @Test
        fun `should throw exception when transfer not found`() {
            // given
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 200L) } returns mockMember
            every { bookingTransferRepository.findByIdOrNull(99L) } returns null

            // when & then
            assertThatThrownBy {
                service.acceptTransfer(transferId = 99L, buyerTeamId = 20L, userId = 200L)
            }.isInstanceOf(BookingTransferNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when booking not found during accept`() {
            // given
            val transfer = createMockTransfer()
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 200L) } returns mockMember
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { bookingRepository.findByIdOrNull(transfer.bookingId) } returns null

            // when & then
            assertThatThrownBy {
                service.acceptTransfer(transferId = 1L, buyerTeamId = 20L, userId = 200L)
            }.isInstanceOf(BookingNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("cancelTransfer")
    inner class CancelTransfer {
        @Test
        fun `should cancel transfer successfully`() {
            // given
            val transfer = createMockTransfer(sellerTeamId = 10L)
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { bookingTransferRepository.save(any()) } returns transfer

            // when
            val result = service.cancelTransfer(transferId = 1L, teamId = 10L)

            // then
            assertThat(result.status).isEqualTo(TransferStatus.CANCELLED)
            verify { bookingTransferRepository.save(any()) }
        }

        @Test
        fun `should throw exception when transfer not found`() {
            // given
            every { bookingTransferRepository.findByIdOrNull(99L) } returns null

            // when & then
            assertThatThrownBy {
                service.cancelTransfer(transferId = 99L, teamId = 10L)
            }.isInstanceOf(BookingTransferNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when team is not the seller`() {
            // given
            val transfer = createMockTransfer(sellerTeamId = 10L)
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer

            // when & then
            assertThatThrownBy {
                service.cancelTransfer(transferId = 1L, teamId = 99L)
            }.isInstanceOf(BookingTransferForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("getAvailableTransfers")
    inner class GetAvailableTransfers {
        @Test
        fun `should return only non-expired open transfers`() {
            // given
            val validTransfer = createMockTransfer()
            val expiredTransfer =
                BookingTransfer.create(
                    bookingId = 2L,
                    sellerTeamId = 20L,
                    transferPrice = null,
                    message = null,
                    expiresAt = Instant.now().plusSeconds(1),
                )
            Thread.sleep(1100)

            every { bookingTransferRepository.findByStatus(TransferStatus.OPEN) } returns
                listOf(validTransfer, expiredTransfer)

            // when
            val result = service.getAvailableTransfers()

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(validTransfer)
        }

        @Test
        fun `should return empty list when no open transfers exist`() {
            // given
            every { bookingTransferRepository.findByStatus(TransferStatus.OPEN) } returns emptyList()

            // when
            val result = service.getAvailableTransfers()

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getTransfersByTeamId")
    inner class GetTransfersByTeamId {
        @Test
        fun `should return transfers where team is seller`() {
            // given
            val transfer = createMockTransfer(sellerTeamId = 10L)
            every { bookingTransferRepository.findBySellerTeamId(10L) } returns listOf(transfer)
            every { bookingTransferRepository.findByBuyerTeamId(10L) } returns emptyList()

            // when
            val result = service.getTransfersByTeamId(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].sellerTeamId).isEqualTo(10L)
        }

        @Test
        fun `should return transfers where team is buyer`() {
            // given
            val transfer =
                createMockTransfer(id = 3L, sellerTeamId = 20L).also {
                    it.accept(10L)
                }
            every { bookingTransferRepository.findBySellerTeamId(10L) } returns emptyList()
            every { bookingTransferRepository.findByBuyerTeamId(10L) } returns listOf(transfer)

            // when
            val result = service.getTransfersByTeamId(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].buyerTeamId).isEqualTo(10L)
        }

        @Test
        fun `should deduplicate transfers and sort by createdAt descending`() {
            // given
            val transfer1 = createMockTransfer(id = 1L, sellerTeamId = 10L, bookingId = 1L)
            val transfer2 = createMockTransfer(id = 2L, sellerTeamId = 10L, bookingId = 2L)
            every { bookingTransferRepository.findBySellerTeamId(10L) } returns
                listOf(transfer1, transfer2)
            every { bookingTransferRepository.findByBuyerTeamId(10L) } returns emptyList()

            // when
            val result = service.getTransfersByTeamId(10L)

            // then
            assertThat(result).hasSize(2)
        }

        @Test
        fun `should return empty list when team has no transfers`() {
            // given
            every { bookingTransferRepository.findBySellerTeamId(99L) } returns emptyList()
            every { bookingTransferRepository.findByBuyerTeamId(99L) } returns emptyList()

            // when
            val result = service.getTransfersByTeamId(99L)

            // then
            assertThat(result).isEmpty()
        }
    }
}
