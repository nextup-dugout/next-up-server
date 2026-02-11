package com.nextup.infrastructure.persistence.match

import com.nextup.core.domain.match.MatchResponse
import com.nextup.core.port.repository.MatchResponseRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class MatchResponseRepositoryAdapter(
    private val jpaRepository: MatchResponseJpaRepository,
) : MatchResponseRepositoryPort {
    override fun save(matchResponse: MatchResponse): MatchResponse = jpaRepository.save(matchResponse)

    override fun findByIdOrNull(id: Long): MatchResponse? = jpaRepository.findByIdOrNull(id)

    override fun findByMatchRequestId(matchRequestId: Long): List<MatchResponse> =
        jpaRepository.findByMatchRequestId(matchRequestId)

    override fun findByRespondTeamId(respondTeamId: Long): List<MatchResponse> =
        jpaRepository.findByRespondTeamId(respondTeamId)
}
