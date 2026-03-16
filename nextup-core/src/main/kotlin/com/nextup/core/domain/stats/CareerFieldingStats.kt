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
    }

    /**
     * 시즌을 추가합니다.
     */
    fun addSeason() {
        seasonsPlayed++
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
    }

    companion object {
        /**
         * 선수의 통산 수비 통계를 생성합니다.
         */
        fun create(player: Player): CareerFieldingStats = CareerFieldingStats(player = player)
    }
}
