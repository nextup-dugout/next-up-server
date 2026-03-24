package com.nextup.core.service.lineup

import com.nextup.common.exception.LineupExchangeNotAuthorizedException
import com.nextup.common.exception.LineupNotExchangedException
import com.nextup.core.domain.attendance.VoteType
import com.nextup.core.domain.event.LineupConfirmedEvent
import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.port.attendance.AttendancePollRepositoryPort
import com.nextup.core.port.attendance.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.MercenaryParticipationRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 라인업 서비스
 *
 * 라인업 작성, 제출, 확인, 반려 등의 워크플로우를 관리합니다.
 */
@Service
@Transactional(readOnly = true)
class LineupService(
    private val lineupSubmissionRepository: LineupSubmissionRepositoryPort,
    private val lineupEntryRepository: LineupEntryRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val attendancePollRepository: AttendancePollRepositoryPort,
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort,
    private val mercenaryParticipationRepository: MercenaryParticipationRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    // ========== 라인업 제출 관리 ==========

    /**
     * 라인업 제출을 생성합니다.
     *
     * @throws IllegalArgumentException 이미 해당 경기/팀의 라인업이 존재할 때
     */
    @Transactional
    fun createLineupSubmission(
        gameId: Long,
        teamId: Long,
        submittedByUserId: Long,
    ): LineupSubmission {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw IllegalArgumentException("경기 ID $gameId 를 찾을 수 없습니다.")

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw IllegalArgumentException("팀 ID $teamId 를 찾을 수 없습니다.")

        val user =
            userRepository.findByIdOrNull(submittedByUserId)
                ?: throw IllegalArgumentException("사용자 ID $submittedByUserId 를 찾을 수 없습니다.")

        // 이미 존재하는 라인업 확인
        lineupSubmissionRepository.findByGameIdAndTeamId(gameId, teamId)?.let {
            throw IllegalArgumentException("이미 해당 경기/팀의 라인업이 존재합니다. (ID: ${it.id})")
        }

        val submission = LineupSubmission.create(game, team, user)
        return lineupSubmissionRepository.save(submission)
    }

    /**
     * 라인업 제출을 조회합니다.
     */
    fun getLineupSubmission(submissionId: Long): LineupSubmission =
        lineupSubmissionRepository.findByIdOrNull(submissionId)
            ?: throw IllegalArgumentException("라인업 제출 ID $submissionId 를 찾을 수 없습니다.")

    /**
     * 경기의 모든 라인업 제출을 조회합니다.
     */
    fun getLineupSubmissionsByGame(gameId: Long): List<LineupSubmission> =
        lineupSubmissionRepository.findAllByGameId(gameId)

    /**
     * 팀의 모든 라인업 제출을 조회합니다.
     */
    fun getLineupSubmissionsByTeam(teamId: Long): List<LineupSubmission> =
        lineupSubmissionRepository.findAllByTeamId(teamId)

    /**
     * 경기/팀으로 라인업 제출을 조회합니다.
     */
    fun getLineupSubmissionByGameAndTeam(
        gameId: Long,
        teamId: Long,
    ): LineupSubmission? = lineupSubmissionRepository.findByGameIdAndTeamId(gameId, teamId)

    /**
     * 경기의 제출된 라인업을 조회합니다. (기록원용)
     */
    fun getSubmittedLineupsByGame(gameId: Long): List<LineupSubmission> =
        lineupSubmissionRepository.findAllByGameIdAndStatus(gameId, LineupSubmissionStatus.SUBMITTED)

    // ========== 라인업 엔트리 관리 ==========

    /**
     * 라인업 엔트리를 추가합니다.
     *
     * @throws IllegalArgumentException 중복 선수 또는 중복 타순일 때
     */
    @Transactional
    fun addLineupEntry(
        submissionId: Long,
        playerId: Long,
        position: Position,
        battingOrder: Int?,
        backNumber: Int?,
        isStarter: Boolean = true,
    ): LineupEntry {
        val submission = getLineupSubmission(submissionId)

        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw IllegalArgumentException("선수 ID $playerId 를 찾을 수 없습니다.")

        // 중복 선수 검증
        lineupEntryRepository.findBySubmissionIdAndPlayerId(submissionId, playerId)?.let {
            throw IllegalArgumentException("이미 라인업에 등록된 선수입니다. (선수: ${player.name})")
        }

        // 중복 타순 검증 (선발인 경우에만)
        if (isStarter && battingOrder != null) {
            lineupEntryRepository.findBySubmissionIdAndBattingOrder(submissionId, battingOrder)?.let {
                throw IllegalArgumentException("이미 해당 타순에 선수가 등록되어 있습니다. (타순: $battingOrder)")
            }
        }

        val entry =
            LineupEntry(
                submission = submission,
                player = player,
                position = position,
                battingOrder = battingOrder,
                backNumber = backNumber,
                isStarter = isStarter,
            )

        submission.addEntry(entry)
        return lineupEntryRepository.save(entry)
    }

    /**
     * 라인업 엔트리 목록을 일괄 추가합니다.
     */
    @Transactional
    fun setLineupEntries(
        submissionId: Long,
        entries: List<LineupEntryInput>,
    ): List<LineupEntry> {
        val submission = getLineupSubmission(submissionId)

        // 기존 엔트리 삭제
        submission.clearEntries()
        lineupEntryRepository.deleteAllBySubmissionId(submissionId)

        // 검증
        validateLineupEntries(entries)

        // 새 엔트리 추가
        return entries.map { input ->
            val player =
                playerRepository.findByIdOrNull(input.playerId)
                    ?: throw IllegalArgumentException("선수 ID ${input.playerId} 를 찾을 수 없습니다.")

            val entry =
                LineupEntry(
                    submission = submission,
                    player = player,
                    position = input.position,
                    battingOrder = input.battingOrder,
                    backNumber = input.backNumber,
                    isStarter = input.isStarter,
                )

            submission.addEntry(entry)
            lineupEntryRepository.save(entry)
        }
    }

    /**
     * 라인업 엔트리를 조회합니다.
     */
    fun getLineupEntries(submissionId: Long): List<LineupEntry> =
        lineupEntryRepository.findAllBySubmissionId(submissionId)

    // ========== 워크플로우 ==========

    /**
     * 라인업을 기록원에게 제출합니다.
     *
     * LineupSubmission.submit() 내부에서 LineupValidator를 통해
     * 포수 필수, 중복 선수, DH 규칙, 참석자만 등록, 용병 쿼터 등을 검증합니다.
     */
    @Transactional
    fun submitLineup(submissionId: Long): LineupSubmission {
        val submission = getLineupSubmission(submissionId)

        // 최소 인원 검증 (9명 이상)
        val starters = submission.entries.filter { it.isStarter }
        require(starters.size >= 9) {
            "선발 라인업은 최소 9명이 필요합니다. (현재: ${starters.size}명)"
        }

        // 참석(ATTENDING) 선수 ID 목록 조회
        val attendingPlayerIds = getAttendingPlayerIds(submission.game.id, submission.team.id)

        // L-3: 용병 쿼터 검증을 위한 데이터 조회
        val mercenaryPlayerIds =
            getMercenaryPlayerIds(submission.game.id, submission.team.id)
        val maxMercenaryCount =
            submission.game.competition.gameRules.maxMercenaryCount

        // 필수 포지션 + 중복 선수 + DH 규칙 + 참석자만 등록 + 용병 쿼터 검증은 submit() 내부에서 수행
        submission.submit(
            attendingPlayerIds = attendingPlayerIds,
            mercenaryPlayerIds = mercenaryPlayerIds,
            maxMercenaryCount = maxMercenaryCount,
        )
        lineupSubmissionRepository.save(submission)

        // 양 팀 모두 제출 완료 시 교환 대기(EXCHANGE_PENDING) 상태로 전환
        tryMarkExchangePending(gameId = submission.game.id)

        return submission
    }

    /**
     * 양 팀이 모두 라인업을 제출한 경우 EXCHANGE_PENDING 상태로 전환합니다.
     *
     * 양 팀 모두 SUBMITTED 상태일 때 양쪽 모두 EXCHANGE_PENDING으로 변경합니다.
     * 이후 각 팀 감독이 상대팀 라인업을 승인해야 EXCHANGED 상태가 됩니다.
     */
    private fun tryMarkExchangePending(gameId: Long) {
        val allSubmissions = lineupSubmissionRepository.findAllByGameId(gameId)
        val submittedSubmissions =
            allSubmissions.filter { it.status == LineupSubmissionStatus.SUBMITTED }

        if (submittedSubmissions.size >= 2) {
            submittedSubmissions.forEach { sub ->
                sub.markExchangePending()
                lineupSubmissionRepository.save(sub)
            }
        }
    }

    /**
     * 감독이 상대팀 라인업 교환을 승인합니다.
     *
     * 승인하는 감독의 팀이 아닌 상대팀 라인업을 승인합니다.
     * 양 팀 모두 승인하면 두 라인업 모두 EXCHANGED 상태로 전환됩니다.
     *
     * @param gameId 경기 ID
     * @param approvingTeamId 승인하는 감독의 팀 ID
     * @return 승인된 상대팀 라인업 제출
     * @throws LineupExchangeNotAuthorizedException 해당 팀의 라인업이 교환 대기 상태가 아닐 때
     */
    @Transactional
    fun approveLineupExchange(
        gameId: Long,
        approvingTeamId: Long,
    ): LineupSubmission {
        val allSubmissions = lineupSubmissionRepository.findAllByGameId(gameId)

        val mySubmission =
            allSubmissions.find { it.team.id == approvingTeamId }
                ?: throw IllegalArgumentException("경기 ID $gameId 에서 팀 ID $approvingTeamId 의 라인업을 찾을 수 없습니다.")

        val opponentSubmission =
            allSubmissions.find { it.team.id != approvingTeamId }
                ?: throw IllegalArgumentException("경기 ID $gameId 에서 상대팀 라인업을 찾을 수 없습니다.")

        // 상대팀 라인업이 교환 대기 상태인지 확인
        if (!opponentSubmission.status.canApproveExchange()) {
            throw LineupExchangeNotAuthorizedException(opponentSubmission.id)
        }

        // 상대팀 라인업 승인
        opponentSubmission.approveExchange()
        lineupSubmissionRepository.save(opponentSubmission)

        // 내 라인업도 이미 EXCHANGED이거나 상대방도 승인 완료이면 내 라인업도 EXCHANGED로 전환
        if (mySubmission.status.canApproveExchange()) {
            mySubmission.approveExchange()
            lineupSubmissionRepository.save(mySubmission)
        }

        return opponentSubmission
    }

    /**
     * 감독이 상대팀 라인업 교환을 거부합니다.
     *
     * 거부당한 상대팀은 라인업을 수정하여 재제출해야 합니다.
     * 거부 시 상대팀 라인업은 EXCHANGE_REJECTED 상태로, 내 라인업도 SUBMITTED 상태로 복원됩니다.
     *
     * @param gameId 경기 ID
     * @param rejectingTeamId 거부하는 감독의 팀 ID
     * @param rejectingUserId 거부하는 감독의 사용자 ID
     * @param reason 거부 사유
     * @return 거부된 상대팀 라인업 제출
     * @throws LineupExchangeNotAuthorizedException 해당 팀의 라인업이 교환 대기 상태가 아닐 때
     */
    @Transactional
    fun rejectLineupExchange(
        gameId: Long,
        rejectingTeamId: Long,
        rejectingUserId: Long,
        reason: String,
    ): LineupSubmission {
        val allSubmissions = lineupSubmissionRepository.findAllByGameId(gameId)

        val mySubmission =
            allSubmissions.find { it.team.id == rejectingTeamId }
                ?: throw IllegalArgumentException("경기 ID $gameId 에서 팀 ID $rejectingTeamId 의 라인업을 찾을 수 없습니다.")

        val opponentSubmission =
            allSubmissions.find { it.team.id != rejectingTeamId }
                ?: throw IllegalArgumentException("경기 ID $gameId 에서 상대팀 라인업을 찾을 수 없습니다.")

        // 상대팀 라인업이 교환 대기 상태인지 확인
        if (!opponentSubmission.status.canRejectExchange()) {
            throw LineupExchangeNotAuthorizedException(opponentSubmission.id)
        }

        val rejectingManager =
            userRepository.findByIdOrNull(rejectingUserId)
                ?: throw IllegalArgumentException("사용자 ID $rejectingUserId 를 찾을 수 없습니다.")

        // 상대팀 라인업 교환 거부
        opponentSubmission.rejectExchange(rejectingManager, reason)
        lineupSubmissionRepository.save(opponentSubmission)

        // 내 라인업도 EXCHANGE_PENDING이었으면 SUBMITTED로 복원
        if (mySubmission.status == LineupSubmissionStatus.EXCHANGE_PENDING) {
            mySubmission.revertToSubmitted()
            lineupSubmissionRepository.save(mySubmission)
        }

        return opponentSubmission
    }

    /**
     * 상대팀 라인업을 조회합니다.
     *
     * 양 팀의 라인업이 모두 EXCHANGED 상태인 경우에만 상대팀 라인업을 반환합니다.
     *
     * @param gameId 경기 ID
     * @param myTeamId 요청 팀 ID
     * @return 상대팀의 라인업 제출 (EXCHANGED 상태일 때만)
     * @throws LineupNotExchangedException 아직 교환이 완료되지 않았을 때
     * @throws IllegalArgumentException 라인업 데이터가 없을 때
     */
    fun getOpponentLineup(
        gameId: Long,
        myTeamId: Long,
    ): LineupSubmission {
        val allSubmissions = lineupSubmissionRepository.findAllByGameId(gameId)

        val mySubmission =
            allSubmissions.find { it.team.id == myTeamId }
                ?: throw IllegalArgumentException("경기 ID $gameId 에서 팀 ID $myTeamId 의 라인업을 찾을 수 없습니다.")

        val opponentSubmission =
            allSubmissions.find { it.team.id != myTeamId }
                ?: throw IllegalArgumentException("경기 ID $gameId 에서 상대팀 라인업을 찾을 수 없습니다.")

        if (!mySubmission.status.isVisibleToOpponent() || !opponentSubmission.status.isVisibleToOpponent()) {
            throw LineupNotExchangedException(gameId)
        }

        return opponentSubmission
    }

    /**
     * 경기에서 해당 팀의 용병 선수 ID 목록을 조회합니다.
     *
     * L-3: MercenaryParticipation에서 해당 경기/팀의 용병 참가 기록을 조회하여
     * 용병 선수 ID 목록을 반환합니다.
     */
    private fun getMercenaryPlayerIds(
        gameId: Long,
        teamId: Long,
    ): Set<Long> =
        mercenaryParticipationRepository.findByGameId(gameId)
            .filter { it.teamId == teamId }
            .map { it.playerId }
            .toSet()

    /**
     * 경기의 특정 팀에서 참석(ATTEND) 상태인 선수 ID 목록을 조회합니다.
     * AttendancePoll 통합 모델을 사용합니다.
     */
    private fun getAttendingPlayerIds(
        gameId: Long,
        teamId: Long,
    ): Set<Long> {
        val poll =
            attendancePollRepository.findByGameIdAndTeamId(gameId, teamId)
                ?: return emptySet()

        val votes = attendanceVoteRepository.findByPollId(poll.id)

        return votes
            .filter { it.voteType == VoteType.ATTEND }
            .map { it.player.id }
            .toSet()
    }

    /**
     * 기록원이 라인업을 확인합니다.
     */
    @Transactional
    fun confirmLineup(
        submissionId: Long,
        scorerUserId: Long,
    ): LineupSubmission {
        val submission = getLineupSubmission(submissionId)

        val scorer =
            userRepository.findByIdOrNull(scorerUserId)
                ?: throw IllegalArgumentException("기록원 ID $scorerUserId 를 찾을 수 없습니다.")

        submission.confirm(scorer)
        val savedSubmission = lineupSubmissionRepository.save(submission)

        eventPublisher.publishEvent(
            LineupConfirmedEvent(
                gameId = submission.game.id,
                teamId = submission.team.id,
            ),
        )

        return savedSubmission
    }

    /**
     * 기록원이 라인업을 반려합니다.
     */
    @Transactional
    fun rejectLineup(
        submissionId: Long,
        scorerUserId: Long,
        reason: String,
    ): LineupSubmission {
        val submission = getLineupSubmission(submissionId)

        val scorer =
            userRepository.findByIdOrNull(scorerUserId)
                ?: throw IllegalArgumentException("기록원 ID $scorerUserId 를 찾을 수 없습니다.")

        submission.reject(scorer, reason)
        return lineupSubmissionRepository.save(submission)
    }

    // ========== 검증 헬퍼 ==========

    /**
     * 라인업 엔트리 입력을 검증합니다.
     */
    private fun validateLineupEntries(entries: List<LineupEntryInput>) {
        // 중복 선수 검증
        val playerIds = entries.map { it.playerId }
        val uniquePlayerIds = playerIds.toSet()
        require(playerIds.size == uniquePlayerIds.size) {
            "라인업에 중복된 선수가 있습니다."
        }

        // 중복 타순 검증 (선발만)
        val starterBattingOrders =
            entries
                .filter { it.isStarter && it.battingOrder != null }
                .map { it.battingOrder }
        val uniqueBattingOrders = starterBattingOrders.toSet()
        require(starterBattingOrders.size == uniqueBattingOrders.size) {
            "라인업에 중복된 타순이 있습니다."
        }
    }
}

/**
 * 라인업 엔트리 입력 데이터 클래스
 */
data class LineupEntryInput(
    val playerId: Long,
    val position: Position,
    val battingOrder: Int?,
    val backNumber: Int?,
    val isStarter: Boolean = true,
)
