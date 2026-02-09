package com.nextup.infrastructure.persistence.attendance

import com.nextup.core.domain.attendance.ActivityScore
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.port.attendance.ActivityScoreRepositoryPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ActivityScoreRepositoryAdapter(
    private val jpaRepository: ActivityScoreJpaRepository,
) : ActivityScoreRepositoryPort {
    override fun save(activityScore: ActivityScore): ActivityScore = jpaRepository.save(activityScore)

    override fun findById(id: Long): ActivityScore? = jpaRepository.findByIdOrNull(id)

    override fun findByTeamAndMember(
        team: Team,
        member: TeamMember,
    ): ActivityScore? = jpaRepository.findByTeamIdAndMemberId(team.id, member.id)

    override fun findByTeamIdAndMemberId(
        teamId: Long,
        memberId: Long,
    ): ActivityScore? = jpaRepository.findByTeamIdAndMemberId(teamId, memberId)

    override fun findByTeam(team: Team): List<ActivityScore> = jpaRepository.findByTeamId(team.id)

    override fun findByTeamId(teamId: Long): List<ActivityScore> = jpaRepository.findByTeamId(teamId)

    override fun delete(activityScore: ActivityScore) {
        jpaRepository.delete(activityScore)
    }

    override fun existsByTeamAndMember(
        team: Team,
        member: TeamMember,
    ): Boolean = jpaRepository.existsByTeamIdAndMemberId(team.id, member.id)
}
