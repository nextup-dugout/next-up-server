package com.nextup.core.service.game

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.AttendanceVote
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.service.game.dto.AttendanceSummaryDto

/**
 * 출석 투표 서비스 인터페이스
 *
 * 경기 출석 투표 관련 비즈니스 로직을 정의합니다.
 */
interface AttendanceService {
    /**
     * 출석 투표를 합니다.
     *
     * @param gameId 경기 ID
     * @param memberId 회원 ID
     * @param status 투표 상태
     * @param reason 사유
     * @return 투표 결과
     * @throws AttendanceVoteNotFoundException 투표를 찾을 수 없는 경우
     * @throws VoteClosedException 투표가 마감된 경우
     */
    fun vote(
        gameId: Long,
        memberId: Long,
        status: AttendanceStatus,
        reason: String?,
    ): AttendanceVote

    /**
     * 경기의 출석 투표 요약을 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 출석 투표 요약
     */
    fun getVoteSummary(gameId: Long): AttendanceSummaryDto

    /**
     * 경기의 미투표자 목록을 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 미투표자(UNDECIDED) 목록
     */
    fun getNonVoters(gameId: Long): List<TeamMember>

    /**
     * 경기에 대한 출석 투표를 자동 생성합니다.
     *
     * @param gameId 경기 ID
     * @return 생성된 투표 목록
     */
    fun createVotesForGame(gameId: Long): List<AttendanceVote>
}
