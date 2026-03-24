package com.nextup.core.domain.stadium

import com.nextup.common.exception.BookingTransferInvalidStateException
import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version

/**
 * 구장 예약 양도 엔티티
 *
 * 팀이 보유한 구장 예약을 다른 팀에게 단순 양도하는 기능을 관리합니다.
 * 가격 없이, 양도 요청(PENDING) -> 수락(ACCEPTED) / 거절(REJECTED) 상태만 존재합니다.
 */
@Entity
@Table(
    name = "booking_transfers",
    indexes = [
        Index(name = "idx_booking_transfers_booking", columnList = "booking_id"),
        Index(name = "idx_booking_transfers_from_team", columnList = "from_team_id"),
        Index(name = "idx_booking_transfers_to_team", columnList = "to_team_id"),
        Index(name = "idx_booking_transfers_status", columnList = "status"),
    ],
)
class BookingTransfer private constructor(
    @Column(nullable = false, name = "booking_id")
    val bookingId: Long,
    @Column(nullable = false, name = "from_team_id")
    val fromTeamId: Long,
    @Column(nullable = false, name = "to_team_id")
    val toTeamId: Long,
    @Column(nullable = true, length = 500)
    val message: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TransferStatus = TransferStatus.PENDING,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Version
    var version: Long = 0
        protected set

    companion object {
        fun create(
            bookingId: Long,
            fromTeamId: Long,
            toTeamId: Long,
            message: String?,
        ): BookingTransfer {
            require(bookingId > 0) { "Booking ID must be positive" }
            require(fromTeamId > 0) { "From team ID must be positive" }
            require(toTeamId > 0) { "To team ID must be positive" }
            require(fromTeamId != toTeamId) { "Cannot transfer to the same team" }

            return BookingTransfer(
                bookingId = bookingId,
                fromTeamId = fromTeamId,
                toTeamId = toTeamId,
                message = message,
            )
        }
    }

    /**
     * 양도 요청을 수락합니다.
     *
     * 상태가 PENDING인 경우에만 수락 가능합니다.
     */
    fun accept() {
        if (status != TransferStatus.PENDING) {
            throw BookingTransferInvalidStateException(
                "Transfer cannot be accepted. Current status: $status",
            )
        }
        this.status = TransferStatus.ACCEPTED
    }

    /**
     * 양도 요청을 거절합니다.
     *
     * 상태가 PENDING인 경우에만 거절 가능합니다.
     */
    fun reject() {
        if (status != TransferStatus.PENDING) {
            throw BookingTransferInvalidStateException(
                "Transfer cannot be rejected. Current status: $status",
            )
        }
        this.status = TransferStatus.REJECTED
    }
}
