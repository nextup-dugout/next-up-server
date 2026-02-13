package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.competition.Competition
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 경기 엔티티
 *
 * 대회에 속한 개별 경기를 나타냅니다.
 * 경기에 참여하는 팀(GameTeam)과 선수(GamePlayer)는
 * 각각 ManyToOne으로 Game을 참조합니다.
 */
@Entity
@Table(
    name = "games",
    indexes = [
        Index(name = "idx_games_competition", columnList = "competition_id"),
        Index(name = "idx_games_scheduled_at", columnList = "scheduled_at"),
        Index(name = "idx_games_status", columnList = "status"),
        Index(name = "idx_games_competition_date", columnList = "competition_id, scheduled_at"),
    ],
)
class Game(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    val competition: Competition,
    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: LocalDateTime,
    @Column(length = 100)
    var location: String? = null,
    @Column(name = "field_name", length = 100)
    var fieldName: String? = null,
    @Column(name = "game_number")
    var gameNumber: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: GameStatus = GameStatus.SCHEDULED,
    @Column(name = "current_inning")
    var currentInning: Int = 0,
    @Column(name = "is_top_inning")
    var isTopInning: Boolean = true,
    @Column(name = "total_innings")
    var totalInnings: Int = 9,
    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,
    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,
    @Column(length = 500)
    var note: String? = null,
    @Column(name = "forfeit_reason", length = 500)
    var forfeitReason: String? = null,
    @Embedded
    var gameState: GameState = GameState(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 경기를 시작합니다.
     */
    fun start() {
        require(status.canStart()) { "예정 상태의 경기만 시작할 수 있습니다. 현재 상태: ${status.displayName}" }
        status = GameStatus.IN_PROGRESS
        currentInning = 1
        isTopInning = true
        startedAt = LocalDateTime.now()
        gameState.resetForNewInning()
    }

    /**
     * 다음 이닝으로 진행합니다.
     */
    fun nextHalfInning() {
        require(status.isOngoing()) { "진행 중인 경기만 이닝을 진행할 수 있습니다." }

        if (isTopInning) {
            isTopInning = false
        } else {
            currentInning++
            isTopInning = true
        }
        gameState.resetForNewInning()
    }

    /**
     * 경기를 정상 종료합니다.
     */
    fun finish() {
        require(status.isOngoing()) { "진행 중인 경기만 종료할 수 있습니다." }
        status = GameStatus.FINISHED
        endedAt = LocalDateTime.now()
    }

    /**
     * 콜드게임으로 종료합니다.
     */
    fun callGame(reason: String? = null) {
        require(status.isOngoing()) { "진행 중인 경기만 콜드게임 처리할 수 있습니다." }
        status = GameStatus.CALLED
        endedAt = LocalDateTime.now()
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "콜드게임 사유: $reason"
        }
    }

    /**
     * 경기를 취소합니다.
     */
    fun cancel(reason: String? = null) {
        require(status == GameStatus.SCHEDULED || status == GameStatus.POSTPONED) {
            "예정 또는 연기 상태의 경기만 취소할 수 있습니다."
        }
        status = GameStatus.CANCELLED
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "취소 사유: $reason"
        }
    }

    /**
     * 경기를 연기합니다.
     */
    fun postpone(
        newScheduledAt: LocalDateTime,
        reason: String? = null,
    ) {
        require(status == GameStatus.SCHEDULED) { "예정 상태의 경기만 연기할 수 있습니다." }
        status = GameStatus.POSTPONED
        scheduledAt = newScheduledAt
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "연기 사유: $reason"
        }
    }

    /**
     * 몰수패 처리합니다.
     *
     * 승리팀에 7점, 패배팀에 0점을 자동 반영하고,
     * 각 GameTeam의 결과(WIN/LOSS)를 설정합니다.
     *
     * @param winnerTeamId 몰수승 팀 ID
     * @param reason 몰수 사유
     * @param gameTeams 해당 경기에 참여하는 GameTeam 목록 (정확히 2개)
     */
    fun forfeit(
        winnerTeamId: Long,
        reason: String,
        gameTeams: List<GameTeam>,
    ) {
        require(status == GameStatus.SCHEDULED || status.isOngoing()) {
            "예정 또는 진행 중인 경기만 몰수 처리할 수 있습니다."
        }
        require(gameTeams.size == 2) {
            "몰수 처리를 위해서는 정확히 2개의 GameTeam이 필요합니다."
        }
        require(gameTeams.any { it.team.id == winnerTeamId }) {
            "승리팀 ID($winnerTeamId)가 해당 경기에 참여하는 팀이 아닙니다."
        }

        status = GameStatus.FORFEITED
        endedAt = LocalDateTime.now()
        forfeitReason = reason
        note = (note?.let { "$it\n" } ?: "") + "몰수 사유: $reason"

        // 승리팀 7:0 점수 반영
        gameTeams.forEach { gameTeam ->
            if (gameTeam.team.id == winnerTeamId) {
                gameTeam.updateScore(totalScore = FORFEIT_WIN_SCORE, totalHits = 0, totalErrors = 0)
                gameTeam.updateResult(GameResult.WIN)
            } else {
                gameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
                gameTeam.updateResult(GameResult.LOSS)
            }
        }
    }

    companion object {
        /** 몰수승 점수 */
        const val FORFEIT_WIN_SCORE = 7
    }

    /**
     * 경기 일정을 변경합니다.
     */
    fun reschedule(newScheduledAt: LocalDateTime) {
        require(status == GameStatus.SCHEDULED || status == GameStatus.POSTPONED) {
            "예정 또는 연기 상태의 경기만 일정을 변경할 수 있습니다."
        }
        if (status == GameStatus.POSTPONED) {
            status = GameStatus.SCHEDULED
        }
        scheduledAt = newScheduledAt
    }

    /**
     * Undo가 가능한 상태인지 확인합니다.
     */
    fun canUndo(): Boolean = status == GameStatus.IN_PROGRESS

    /**
     * 이전 이닝 상태로 복원합니다 (Undo 시 이닝/아웃 카운트 롤백용).
     */
    fun restoreInningState(
        inning: Int,
        isTop: Boolean,
        outs: Int,
    ) {
        require(status.isOngoing()) { "진행 중인 경기만 상태를 복원할 수 있습니다." }
        this.currentInning = inning
        this.isTopInning = isTop
        gameState.restoreOuts(outs)
    }

    /**
     * 타순을 되돌립니다 (Undo 시 타순 롤백용).
     */
    fun revertBatter() {
        require(status.isOngoing()) { "진행 중인 경기만 타순을 되돌릴 수 있습니다." }
        gameState.revertBatter(isHomeTeam = !isTopInning)
    }

    /**
     * 현재 이닝 표시를 반환합니다 (예: "5회초", "7회말").
     */
    val currentInningDisplay: String
        get() = if (currentInning == 0) "경기 전" else "${currentInning}회${if (isTopInning) "초" else "말"}"

    /**
     * 경기가 연장전인지 확인합니다.
     */
    val isExtraInning: Boolean
        get() = currentInning > totalInnings

    /**
     * 아웃을 기록합니다.
     * @return 3아웃으로 이닝이 종료되었는지 여부
     */
    fun recordOut(): Boolean {
        require(status.isOngoing()) { "진행 중인 경기만 아웃을 기록할 수 있습니다." }
        return gameState.recordOut()
    }

    /**
     * 다음 타자로 타순을 진행합니다.
     */
    fun advanceBatter() {
        require(status.isOngoing()) { "진행 중인 경기만 타순을 진행할 수 있습니다." }
        gameState.advanceBatter(isHomeTeam = !isTopInning)
    }

    /**
     * 주자를 설정합니다.
     */
    fun setRunner(
        base: Base,
        playerId: Long?,
    ) {
        require(status.isOngoing()) { "진행 중인 경기만 주자를 설정할 수 있습니다." }
        gameState.setRunner(base, playerId)
    }

    /**
     * 모든 베이스를 클리어합니다.
     */
    fun clearBases() {
        require(status.isOngoing()) { "진행 중인 경기만 베이스를 클리어할 수 있습니다." }
        gameState.clearBases()
    }

    /**
     * 볼카운트를 리셋합니다.
     */
    fun resetCount() {
        require(status.isOngoing()) { "진행 중인 경기만 볼카운트를 리셋할 수 있습니다." }
        gameState.resetCount()
    }

    /**
     * 볼을 추가합니다.
     * @return 4볼로 볼넷 여부
     */
    fun addBall(): Boolean {
        require(status.isOngoing()) { "진행 중인 경기만 볼을 추가할 수 있습니다." }
        return gameState.addBall()
    }

    /**
     * 스트라이크를 추가합니다.
     * @return 3스트라이크로 삼진 여부
     */
    fun addStrike(): Boolean {
        require(status.isOngoing()) { "진행 중인 경기만 스트라이크를 추가할 수 있습니다." }
        return gameState.addStrike()
    }
}
