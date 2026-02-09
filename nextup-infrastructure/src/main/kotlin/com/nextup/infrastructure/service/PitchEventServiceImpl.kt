package com.nextup.infrastructure.service

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.common.exception.PitchEventNotFoundException
import com.nextup.core.domain.game.PitchEvent
import com.nextup.core.domain.game.PitchResult
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchEventRepositoryPort
import com.nextup.core.service.BatterPitchStats
import com.nextup.core.service.PitchEventService
import com.nextup.core.service.PitcherPitchStats
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PitchEventServiceImpl(
    private val pitchEventRepository: PitchEventRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
) : PitchEventService {
    @Transactional
    override fun recordPitch(
        gameId: Long,
        pitcherId: Long,
        batterId: Long,
        result: PitchResult,
        description: String?,
    ): PitchEvent {
        val game = gameRepository.findByIdOrNull(gameId) ?: throw GameNotFoundException(gameId)
        require(game.status.isOngoing()) { throw InvalidGameStateException("진행 중인 경기에서만 투구를 기록할 수 있습니다.") }

        val pitcher = gamePlayerRepository.findByIdOrNull(pitcherId) ?: throw GamePlayerNotFoundException(pitcherId)
        val batter = gamePlayerRepository.findByIdOrNull(batterId) ?: throw GamePlayerNotFoundException(batterId)

        // 현재 볼카운트 조회
        val currentBalls = game.gameState.balls
        val currentStrikes = game.gameState.strikes

        // 투구 결과에 따른 새로운 볼카운트 계산
        val newBalls =
            if (result.incrementsBall) {
                (currentBalls + 1).coerceAtMost(4)
            } else {
                currentBalls
            }

        val newStrikes =
            when {
                result.incrementsStrike -> (currentStrikes + 1).coerceAtMost(3)
                result.isFoul && currentStrikes < 2 -> currentStrikes + 1
                else -> currentStrikes
            }

        // 다음 투구 번호 조회
        val pitchNumber = pitchEventRepository.getNextPitchNumber(gameId)

        // 투구 이벤트 생성
        val pitchEvent =
            PitchEvent.create(
                game = game,
                pitcher = pitcher,
                batter = batter,
                pitchNumber = pitchNumber,
                result = result,
                ballCount = newBalls,
                strikeCount = newStrikes,
                description = description,
            )

        return pitchEventRepository.save(pitchEvent)
    }

    override fun getPitchEvent(pitchEventId: Long): PitchEvent =
        pitchEventRepository.findByIdOrNull(pitchEventId) ?: throw PitchEventNotFoundException(pitchEventId)

    override fun getGamePitchEvents(gameId: Long): List<PitchEvent> {
        // 경기 존재 확인
        gameRepository.findByIdOrNull(gameId) ?: throw GameNotFoundException(gameId)
        return pitchEventRepository.findByGameId(gameId)
    }

    override fun getInningPitchEvents(
        gameId: Long,
        inning: Int,
        isTopInning: Boolean,
    ): List<PitchEvent> {
        // 경기 존재 확인
        gameRepository.findByIdOrNull(gameId) ?: throw GameNotFoundException(gameId)
        return pitchEventRepository.findByGameIdAndInning(gameId, inning, isTopInning)
    }

    override fun getCurrentBallCount(gameId: Long): Pair<Int, Int> {
        val game = gameRepository.findByIdOrNull(gameId) ?: throw GameNotFoundException(gameId)
        return Pair(game.gameState.balls, game.gameState.strikes)
    }

    override fun calculatePitcherStats(pitcherId: Long): PitcherPitchStats {
        // 투수 존재 확인
        gamePlayerRepository.findByIdOrNull(pitcherId) ?: throw GamePlayerNotFoundException(pitcherId)

        val pitchEvents = pitchEventRepository.findByPitcherId(pitcherId)

        if (pitchEvents.isEmpty()) {
            return PitcherPitchStats(
                totalPitches = 0,
                strikes = 0,
                balls = 0,
                fouls = 0,
                inPlayPitches = 0,
                strikePercentage = 0.0,
                avgPitchesPerAtBat = 0.0,
            )
        }

        val totalPitches = pitchEvents.size
        val strikes = pitchEvents.count { it.result == PitchResult.STRIKE || it.result == PitchResult.SWING_MISS }
        val balls = pitchEvents.count { it.result == PitchResult.BALL }
        val fouls = pitchEvents.count { it.result == PitchResult.FOUL }
        val inPlayPitches = pitchEvents.count { it.result == PitchResult.IN_PLAY }

        // 스트라이크 비율 계산 (스트라이크 + 헛스윙 + 파울)
        val totalStrikes = strikes + fouls
        val strikePercentage =
            if (totalPitches > 0) {
                (totalStrikes.toDouble() / totalPitches.toDouble()) * 100.0
            } else {
                0.0
            }

        // 타석당 평균 투구 수 계산 (인플레이로 종료된 타석 기준)
        val atBats = inPlayPitches
        val avgPitchesPerAtBat =
            if (atBats > 0) {
                totalPitches.toDouble() / atBats.toDouble()
            } else {
                0.0
            }

        return PitcherPitchStats(
            totalPitches = totalPitches,
            strikes = strikes,
            balls = balls,
            fouls = fouls,
            inPlayPitches = inPlayPitches,
            strikePercentage = strikePercentage,
            avgPitchesPerAtBat = avgPitchesPerAtBat,
        )
    }

    override fun calculateBatterPitchStats(batterId: Long): BatterPitchStats {
        // 타자 존재 확인
        gamePlayerRepository.findByIdOrNull(batterId) ?: throw GamePlayerNotFoundException(batterId)

        val pitchEvents = pitchEventRepository.findByBatterId(batterId)

        if (pitchEvents.isEmpty()) {
            return BatterPitchStats(
                totalPitches = 0,
                strikes = 0,
                balls = 0,
                fouls = 0,
                swingAndMiss = 0,
                avgPitchesPerAtBat = 0.0,
            )
        }

        val totalPitches = pitchEvents.size
        val strikes = pitchEvents.count { it.result == PitchResult.STRIKE }
        val balls = pitchEvents.count { it.result == PitchResult.BALL }
        val fouls = pitchEvents.count { it.result == PitchResult.FOUL }
        val swingAndMiss = pitchEvents.count { it.result == PitchResult.SWING_MISS }

        // 타석당 평균 투구 수 계산 (인플레이로 종료된 타석 기준)
        val atBats = pitchEvents.count { it.result == PitchResult.IN_PLAY }
        val avgPitchesPerAtBat =
            if (atBats > 0) {
                totalPitches.toDouble() / atBats.toDouble()
            } else {
                0.0
            }

        return BatterPitchStats(
            totalPitches = totalPitches,
            strikes = strikes,
            balls = balls,
            fouls = fouls,
            swingAndMiss = swingAndMiss,
            avgPitchesPerAtBat = avgPitchesPerAtBat,
        )
    }
}
