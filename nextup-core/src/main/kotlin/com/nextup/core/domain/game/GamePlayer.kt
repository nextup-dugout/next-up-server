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
     * 교체 선수로 출전합니다.
     */
    fun enterAsSubstitute(
        inning: Int,
        newPosition: Position,
        newBattingOrder: Int?,
    ) {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
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
     * 포지션을 변경합니다.
     */
    fun changePosition(newPosition: Position) {
        require(isCurrentlyPlaying) { "현재 출전 중인 선수만 포지션을 변경할 수 있습니다." }
        this.position = newPosition
    }

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
     */
    fun releaseDH() {
        this.isDesignatedHitter = false
        this.pitcherBattingOrder = null
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
