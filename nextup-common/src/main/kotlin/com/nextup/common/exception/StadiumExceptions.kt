package com.nextup.common.exception

/**
 * 구장을 찾을 수 없을 때 발생하는 예외
 */
class StadiumNotFoundException(
    stadiumId: Long,
) : NotFoundException(
        "STADIUM_NOT_FOUND",
        "Stadium not found: $stadiumId",
    )

/**
 * 구장 슬롯을 찾을 수 없을 때 발생하는 예외
 */
class SlotNotFoundException(
    slotId: Long,
) : NotFoundException(
        "SLOT_NOT_FOUND",
        "Stadium slot not found: $slotId",
    )

/**
 * 구장 예약을 찾을 수 없을 때 발생하는 예외
 */
class BookingNotFoundException(
    bookingId: Long,
) : NotFoundException(
        "BOOKING_NOT_FOUND",
        "Stadium booking not found: $bookingId",
    )

/**
 * 슬롯이 예약 가능하지 않을 때 발생하는 예외
 */
class SlotNotAvailableException(
    code: String,
    message: String,
) : BusinessException(code, message)
