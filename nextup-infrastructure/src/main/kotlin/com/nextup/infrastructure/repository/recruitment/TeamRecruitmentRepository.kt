package com.nextup.infrastructure.repository.recruitment

import com.nextup.core.domain.recruitment.RecruitmentStatus
import com.nextup.core.domain.recruitment.TeamRecruitment
import com.nextup.core.port.repository.TeamRecruitmentRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TeamRecruitmentRepository :
    JpaRepository<TeamRecruitment, Long>,
    TeamRecruitmentRepositoryPort {
    override fun findByIdOrNull(id: Long): TeamRecruitment? = findById(id).orElse(null)

    override fun findByTeamId(teamId: Long): List<TeamRecruitment>

    @Query("SELECT r FROM TeamRecruitment r WHERE r.status = 'OPEN' ORDER BY r.deadline ASC")
    override fun findAllOpen(): List<TeamRecruitment>

    override fun findByStatus(status: RecruitmentStatus): List<TeamRecruitment>
}
