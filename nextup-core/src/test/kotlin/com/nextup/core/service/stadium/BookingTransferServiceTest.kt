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
        val idField = BookingTransfer::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(transfer, id)
        return transfer
    }

    @Nested
    @DisplayName("requestTransfer")
    inner class RequestTransfer {
        @Test
        fun `should request transfer successfully`() {
            // given
            val booking = createMockBooking(id = 1L, teamId = 10L)
            val transfer = createMockTransfer()
            val mockMember = mockk<TeamMember>(relaxed = true)

            every { teamMemberRepository.findByTeamIdAndUserId(10L, 100L) } returns mockMember
            every { bookingRepository.findByIdOrNull(1L) } returns booking
            every { bookingTransferRepository.existsPendingTransferForBooking(1L) } returns false
            every { bookingTransferRepository.save(any()) } returns transfer

            // when
            val result =
                service.requestTransfer(
                    bookingId = 1L,
                    fromTeamId = 10L,
                    toTeamId = 20L,
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
                service.requestTransfer(
                    bookingId = 1L,
                    fromTeamId = 10L,
                    toTeamId = 20L,
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
                service.requestTransfer(
                    bookingId = 99L,
                    fromTeamId = 10L,
                    toTeamId = 20L,
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
                service.requestTransfer(
                    bookingId = 1L,
                    fromTeamId = 99L,
                    toTeamId = 20L,
                    message = null,
                    userId = 100L,
                )
            }.isInstanceOf(BookingTransferForbiddenException::class.java)
        }

        @Test
        fun `should throw exception when pending transfer already exists`() {
            // given
            val booking = createMockBooking(teamId = 10L)
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { teamMemberRepository.findByTeamIdAndUserId(10L, 100L) } returns mockMember
            every { bookingRepository.findByIdOrNull(1L) } returns booking
            every { bookingTransferRepository.existsPendingTransferForBooking(1L) } returns true

            // when & then
            assertThatThrownBy {
                service.requestTransfer(
                    bookingId = 1L,
                    fromTeamId = 10L,
                    toTeamId = 20L,
                    message = null,
                    userId = 100L,
                )
            }.isInstanceOf(BookingTransferInvalidStateException::class.java)
                .hasMessageContaining("pending transfer already exists")
        }
    }

    @Nested
    @DisplayName("acceptTransfer")
    inner class AcceptTransfer {
        @Test
        fun `should accept transfer successfully`() {
            // given
            val transfer = createMockTransfer(fromTeamId = 10L, toTeamId = 20L)
            val booking = createMockBooking(teamId = 10L)
            val mockMember = mockk<TeamMember>(relaxed = true)

            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 200L) } returns mockMember
            every { bookingRepository.findByIdOrNull(transfer.bookingId) } returns booking
            every { bookingTransferRepository.save(any()) } returns transfer
            every { bookingRepository.save(any()) } returns booking

            // when
            val result = service.acceptTransfer(transferId = 1L, userId = 200L)

            // then
            assertThat(result.status).isEqualTo(TransferStatus.ACCEPTED)
            verify { bookingTransferRepository.save(any()) }
            verify { bookingRepository.save(any()) }
        }

        @Test
        fun `should throw exception when user is not a toTeam member`() {
            // given
            val transfer = createMockTransfer(toTeamId = 20L)
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 999L) } returns null

            // when & then
            assertThatThrownBy {
                service.acceptTransfer(transferId = 1L, userId = 999L)
            }.isInstanceOf(BookingTransferForbiddenException::class.java)
        }

        @Test
        fun `should throw exception when transfer not found`() {
            // given
            every { bookingTransferRepository.findByIdOrNull(99L) } returns null

            // when & then
            assertThatThrownBy {
                service.acceptTransfer(transferId = 99L, userId = 200L)
            }.isInstanceOf(BookingTransferNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when booking not found during accept`() {
            // given
            val transfer = createMockTransfer(toTeamId = 20L)
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 200L) } returns mockMember
            every { bookingRepository.findByIdOrNull(transfer.bookingId) } returns null

            // when & then
            assertThatThrownBy {
                service.acceptTransfer(transferId = 1L, userId = 200L)
            }.isInstanceOf(BookingNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("rejectTransfer")
    inner class RejectTransfer {
        @Test
        fun `should reject transfer successfully`() {
            // given
            val transfer = createMockTransfer(toTeamId = 20L)
            val mockMember = mockk<TeamMember>(relaxed = true)
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 200L) } returns mockMember
            every { bookingTransferRepository.save(any()) } returns transfer

            // when
            val result = service.rejectTransfer(transferId = 1L, userId = 200L)

            // then
            assertThat(result.status).isEqualTo(TransferStatus.REJECTED)
            verify { bookingTransferRepository.save(any()) }
        }

        @Test
        fun `should throw exception when transfer not found`() {
            // given
            every { bookingTransferRepository.findByIdOrNull(99L) } returns null

            // when & then
            assertThatThrownBy {
                service.rejectTransfer(transferId = 99L, userId = 200L)
            }.isInstanceOf(BookingTransferNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when user is not a toTeam member`() {
            // given
            val transfer = createMockTransfer(toTeamId = 20L)
            every { bookingTransferRepository.findByIdOrNull(1L) } returns transfer
            every { teamMemberRepository.findByTeamIdAndUserId(20L, 999L) } returns null

            // when & then
            assertThatThrownBy {
                service.rejectTransfer(transferId = 1L, userId = 999L)
            }.isInstanceOf(BookingTransferForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("getSentTransfers")
    inner class GetSentTransfers {
        @Test
        fun `should return sent transfers for team`() {
            // given
            val transfer = createMockTransfer(fromTeamId = 10L)
            every { bookingTransferRepository.findByFromTeamId(10L) } returns listOf(transfer)

            // when
            val result = service.getSentTransfers(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].fromTeamId).isEqualTo(10L)
        }

        @Test
        fun `should return empty list when no sent transfers`() {
            // given
            every { bookingTransferRepository.findByFromTeamId(99L) } returns emptyList()

            // when
            val result = service.getSentTransfers(99L)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getReceivedTransfers")
    inner class GetReceivedTransfers {
        @Test
        fun `should return received transfers for team`() {
            // given
            val transfer = createMockTransfer(fromTeamId = 30L, toTeamId = 10L)
            every { bookingTransferRepository.findByToTeamId(10L) } returns listOf(transfer)

            // when
            val result = service.getReceivedTransfers(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].toTeamId).isEqualTo(10L)
        }

        @Test
        fun `should return empty list when no received transfers`() {
            // given
            every { bookingTransferRepository.findByToTeamId(99L) } returns emptyList()

            // when
            val result = service.getReceivedTransfers(99L)

            // then
            assertThat(result).isEmpty()
        }
    }
}
