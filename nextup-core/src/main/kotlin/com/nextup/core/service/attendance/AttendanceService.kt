package com.nextup.core.service.attendance

import com.nextup.core.domain.attendance.AttendancePoll
import com.nextup.core.domain.attendance.AttendanceVote
import com.nextup.core.domain.attendance.PollStatus
import com.nextup.core.domain.attendance.VoteType

/**
 * 출석 관리 서비스 인터페이스
 *
 * 출석 투표와 투표 응답을 관리하는 비즈니스 로직을 정의합니다.
 */
interface AttendanceService {
    /**
     * 출석 투표를 생성합니다.
     *
     * @param teamId 팀 ID
     * @param title 투표 제목
     * @param eventDate 이벤트 날짜 (ISO-8601)
     * @param deadline 투표 마감 시간 (ISO-8601)
     * @return 생성된 AttendancePoll
     */
    fun createPoll(
        teamId: Long,
        title: String,
        eventDate: String,
        deadline: String,
    ): AttendancePoll

    /**
     * 출석 투표에 응답합니다.
     *
     * @param pollId 투표 ID
     * @param playerId 선수 ID
     * @param voteType 투표 유형
     * @return 생성된 AttendanceVote
     */
    fun submitVote(
        pollId: Long,
        playerId: Long,
        voteType: VoteType,
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
}
