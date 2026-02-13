package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.Stadium
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface StadiumJpaRepository : JpaRepository<Stadium, Long> {
    fun findByIsActiveTrue(): List<Stadium>

    @Query(
        """
        SELECT * FROM stadiums s
        WHERE ST_DWithin(
            ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
            :radiusMeters
        ) = true
        AND s.is_active = true
        """,
        nativeQuery = true,
    )
    fun findNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double,
    ): List<Stadium>
}
