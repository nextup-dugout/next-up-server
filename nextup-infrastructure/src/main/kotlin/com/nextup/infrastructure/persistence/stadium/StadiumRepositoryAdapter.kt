package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.port.repository.StadiumRepositoryPort
import com.nextup.infrastructure.common.toPageResult
import com.nextup.infrastructure.common.toPageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class StadiumRepositoryAdapter(
    private val jpaRepository: StadiumJpaRepository,
) : StadiumRepositoryPort {
    override fun save(stadium: Stadium): Stadium = jpaRepository.save(stadium)

    override fun findByIdOrNull(id: Long): Stadium? = jpaRepository.findByIdOrNull(id)

    override fun findAll(): List<Stadium> = jpaRepository.findAll()

    override fun findAllActive(): List<Stadium> = jpaRepository.findByIsActiveTrue()

    override fun findNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): List<Stadium> {
        val radiusMeters = radiusKm * 1000.0
        return jpaRepository.findNearby(latitude, longitude, radiusMeters)
    }

    override fun findNearbyStadiums(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        pageCommand: PageCommand,
    ): PageResult<Stadium> {
        val radiusMeters = radiusKm * 1000.0
        return jpaRepository.findNearbyOrderByDistance(
            latitude,
            longitude,
            radiusMeters,
            pageCommand.toPageable()
        ).toPageResult()
    }
}
