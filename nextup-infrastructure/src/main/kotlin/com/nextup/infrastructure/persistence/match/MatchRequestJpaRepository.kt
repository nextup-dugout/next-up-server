package com.nextup.infrastructure.persistence.match

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.domain.match.SkillLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface MatchRequestJpaRepository : JpaRepository<MatchRequest, Long> {
    @Query("SELECT mr FROM MatchRequest mr WHERE mr.team.id = :teamId")
    fun findByTeamId(teamId: Long): List<MatchRequest>

    @Query("SELECT mr FROM MatchRequest mr WHERE mr.status = 'OPEN'")
    fun findAllOpen(): List<MatchRequest>

    fun findByStatus(status: MatchRequestStatus): List<MatchRequest>

    @Query(
        "SELECT mr FROM MatchRequest mr WHERE mr.status = 'OPEN' " +
            "AND (:area IS NULL OR mr.preferredLocation LIKE CONCAT('%', :area, '%')) " +
            "AND (:date IS NULL OR mr.preferredDate = :date) " +
            "AND (:skillLevel IS NULL OR mr.skillLevel = :skillLevel)",
    )
    fun findAllOpenWithFilter(
        @Param("area") area: String?,
        @Param("date") date: LocalDate?,
        @Param("skillLevel") skillLevel: SkillLevel?,
    ): List<MatchRequest>
}
