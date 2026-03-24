package com.nextup.core.domain.stats

import com.nextup.common.exception.FrozenStatsException
import com.nextup.common.exception.StatsValidationException
import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.FieldingEventType
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.player.Player
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
            name = "uk_season_fielding_stats_player_year_team_ct",
            columnNames = ["player_id", "year", "team_id", "competition_type"],
        ),
    ],
    indexes = [
        Index(name = "idx_season_fielding_stats_player", columnList = "player_id"),
        Index(name = "idx_season_fielding_stats_year", columnList = "year"),
        Index(name = "idx_season_fielding_stats_team", columnList = "team_id"),
        Index(name = "idx_season_fielding_stats_comp_type", columnList = "competition_type"),
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
    @Enumerated(EnumType.STRING)
    @Column(name = "competition_type", nullable = false, length = 20)
    val competitionType: CompetitionType = CompetitionType.LEAGUE,
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

    /** L-8: 시즌 통계 확정 여부 (확정 후에는 수정 불가) */
    @Column(name = "is_finalized", nullable = false)
    var isFinalized: Boolean = false
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
        requireNotFinalized()
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
        requireNotFinalized()
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
     * 경기 수비 기록을 롤백합니다 (경기 취소 시).
     */
    fun revertGameRecord(record: FieldingRecord) {
        requireNotFinalized()
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
     * L-6: 경기 중 수비 기록을 실시간으로 시즌 통계에 반영합니다.
     *
     * FieldingRecordUpdatedEvent를 수신하여 호출되며,
     * 경기 종료 전에도 수비 통계가 실시간으로 조회 가능합니다.
     *
     * @param type 수비 기록 유형
     */
    fun applyLiveFieldingUpdate(type: FieldingEventType) {
        requireNotFinalized()
        when (type) {
            FieldingEventType.PUT_OUT -> putOuts++
            FieldingEventType.ASSIST -> assists++
            FieldingEventType.ERROR -> errors++
            FieldingEventType.DOUBLE_PLAY -> doublePlays++
            FieldingEventType.TRIPLE_PLAY -> triplePlays++
            FieldingEventType.PASSED_BALL -> passedBalls++
            FieldingEventType.CAUGHT_STEALING -> caughtStealing++
            FieldingEventType.STOLEN_BASE_ALLOWED -> stolenBasesAllowed++
        }
    }

    /**
     * L-6: 경기 중 수비 기록을 시즌 통계에서 역산합니다 (Undo 처리).
     *
     * applyLiveFieldingUpdate의 역연산입니다.
     *
     * @param type 수비 기록 유형
     */
    fun revertLiveFieldingUpdate(type: FieldingEventType) {
        requireNotFinalized()
        when (type) {
            FieldingEventType.PUT_OUT -> putOuts = maxOf(0, putOuts - 1)
            FieldingEventType.ASSIST -> assists = maxOf(0, assists - 1)
            FieldingEventType.ERROR -> errors = maxOf(0, errors - 1)
            FieldingEventType.DOUBLE_PLAY -> doublePlays = maxOf(0, doublePlays - 1)
            FieldingEventType.TRIPLE_PLAY -> triplePlays = maxOf(0, triplePlays - 1)
            FieldingEventType.PASSED_BALL -> passedBalls = maxOf(0, passedBalls - 1)
            FieldingEventType.CAUGHT_STEALING -> caughtStealing = maxOf(0, caughtStealing - 1)
            FieldingEventType.STOLEN_BASE_ALLOWED -> stolenBasesAllowed = maxOf(0, stolenBasesAllowed - 1)
        }
    }

    /**
     * L-7: 경기 종료 시 BoxScore와 교차 검증하여 정합성을 확인합니다.
     *
     * 실시간 갱신된 시즌 통계가 경기별 FieldingRecord 합산과 일치하는지 검증합니다.
     *
     * @param totalPutOuts 경기별 FieldingRecord에서 합산한 총 자살 수
     * @param totalAssists 경기별 FieldingRecord에서 합산한 총 보살 수
     * @param totalErrors 경기별 FieldingRecord에서 합산한 총 실책 수
     * @return 불일치 항목 목록 (비어있으면 정합성 OK)
     */
    fun verifyConsistency(
        totalPutOuts: Int,
        totalAssists: Int,
        totalErrors: Int,
    ): List<String> {
        val mismatches = mutableListOf<String>()
        if (putOuts != totalPutOuts) {
            mismatches.add("자살: 시즌통계=$putOuts, FieldingRecord합산=$totalPutOuts")
        }
        if (assists != totalAssists) {
            mismatches.add("보살: 시즌통계=$assists, FieldingRecord합산=$totalAssists")
        }
        if (errors != totalErrors) {
            mismatches.add("실책: 시즌통계=$errors, FieldingRecord합산=$totalErrors")
        }
        return mismatches
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
        requireNotFinalized()
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

    /**
     * L-1: gamesPlayed만 1 증가시킵니다 (수비 기록 필드는 변경하지 않음).
     *
     * revertGameRecord가 레코드마다 gamesPlayed를 차감하는 문제를 보정할 때 사용합니다.
     * 예: 3개 포지션 레코드 → revertGameRecord 3회 → gamesPlayed -3 → addGamePlayedOnly 2회로 보정.
     */
    fun addGamePlayedOnly() {
        gamesPlayed++
    }

    /**
     * L-8: 시즌 통계를 확정합니다.
     *
     * 확정된 통계는 추가 갱신이 불가합니다.
     */
    fun finalize() {
        require(!isFinalized) { "이미 확정된 시즌 통계입니다." }
        validate()
        this.isFinalized = true
    }

    /**
     * L-8: 시즌 통계 확정을 해제합니다 (관리자용).
     */
    fun unfinalize() {
        require(isFinalized) { "확정되지 않은 시즌 통계입니다." }
        this.isFinalized = false
    }

    /**
     * 모든 통계 필드를 0으로 초기화합니다 (재계산을 위한 리셋).
     *
     * 시즌 통계를 처음부터 다시 집계할 때 사용합니다.
     * isFinalized 상태에서는 호출할 수 없습니다.
     */
    fun reset() {
        requireNotFinalized()
        gamesPlayed = 0
        putOuts = 0
        assists = 0
        errors = 0
        doublePlays = 0
        passedBalls = 0
        triplePlays = 0
        caughtStealing = 0
        stolenBasesAllowed = 0
    }

    /**
     * 확정된 통계의 수정을 방지하는 가드 메서드.
     */
    private fun requireNotFinalized() {
        if (isFinalized) {
            throw FrozenStatsException()
        }
    }

    companion object {
        /**
         * 선수의 시즌 수비 통계를 생성합니다.
         *
         * @param player 선수
         * @param year 연도
         * @param teamId 팀 ID (이적 시 팀별 기록 분리 지원, null이면 팀 구분 없음)
         * @param competitionType 대회 유형 (기본값 LEAGUE, FRIENDLY이면 공식 순위에서 제외)
         */
        fun create(
            player: Player,
            year: Int,
            teamId: Long? = null,
            competitionType: CompetitionType = CompetitionType.LEAGUE,
        ): SeasonFieldingStats {
            if (year <= 0) {
                throw StatsValidationException("연도는 양수여야 합니다.")
            }
            return SeasonFieldingStats(
                player = player,
                year = year,
                teamId = teamId,
                competitionType = competitionType,
            )
        }
    }
}
