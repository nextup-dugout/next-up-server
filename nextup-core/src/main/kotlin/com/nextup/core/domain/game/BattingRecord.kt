package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 타격 기록 엔티티
 *
 * 경기 출전 선수(GamePlayer)의 타격 기록을 저장합니다.
 * 한 경기에서 선수당 하나의 타격 기록만 존재합니다.
 */
@Entity
@Table(
    name = "batting_records",
    indexes = [
        Index(name = "idx_batting_records_game_player", columnList = "game_player_id"),
    ],
)
class BattingRecord(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id", nullable = false, unique = true)
    val gamePlayer: GamePlayer,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
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

    // Calculated properties

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
     * 타석 결과에 따른 기록을 갱신합니다.
     *
     * 모든 타석 결과를 망라하는 단일 정규 메서드입니다.
     * 홈런은 타자 자신의 득점(runs)도 자동으로 포함됩니다.
     * 주루 중 별도 득점은 [recordRun]을 사용하세요.
     */
    fun applyPlateAppearanceResult(
        result: PlateAppearanceResult,
        rbis: Int = 0,
    ) {
        require(rbis >= 0) { "타점은 0 이상이어야 합니다." }

        plateAppearances++

        when (result) {
            PlateAppearanceResult.SINGLE -> {
                atBats++
                hits++
            }
            PlateAppearanceResult.DOUBLE -> {
                atBats++
                hits++
                doubles++
            }
            PlateAppearanceResult.TRIPLE -> {
                atBats++
                hits++
                triples++
            }
            PlateAppearanceResult.HOME_RUN -> {
                atBats++
                hits++
                homeRuns++
                runs++ // 홈런은 타자 자신도 득점
            }
            PlateAppearanceResult.STRIKEOUT -> {
                atBats++
                strikeouts++
            }
            PlateAppearanceResult.GROUND_OUT,
            PlateAppearanceResult.FLY_OUT,
            PlateAppearanceResult.LINE_OUT,
            PlateAppearanceResult.FIELDERS_CHOICE,
            PlateAppearanceResult.ERROR,
            -> {
                atBats++
            }
            PlateAppearanceResult.WALK -> {
                walks++
            }
            PlateAppearanceResult.INTENTIONAL_WALK -> {
                intentionalWalks++
            }
            PlateAppearanceResult.HIT_BY_PITCH -> {
                hitByPitch++
            }
            PlateAppearanceResult.SACRIFICE_FLY -> {
                sacrificeFlies++
            }
            PlateAppearanceResult.SACRIFICE_BUNT -> {
                sacrificeBunts++
            }
            PlateAppearanceResult.DOUBLE_PLAY,
            PlateAppearanceResult.TRIPLE_PLAY,
            -> {
                atBats++
                groundedIntoDoublePlays++
            }
            PlateAppearanceResult.INTERFERENCE -> {
                // 방해는 타수에 포함되지 않음
            }
        }

        this.runsBattedIn += rbis
    }

    /**
     * 도루를 기록합니다.
     */
    fun recordStolenBase() {
        stolenBases++
    }

    /**
     * 도루 실패를 기록합니다.
     */
    fun recordCaughtStealing() {
        caughtStealing++
    }

    /**
     * 득점을 기록합니다 (타석 결과와 별개로 주루 중 득점).
     */
    fun recordRun() {
        runs++
    }

    /**
     * 타석 결과를 역방향으로 롤백합니다 (Undo용).
     * applyPlateAppearanceResult의 역연산입니다.
     */
    fun revertPlateAppearanceResult(
        result: PlateAppearanceResult,
        rbis: Int = 0,
    ) {
        require(plateAppearances > 0) { "롤백할 타석 기록이 없습니다." }
        require(rbis >= 0) { "타점은 0 이상이어야 합니다." }
        require(runsBattedIn >= rbis) { "롤백할 타점($rbis)이 현재 타점($runsBattedIn)보다 클 수 없습니다." }

        plateAppearances--

        when (result) {
            PlateAppearanceResult.SINGLE -> {
                require(atBats > 0) { "롤백할 타수 기록이 없습니다." }
                require(hits > 0) { "롤백할 안타 기록이 없습니다." }
                atBats--
                hits--
            }
            PlateAppearanceResult.DOUBLE -> {
                require(atBats > 0) { "롤백할 타수 기록이 없습니다." }
                require(hits > 0) { "롤백할 안타 기록이 없습니다." }
                require(doubles > 0) { "롤백할 2루타 기록이 없습니다." }
                atBats--
                hits--
                doubles--
            }
            PlateAppearanceResult.TRIPLE -> {
                require(atBats > 0) { "롤백할 타수 기록이 없습니다." }
                require(hits > 0) { "롤백할 안타 기록이 없습니다." }
                require(triples > 0) { "롤백할 3루타 기록이 없습니다." }
                atBats--
                hits--
                triples--
            }
            PlateAppearanceResult.HOME_RUN -> {
                require(atBats > 0) { "롤백할 타수 기록이 없습니다." }
                require(hits > 0) { "롤백할 안타 기록이 없습니다." }
                require(homeRuns > 0) { "롤백할 홈런 기록이 없습니다." }
                require(runs > 0) { "롤백할 득점 기록이 없습니다." }
                atBats--
                hits--
                homeRuns--
                runs--
            }
            PlateAppearanceResult.STRIKEOUT -> {
                require(atBats > 0) { "롤백할 타수 기록이 없습니다." }
                require(strikeouts > 0) { "롤백할 삼진 기록이 없습니다." }
                atBats--
                strikeouts--
            }
            PlateAppearanceResult.GROUND_OUT,
            PlateAppearanceResult.FLY_OUT,
            PlateAppearanceResult.LINE_OUT,
            PlateAppearanceResult.FIELDERS_CHOICE,
            PlateAppearanceResult.ERROR,
            -> {
                require(atBats > 0) { "롤백할 타수 기록이 없습니다." }
                atBats--
            }
            PlateAppearanceResult.WALK -> {
                require(walks > 0) { "롤백할 볼넷 기록이 없습니다." }
                walks--
            }
            PlateAppearanceResult.INTENTIONAL_WALK -> {
                require(intentionalWalks > 0) { "롤백할 고의사구 기록이 없습니다." }
                intentionalWalks--
            }
            PlateAppearanceResult.HIT_BY_PITCH -> {
                require(hitByPitch > 0) { "롤백할 사구 기록이 없습니다." }
                hitByPitch--
            }
            PlateAppearanceResult.SACRIFICE_FLY -> {
                require(sacrificeFlies > 0) { "롤백할 희생플라이 기록이 없습니다." }
                sacrificeFlies--
            }
            PlateAppearanceResult.SACRIFICE_BUNT -> {
                require(sacrificeBunts > 0) { "롤백할 희생번트 기록이 없습니다." }
                sacrificeBunts--
            }
            PlateAppearanceResult.DOUBLE_PLAY,
            PlateAppearanceResult.TRIPLE_PLAY,
            -> {
                require(atBats > 0) { "롤백할 타수 기록이 없습니다." }
                require(groundedIntoDoublePlays > 0) { "롤백할 병살타 기록이 없습니다." }
                atBats--
                groundedIntoDoublePlays--
            }
            PlateAppearanceResult.INTERFERENCE -> {
                // 방해는 타수에 포함되지 않음
            }
        }

        this.runsBattedIn -= rbis
    }

    /**
     * 득점을 취소합니다 (Undo용).
     */
    fun revertRun() {
        require(runs > 0) { "취소할 득점이 없습니다." }
        runs--
    }

    /**
     * 기록 유효성을 검증합니다.
     */
    fun validate() {
        require(plateAppearances >= 0) { "타석($plateAppearances)은 음수일 수 없습니다." }
        require(atBats >= 0) { "타수($atBats)는 음수일 수 없습니다." }
        require(hits >= 0) { "안타($hits)는 음수일 수 없습니다." }
        require(doubles >= 0) { "2루타($doubles)는 음수일 수 없습니다." }
        require(triples >= 0) { "3루타($triples)는 음수일 수 없습니다." }
        require(homeRuns >= 0) { "홈런($homeRuns)은 음수일 수 없습니다." }
        require(runs >= 0) { "득점($runs)은 음수일 수 없습니다." }
        require(runsBattedIn >= 0) { "타점($runsBattedIn)은 음수일 수 없습니다." }
        require(walks >= 0) { "볼넷($walks)은 음수일 수 없습니다." }
        require(intentionalWalks >= 0) { "고의사구($intentionalWalks)는 음수일 수 없습니다." }
        require(hitByPitch >= 0) { "사구($hitByPitch)는 음수일 수 없습니다." }
        require(strikeouts >= 0) { "삼진($strikeouts)은 음수일 수 없습니다." }
        require(sacrificeBunts >= 0) { "희생번트($sacrificeBunts)는 음수일 수 없습니다." }
        require(sacrificeFlies >= 0) { "희생플라이($sacrificeFlies)는 음수일 수 없습니다." }
        require(stolenBases >= 0) { "도루($stolenBases)는 음수일 수 없습니다." }
        require(caughtStealing >= 0) { "도루실패($caughtStealing)는 음수일 수 없습니다." }
        require(groundedIntoDoublePlays >= 0) { "병살타($groundedIntoDoublePlays)는 음수일 수 없습니다." }
        require(hits <= atBats) {
            "안타 수($hits)가 타수($atBats)보다 클 수 없습니다."
        }
        require(doubles + triples + homeRuns <= hits) {
            "2루타($doubles) + 3루타($triples) + 홈런($homeRuns)이 총 안타 수($hits)보다 클 수 없습니다."
        }
        require(atBats <= plateAppearances) {
            "타수($atBats)가 타석($plateAppearances)보다 클 수 없습니다."
        }
    }

    companion object {
        /**
         * 경기 출전 선수의 타격 기록을 생성합니다.
         */
        fun create(gamePlayer: GamePlayer): BattingRecord = BattingRecord(gamePlayer = gamePlayer)
    }
}
