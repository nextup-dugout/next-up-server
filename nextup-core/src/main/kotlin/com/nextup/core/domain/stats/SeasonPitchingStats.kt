package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.Player
import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 시즌 투수 통계 엔티티
 *
 * 선수의 시즌별 투수 통계를 저장합니다.
 * PitchingRecord를 누적하여 시즌 통산 기록을 관리합니다.
 */
@Entity
@Table(
    name = "season_pitching_stats",
    indexes = [
        Index(name = "idx_season_pitching_stats_player", columnList = "player_id"),
        Index(name = "idx_season_pitching_stats_year", columnList = "year"),
        Index(name = "idx_season_pitching_stats_games", columnList = "games_played"),
        Index(name = "idx_season_pitching_stats_team", columnList = "team_id"),
        Index(name = "idx_season_pitching_stats_comp_type", columnList = "competition_type"),
    ],
)
class SeasonPitchingStats(
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

    @Column(name = "games_started", nullable = false)
    var gamesStarted: Int = 0
        protected set

    // 기본 투수 기록
    @Column(name = "innings_pitched_outs", nullable = false)
    var inningsPitchedOuts: Int = 0
        protected set

    @Column(nullable = false)
    var wins: Int = 0
        protected set

    @Column(nullable = false)
    var losses: Int = 0
        protected set

    @Column(nullable = false)
    var saves: Int = 0
        protected set

    @Column(nullable = false)
    var holds: Int = 0
        protected set

    @Column(name = "blown_saves", nullable = false)
    var blownSaves: Int = 0
        protected set

    @Column(name = "earned_runs", nullable = false)
    var earnedRuns: Int = 0
        protected set

    @Column(name = "runs_allowed", nullable = false)
    var runsAllowed: Int = 0
        protected set

    @Column(name = "hits_allowed", nullable = false)
    var hitsAllowed: Int = 0
        protected set

    @Column(name = "walks_allowed", nullable = false)
    var walksAllowed: Int = 0
        protected set

    @Column(nullable = false)
    var strikeouts: Int = 0
        protected set

    @Column(name = "home_runs_allowed", nullable = false)
    var homeRunsAllowed: Int = 0
        protected set

    @Column(name = "hit_batsmen", nullable = false)
    var hitBatsmen: Int = 0
        protected set

    @Column(name = "wild_pitches", nullable = false)
    var wildPitches: Int = 0
        protected set

    @Column(nullable = false)
    var balks: Int = 0
        protected set

    @Column(name = "batters_faced", nullable = false)
    var battersFaced: Int = 0
        protected set

    @Column(name = "pitches_thrown")
    var pitchesThrown: Int? = null
        protected set

    @Column(name = "strikes_thrown")
    var strikesThrown: Int? = null
        protected set

    // Calculated properties (PitchingRecord와 동일한 로직)

    /**
     * 완전한 이닝 수
     */
    val completeInnings: Int
        get() = inningsPitchedOuts / 3

    /**
     * 이닝의 잔여 아웃 수
     */
    val remainingOuts: Int
        get() = inningsPitchedOuts % 3

    /**
     * 이닝 (실수 형태, 계산용)
     */
    val inningsPitched: BigDecimal
        get() = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 2, RoundingMode.HALF_UP)

    /**
     * 이닝 표시 문자열 (예: "5.1", "7.0", "6.2")
     */
    val inningsPitchedDisplay: String
        get() = "$completeInnings.$remainingOuts"

    /**
     * 자책점 평균 (ERA) = (자책점 / 이닝) * 9
     * 이닝이 0이고 자책점이 있으면 null (계산 불가 - 무한대)
     * 이닝이 0이고 자책점이 없으면 0.00
     */
    val earnedRunAverage: BigDecimal?
        get() =
            if (inningsPitchedOuts == 0) {
                if (earnedRuns > 0) null else BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(earnedRuns)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * WHIP = (피안타 + 볼넷) / 이닝
     */
    val whip: BigDecimal
        get() =
            if (inningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(hitsAllowed + walksAllowed).divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * 9이닝당 삼진 (K/9) = (삼진 / 이닝) * 9
     */
    val strikeoutsPer9: BigDecimal
        get() =
            if (inningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(strikeouts)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * 9이닝당 볼넷 (BB/9) = (볼넷 / 이닝) * 9
     */
    val walksPer9: BigDecimal
        get() =
            if (inningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(walksAllowed)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * 삼진/볼넷 비율 (K/BB)
     */
    val strikeoutToWalkRatio: BigDecimal
        get() =
            if (walksAllowed == 0) {
                if (strikeouts == 0) BigDecimal.ZERO else BigDecimal(strikeouts)
            } else {
                BigDecimal(strikeouts).divide(BigDecimal(walksAllowed), 2, RoundingMode.HALF_UP)
            }.setScale(2, RoundingMode.HALF_UP)

    /**
     * 스트라이크 비율 (투구 수 대비 스트라이크)
     */
    val strikePercentage: BigDecimal?
        get() =
            if (pitchesThrown == null || strikesThrown == null || pitchesThrown == 0) {
                null
            } else {
                BigDecimal(strikesThrown!!)
                    .divide(BigDecimal(pitchesThrown!!), 3, RoundingMode.HALF_UP)
            }

    /**
     * 비자책 실점 = 실점 - 자책점
     */
    val unearnedRuns: Int
        get() = runsAllowed - earnedRuns

    /**
     * 승률 = 승 / (승 + 패)
     */
    val winningPercentage: BigDecimal
        get() {
            val decisions = wins + losses
            return if (decisions == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(wins).divide(BigDecimal(decisions), 3, RoundingMode.HALF_UP)
            }
        }

    // Business logic

    /**
     * 경기 투수 기록을 누적합니다.
     *
     * 실시간 갱신(applyLiveUpdate)이 적용되지 않은 경우에 사용합니다.
     * 경기 취소 후 전체 재집계, 또는 실시간 갱신 없이 경기가 종료된 경우에 해당합니다.
     */
    fun addGameRecord(record: PitchingRecord) {
        addGameSummaryFields(record)
        addLiveTrackingFields(record)
    }

    /**
     * 경기 종료 시 경기 요약 필드만 누적합니다.
     *
     * 경기 중 applyLiveUpdate로 실시간 갱신된 필드(피안타, 삼진, 볼넷, 사구, 피홈런, 대면타자)는
     * 이미 반영되어 있으므로 제외하고, 경기 종료 시에만 확정되는 필드만 추가합니다:
     * - 출전 경기 수, 선발 경기 수
     * - 이닝, 실점, 자책점
     * - 와일드피치, 보크
     * - 투구 수, 스트라이크 수
     * - 승/패/세이브/홀드/블론세이브
     */
    fun addGameRecordForEndOfGame(record: PitchingRecord) {
        addGameSummaryFields(record)
    }

    /**
     * 경기 요약 필드를 누적합니다 (경기 종료 시에만 확정되는 필드).
     */
    private fun addGameSummaryFields(record: PitchingRecord) {
        gamesPlayed++
        if (record.isStartingPitcher) {
            gamesStarted++
        }
        inningsPitchedOuts += record.inningsPitchedOuts
        earnedRuns += record.earnedRuns
        runsAllowed += record.runsAllowed
        wildPitches += record.wildPitches
        balks += record.balks

        // 투구 수 누적 (nullable이므로 null 체크)
        if (record.pitchesThrown != null) {
            pitchesThrown = (pitchesThrown ?: 0) + record.pitchesThrown!!
        }
        if (record.strikesThrown != null) {
            strikesThrown = (strikesThrown ?: 0) + record.strikesThrown!!
        }

        // 승/패/세이브/홀드/블론세이브 누적
        when (record.decision) {
            com.nextup.core.domain.game.PitchingDecision.WIN -> wins++
            com.nextup.core.domain.game.PitchingDecision.LOSS -> losses++
            com.nextup.core.domain.game.PitchingDecision.SAVE -> saves++
            com.nextup.core.domain.game.PitchingDecision.HOLD -> holds++
            com.nextup.core.domain.game.PitchingDecision.BLOWN_SAVE -> blownSaves++
            else -> { /* NONE */ }
        }
    }

    /**
     * 실시간 추적 필드를 누적합니다 (applyLiveUpdate로 이미 갱신되는 필드).
     */
    private fun addLiveTrackingFields(record: PitchingRecord) {
        hitsAllowed += record.hitsAllowed
        walksAllowed += record.walksAllowed
        strikeouts += record.strikeouts
        homeRunsAllowed += record.homeRunsAllowed
        hitBatsmen += record.hitBatsmen
        battersFaced += record.battersFaced
    }

    /**
     * 경기 투수 기여분을 시즌 통계에서 차감합니다 (경기 취소 롤백).
     *
     * 취소된 경기의 PitchingRecord에 집계된 값을 역산하여
     * 해당 경기의 기여분 전체를 시즌 통계에서 제거합니다.
     * 음수 방지를 위해 각 항목은 0 미만으로 내려가지 않습니다.
     */
    fun revertGameRecord(record: PitchingRecord) {
        gamesPlayed = maxOf(0, gamesPlayed - 1)
        if (record.isStartingPitcher) {
            gamesStarted = maxOf(0, gamesStarted - 1)
        }
        inningsPitchedOuts = maxOf(0, inningsPitchedOuts - record.inningsPitchedOuts)
        earnedRuns = maxOf(0, earnedRuns - record.earnedRuns)
        runsAllowed = maxOf(0, runsAllowed - record.runsAllowed)
        hitsAllowed = maxOf(0, hitsAllowed - record.hitsAllowed)
        walksAllowed = maxOf(0, walksAllowed - record.walksAllowed)
        strikeouts = maxOf(0, strikeouts - record.strikeouts)
        homeRunsAllowed = maxOf(0, homeRunsAllowed - record.homeRunsAllowed)
        hitBatsmen = maxOf(0, hitBatsmen - record.hitBatsmen)
        wildPitches = maxOf(0, wildPitches - record.wildPitches)
        balks = maxOf(0, balks - record.balks)
        battersFaced = maxOf(0, battersFaced - record.battersFaced)

        if (record.pitchesThrown != null) {
            pitchesThrown = maxOf(0, (pitchesThrown ?: 0) - record.pitchesThrown!!)
        }
        if (record.strikesThrown != null) {
            strikesThrown = maxOf(0, (strikesThrown ?: 0) - record.strikesThrown!!)
        }

        when (record.decision) {
            com.nextup.core.domain.game.PitchingDecision.WIN -> wins = maxOf(0, wins - 1)
            com.nextup.core.domain.game.PitchingDecision.LOSS -> losses = maxOf(0, losses - 1)
            com.nextup.core.domain.game.PitchingDecision.SAVE -> saves = maxOf(0, saves - 1)
            com.nextup.core.domain.game.PitchingDecision.HOLD -> holds = maxOf(0, holds - 1)
            com.nextup.core.domain.game.PitchingDecision.BLOWN_SAVE -> blownSaves = maxOf(0, blownSaves - 1)
            else -> { /* NONE */ }
        }
    }

    /**
     * 경기 중 타석 결과를 즉시 시즌 투수 통계에 반영합니다 (실시간 갱신).
     *
     * 이벤트 기반으로 호출되며, 경기 종료 전에도 투수 통계가 반영됩니다.
     * 대면 타자 수, 피안타, 삼진, 볼넷, 사구, 피홈런을 갱신합니다.
     */
    fun applyLiveUpdate(result: PlateAppearanceResult) {
        battersFaced++

        when (result) {
            PlateAppearanceResult.SINGLE,
            PlateAppearanceResult.DOUBLE,
            PlateAppearanceResult.TRIPLE,
            -> {
                hitsAllowed++
            }
            PlateAppearanceResult.HOME_RUN -> {
                hitsAllowed++
                homeRunsAllowed++
            }
            PlateAppearanceResult.STRIKEOUT,
            PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD,
            -> {
                strikeouts++
            }
            PlateAppearanceResult.WALK,
            PlateAppearanceResult.INTENTIONAL_WALK,
            -> {
                walksAllowed++
            }
            PlateAppearanceResult.HIT_BY_PITCH -> {
                hitBatsmen++
            }
            else -> {
                // 다른 결과는 투수 시즌 통계에 직접적인 영향 없음
            }
        }
    }

    /**
     * 경기 중 타석 결과를 시즌 투수 통계에서 역산합니다 (Undo 처리).
     *
     * 이벤트 기반으로 호출되며, applyLiveUpdate의 역연산입니다.
     */
    fun revertLiveUpdate(result: PlateAppearanceResult) {
        if (battersFaced > 0) battersFaced--

        when (result) {
            PlateAppearanceResult.SINGLE,
            PlateAppearanceResult.DOUBLE,
            PlateAppearanceResult.TRIPLE,
            -> {
                if (hitsAllowed > 0) hitsAllowed--
            }
            PlateAppearanceResult.HOME_RUN -> {
                if (hitsAllowed > 0) hitsAllowed--
                if (homeRunsAllowed > 0) homeRunsAllowed--
            }
            PlateAppearanceResult.STRIKEOUT,
            PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD,
            -> {
                if (strikeouts > 0) strikeouts--
            }
            PlateAppearanceResult.WALK,
            PlateAppearanceResult.INTENTIONAL_WALK,
            -> {
                if (walksAllowed > 0) walksAllowed--
            }
            PlateAppearanceResult.HIT_BY_PITCH -> {
                if (hitBatsmen > 0) hitBatsmen--
            }
            else -> {
                // 다른 결과는 투수 시즌 통계에 직접적인 영향 없음
            }
        }
    }

    /**
     * 기록 정정에 따른 필드별 델타를 적용합니다.
     *
     * @param fieldName 정정된 필드명
     * @param delta 변경량 (양수: 증가, 음수: 감소)
     */
    fun applyFieldCorrection(
        fieldName: String,
        delta: Int,
    ) {
        when (fieldName) {
            "inningsPitchedOuts" -> inningsPitchedOuts = maxOf(0, inningsPitchedOuts + delta)
            "earnedRuns" -> earnedRuns = maxOf(0, earnedRuns + delta)
            "runsAllowed" -> runsAllowed = maxOf(0, runsAllowed + delta)
            "hitsAllowed" -> hitsAllowed = maxOf(0, hitsAllowed + delta)
            "walksAllowed" -> walksAllowed = maxOf(0, walksAllowed + delta)
            "strikeouts" -> strikeouts = maxOf(0, strikeouts + delta)
            "homeRunsAllowed" -> homeRunsAllowed = maxOf(0, homeRunsAllowed + delta)
            "hitBatsmen" -> hitBatsmen = maxOf(0, hitBatsmen + delta)
            "wildPitches" -> wildPitches = maxOf(0, wildPitches + delta)
            "balks" -> balks = maxOf(0, balks + delta)
            "battersFaced" -> battersFaced = maxOf(0, battersFaced + delta)
            "pitchesThrown" -> pitchesThrown = maxOf(0, (pitchesThrown ?: 0) + delta)
            "strikesThrown" -> strikesThrown = maxOf(0, (strikesThrown ?: 0) + delta)
            else -> throw IllegalArgumentException("유효하지 않은 시즌 투수 통계 필드입니다: $fieldName")
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
        if (gamesStarted > gamesPlayed) {
            throw StatsValidationException("선발 경기 수($gamesStarted)가 총 출전 경기 수($gamesPlayed)보다 클 수 없습니다.")
        }
        if (earnedRuns > runsAllowed) {
            throw StatsValidationException("자책점($earnedRuns)이 실점($runsAllowed)보다 클 수 없습니다.")
        }
        if (pitchesThrown != null && strikesThrown != null && strikesThrown!! > pitchesThrown!!) {
            throw StatsValidationException("스트라이크 수($strikesThrown)가 총 투구 수($pitchesThrown)보다 클 수 없습니다.")
        }
    }

    companion object {
        /**
         * 선수의 시즌 투수 통계를 생성합니다.
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
        ): SeasonPitchingStats {
            if (year <= 0) {
                throw StatsValidationException("연도는 양수여야 합니다.")
            }
            return SeasonPitchingStats(
                player = player,
                year = year,
                teamId = teamId,
                competitionType = competitionType,
            )
        }
    }
}
