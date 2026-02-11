package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.player.Player
import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 시즌 타격 통계 엔티티
 *
 * 선수의 시즌별 타격 통계를 저장합니다.
 * BattingRecord를 누적하여 시즌 통산 기록을 관리합니다.
 */
@Entity
@Table(
    name = "season_batting_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_season_batting_stats_player_year",
            columnNames = ["player_id", "year"],
        ),
    ],
    indexes = [
        Index(name = "idx_season_batting_stats_player", columnList = "player_id"),
        Index(name = "idx_season_batting_stats_year", columnList = "year"),
        Index(name = "idx_season_batting_stats_games", columnList = "games_played"),
    ],
)
class SeasonBattingStats(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Column(nullable = false)
    val year: Int,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    // 출전 경기 수
    @Column(name = "games_played", nullable = false)
    var gamesPlayed: Int = 0
        protected set

    // 기본 타격 기록
    @Column(name = "plate_appearances", nullable = false)
    var plateAppearances: Int = 0
        protected set

    @Column(name = "at_bats", nullable = false)
    var atBats: Int = 0
        protected set

    @Column(nullable = false)
    var hits: Int = 0
        protected set

    @Column(nullable = false)
    var doubles: Int = 0
        protected set

    @Column(nullable = false)
    var triples: Int = 0
        protected set

    @Column(name = "home_runs", nullable = false)
    var homeRuns: Int = 0
        protected set

    @Column(nullable = false)
    var runs: Int = 0
        protected set

    @Column(name = "runs_batted_in", nullable = false)
    var runsBattedIn: Int = 0
        protected set

    @Column(nullable = false)
    var walks: Int = 0
        protected set

    @Column(name = "intentional_walks", nullable = false)
    var intentionalWalks: Int = 0
        protected set

    @Column(name = "hit_by_pitch", nullable = false)
    var hitByPitch: Int = 0
        protected set

    @Column(nullable = false)
    var strikeouts: Int = 0
        protected set

    @Column(name = "sacrifice_bunts", nullable = false)
    var sacrificeBunts: Int = 0
        protected set

    @Column(name = "sacrifice_flies", nullable = false)
    var sacrificeFlies: Int = 0
        protected set

    @Column(name = "stolen_bases", nullable = false)
    var stolenBases: Int = 0
        protected set

    @Column(name = "caught_stealing", nullable = false)
    var caughtStealing: Int = 0
        protected set

    @Column(name = "grounded_into_double_plays", nullable = false)
    var groundedIntoDoublePlays: Int = 0
        protected set

    // Calculated properties (BattingRecord와 동일한 로직)

    /**
     * 단타 수 = 안타 - 2루타 - 3루타 - 홈런
     */
    val singles: Int
        get() = hits - doubles - triples - homeRuns

    /**
     * 총 루타 수 = 1*단타 + 2*2루타 + 3*3루타 + 4*홈런
     */
    val totalBases: Int
        get() = singles + (2 * doubles) + (3 * triples) + (4 * homeRuns)

    /**
     * 장타 수 = 2루타 + 3루타 + 홈런
     */
    val extraBaseHits: Int
        get() = doubles + triples + homeRuns

    /**
     * 희생타 수 = 희생번트 + 희생플라이
     */
    val sacrifices: Int
        get() = sacrificeBunts + sacrificeFlies

    /**
     * 총 볼넷 수 = 볼넷 + 고의사구
     */
    val totalWalks: Int
        get() = walks + intentionalWalks

    /**
     * 타율 (AVG) = 안타 / 타수
     * 타수가 0이면 .000
     */
    val battingAverage: BigDecimal
        get() =
            if (atBats == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(hits).divide(BigDecimal(atBats), 3, RoundingMode.HALF_UP)
            }

    /**
     * 출루율 (OBP) = (안타 + 볼넷 + 사구) / (타수 + 볼넷 + 사구 + 희생플라이)
     */
    val onBasePercentage: BigDecimal
        get() {
            val numerator = hits + totalWalks + hitByPitch
            val denominator = atBats + totalWalks + hitByPitch + sacrificeFlies
            return if (denominator == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(numerator).divide(BigDecimal(denominator), 3, RoundingMode.HALF_UP)
            }
        }

    /**
     * 장타율 (SLG) = 총 루타 / 타수
     */
    val sluggingPercentage: BigDecimal
        get() =
            if (atBats == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(totalBases).divide(BigDecimal(atBats), 3, RoundingMode.HALF_UP)
            }

    /**
     * OPS = 출루율 + 장타율
     */
    val ops: BigDecimal
        get() = onBasePercentage.add(sluggingPercentage).setScale(3, RoundingMode.HALF_UP)

    /**
     * 도루 성공률 = 도루 / (도루 + 도루 실패)
     */
    val stolenBasePercentage: BigDecimal
        get() {
            val attempts = stolenBases + caughtStealing
            return if (attempts == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(stolenBases).divide(BigDecimal(attempts), 3, RoundingMode.HALF_UP)
            }
        }

    // Business logic

    /**
     * 경기 타격 기록을 누적합니다.
     */
    fun addGameRecord(record: BattingRecord) {
        gamesPlayed++
        plateAppearances += record.plateAppearances
        atBats += record.atBats
        hits += record.hits
        doubles += record.doubles
        triples += record.triples
        homeRuns += record.homeRuns
        runs += record.runs
        runsBattedIn += record.runsBattedIn
        walks += record.walks
        intentionalWalks += record.intentionalWalks
        hitByPitch += record.hitByPitch
        strikeouts += record.strikeouts
        sacrificeBunts += record.sacrificeBunts
        sacrificeFlies += record.sacrificeFlies
        stolenBases += record.stolenBases
        caughtStealing += record.caughtStealing
        groundedIntoDoublePlays += record.groundedIntoDoublePlays
    }

    /**
     * 기록 유효성을 검증합니다.
     */
    fun validate() {
        if (gamesPlayed < 0) {
            throw StatsValidationException("출전 경기 수는 0 이상이어야 합니다.")
        }
        if (hits > atBats) {
            throw StatsValidationException("안타 수($hits)가 타수($atBats)보다 클 수 없습니다.")
        }
        if (doubles + triples + homeRuns > hits) {
            throw StatsValidationException("2루타($doubles) + 3루타($triples) + 홈런($homeRuns)이 총 안타 수($hits)보다 클 수 없습니다.")
        }
        if (atBats > plateAppearances) {
            throw StatsValidationException("타수($atBats)가 타석($plateAppearances)보다 클 수 없습니다.")
        }
    }

    companion object {
        /**
         * 선수의 시즌 타격 통계를 생성합니다.
         */
        fun create(
            player: Player,
            year: Int,
        ): SeasonBattingStats {
            if (year <= 0) {
                throw StatsValidationException("연도는 양수여야 합니다.")
            }
            return SeasonBattingStats(player = player, year = year)
        }
    }
}
