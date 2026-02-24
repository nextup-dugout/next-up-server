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
import java.math.BigDecimal
import java.time.Instant

/**
 * 구장 예약 양도 엔티티
 *
 * 팀이 보유한 구장 예약을 다른 팀에게 양도하는 기능을 관리합니다.
 * 긴급 양도(판매자가 양도 등록) / 양수(구매자가 수락)를 처리합니다.
 */
@Entity
@Table(
    name = "booking_transfers",
    indexes = [
        Index(name = "idx_booking_transfers_booking", columnList = "booking_id"),
        Index(name = "idx_booking_transfers_seller", columnList = "seller_team_id"),
        Index(name = "idx_booking_transfers_status", columnList = "status"),
    ],
)
class BookingTransfer private constructor(
    @Column(nullable = false, name = "booking_id")
    val bookingId: Long,
    @Column(nullable = false, name = "seller_team_id")
    val sellerTeamId: Long,
    @Column(nullable = true, precision = 10, scale = 2, name = "transfer_price")
    val transferPrice: BigDecimal?,
    @Column(nullable = true, length = 500)
    val message: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TransferStatus = TransferStatus.OPEN,
    @Column(nullable = true, name = "buyer_team_id")
    var buyerTeamId: Long? = null,
    @Column(nullable = false, name = "expires_at")
    val expiresAt: Instant,
    @Column(nullable = true, name = "accepted_at")
    var acceptedAt: Instant? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        fun create(
            bookingId: Long,
            sellerTeamId: Long,
            transferPrice: BigDecimal?,
            message: String?,
            expiresAt: Instant,
        ): BookingTransfer {
            require(bookingId > 0) { "Booking ID must be positive" }
            require(sellerTeamId > 0) { "Seller team ID must be positive" }
            transferPrice?.let {
                require(it >= BigDecimal.ZERO) { "Transfer price must be non-negative" }
            }
            require(expiresAt.isAfter(Instant.now())) { "Expiry time must be in the future" }

            return BookingTransfer(
                bookingId = bookingId,
                sellerTeamId = sellerTeamId,
                transferPrice = transferPrice,
                message = message,
                expiresAt = expiresAt,
            )
        }
    }

    /**
     * 양도를 만료 여부를 확인합니다.
     */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    /**
     * 양도를 수락합니다.
     *
     * 상태가 OPEN이고 만료되지 않은 경우에만 수락 가능합니다.
     */
    fun accept(buyerTeamId: Long) {
        if (status != TransferStatus.OPEN) {
            throw BookingTransferInvalidStateException(
                "Transfer cannot be accepted. Current status: $status",
            )
        }
        if (isExpired()) {
            this.status = TransferStatus.EXPIRED
            throw BookingTransferInvalidStateException(
                "Transfer has expired and cannot be accepted",
            )
        }
        require(buyerTeamId > 0) { "Buyer team ID must be positive" }
        require(buyerTeamId != sellerTeamId) { "Buyer team cannot be the same as seller team" }

        this.buyerTeamId = buyerTeamId
        this.acceptedAt = Instant.now()
        this.status = TransferStatus.ACCEPTED
    }

    /**
     * 양도를 취소합니다.
     *
     * 상태가 OPEN인 경우에만 취소 가능합니다.
     */
    fun cancel() {
        if (status != TransferStatus.OPEN) {
            throw BookingTransferInvalidStateException(
                "Transfer cannot be cancelled. Current status: $status",
            )
        }
        this.status = TransferStatus.CANCELLED
    }

    /**
     * 만료 처리합니다.
     */
    fun expire() {
        if (status != TransferStatus.OPEN) {
            throw BookingTransferInvalidStateException(
                "Transfer cannot be expired. Current status: $status",
            )
        }
        this.status = TransferStatus.EXPIRED
    }
}
