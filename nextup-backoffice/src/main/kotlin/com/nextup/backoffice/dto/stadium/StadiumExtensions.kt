package com.nextup.backoffice.dto.stadium

import com.nextup.core.domain.stadium.BookingTransfer
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumSlot

/**
 * Stadium Entity를 StadiumResponse DTO로 변환하는 Extension Function
 */
fun Stadium.toResponse(): StadiumResponse =
    StadiumResponse(
        id = this.id,
        name = this.name,
        address = this.address,
        latitude = this.latitude,
        longitude = this.longitude,
        capacity = this.capacity,
        facilities = this.facilities,
        contactInfo = this.contactInfo,
        imageUrls = this.imageUrls,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

/**
 * StadiumSlot Entity를 StadiumSlotResponse DTO로 변환하는 Extension Function
 */
fun StadiumSlot.toResponse(): StadiumSlotResponse =
    StadiumSlotResponse(
        id = this.id,
        stadiumId = this.stadium.id,
        stadiumName = this.stadium.name,
        date = this.date,
        startTime = this.startTime,
        endTime = this.endTime,
        price = this.price,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

/**
 * BookingTransfer Entity를 BookingTransferResponse DTO로 변환하는 Extension Function
 */
fun BookingTransfer.toResponse(): BookingTransferResponse =
    BookingTransferResponse(
        id = this.id,
        bookingId = this.bookingId,
        sellerTeamId = this.sellerTeamId,
        transferPrice = this.transferPrice,
        message = this.message,
        status = this.status,
        buyerTeamId = this.buyerTeamId,
        expiresAt = this.expiresAt,
        acceptedAt = this.acceptedAt,
        createdAt = this.createdAt,
    )

/**
 * List<StadiumSlot>를 List<StadiumSlotResponse>로 변환
 */
fun List<StadiumSlot>.toSlotResponse(): List<StadiumSlotResponse> = this.map { it.toResponse() }

/**
 * List<BookingTransfer>를 List<BookingTransferResponse>로 변환
 */
fun List<BookingTransfer>.toTransferResponse(): List<BookingTransferResponse> = this.map { it.toResponse() }
