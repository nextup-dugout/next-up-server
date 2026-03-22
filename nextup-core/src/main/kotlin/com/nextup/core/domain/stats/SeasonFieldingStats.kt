package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.player.Player
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 시즌 수비 통계 엔티티
 *
 * 선수의 시즌별 수비 통계를 저장합니다.
 * FieldingRecord를 누적하여 시즌 통산 기록을 관리합니다.
 */
@Entity
@Table(
    name = "season_fielding_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_season_fielding_stats_player_year_team",
            columnNames = ["player_id", "year", "team_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_season_fielding_stats_player", columnList = "player_id"),
        Index(name = "idx_season_fielding_stats_year", columnList = "year"),
        Index(name = "idx_season_fielding_stats_team", columnList = "team_id"),
    ],
)
class SeasonFieldingStats(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Column(nullable = false)
    val year: Int,
    @Column(name = "team_id")
    val teamId: Long? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Version
    var version: Long = 0
        protected set

    // 출전 경기 수
    @Column(name = "games_played", nullable = false)
    var gamesPlayed: Int = 0
        protected set

    // 기본 수비 기록
    @Column(name = "put_outs", nullable = false)
    var putOuts: Int = 0
        protected set

    @Column(nullable = false)
    var assists: Int = 0
        protected set

    @Column(nullable = false)
    var errors: Int = 0
        protected set

    @Column(name = "double_plays", nullable = false)
    var doublePlays: Int = 0
        protected set

    @Column(name = "passed_balls", nullable = false)
    var passedBalls: Int = 0
        protected set

    @Column(name = "triple_plays", nullable = false)
    var triplePlays: Int = 0
        protected set

    @Column(name = "caught_stealing", nullable = false)
    var caughtStealing: Int = 0
        protected set

    @Column(name = "stolen_bases_allowed", nullable = false)
    var stolenBasesAllowed: Int = 0
        protected set

    // Calculated properties

    /**
     * 수비 기회(TC) = 자살 + 보살 + 실책
     */
    val totalChances: Int
        get() = putOuts + assists + errors

    /**
     * 수비율(FPCT) = (자살 + 보살) / 수비 기회
     * 수비 기회가 0이면 null
     */
    val fieldingPercentage: BigDecimal?
        get() =
            if (totalChances == 0) {
                null
            } else {
                BigDecimal(putOuts + assists).divide(BigDecimal(totalChances), 3, RoundingMode.HALF_UP)
            }

    // Business logic

    /**
     * 경기 수비 기록을 누적합니다.
     */
    fun addGameRecord(record: FieldingRecord) {
        gamesPlayed++
        putOuts += record.putOuts
        assists += record.assists
        errors += record.errors
        doublePlays += record.doublePlays
        passedBalls += record.passedBalls
        triplePlays += record.triplePlays
        caughtStealing += record.caughtStealing
        stolenBasesAllowed += record.stolenBasesAllowed
    }

    /**
     * 경기 수비 기록을 롤백합니다 (경기 취소 시).
     */
    fun revertGameRecord(record: FieldingRecord) {
        gamesPlayed--
        putOuts -= record.putOuts
        assists -= record.assists
        errors -= record.errors
        doublePlays -= record.doublePlays
        passedBalls -= record.passedBalls
        triplePlays -= record.triplePlays
        caughtStealing -= record.caughtStealing
        stolenBasesAllowed -= record.stolenBasesAllowed
        validate()
    }

    /**
     * 기록 정정 시 델타를 적용합니다.
     *
     * @param fieldName 정정할 필드명
     * @param delta 변경량 (양수: 증가, 음수: 감소)
     */
    fun applyFieldCorrection(
        fieldName: String,
        delta: Int,
    ) {
        when (fieldName) {
            "putOuts" -> putOuts = maxOf(0, putOuts + delta)
            "assists" -> assists = maxOf(0, assists + delta)
            "errors" -> errors = maxOf(0, errors + delta)
            "doublePlays" -> doublePlays = maxOf(0, doublePlays + delta)
            "passedBalls" -> passedBalls = maxOf(0, passedBalls + delta)
            "triplePlays" -> triplePlays = maxOf(0, triplePlays + delta)
            "caughtStealing" -> caughtStealing = maxOf(0, caughtStealing + delta)
            "stolenBasesAllowed" -> stolenBasesAllowed = maxOf(0, stolenBasesAllowed + delta)
            else -> throw IllegalArgumentException("유효하지 않은 시즌 수비 통계 필드입니다: $fieldName")
        }
        validate()
    }

    /**
     * 기록 유효성을 검증합니다.
     */
    fun validate() {
        if (gamesPlayed < 0) {
            throw StatsValidationException("출전 경기 수는 0 이상이어야 합니다.")
        }
        if (putOuts < 0) {
            throw StatsValidationException("자살($putOuts)은 음수일 수 없습니다.")
        }
        if (assists < 0) {
            throw StatsValidationException("보살($assists)은 음수일 수 없습니다.")
        }
        if (errors < 0) {
            throw StatsValidationException("실책($errors)은 음수일 수 없습니다.")
        }
        if (doublePlays < 0) {
            throw StatsValidationException("병살 관여($doublePlays)는 음수일 수 없습니다.")
        }
        if (passedBalls < 0) {
            throw StatsValidationException("포일($passedBalls)은 음수일 수 없습니다.")
        }
        if (triplePlays < 0) {
            throw StatsValidationException("삼중살 관여($triplePlays)는 음수일 수 없습니다.")
        }
        if (caughtStealing < 0) {
            throw StatsValidationException("도루 저지($caughtStealing)는 음수일 수 없습니다.")
        }
        if (stolenBasesAllowed < 0) {
            throw StatsValidationException("도루 허용($stolenBasesAllowed)은 음수일 수 없습니다.")
        }
    }

    companion object {
        /**
         * 선수의 시즌 수비 통계를 생성합니다.
         *
         * @param player 선수
         * @param year 연도
         * @param teamId 팀 ID (이적 시 팀별 기록 분리 지원, null이면 팀 구분 없음)
         */
        fun create(
            player: Player,
            year: Int,
            teamId: Long? = null,
        ): SeasonFieldingStats {
            if (year <= 0) {
                throw StatsValidationException("연도는 양수여야 합니다.")
            }
            return SeasonFieldingStats(player = player, year = year, teamId = teamId)
        }
    }
}
