package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.port.repository.StadiumRepositoryPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
        pageable: Pageable,
    ): Page<Stadium> {
        val radiusMeters = radiusKm * 1000.0
        return jpaRepository.findNearbyOrderByDistance(latitude, longitude, radiusMeters, pageable)
    }
}
