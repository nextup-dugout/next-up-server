package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.TeamMemberNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.attendance.ActivityScore
import com.nextup.core.port.attendance.ActivityScoreRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.attendance.ActivityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 활동 점수 관리 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class ActivityServiceImpl(
    private val activityScoreRepository: ActivityScoreRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) : ActivityService {
    @Transactional
    override fun getActivityScore(
        teamId: Long,
        memberId: Long,
    ): ActivityScore {
        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)

        val member =
            teamMemberRepository.findByIdOrNull(memberId)
                ?: throw TeamMemberNotFoundException(memberId)

        // Get or create activity score
        val existingScore = activityScoreRepository.findByTeamIdAndMemberId(teamId, memberId)
        if (existingScore != null) {
            return existingScore
        }

        val newScore =
            ActivityScore.create(
                team = team,
                member = member,
            )

        return activityScoreRepository.save(newScore)
    }

    override fun listActivityScores(teamId: Long): List<ActivityScore> {
        if (!teamRepository.existsById(teamId)) {
            throw TeamNotFoundException(teamId)
        }

        return activityScoreRepository.findByTeamId(teamId)
    }

    @Transactional
    override fun updateGameParticipationRate(
        teamId: Long,
        memberId: Long,
        rate: BigDecimal,
    ): ActivityScore {
        val activityScore = getActivityScore(teamId, memberId)
        activityScore.updateGameParticipationRate(rate)
        return activityScoreRepository.save(activityScore)
    }

    @Transactional
    override fun updatePracticeAttendanceRate(
        teamId: Long,
        memberId: Long,
        rate: BigDecimal,
    ): ActivityScore {
        val activityScore = getActivityScore(teamId, memberId)
        activityScore.updatePracticeAttendanceRate(rate)
        return activityScoreRepository.save(activityScore)
    }

    @Transactional
    override fun updateContributionScore(
        teamId: Long,
        memberId: Long,
        score: BigDecimal,
    ): ActivityScore {
        val activityScore = getActivityScore(teamId, memberId)
        activityScore.updateContributionScore(score)
        return activityScoreRepository.save(activityScore)
    }
}
