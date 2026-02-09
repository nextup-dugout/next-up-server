package com.nextup.core.service.stadium

import com.nextup.common.exception.StadiumNotFoundException
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumSlot
import com.nextup.core.port.repository.StadiumRepositoryPort
import com.nextup.core.port.repository.StadiumSlotRepositoryPort
import com.nextup.core.service.stadium.dto.CreateSlotRequest
import com.nextup.core.service.stadium.dto.CreateStadiumRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 구장 관리 서비스
 *
 * 관리자용 구장 생성, 수정, 슬롯 생성 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class StadiumAdminService(
    private val stadiumRepository: StadiumRepositoryPort,
    private val slotRepository: StadiumSlotRepositoryPort,
) {
    /**
     * 구장을 생성합니다.
     */
    @Transactional
    fun createStadium(request: CreateStadiumRequest): Stadium {
        val stadium =
            Stadium.create(
                name = request.name,
                address = request.address,
                latitude = request.latitude,
                longitude = request.longitude,
                capacity = request.capacity,
                facilities = request.facilities,
                contactInfo = request.contactInfo,
                imageUrls = request.imageUrls,
            )

        return stadiumRepository.save(stadium)
    }

    /**
     * 구장 정보를 수정합니다.
     */
    @Transactional
    fun updateStadium(
        id: Long,
        address: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        capacity: Int? = null,
        facilities: String? = null,
        contactInfo: String? = null,
        imageUrls: String? = null,
    ): Stadium {
        val stadium =
            stadiumRepository.findByIdOrNull(id)
                ?: throw StadiumNotFoundException(id)

        stadium.update(
            address = address,
            latitude = latitude,
            longitude = longitude,
            capacity = capacity,
            facilities = facilities,
            contactInfo = contactInfo,
            imageUrls = imageUrls,
        )

        return stadium
    }

    /**
     * 구장 슬롯을 생성합니다.
     */
    @Transactional
    fun createSlots(requests: List<CreateSlotRequest>): List<StadiumSlot> {
        val slots =
            requests.map { request ->
                val stadium =
                    stadiumRepository.findByIdOrNull(request.stadiumId)
                        ?: throw StadiumNotFoundException(request.stadiumId)

                StadiumSlot.create(
                    stadium = stadium,
                    date = request.date,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    price = request.price,
                )
            }

        return slots.map { slotRepository.save(it) }
    }

    /**
     * 구장을 비활성화합니다.
     */
    @Transactional
    fun deactivateStadium(id: Long): Stadium {
        val stadium =
            stadiumRepository.findByIdOrNull(id)
                ?: throw StadiumNotFoundException(id)

        stadium.deactivate()
        return stadium
    }

    /**
     * 구장을 활성화합니다.
     */
    @Transactional
    fun activateStadium(id: Long): Stadium {
        val stadium =
            stadiumRepository.findByIdOrNull(id)
                ?: throw StadiumNotFoundException(id)

        stadium.activate()
        return stadium
    }
}
