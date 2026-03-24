package com.nextup.common.exception

/**
 * 구장 예약 양도를 찾을 수 없을 때 발생하는 예외
 */
class BookingTransferNotFoundException(
    transferId: Long,
) : NotFoundException(
        "BOOKING_TRANSFER_NOT_FOUND",
        "Booking transfer not found: $transferId",
    )

/**
 * 양도 권한이 없을 때 발생하는 예외
 */
class BookingTransferForbiddenException(
    message: String,
) : ForbiddenException(
        "BOOKING_TRANSFER_FORBIDDEN",
        message,
    )

/**
 * 양도 상태가 유효하지 않을 때 발생하는 예외
 */
class BookingTransferInvalidStateException(
    message: String,
) : InvalidStateException(
        "BOOKING_TRANSFER_INVALID_STATE",
        message,
    )
