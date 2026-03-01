package com.nextup.core.service.game

import com.nextup.common.exception.FieldingRecordNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 수비 기록 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class FieldingRecordService(
    private val fieldingRecordRepository: FieldingRecordRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
) {
    /**
     * 수비 기록을 생성합니다.
     */
    @Transactional
    fun createRecord(gamePlayerId: Long): FieldingRecord {
        val gamePlayer =
            gamePlayerRepository.findByIdOrNull(gamePlayerId)
                ?: throw GamePlayerNotFoundException(gamePlayerId)

        // 중복 체크
        fieldingRecordRepository.findByGamePlayer(gamePlayer)?.let {
            throw RecordAlreadyExistsException(gamePlayerId, "Fielding")
        }

        val fieldingRecord = FieldingRecord.create(gamePlayer)
        return fieldingRecordRepository.save(fieldingRecord)
    }

    /**
     * GamePlayer ID로 수비 기록을 조회합니다.
     */
    fun getByGamePlayerId(gamePlayerId: Long): FieldingRecord =
        fieldingRecordRepository.findByGamePlayerId(gamePlayerId)
            ?: throw FieldingRecordNotFoundException(gamePlayerId)

    /**
     * 자살(PO)을 기록합니다.
     */
    @Transactional
    fun recordPutOut(gamePlayerId: Long) {
        val record = getByGamePlayerId(gamePlayerId)
        record.recordPutOut()
    }

    /**
     * 보살(A)을 기록합니다.
     */
    @Transactional
    fun recordAssist(gamePlayerId: Long) {
        val record = getByGamePlayerId(gamePlayerId)
        record.recordAssist()
    }

    /**
     * 실책(E)을 기록합니다.
     */
    @Transactional
    fun recordError(gamePlayerId: Long) {
        val record = getByGamePlayerId(gamePlayerId)
        record.recordError()
    }

    /**
     * 병살 관여(DP)를 기록합니다.
     */
    @Transactional
    fun recordDoublePlay(gamePlayerId: Long) {
        val record = getByGamePlayerId(gamePlayerId)
        record.recordDoublePlay()
    }

    /**
     * 포일(PB)을 기록합니다.
     */
    @Transactional
    fun recordPassedBall(gamePlayerId: Long) {
        val record = getByGamePlayerId(gamePlayerId)
        record.recordPassedBall()
    }

    /**
     * 경기 ID로 모든 수비 기록을 조회합니다.
     */
    fun getAllByGameId(gameId: Long): List<FieldingRecord> = fieldingRecordRepository.findAllByGameId(gameId)

    /**
     * 선수 ID로 모든 수비 기록을 조회합니다.
     */
    fun getAllByPlayerId(playerId: Long): List<FieldingRecord> = fieldingRecordRepository.findAllByPlayerId(playerId)
}
