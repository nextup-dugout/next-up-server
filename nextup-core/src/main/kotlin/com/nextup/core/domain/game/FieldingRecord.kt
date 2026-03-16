package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
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
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 수비 기록 엔티티
 *
 * 경기 출전 선수(GamePlayer)의 수비 기록을 저장합니다.
 * 한 경기에서 선수당 하나의 수비 기록만 존재합니다.
 */
@Entity
@Table(
    name = "fielding_records",
    indexes = [
        Index(name = "idx_fielding_records_game_player", columnList = "game_player_id"),
    ],
)
class FieldingRecord(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id", nullable = false)
    val gamePlayer: GamePlayer,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 자살(PO, Put Out) - 아웃을 직접 성립시킨 횟수
     */
    @Column(name = "put_outs", nullable = false)
    var putOuts: Int = 0
        protected set

    /**
     * 보살(A, Assist) - 아웃에 도움을 준 횟수
     */
    @Column(nullable = false)
    var assists: Int = 0
        protected set

    /**
     * 실책(E, Error) - 수비 실수로 인한 오류 횟수
     */
    @Column(nullable = false)
    var errors: Int = 0
        protected set

    /**
     * 병살 관여(DP, Double Play) - 병살에 관여한 횟수
     */
    @Column(name = "double_plays", nullable = false)
    var doublePlays: Int = 0
        protected set

    /**
     * 삼중살 관여(TP, Triple Play) - 삼중살에 관여한 횟수
     */
    @Column(name = "triple_plays", nullable = false)
    var triplePlays: Int = 0
        protected set

    /**
     * 포일(PB, Passed Ball) - 포수의 공 처리 실패 횟수 (포수 전용)
     */
    @Column(name = "passed_balls", nullable = false)
    var passedBalls: Int = 0
        protected set

    // Calculated properties

    /**
     * 수비 기회(TC, Total Chances) = 자살 + 보살 + 실책
     */
    val totalChances: Int
        get() = putOuts + assists + errors

    /**
     * 수비율(FPCT, Fielding Percentage) = (자살 + 보살) / 수비 기회
     * 수비 기회가 0이면 null (계산 불가)
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
     * 자살(PO)을 기록합니다.
     */
    fun recordPutOut() {
        putOuts++
    }

    /**
     * 보살(A)을 기록합니다.
     */
    fun recordAssist() {
        assists++
    }

    /**
     * 실책(E)을 기록합니다.
     */
    fun recordError() {
        errors++
    }

    /**
     * 병살 관여(DP)를 기록합니다.
     */
    fun recordDoublePlay() {
        doublePlays++
    }

    /**
     * 삼중살 관여(TP)를 기록합니다.
     */
    fun recordTriplePlay() {
        triplePlays++
    }

    /**
     * 포일(PB)을 기록합니다.
     */
    fun recordPassedBall() {
        passedBalls++
    }

    /**
     * 자살(PO)을 취소합니다 (Undo용).
     */
    fun revertPutOut() {
        require(putOuts > 0) { "취소할 자살 기록이 없습니다." }
        putOuts--
    }

    /**
     * 보살(A)을 취소합니다 (Undo용).
     */
    fun revertAssist() {
        require(assists > 0) { "취소할 보살 기록이 없습니다." }
        assists--
    }

    /**
     * 실책(E)을 취소합니다 (Undo용).
     */
    fun revertError() {
        require(errors > 0) { "취소할 실책 기록이 없습니다." }
        errors--
    }

    /**
     * 병살 관여(DP)를 취소합니다 (Undo용).
     */
    fun revertDoublePlay() {
        require(doublePlays > 0) { "취소할 병살 관여 기록이 없습니다." }
        doublePlays--
    }

    /**
     * 삼중살 관여(TP)를 취소합니다 (Undo용).
     */
    fun revertTriplePlay() {
        require(triplePlays > 0) { "취소할 삼중살 관여 기록이 없습니다." }
        triplePlays--
    }

    /**
     * 포일(PB)을 취소합니다 (Undo용).
     */
    fun revertPassedBall() {
        require(passedBalls > 0) { "취소할 포일 기록이 없습니다." }
        passedBalls--
    }

    /**
     * 기록 유효성을 검증합니다.
     */
    fun validate() {
        require(putOuts >= 0) { "자살($putOuts)은 음수일 수 없습니다." }
        require(assists >= 0) { "보살($assists)은 음수일 수 없습니다." }
        require(errors >= 0) { "실책($errors)은 음수일 수 없습니다." }
        require(doublePlays >= 0) { "병살 관여($doublePlays)는 음수일 수 없습니다." }
        require(triplePlays >= 0) { "삼중살 관여($triplePlays)는 음수일 수 없습니다." }
        require(passedBalls >= 0) { "포일($passedBalls)은 음수일 수 없습니다." }
    }

    companion object {
        /**
         * 경기 출전 선수의 수비 기록을 생성합니다.
         */
        fun create(gamePlayer: GamePlayer): FieldingRecord = FieldingRecord(gamePlayer = gamePlayer)
    }
}
