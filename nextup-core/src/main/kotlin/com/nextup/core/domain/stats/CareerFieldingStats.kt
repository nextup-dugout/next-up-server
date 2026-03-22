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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 통산 수비 통계 엔티티
 *
 * 선수의 통산 수비 통계를 저장합니다.
 * FieldingRecord를 누적하여 커리어 전체 기록을 관리합니다.
 */
@Entity
@Table(
    name = "career_fielding_stats",
    indexes = [
        Index(name = "idx_career_fielding_stats_player", columnList = "player_id"),
    ],
)
class CareerFieldingStats(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    val player: Player,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Version
    var version: Long = 0
        protected set

    // 시즌 수
    @Column(name = "seasons_played", nullable = false)
    var seasonsPlayed: Int = 0
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
     * L-1: 한 경기의 포지션별 수비 기록 목록을 한꺼번에 누적합니다.
     *
     * gamesPlayed는 1회만 증가시키고, 각 포지션별 기록의 필드를 합산합니다.
     * 같은 선수가 여러 포지션을 소화한 경우 사용합니다.
     */
    fun addGameRecords(records: List<FieldingRecord>) {
        require(records.isNotEmpty()) { "수비 기록 목록이 비어있습니다." }
        gamesPlayed++
        for (record in records) {
            putOuts += record.putOuts
            assists += record.assists
            errors += record.errors
            doublePlays += record.doublePlays
            passedBalls += record.passedBalls
            triplePlays += record.triplePlays
            caughtStealing += record.caughtStealing
            stolenBasesAllowed += record.stolenBasesAllowed
        }
    }

    /**
     * 경기 수비 기록을 롤백합니다 (경기 취소 시 사용).
     * 모든 필드를 maxOf(0, ...) 로 보호하여 음수 방지합니다.
     */
    fun revertGameRecord(record: FieldingRecord) {
        gamesPlayed = maxOf(0, gamesPlayed - 1)
        putOuts = maxOf(0, putOuts - record.putOuts)
        assists = maxOf(0, assists - record.assists)
        errors = maxOf(0, errors - record.errors)
        doublePlays = maxOf(0, doublePlays - record.doublePlays)
        passedBalls = maxOf(0, passedBalls - record.passedBalls)
        triplePlays = maxOf(0, triplePlays - record.triplePlays)
        caughtStealing = maxOf(0, caughtStealing - record.caughtStealing)
        stolenBasesAllowed = maxOf(0, stolenBasesAllowed - record.stolenBasesAllowed)
    }

    /**
     * L-1: gamesPlayed만 1 증가시킵니다 (수비 기록 필드는 변경하지 않음).
     *
     * revertGameRecord가 레코드마다 gamesPlayed를 차감하는 문제를 보정할 때 사용합니다.
     */
    fun addGamePlayedOnly() {
        gamesPlayed++
    }

    /**
     * 시즌을 추가합니다.
     */
    fun addSeason() {
        seasonsPlayed++
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
            else -> throw IllegalArgumentException("유효하지 않은 통산 수비 통계 필드입니다: $fieldName")
        }
        validate()
    }

    /**
     * 기록 유효성을 검증합니다.
     */
    fun validate() {
        if (seasonsPlayed < 0) {
            throw StatsValidationException("시즌 수는 0 이상이어야 합니다.")
        }
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
         * 선수의 통산 수비 통계를 생성합니다.
         */
        fun create(player: Player): CareerFieldingStats = CareerFieldingStats(player = player)
    }
}
