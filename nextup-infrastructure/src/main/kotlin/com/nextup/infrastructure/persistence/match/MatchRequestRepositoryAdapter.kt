package com.nextup.infrastructure.persistence.match

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.domain.match.SkillLevel
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MatchRequestRepositoryAdapter(
    private val jpaRepository: MatchRequestJpaRepository,
) : MatchRequestRepositoryPort {
    override fun save(matchRequest: MatchRequest): MatchRequest = jpaRepository.save(matchRequest)

    override fun findByIdOrNull(id: Long): MatchRequest? = jpaRepository.findByIdOrNull(id)

    override fun findByTeamId(teamId: Long): List<MatchRequest> = jpaRepository.findByTeamId(teamId)

    override fun findAllOpen(): List<MatchRequest> = jpaRepository.findAllOpen()

    override fun findByStatus(status: MatchRequestStatus): List<MatchRequest> = jpaRepository.findByStatus(status)

    override fun findAllOpenWithFilter(
        area: String?,
        date: LocalDate?,
        skillLevel: SkillLevel?,
    ): List<MatchRequest> = jpaRepository.findAllOpenWithFilter(area, date, skillLevel)
}
