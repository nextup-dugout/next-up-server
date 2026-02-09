package com.nextup.core.port.attendance

import com.nextup.core.domain.attendance.ActivityScore
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember

/**
 * 활동 점수 Repository Port
 *
 * Hexagonal Architecture의 Port 역할을 수행합니다.
 * Infrastructure 계층에서 이 인터페이스를 구현합니다.
 */
interface ActivityScoreRepositoryPort {
    /**
     * 활동 점수를 저장합니다.
     */
    fun save(activityScore: ActivityScore): ActivityScore

    /**
     * ID로 활동 점수를 조회합니다.
     */
    fun findById(id: Long): ActivityScore?

    /**
     * 팀과 멤버로 활동 점수를 조회합니다.
     */
    fun findByTeamAndMember(
        team: Team,
        member: TeamMember,
    ): ActivityScore?

    /**
     * 팀 ID와 멤버 ID로 활동 점수를 조회합니다.
     */
    fun findByTeamIdAndMemberId(
        teamId: Long,
        memberId: Long,
    ): ActivityScore?

    /**
     * 팀의 모든 활동 점수를 조회합니다.
     */
    fun findByTeam(team: Team): List<ActivityScore>

    /**
     * 팀 ID로 모든 활동 점수를 조회합니다.
     */
    fun findByTeamId(teamId: Long): List<ActivityScore>

    /**
     * 활동 점수를 삭제합니다.
     */
    fun delete(activityScore: ActivityScore)

    /**
     * 팀과 멤버 조합이 존재하는지 확인합니다.
     */
    fun existsByTeamAndMember(
        team: Team,
        member: TeamMember,
    ): Boolean
}
