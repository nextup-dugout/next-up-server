package com.nextup.core.service.attendance

import com.nextup.core.domain.attendance.ActivityScore
import java.math.BigDecimal

/**
 * 활동 점수 관리 서비스 인터페이스
 *
 * 팀원의 활동 점수를 관리하는 비즈니스 로직을 정의합니다.
 */
interface ActivityService {
    /**
     * 팀원의 활동 점수를 조회합니다.
     * 존재하지 않으면 새로 생성합니다.
     *
     * @param teamId 팀 ID
     * @param memberId 팀원 ID
     * @return ActivityScore
     */
    fun getActivityScore(
        teamId: Long,
        memberId: Long,
    ): ActivityScore

    /**
     * 팀의 모든 활동 점수를 조회합니다. (탈퇴/강퇴 멤버 포함)
     *
     * @param teamId 팀 ID
     * @return 활동 점수 목록
     */
    fun listActivityScores(teamId: Long): List<ActivityScore>

    /**
     * 팀의 활성(ACTIVE) 멤버 활동 점수만 조회합니다.
     * LEFT/KICKED 멤버의 점수는 이력으로 보존되지만 활성 통계에서 제외됩니다.
     *
     * @param teamId 팀 ID
     * @return 활성 멤버의 활동 점수 목록
     */
    fun listActiveActivityScores(teamId: Long): List<ActivityScore>

    /**
     * 경기 참여율을 업데이트합니다.
     *
     * @param teamId 팀 ID
     * @param memberId 팀원 ID
     * @param rate 참여율 (0~100)
     * @return 업데이트된 ActivityScore
     */
    fun updateGameParticipationRate(
        teamId: Long,
        memberId: Long,
        rate: BigDecimal,
    ): ActivityScore

    /**
     * 연습 참석률을 업데이트합니다.
     *
     * @param teamId 팀 ID
     * @param memberId 팀원 ID
     * @param rate 참석률 (0~100)
     * @return 업데이트된 ActivityScore
     */
    fun updatePracticeAttendanceRate(
        teamId: Long,
        memberId: Long,
        rate: BigDecimal,
    ): ActivityScore

    /**
     * 기여도 점수를 업데이트합니다.
     *
     * @param teamId 팀 ID
     * @param memberId 팀원 ID
     * @param score 기여도 점수 (0~100)
     * @return 업데이트된 ActivityScore
     */
    fun updateContributionScore(
        teamId: Long,
        memberId: Long,
        score: BigDecimal,
    ): ActivityScore
}
