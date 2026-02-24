package com.nextup.core.service.game

import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.GameParticipation
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.service.game.dto.AttendanceSummaryDto
import java.time.LocalDateTime

/**
 * 출석 투표 서비스 인터페이스
 *
 * 경기 출석 투표 관련 비즈니스 로직을 정의합니다.
 */
interface GameParticipationService {
    /**
     * 출석 투표를 합니다.
     *
     * @param gameId 경기 ID
     * @param memberId 회원 ID
     * @param status 투표 상태
     * @param absenceReason 불참 사유 (불참/미정 시 선택 가능)
     * @param reasonDetail 상세 사유 (OTHER 선택 시 입력 가능)
     * @return 투표 결과
     * @throws AttendanceVoteNotFoundException 투표를 찾을 수 없는 경우
     * @throws VoteClosedException 투표가 마감된 경우
     */
    fun vote(
        gameId: Long,
        memberId: Long,
        status: AttendanceStatus,
        absenceReason: AbsenceReason? = null,
        reasonDetail: String? = null,
    ): GameParticipation

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
    fun createVotesForGame(gameId: Long): List<GameParticipation>

    /**
     * 경기의 출석 투표 목록을 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 출석 투표 목록
     */
    fun getVotesByGameId(gameId: Long): List<GameParticipation>

    /**
     * 경기 참가 팀의 멤버인지 검증합니다.
     *
     * @param gameId 경기 ID
     * @param userId 사용자 ID
     * @throws IllegalStateException if user is not a member of either team
     */
    fun verifyGameTeamMember(
        gameId: Long,
        userId: Long,
    )

    /**
     * 경기에 참가하는 팀에서 사용자의 멤버를 찾습니다.
     *
     * @param gameId 경기 ID
     * @param userId 사용자 ID
     * @return 팀 멤버
     * @throws IllegalStateException if game or member not found
     */
    fun findMemberInGame(
        gameId: Long,
        userId: Long,
    ): TeamMember

    /**
     * 경기의 예정 시간을 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 경기 예정 시간
     * @throws GameNotFoundException if game not found
     */
    fun getGameScheduledAt(gameId: Long): LocalDateTime
}
