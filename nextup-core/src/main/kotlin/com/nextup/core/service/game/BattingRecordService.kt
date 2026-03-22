package com.nextup.core.service.game

import com.nextup.common.exception.BattingRecordNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundByGameAndPlayerException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 타격 기록 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class BattingRecordService(
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
) {
    /**
     * gameId와 playerId로 타격 기록을 생성합니다.
     */
    @Transactional
    fun createRecordByGameAndPlayer(
        gameId: Long,
        playerId: Long,
    ): BattingRecord {
        val gamePlayer =
            gamePlayerRepository.findByGameIdAndPlayerId(gameId, playerId)
                ?: throw GamePlayerNotFoundByGameAndPlayerException(gameId, playerId)
        return createRecord(gamePlayer.id)
    }

    /**
     * 타격 기록을 생성합니다.
     */
    @Transactional
    fun createRecord(gamePlayerId: Long): BattingRecord {
        val gamePlayer =
            gamePlayerRepository.findByIdOrNull(gamePlayerId)
                ?: throw GamePlayerNotFoundException(gamePlayerId)

        // 중복 체크
        battingRecordRepository.findByGamePlayer(gamePlayer)?.let {
            throw RecordAlreadyExistsException(gamePlayerId, "Batting")
        }

        val battingRecord = BattingRecord.create(gamePlayer)
        return battingRecordRepository.save(battingRecord)
    }

    /**
     * GamePlayer ID로 타격 기록을 조회합니다.
     */
    fun getByGamePlayerId(gamePlayerId: Long): BattingRecord =
        battingRecordRepository.findByGamePlayerId(gamePlayerId)
            ?: throw BattingRecordNotFoundException(gamePlayerId)

    /**
     * 타석 결과를 기록합니다.
     *
     * 홈런은 [applyPlateAppearanceResult]에서 득점이 자동 처리됩니다.
     * 주루 중 별도 득점이 있으면 [runsScored] 플래그로 [recordRun]을 추가 호출합니다.
     */
    @Transactional
    fun recordPlateAppearance(
        gamePlayerId: Long,
        result: PlateAppearanceResult,
        runsBattedIn: Int = 0,
        runsScored: Boolean = false,
    ) {
        val battingRecord = getByGamePlayerId(gamePlayerId)
        battingRecord.applyPlateAppearanceResult(result, runsBattedIn)
        if (runsScored) {
            battingRecord.recordRun()
        }
    }

    /**
     * 도루를 기록합니다.
     */
    @Transactional
    fun recordStolenBase(gamePlayerId: Long) {
        val battingRecord = getByGamePlayerId(gamePlayerId)
        battingRecord.recordStolenBase()
    }

    /**
     * 도루 실패를 기록합니다.
     */
    @Transactional
    fun recordCaughtStealing(gamePlayerId: Long) {
        val battingRecord = getByGamePlayerId(gamePlayerId)
        battingRecord.recordCaughtStealing()
    }

    /**
     * 득점을 기록합니다 (타석 결과와 별개로 주루 중 득점).
     */
    @Transactional
    fun recordRun(gamePlayerId: Long) {
        val battingRecord = getByGamePlayerId(gamePlayerId)
        battingRecord.recordRun()
    }

    /**
     * 경기 ID로 모든 타격 기록을 조회합니다.
     */
    fun getAllByGameId(gameId: Long): List<BattingRecord> = battingRecordRepository.findAllByGameId(gameId)

    /**
     * 선수 ID로 모든 타격 기록을 조회합니다.
     */
    fun getAllByPlayerId(playerId: Long): List<BattingRecord> = battingRecordRepository.findAllByPlayerId(playerId)
}
