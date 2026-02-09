package com.nextup.core.service.lineup

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.AttendanceVoteRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
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
    private val attendanceVoteRepository: AttendanceVoteRepositoryPort,
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
                ?: throw IllegalArgumentException("경기 ID \$gameId 를 찾을 수 없습니다.")

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw IllegalArgumentException("팀 ID \$teamId 를 찾을 수 없습니다.")

        val user =
            userRepository.findByIdOrNull(submittedByUserId)
                ?: throw IllegalArgumentException("사용자 ID \$submittedByUserId 를 찾을 수 없습니다.")

        // 이미 존재하는 라인업 확인
        lineupSubmissionRepository.findByGameIdAndTeamId(gameId, teamId)?.let {
            throw IllegalArgumentException("이미 해당 경기/팀의 라인업이 존재합니다. (ID: \${it.id})")
        }

        val submission = LineupSubmission.create(game, team, user)
        return lineupSubmissionRepository.save(submission)
    }

    /**
     * 라인업 제출을 조회합니다.
     */
    fun getLineupSubmission(submissionId: Long): LineupSubmission =
        lineupSubmissionRepository.findByIdOrNull(submissionId)
            ?: throw IllegalArgumentException("라인업 제출 ID \$submissionId 를 찾을 수 없습니다.")

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
                ?: throw IllegalArgumentException("선수 ID \$playerId 를 찾을 수 없습니다.")

        // 중복 선수 검증
        lineupEntryRepository.findBySubmissionIdAndPlayerId(submissionId, playerId)?.let {
            throw IllegalArgumentException("이미 라인업에 등록된 선수입니다. (선수: \${player.name})")
        }

        // 중복 타순 검증 (선발인 경우에만)
        if (isStarter && battingOrder != null) {
            lineupEntryRepository.findBySubmissionIdAndBattingOrder(submissionId, battingOrder)?.let {
                throw IllegalArgumentException("이미 해당 타순에 선수가 등록되어 있습니다. (타순: \$battingOrder)")
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
                    ?: throw IllegalArgumentException("선수 ID \${input.playerId} 를 찾을 수 없습니다.")

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
     * 포수 필수, 중복 선수, DH 규칙, 참석자만 등록 등을 검증합니다.
     */
    @Transactional
    fun submitLineup(submissionId: Long): LineupSubmission {
        val submission = getLineupSubmission(submissionId)

        // 최소 인원 검증 (9명 이상)
        val starters = submission.entries.filter { it.isStarter }
        require(starters.size >= 9) {
            "선발 라인업은 최소 9명이 필요합니다. (현재: \${starters.size}명)"
        }

        // 참석(ATTENDING) 선수 ID 목록 조회
        val attendingPlayerIds = getAttendingPlayerIds(submission.game.id, submission.team.id)

        // 필수 포지션 + 중복 선수 + DH 규칙 + 참석자만 등록 검증은 submit() 내부에서 수행
        submission.submit(attendingPlayerIds)
        return lineupSubmissionRepository.save(submission)
    }

    /**
     * 경기의 특정 팀에서 참석(ATTENDING) 상태인 선수 ID 목록을 조회합니다.
     */
    private fun getAttendingPlayerIds(
        gameId: Long,
        teamId: Long,
    ): Set<Long> {
        val attendingVotes =
            attendanceVoteRepository.findByGameIdAndStatus(
                gameId,
                AttendanceStatus.ATTENDING,
            )

        // 해당 팀 소속 선수만 필터링
        return attendingVotes
            .filter { it.member.team.id == teamId }
            .map { it.member.player.id }
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
                ?: throw IllegalArgumentException("기록원 ID \$scorerUserId 를 찾을 수 없습니다.")

        submission.confirm(scorer)
        return lineupSubmissionRepository.save(submission)
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
                ?: throw IllegalArgumentException("기록원 ID \$scorerUserId 를 찾을 수 없습니다.")

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
