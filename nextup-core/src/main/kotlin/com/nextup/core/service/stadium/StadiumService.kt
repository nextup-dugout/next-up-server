package com.nextup.core.service.stadium

import com.nextup.common.exception.BookingNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.SlotNotFoundException
import com.nextup.common.exception.StadiumNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.domain.stadium.SlotStatus
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumBooking
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import com.nextup.core.port.repository.StadiumRepositoryPort
import com.nextup.core.port.repository.StadiumSlotRepositoryPort
import com.nextup.core.service.stadium.dto.BookSlotRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 구장 서비스
 *
 * 일반 사용자용 구장 조회 및 예약 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class StadiumService(
    private val stadiumRepository: StadiumRepositoryPort,
    private val slotRepository: StadiumSlotRepositoryPort,
    private val bookingRepository: StadiumBookingRepositoryPort,
) {
    /**
     * 위치 기반으로 근처 구장을 검색합니다.
     */
    fun searchStadiums(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): List<Stadium> = stadiumRepository.findNearby(latitude, longitude, radiusKm)

    /**
     * 위치 기반으로 근처 구장을 거리 순으로 페이징하여 검색합니다.
     *
     * @param latitude 위도 (-90 ~ 90)
     * @param longitude 경도 (-180 ~ 180)
     * @param radiusKm 검색 반경 킬로미터 (0 초과)
     * @param pageCommand 페이징 정보
     * @return 거리 순으로 정렬된 구장 페이지
     */
    fun findNearbyStadiums(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        pageCommand: PageCommand,
    ): PageResult<Stadium> {
        if (latitude !in -90.0..90.0) {
            throw InvalidInputException(
                "INVALID_LATITUDE",
                "위도는 -90 ~ 90 범위여야 합니다: $latitude",
            )
        }
        if (longitude !in -180.0..180.0) {
            throw InvalidInputException(
                "INVALID_LONGITUDE",
                "경도는 -180 ~ 180 범위여야 합니다: $longitude",
            )
        }
        if (radiusKm <= 0) {
            throw InvalidInputException(
                "INVALID_RADIUS",
                "검색 반경은 0보다 커야 합니다: $radiusKm",
            )
        }
        return stadiumRepository.findNearbyStadiums(latitude, longitude, radiusKm, pageCommand)
    }

    /**
     * ID로 구장을 조회합니다.
     */
    fun getById(id: Long): Stadium =
        stadiumRepository.findByIdOrNull(id)
            ?: throw StadiumNotFoundException(id)

    /**
     * 구장의 특정 날짜에 사용 가능한 슬롯을 조회합니다.
     */
    fun getAvailableSlots(
        stadiumId: Long,
        date: LocalDate,
    ): List<StadiumSlot> {
        // 구장 존재 확인
        getById(stadiumId)

        return slotRepository.findByStadiumIdAndDate(stadiumId, date)
            .filter { it.status == SlotStatus.AVAILABLE }
    }

    /**
     * 구장 슬롯을 예약합니다.
     */
    @Transactional
    fun bookSlot(request: BookSlotRequest): StadiumBooking {
        val slot =
            slotRepository.findByIdOrNull(request.slotId)
                ?: throw SlotNotFoundException(request.slotId)

        // 슬롯 상태 변경 (비즈니스 로직은 Entity에 위임)
        slot.book()

        // 예약 생성
        val booking =
            StadiumBooking.create(
                slot = slot,
                teamId = request.teamId,
                bookedBy = request.bookedBy,
            )

        return bookingRepository.save(booking)
    }

    /**
     * 예약을 취소합니다.
     */
    @Transactional
    fun cancelBooking(bookingId: Long): StadiumBooking {
        val booking =
            bookingRepository.findByIdOrNull(bookingId)
                ?: throw BookingNotFoundException(bookingId)

        // 예약 취소 (비즈니스 로직은 Entity에 위임)
        booking.cancel()

        // 슬롯 상태도 원복
        booking.slot.cancel()

        return booking
    }

    /**
     * 예약을 완료 처리합니다.
     */
    @Transactional
    fun completeBooking(bookingId: Long): StadiumBooking {
        val booking =
            bookingRepository.findByIdOrNull(bookingId)
                ?: throw BookingNotFoundException(bookingId)

        // 예약 완료 처리
        booking.complete()

        return booking
    }

    /**
     * ID로 예약을 조회합니다.
     */
    fun getBookingById(bookingId: Long): StadiumBooking =
        bookingRepository.findByIdOrNull(bookingId)
            ?: throw BookingNotFoundException(bookingId)

    /**
     * 팀의 예약 목록을 조회합니다.
     */
    fun getTeamBookings(
        teamId: Long,
        status: BookingStatus? = null,
    ): List<StadiumBooking> =
        if (status != null) {
            bookingRepository.findByTeamIdAndStatus(teamId, status)
        } else {
            bookingRepository.findByTeamId(teamId)
        }
}
