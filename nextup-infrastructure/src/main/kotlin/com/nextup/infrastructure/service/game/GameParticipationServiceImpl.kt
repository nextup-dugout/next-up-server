package com.nextup.infrastructure.service.game

import com.nextup.common.exception.*
import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.GameParticipation
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus
import com.nextup.core.port.repository.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.game.GameParticipationService
import com.nextup.core.service.game.dto.AttendanceSummaryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 출석 투표 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class GameParticipationServiceImpl(
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
) : GameParticipationService {
    @Transactional
    override fun vote(
        gameId: Long,
        memberId: Long,
        status: AttendanceStatus,
        absenceReason: AbsenceReason?,
        reasonDetail: String?,
    ): GameParticipation {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        val member =
            teamMemberRepository.findByIdOrNull(memberId)
                ?: throw TeamMemberNotFoundException(memberId)

        // 투표 권한 검증 (ACTIVE, SUSPENDED 모두 가능)
        if (!member.canVote) {
            throw InvalidStateException(
                "CANNOT_VOTE",
                "Member $memberId cannot vote (status: ${member.status})",
            )
        }

        // 경기 시작 전까지만 투표 가능
        if (game.status != GameStatus.SCHEDULED) {
            throw VoteClosedException(gameId)
        }

        // 기존 투표 조회
        val existingVote = attendanceVoteRepository.findByGameIdAndMemberId(gameId, memberId)

        return if (existingVote != null) {
            // 투표 변경
            existingVote.vote(status, absenceReason, reasonDetail)
            attendanceVoteRepository.save(existingVote)
        } else {
            // 새로 투표 생성 (자동 생성되지 않은 경우 대비)
            val newVote = GameParticipation.createForGame(game, member)
            newVote.vote(status, absenceReason, reasonDetail)
            attendanceVoteRepository.save(newVote)
        }
    }

    override fun getVoteSummary(gameId: Long): AttendanceSummaryDto {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        val votes = attendanceVoteRepository.findByGameId(gameId)

        val totalMembers = votes.size
        val attending = votes.count { it.status == AttendanceStatus.ATTENDING }
        val absent = votes.count { it.status == AttendanceStatus.ABSENT }
        val undecided = votes.count { it.status == AttendanceStatus.UNDECIDED }

        return AttendanceSummaryDto(
            gameId = gameId,
            totalMembers = totalMembers,
            attending = attending,
            absent = absent,
            undecided = undecided,
        )
    }

    override fun getNonVoters(gameId: Long): List<TeamMember> {
        val votes = attendanceVoteRepository.findByGameId(gameId)

        // UNDECIDED 상태인 투표의 멤버 목록 반환
        return votes
            .filter { it.status == AttendanceStatus.UNDECIDED }
            .map { it.member }
    }

    @Transactional
    override fun createVotesForGame(gameId: Long): List<GameParticipation> {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        // GameTeam을 통해 홈팀과 원정팀 조회
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        if (gameTeams.size != 2) {
            throw InvalidStateException(
                "INVALID_GAME_TEAMS",
                "Game $gameId must have exactly 2 teams (home and away)",
            )
        }

        // 각 팀의 ACTIVE 멤버 조회
        val allMembers =
            gameTeams.flatMap { gameTeam ->
                teamMemberRepository.findByTeamIdAndStatus(
                    gameTeam.team.id,
                    TeamMemberStatus.ACTIVE,
                )
            }

        // 중복 생성 방지: 이미 투표가 있는 멤버는 제외
        val existingVotes = attendanceVoteRepository.findByGameId(gameId)
        val existingMemberIds = existingVotes.map { it.member.id }.toSet()

        val membersToCreate = allMembers.filter { it.id !in existingMemberIds }

        // 각 멤버별 투표 생성 (UNDECIDED 상태)
        val newVotes =
            membersToCreate.map { member ->
                GameParticipation.createForGame(game, member)
            }

        return attendanceVoteRepository.saveAll(newVotes)
    }

    override fun getVotesByGameId(gameId: Long): List<GameParticipation> = attendanceVoteRepository.findByGameId(gameId)

    override fun verifyGameTeamMember(
        gameId: Long,
        userId: Long,
    ) {
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val isMember =
            gameTeams.any { gameTeam ->
                val members = teamMemberRepository.findByTeamId(gameTeam.team.id)
                members.any { it.user.id == userId }
            }
        if (!isMember) {
            throw IllegalStateException("You are not a member of either team in this game")
        }
    }

    override fun findMemberInGame(
        gameId: Long,
        userId: Long,
    ): TeamMember {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)

        return gameTeams
            .flatMap { gameTeam ->
                teamMemberRepository.findByTeamId(gameTeam.team.id)
            }
            .firstOrNull { it.user.id == userId }
            ?: throw IllegalStateException("You are not a member of either team in this game")
    }

    override fun getGameScheduledAt(gameId: Long): LocalDateTime {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)
        return game.scheduledAt
    }
}
