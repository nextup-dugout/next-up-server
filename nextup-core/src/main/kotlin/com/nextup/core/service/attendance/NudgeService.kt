package com.nextup.core.service.attendance

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.notification.NotificationType
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.notification.NotificationService
import com.nextup.core.service.notification.dto.SendNotificationRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 출석 재촉 서비스
 *
 * 미투표자에게 출석 투표를 독려하는 알림을 전송합니다.
 * AttendancePoll 통합 모델을 사용합니다.
 */
@Service
@Transactional(readOnly = true)
class NudgeService(
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val attendancePollRepository: AttendancePollRepositoryPort,
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val notificationService: NotificationService,
) {
    /**
     * 미투표자에게 출석 투표 독려 알림을 전송합니다.
     *
     * @param gameId 경기 ID
     * @param customMessage 사용자 정의 메시지 (선택)
     * @return 알림 전송 결과 (전송 수, 미투표자 이름 목록)
     */
    @Transactional
    fun nudgeNonVoters(
        gameId: Long,
        customMessage: String? = null,
    ): NudgeResult {
        // 1. 경기 조회
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        // 2. 경기의 모든 팀별 투표에서 미투표자 조회
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val nonVoterNames = mutableListOf<String>()
        var notifiedCount = 0

        for (gameTeam in gameTeams) {
            val poll =
                attendancePollRepository.findByGameIdAndTeamId(gameId, gameTeam.team.id)
                    ?: continue

            val votes = attendanceVoteRepository.findByPollId(poll.id)
            val undecidedVotes = votes.filter { it.voteType == VoteType.UNDECIDED }

            for (vote in undecidedVotes) {
                // Player를 통해 TeamMember → User 경로로 사용자 정보 조회
                val members = teamMemberRepository.findByPlayerIdActive(vote.player.id)
                val member =
                    members.firstOrNull { it.team.id == gameTeam.team.id }
                        ?: continue

                val userName = member.user.nickname
                nonVoterNames.add(userName)

                val title = "출석 투표 요청"
                val body =
                    customMessage
                        ?: "경기(${game.scheduledAt})에 대한 출석 투표를 진행해주세요."

                val request =
                    SendNotificationRequest(
                        userId = member.user.id,
                        type = NotificationType.ATTENDANCE_NUDGE,
                        title = title,
                        body = body,
                        data = "gameId=$gameId",
                    )

                notificationService.sendNotification(request)
                notifiedCount++
            }
        }

        return NudgeResult(
            notifiedCount = notifiedCount,
            nonVoterNames = nonVoterNames,
        )
    }
}

/**
 * 출석 재촉 결과
 */
data class NudgeResult(
    val notifiedCount: Int,
    val nonVoterNames: List<String>,
)
