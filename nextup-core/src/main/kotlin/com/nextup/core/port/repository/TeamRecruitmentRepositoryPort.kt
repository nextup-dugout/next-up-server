package com.nextup.core.port.repository

import com.nextup.core.domain.recruitment.RecruitmentStatus
import com.nextup.core.domain.recruitment.TeamRecruitment

interface TeamRecruitmentRepositoryPort {
    fun save(recruitment: TeamRecruitment): TeamRecruitment

    fun findByIdOrNull(id: Long): TeamRecruitment?

    fun findByTeamId(teamId: Long): List<TeamRecruitment>

    fun findAllOpen(): List<TeamRecruitment>

    fun findByStatus(status: RecruitmentStatus): List<TeamRecruitment>

    fun delete(recruitment: TeamRecruitment)
}
