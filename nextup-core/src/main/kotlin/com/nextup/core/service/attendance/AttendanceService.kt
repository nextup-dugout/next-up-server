package com.nextup.core.service.attendance

import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.EventCategory
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType

/**
 * 출석 관리 서비스 인터페이스
 *
 * 출석 투표와 투표 응답을 관리하는 비즈니스 로직을 정의합니다.
 * 경기(GAME) 카테고리를 포함한 모든 이벤트 유형의 출석 투표를 통합 관리합니다.
 */
interface AttendanceService {
    /**
     * 출석 투표를 생성합니다.
     *
     * @param teamId 팀 ID
     * @param title 투표 제목
     * @param eventDate 이벤트 날짜 (ISO-8601)
     * @param deadline 투표 마감 시간 (ISO-8601)
     * @param category 이벤트 카테고리
     * @param gameId 경기 ID (GAME 카테고리일 때 필수)
     * @return 생성된 AttendancePoll
     */
    fun createPoll(
        teamId: Long,
        title: String,
        eventDate: String,
        deadline: String,
        category: EventCategory = EventCategory.OTHER,
        gameId: Long? = null,
    ): AttendancePoll

    /**
     * 출석 투표에 응답합니다.
     *
     * @param pollId 투표 ID
     * @param playerId 선수 ID
     * @param voteType 투표 유형
     * @param absenceReason 불참 사유 (불참/미정 시 선택 가능)
     * @param reasonDetail 상세 사유 (OTHER 선택 시 입력 가능)
     * @return 생성된 AttendanceVote
     */
    fun submitVote(
        pollId: Long,
        playerId: Long,
        voteType: VoteType,
        absenceReason: AbsenceReason? = null,
        reasonDetail: String? = null,
    ): AttendanceVote

    /**
     * 출석 투표를 조회합니다.
     *
     * @param pollId 투표 ID
     * @return AttendancePoll
     */
    fun getPoll(pollId: Long): AttendancePoll

    /**
     * 팀의 출석 투표 목록을 조회합니다.
     *
     * @param teamId 팀 ID
     * @param status 투표 상태 (선택)
     * @return 출석 투표 목록
     */
    fun listPolls(
        teamId: Long,
        status: PollStatus? = null,
    ): List<AttendancePoll>

    /**
     * 출석 투표를 마감합니다.
     *
     * @param pollId 투표 ID
     * @return 마감된 AttendancePoll
     */
    fun closePoll(pollId: Long): AttendancePoll

    /**
     * 출석 투표의 응답 목록을 조회합니다.
     *
     * @param pollId 투표 ID
     * @return 투표 응답 목록
     */
    fun listVotes(pollId: Long): List<AttendanceVote>

    // === 경기 출석 투표 통합 메서드 ===

    /**
     * 경기에 대한 출석 투표를 자동 생성합니다.
     * 양 팀(홈/원정) 각각에 대해 AttendancePoll을 생성합니다.
     *
     * @param gameId 경기 ID
     * @return 생성된 투표 목록
     */
    fun createPollsForGame(gameId: Long): List<AttendancePoll>

    /**
     * 경기의 출석 투표를 조회합니다.
     * (특정 팀의 투표를 조회)
     *
     * @param gameId 경기 ID
     * @param teamId 팀 ID
     * @return AttendancePoll (없으면 null)
     */
    fun findGamePoll(
        gameId: Long,
        teamId: Long,
    ): AttendancePoll?

    /**
     * 마감 기한이 지난 OPEN 투표를 모두 마감합니다.
     * (스케줄러용)
     *
     * @return 마감된 투표 수
     */
    fun closeExpiredPolls(): Int

    /**
     * 경기 출석 투표에 응답합니다.
     * 사용자의 팀에 해당하는 경기 투표를 찾아 투표합니다.
     *
     * @param gameId 경기 ID
     * @param userId 사용자 ID
     * @param voteType 투표 유형
     * @param absenceReason 불참 사유 (불참/미정 시 선택 가능)
     * @param reasonDetail 상세 사유 (OTHER 선택 시 입력 가능)
     * @return 투표 결과
     */
    fun voteForGame(
        gameId: Long,
        userId: Long,
        voteType: VoteType,
        absenceReason: AbsenceReason? = null,
        reasonDetail: String? = null,
    ): AttendanceVote

    /**
     * 경기의 출석 투표 응답 목록을 조회합니다.
     * 사용자의 팀에 해당하는 경기 투표의 응답을 반환합니다.
     *
     * @param gameId 경기 ID
     * @param userId 사용자 ID
     * @return 투표 응답 목록
     */
    fun getGameVotes(
        gameId: Long,
        userId: Long,
    ): List<AttendanceVote>

    /**
     * 경기의 출석 투표 요약을 조회합니다.
     *
     * @param gameId 경기 ID
     * @param userId 사용자 ID
     * @return 투표 요약 (참석/불참/미정 카운트)
     */
    fun getGameVoteSummary(
        gameId: Long,
        userId: Long,
    ): GameVoteSummary

    /**
     * 경기의 미투표자(UNDECIDED) 목록을 조회합니다.
     *
     * @param gameId 경기 ID
     * @param userId 사용자 ID
     * @return 미투표 응답 목록
     */
    fun getGameNonVoters(
        gameId: Long,
        userId: Long,
    ): List<AttendanceVote>
}

/**
 * 경기 출석 투표 요약
 */
data class GameVoteSummary(
    val pollId: Long,
    val gameId: Long,
    val totalVotes: Int,
    val attending: Int,
    val absent: Int,
    val undecided: Int,
) {
    val responseRate: Double
        get() =
            if (totalVotes == 0) {
                0.0
            } else {
                (attending + absent).toDouble() / totalVotes
            }
}
