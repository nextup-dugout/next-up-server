package com.nextup.core.service.stats

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 선수 통계 서비스
 *
 * 선수의 시즌/통산 타격/투수 통계를 조회하고 갱신합니다.
 */
@Service
@Transactional(readOnly = true)
class PlayerStatsService(
    private val playerRepository: PlayerRepositoryPort,
    private val seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort,
    private val seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort,
    private val careerBattingStatsRepository: CareerBattingStatsRepositoryPort,
    private val careerPitchingStatsRepository: CareerPitchingStatsRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
) {
    // ========== 조회 메서드 ==========

    /**
     * 시즌 타격 통계를 조회합니다.
     *
     * @throws IllegalArgumentException 통계가 존재하지 않을 때
     */
    fun getSeasonBattingStats(
        playerId: Long,
        year: Int,
    ): SeasonBattingStats =
        seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year)
            ?: throw IllegalArgumentException("선수 ID $playerId 의 ${year}년도 타격 통계가 존재하지 않습니다.")

    /**
     * 시즌 투수 통계를 조회합니다.
     *
     * @throws IllegalArgumentException 통계가 존재하지 않을 때
     */
    fun getSeasonPitchingStats(
        playerId: Long,
        year: Int,
    ): SeasonPitchingStats =
        seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year)
            ?: throw IllegalArgumentException("선수 ID $playerId 의 ${year}년도 투수 통계가 존재하지 않습니다.")

    /**
     * 통산 타격 통계를 조회합니다.
     *
     * @throws IllegalArgumentException 통계가 존재하지 않을 때
     */
    fun getCareerBattingStats(playerId: Long): CareerBattingStats =
        careerBattingStatsRepository.findByPlayerId(playerId)
            ?: throw IllegalArgumentException("선수 ID $playerId 의 통산 타격 통계가 존재하지 않습니다.")

    /**
     * 통산 투수 통계를 조회합니다.
     *
     * @throws IllegalArgumentException 통계가 존재하지 않을 때
     */
    fun getCareerPitchingStats(playerId: Long): CareerPitchingStats =
        careerPitchingStatsRepository.findByPlayerId(playerId)
            ?: throw IllegalArgumentException("선수 ID $playerId 의 통산 투수 통계가 존재하지 않습니다.")

    /**
     * 선수의 모든 시즌 타격 통계를 조회합니다.
     */
    fun getAllSeasonBattingStats(playerId: Long): List<SeasonBattingStats> =
        seasonBattingStatsRepository.findAllByPlayerId(playerId)

    /**
     * 선수의 모든 시즌 투수 통계를 조회합니다.
     */
    fun getAllSeasonPitchingStats(playerId: Long): List<SeasonPitchingStats> =
        seasonPitchingStatsRepository.findAllByPlayerId(playerId)

    // ========== 통계 갱신 메서드 ==========

    /**
     * 선수의 시즌 타격 통계를 갱신합니다.
     * 해당 시즌의 모든 타격 기록을 조회하여 재계산합니다.
     *
     * @return 갱신된 시즌 타격 통계
     */
    @Transactional
    fun updatePlayerBattingStats(
        playerId: Long,
        year: Int,
    ): SeasonBattingStats {
        val player = findPlayer(playerId)

        // 시즌 통계 조회 또는 생성
        val seasonStats =
            seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year)
                ?: SeasonBattingStats.create(player, year)

        // 기존 통계 초기화 (새로 계산하기 위해)
        resetSeasonBattingStats(seasonStats)

        // 해당 시즌의 모든 타격 기록 조회
        val battingRecords = battingRecordRepository.findAllByPlayerIdAndYear(playerId, year)

        // 기록 누적
        battingRecords.forEach { record ->
            seasonStats.addGameRecord(record)
        }

        // 유효성 검증
        seasonStats.validate()

        return seasonBattingStatsRepository.save(seasonStats)
    }

    /**
     * 선수의 시즌 투수 통계를 갱신합니다.
     * 해당 시즌의 모든 투수 기록을 조회하여 재계산합니다.
     *
     * @return 갱신된 시즌 투수 통계
     */
    @Transactional
    fun updatePlayerPitchingStats(
        playerId: Long,
        year: Int,
    ): SeasonPitchingStats {
        val player = findPlayer(playerId)

        // 시즌 통계 조회 또는 생성
        val seasonStats =
            seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year)
                ?: SeasonPitchingStats.create(player, year)

        // 기존 통계 초기화 (새로 계산하기 위해)
        resetSeasonPitchingStats(seasonStats)

        // 해당 시즌의 모든 투수 기록 조회
        val pitchingRecords = pitchingRecordRepository.findAllByPlayerIdAndYear(playerId, year)

        // 기록 누적
        pitchingRecords.forEach { record ->
            seasonStats.addGameRecord(record)
        }

        // 유효성 검증
        seasonStats.validate()

        return seasonPitchingStatsRepository.save(seasonStats)
    }

    /**
     * 선수의 통산 타격 통계를 갱신합니다.
     * 모든 시즌의 타격 기록을 조회하여 재계산합니다.
     *
     * @return 갱신된 통산 타격 통계
     */
    @Transactional
    fun updateCareerBattingStats(playerId: Long): CareerBattingStats {
        val player = findPlayer(playerId)

        // 통산 통계 조회 또는 생성
        val careerStats =
            careerBattingStatsRepository.findByPlayerId(playerId)
                ?: CareerBattingStats.create(player)

        // 기존 통계 초기화 (새로 계산하기 위해)
        resetCareerBattingStats(careerStats)

        // 모든 타격 기록 조회 (FETCH JOIN으로 N+1 방지)
        val battingRecords = battingRecordRepository.findAllByPlayerIdWithGameInfo(playerId)

        // 기록 누적
        battingRecords.forEach { record ->
            careerStats.addGameRecord(record)
        }

        // 시즌 수 계산 (년도별 그룹핑)
        val seasons =
            battingRecords
                .map { record -> record.gamePlayer.gameTeam.game.scheduledAt.year }
                .distinct()
                .size

        // 시즌 수 업데이트
        repeat(seasons - careerStats.seasonsPlayed) {
            careerStats.addSeason()
        }

        // 유효성 검증
        careerStats.validate()

        return careerBattingStatsRepository.save(careerStats)
    }

    /**
     * 선수의 통산 투수 통계를 갱신합니다.
     * 모든 시즌의 투수 기록을 조회하여 재계산합니다.
     *
     * @return 갱신된 통산 투수 통계
     */
    @Transactional
    fun updateCareerPitchingStats(playerId: Long): CareerPitchingStats {
        val player = findPlayer(playerId)

        // 통산 통계 조회 또는 생성
        val careerStats =
            careerPitchingStatsRepository.findByPlayerId(playerId)
                ?: CareerPitchingStats.create(player)

        // 기존 통계 초기화 (새로 계산하기 위해)
        resetCareerPitchingStats(careerStats)

        // 모든 투수 기록 조회 (FETCH JOIN으로 N+1 방지)
        val pitchingRecords = pitchingRecordRepository.findAllByPlayerIdWithGameInfo(playerId)

        // 기록 누적
        pitchingRecords.forEach { record ->
            careerStats.addGameRecord(record)
        }

        // 시즌 수 계산 (년도별 그룹핑)
        val seasons =
            pitchingRecords
                .map { record -> record.gamePlayer.gameTeam.game.scheduledAt.year }
                .distinct()
                .size

        // 시즌 수 업데이트
        repeat(seasons - careerStats.seasonsPlayed) {
            careerStats.addSeason()
        }

        // 유효성 검증
        careerStats.validate()

        return careerPitchingStatsRepository.save(careerStats)
    }

    // ========== 대회별 통계 조회 메서드 ==========

    /**
     * 선수의 대회별 타격 통계를 조회합니다.
     */
    fun getBattingStatsByCompetition(
        playerId: Long,
        competitionId: Long,
    ): CompetitionBattingStatsDto {
        val records = battingRecordRepository.findAllByPlayerIdAndCompetitionId(playerId, competitionId)

        var gamesPlayed = 0
        var plateAppearances = 0
        var atBats = 0
        var hits = 0
        var doubles = 0
        var triples = 0
        var homeRuns = 0
        var runs = 0
        var runsBattedIn = 0
        var walks = 0
        var strikeouts = 0
        var stolenBases = 0

        records.forEach { record ->
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
            strikeouts += record.strikeouts
            stolenBases += record.stolenBases
        }

        val battingAverage =
            if (atBats > 0) {
                java.math.BigDecimal(hits).divide(
                    java.math.BigDecimal(atBats),
                    3,
                    java.math.RoundingMode.HALF_UP,
                )
            } else {
                java.math.BigDecimal.ZERO
            }

        val onBasePercentage =
            if (atBats + walks > 0) {
                java.math.BigDecimal(hits + walks).divide(
                    java.math.BigDecimal(atBats + walks),
                    3,
                    java.math.RoundingMode.HALF_UP,
                )
            } else {
                java.math.BigDecimal.ZERO
            }

        val totalBases = hits + doubles + triples * 2 + homeRuns * 3
        val sluggingPercentage =
            if (atBats > 0) {
                java.math.BigDecimal(totalBases).divide(
                    java.math.BigDecimal(atBats),
                    3,
                    java.math.RoundingMode.HALF_UP,
                )
            } else {
                java.math.BigDecimal.ZERO
            }

        val ops = onBasePercentage.add(sluggingPercentage)

        return CompetitionBattingStatsDto(
            playerId = playerId,
            competitionId = competitionId,
            gamesPlayed = gamesPlayed,
            plateAppearances = plateAppearances,
            atBats = atBats,
            hits = hits,
            doubles = doubles,
            triples = triples,
            homeRuns = homeRuns,
            runs = runs,
            runsBattedIn = runsBattedIn,
            walks = walks,
            strikeouts = strikeouts,
            stolenBases = stolenBases,
            battingAverage = battingAverage.toPlainString(),
            onBasePercentage = onBasePercentage.toPlainString(),
            sluggingPercentage = sluggingPercentage.toPlainString(),
            ops = ops.toPlainString(),
        )
    }

    /**
     * 선수의 대회별 투수 통계를 조회합니다.
     */
    fun getPitchingStatsByCompetition(
        playerId: Long,
        competitionId: Long,
    ): CompetitionPitchingStatsDto {
        val records = pitchingRecordRepository.findAllByPlayerIdAndCompetitionId(playerId, competitionId)

        var gamesPlayed = 0
        var gamesStarted = 0
        var inningsPitchedOuts = 0
        var wins = 0
        var losses = 0
        var saves = 0
        var holds = 0
        var earnedRuns = 0
        var hitsAllowed = 0
        var walksAllowed = 0
        var strikeouts = 0
        var homeRunsAllowed = 0

        records.forEach { record ->
            gamesPlayed++
            if (record.gamePlayer.isStarter) gamesStarted++
            inningsPitchedOuts += record.inningsPitchedOuts
            wins += if (record.decision == com.nextup.core.domain.game.PitchingDecision.WIN) 1 else 0
            losses += if (record.decision == com.nextup.core.domain.game.PitchingDecision.LOSS) 1 else 0
            saves += if (record.decision == com.nextup.core.domain.game.PitchingDecision.SAVE) 1 else 0
            holds += if (record.decision == com.nextup.core.domain.game.PitchingDecision.HOLD) 1 else 0
            earnedRuns += record.earnedRuns
            hitsAllowed += record.hitsAllowed
            walksAllowed += record.walksAllowed
            strikeouts += record.strikeouts
            homeRunsAllowed += record.homeRunsAllowed
        }

        val completeInnings = inningsPitchedOuts / 3
        val remainingOuts = inningsPitchedOuts % 3
        val inningsPitchedDisplay =
            if (remainingOuts == 0) "$completeInnings" else "$completeInnings.$remainingOuts"

        val era =
            if (inningsPitchedOuts > 0) {
                java.math.BigDecimal(earnedRuns * 27).divide(
                    java.math.BigDecimal(inningsPitchedOuts),
                    2,
                    java.math.RoundingMode.HALF_UP,
                ).toPlainString()
            } else {
                null
            }

        val whip =
            if (inningsPitchedOuts > 0) {
                java.math.BigDecimal((hitsAllowed + walksAllowed) * 3).divide(
                    java.math.BigDecimal(inningsPitchedOuts),
                    2,
                    java.math.RoundingMode.HALF_UP,
                )
            } else {
                java.math.BigDecimal.ZERO
            }

        return CompetitionPitchingStatsDto(
            playerId = playerId,
            competitionId = competitionId,
            gamesPlayed = gamesPlayed,
            gamesStarted = gamesStarted,
            inningsPitchedDisplay = inningsPitchedDisplay,
            wins = wins,
            losses = losses,
            saves = saves,
            holds = holds,
            earnedRuns = earnedRuns,
            hitsAllowed = hitsAllowed,
            walksAllowed = walksAllowed,
            strikeouts = strikeouts,
            homeRunsAllowed = homeRunsAllowed,
            earnedRunAverage = era,
            whip = whip.toPlainString(),
        )
    }

    // ========== 리더보드 조회 메서드 ==========

    /**
     * 시즌 타격 리더보드를 조회합니다.
     *
     * @param year 시즌 연도
     * @param category 카테고리 (AVG, HR, RBI, OPS 등)
     * @param minAtBats 최소 타수 (타율, OPS 등에 적용)
     * @param limit 조회 제한 수
     */
    fun getSeasonBattingLeaders(
        year: Int,
        category: BattingCategory,
        minAtBats: Int = 50,
        limit: Int = 10,
    ): List<SeasonBattingStats> =
        when (category) {
            BattingCategory.AVG -> seasonBattingStatsRepository.findTopByBattingAverage(year, minAtBats, limit)
            BattingCategory.HR -> seasonBattingStatsRepository.findTopByHomeRuns(year, limit)
            BattingCategory.RBI -> seasonBattingStatsRepository.findTopByRunsBattedIn(year, limit)
            BattingCategory.OPS -> seasonBattingStatsRepository.findTopByOps(year, minAtBats, limit)
        }

    /**
     * 시즌 투수 리더보드를 조회합니다.
     *
     * @param year 시즌 연도
     * @param category 카테고리 (ERA, WINS, STRIKEOUTS, SAVES, WHIP 등)
     * @param minInnings 최소 이닝 (ERA, WHIP 등에 적용, 아웃 수로 환산: 이닝 * 3)
     * @param limit 조회 제한 수
     */
    fun getSeasonPitchingLeaders(
        year: Int,
        category: PitchingCategory,
        minInnings: Int = 15,
        limit: Int = 10,
    ): List<SeasonPitchingStats> {
        val minInningsPitchedOuts = minInnings * 3

        return when (category) {
            PitchingCategory.ERA -> seasonPitchingStatsRepository.findTopByEra(year, minInningsPitchedOuts, limit)
            PitchingCategory.WINS -> seasonPitchingStatsRepository.findTopByWins(year, limit)
            PitchingCategory.STRIKEOUTS -> seasonPitchingStatsRepository.findTopByStrikeouts(year, limit)
            PitchingCategory.SAVES -> seasonPitchingStatsRepository.findTopBySaves(year, limit)
            PitchingCategory.WHIP -> seasonPitchingStatsRepository.findTopByWhip(year, minInningsPitchedOuts, limit)
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun findPlayer(playerId: Long): Player =
        playerRepository.findByIdOrNull(playerId)
            ?: throw IllegalArgumentException("선수 ID $playerId 를 찾을 수 없습니다.")

    /**
     * 시즌 타격 통계를 초기화합니다 (Reflection 사용하여 protected setter 접근).
     */
    private fun resetSeasonBattingStats(stats: SeasonBattingStats) {
        val clazz = SeasonBattingStats::class.java

        listOf(
            "gamesPlayed",
            "plateAppearances",
            "atBats",
            "hits",
            "doubles",
            "triples",
            "homeRuns",
            "runs",
            "runsBattedIn",
            "walks",
            "intentionalWalks",
            "hitByPitch",
            "strikeouts",
            "sacrificeBunts",
            "sacrificeFlies",
            "stolenBases",
            "caughtStealing",
            "groundedIntoDoublePlays",
        ).forEach { fieldName ->
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(stats, 0)
        }
    }

    /**
     * 시즌 투수 통계를 초기화합니다 (Reflection 사용하여 protected setter 접근).
     */
    private fun resetSeasonPitchingStats(stats: SeasonPitchingStats) {
        val clazz = SeasonPitchingStats::class.java

        listOf(
            "gamesPlayed",
            "gamesStarted",
            "inningsPitchedOuts",
            "wins",
            "losses",
            "saves",
            "holds",
            "blownSaves",
            "earnedRuns",
            "runsAllowed",
            "hitsAllowed",
            "walksAllowed",
            "strikeouts",
            "homeRunsAllowed",
            "hitBatsmen",
            "wildPitches",
            "balks",
            "battersFaced",
        ).forEach { fieldName ->
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(stats, 0)
        }

        // nullable 필드 처리
        listOf("pitchesThrown", "strikesThrown").forEach { fieldName ->
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(stats, null)
        }
    }

    /**
     * 통산 타격 통계를 초기화합니다 (Reflection 사용하여 protected setter 접근).
     */
    private fun resetCareerBattingStats(stats: CareerBattingStats) {
        val clazz = CareerBattingStats::class.java

        listOf(
            "seasonsPlayed",
            "gamesPlayed",
            "plateAppearances",
            "atBats",
            "hits",
            "doubles",
            "triples",
            "homeRuns",
            "runs",
            "runsBattedIn",
            "walks",
            "intentionalWalks",
            "hitByPitch",
            "strikeouts",
            "sacrificeBunts",
            "sacrificeFlies",
            "stolenBases",
            "caughtStealing",
            "groundedIntoDoublePlays",
        ).forEach { fieldName ->
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(stats, 0)
        }
    }

    /**
     * 통산 투수 통계를 초기화합니다 (Reflection 사용하여 protected setter 접근).
     */
    private fun resetCareerPitchingStats(stats: CareerPitchingStats) {
        val clazz = CareerPitchingStats::class.java

        listOf(
            "seasonsPlayed",
            "gamesPlayed",
            "gamesStarted",
            "inningsPitchedOuts",
            "wins",
            "losses",
            "saves",
            "holds",
            "blownSaves",
            "earnedRuns",
            "runsAllowed",
            "hitsAllowed",
            "walksAllowed",
            "strikeouts",
            "homeRunsAllowed",
            "hitBatsmen",
            "wildPitches",
            "balks",
            "battersFaced",
        ).forEach { fieldName ->
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(stats, 0)
        }

        // nullable 필드 처리
        listOf("pitchesThrown", "strikesThrown").forEach { fieldName ->
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(stats, null)
        }
    }
}

/**
 * 타격 통계 카테고리
 */
enum class BattingCategory {
    AVG, // 타율
    HR, // 홈런
    RBI, // 타점
    OPS, // OPS
}

/**
 * 투수 통계 카테고리
 */
enum class PitchingCategory {
    ERA, // 자책점
    WINS, // 승수
    STRIKEOUTS, // 삼진
    SAVES, // 세이브
    WHIP, // WHIP
}
