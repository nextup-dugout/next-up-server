package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import jakarta.persistence.*

/**
 * 경기 출전 선수 엔티티
 *
 * 경기(GameTeam)에 출전하는 선수(Player)를 나타냅니다.
 * 타순, 포지션, 출전 여부 등을 관리합니다.
 */
@Entity
@Table(
    name = "game_players",
    indexes = [
        Index(name = "idx_game_players_game_team", columnList = "game_team_id"),
        Index(name = "idx_game_players_player", columnList = "player_id"),
        Index(name = "idx_game_players_batting_order", columnList = "game_team_id, batting_order"),
        Index(name = "idx_game_players_position", columnList = "game_team_id, position"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_players_game_team_player",
            columnNames = ["game_team_id", "player_id"],
        ),
    ],
)
class GamePlayer(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_team_id", nullable = false)
    val gameTeam: GameTeam,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var position: Position,
    @Column(name = "batting_order")
    var battingOrder: Int? = null,
    @Column(name = "back_number")
    var backNumber: Int? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Column(name = "is_starter", nullable = false)
    var isStarter: Boolean = true
        protected set

    @Column(name = "is_currently_playing", nullable = false)
    var isCurrentlyPlaying: Boolean = true
        protected set

    @Column(name = "entry_inning")
    var entryInning: Int? = null
        protected set

    @Column(name = "exit_inning")
    var exitInning: Int? = null
        protected set

    @Column(name = "is_designated_hitter", nullable = false)
    var isDesignatedHitter: Boolean = false
        protected set

    @Column(name = "pitcher_batting_order")
    var pitcherBattingOrder: Int? = null
        protected set

    /**
     * 퇴장 사유 (부상/심판 퇴장/기타)
     * null이면 퇴장이 아닌 일반 교체로 퇴장한 경우입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ejection_reason", length = 30)
    var ejectionReason: EjectionReason? = null
        protected set

    /**
     * 포지션 변경 이력
     * DB에는 CSV 형식("이닝:포지션,이닝:포지션,...")으로 저장됩니다.
     */
    @Convert(converter = PositionHistoryConverter::class)
    @Column(name = "position_history", columnDefinition = "TEXT")
    var positionHistory: List<PositionHistoryEntry> = emptyList()
        protected set

    /**
     * 선발 출전 선수인지 확인합니다.
     */
    val isStartingLineup: Boolean
        get() = isStarter && battingOrder != null

    /**
     * 투수인지 확인합니다.
     */
    val isPitcher: Boolean
        get() = position.category == com.nextup.core.domain.player.PositionCategory.PITCHER

    /**
     * 타순에 포함된 선수인지 확인합니다 (DH가 아닌 투수는 타순에서 제외될 수 있음).
     */
    val isInBattingOrder: Boolean
        get() = battingOrder != null

    /**
     * 이미 퇴장한 선수인지 확인합니다 (재출전 불가 검증용).
     */
    val hasExited: Boolean
        get() = exitInning != null

    /**
     * 교체 선수로 출전합니다.
     * 이미 퇴장한 선수는 재출전할 수 없습니다.
     */
    fun enterAsSubstitute(
        inning: Int,
        newPosition: Position,
        newBattingOrder: Int?,
    ) {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(!hasExited) {
            "이미 퇴장한 선수는 재출전할 수 없습니다. (선수 ID: $id)"
        }
        this.isStarter = false
        this.isCurrentlyPlaying = true
        this.entryInning = inning
        this.position = newPosition
        this.battingOrder = newBattingOrder
    }

    /**
     * 경기에서 퇴장합니다 (교체 아웃).
     */
    fun exitGame(inning: Int) {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(isCurrentlyPlaying) { "현재 출전 중인 선수만 퇴장할 수 있습니다." }
        this.isCurrentlyPlaying = false
        this.exitInning = inning
    }

    /**
     * 부상/퇴장 사유와 함께 경기에서 퇴장합니다.
     *
     * 일반 교체(exitGame)와 달리 퇴장 사유를 기록합니다.
     * 부상 퇴장, 심판 퇴장 명령 등의 사유를 구분하여 추적합니다.
     *
     * @param inning 퇴장 이닝
     * @param reason 퇴장 사유
     */
    fun eject(
        inning: Int,
        reason: EjectionReason,
    ) {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(isCurrentlyPlaying) { "현재 출전 중인 선수만 퇴장할 수 있습니다." }
        this.isCurrentlyPlaying = false
        this.exitInning = inning
        this.ejectionReason = reason
    }

    /**
     * 퇴장된 선수인지 확인합니다 (퇴장 사유가 있는 경우).
     */
    val isEjected: Boolean
        get() = ejectionReason != null

    /**
     * 포지션을 변경합니다.
     * 변경 전 포지션과 이닝을 이력에 기록합니다.
     *
     * @param newPosition 새 포지션
     * @param currentInning 현재 이닝 (이력 기록용, null이면 이력 미기록)
     */
    fun changePosition(
        newPosition: Position,
        currentInning: Int? = null,
    ) {
        require(isCurrentlyPlaying) { "현재 출전 중인 선수만 포지션을 변경할 수 있습니다." }
        if (currentInning != null) {
            recordPositionHistory(currentInning, this.position)
        }
        this.position = newPosition
    }

    /**
     * 포지션 이력을 추가합니다.
     */
    private fun recordPositionHistory(
        inning: Int,
        previousPosition: Position,
    ) {
        positionHistory = positionHistory + PositionHistoryEntry(inning, previousPosition)
    }

    /**
     * 포지션 이력을 이닝 오름차순으로 반환합니다.
     */
    fun getPositionHistoryList(): List<PositionHistoryEntry> = positionHistory.sortedBy { it.inning }

    /**
     * 타순을 변경합니다.
     */
    fun changeBattingOrder(newBattingOrder: Int?) {
        require(isCurrentlyPlaying) { "현재 출전 중인 선수만 타순을 변경할 수 있습니다." }
        this.battingOrder = newBattingOrder
    }

    /**
     * DH로 설정합니다.
     */
    fun setAsDesignatedHitter(pitcherOrder: Int) {
        require(position == Position.DESIGNATED_HITTER) { "지명타자 포지션만 DH로 설정할 수 있습니다." }
        this.isDesignatedHitter = true
        this.pitcherBattingOrder = pitcherOrder
    }

    /**
     * DH 해제 시 처리합니다 (DH 규칙 해제).
     * 투수가 타순에 들어가게 됩니다.
     *
     * DH 해제 조건:
     * - 투수가 DH(지명타자) 자리 타순에 직접 들어올 때만 가능
     * - 또는 DH가 수비 포지션으로 이동할 때
     * - 이 메서드 호출 전 도메인 서비스에서 조건 검증 필수
     */
    fun releaseDH() {
        this.isDesignatedHitter = false
        this.pitcherBattingOrder = null
    }

    /**
     * 이 선수가 DH 자리에 투수로 교체될 수 있는지 검증합니다.
     * 투수가 DH 타순으로 들어오면 DH 규칙이 해제됩니다.
     *
     * @param incomingPlayer 교체 들어오는 선수
     * @return DH 규칙 해제가 필요한 경우 true
     */
    fun isDhReleaseRequired(incomingPlayer: GamePlayer): Boolean {
        // 현재 선수가 DH이고, 들어오는 선수가 투수일 때 DH 해제
        return this.isDesignatedHitter && incomingPlayer.isPitcher
    }

    /**
     * DH 해제 규칙을 검증합니다.
     * 투수가 DH 자리 이외의 타순으로 들어오는 경우 DH 해제는 허용되지 않습니다.
     *
     * @param incomingPlayer 교체 들어오는 선수
     * @param incomingBattingOrder 들어오는 선수의 타순
     * @throws IllegalArgumentException DH 해제 규칙 위반 시
     */
    fun validateDhRelease(
        incomingPlayer: GamePlayer,
        incomingBattingOrder: Int?,
    ) {
        if (this.isDesignatedHitter && incomingPlayer.isPitcher) {
            // 투수가 DH 타순으로 들어오는 경우만 허용
            val dhBattingOrder = this.battingOrder
            require(incomingBattingOrder == dhBattingOrder) {
                "투수는 DH 타순(${dhBattingOrder}번)으로만 교체될 수 있습니다. 요청 타순: $incomingBattingOrder"
            }
        }
    }

    companion object {
        /**
         * 선발 출전 선수를 생성합니다.
         */
        fun createStarter(
            gameTeam: GameTeam,
            player: Player,
            position: Position,
            battingOrder: Int?,
            backNumber: Int? = null,
        ): GamePlayer =
            GamePlayer(
                gameTeam = gameTeam,
                player = player,
                position = position,
                battingOrder = battingOrder,
                backNumber = backNumber,
            ).apply {
                this.isStarter = true
                this.isCurrentlyPlaying = true
                this.entryInning = 1
            }

        /**
         * 대기 선수(벤치)를 생성합니다.
         */
        fun createBench(
            gameTeam: GameTeam,
            player: Player,
            position: Position,
            backNumber: Int? = null,
        ): GamePlayer =
            GamePlayer(
                gameTeam = gameTeam,
                player = player,
                position = position,
                battingOrder = null,
                backNumber = backNumber,
            ).apply {
                this.isStarter = false
                this.isCurrentlyPlaying = false
            }
    }
}
