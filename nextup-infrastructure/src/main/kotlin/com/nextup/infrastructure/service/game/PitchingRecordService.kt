package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.PitchingRecordNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.infrastructure.repository.game.GamePlayerRepository
import com.nextup.infrastructure.repository.game.PitchingRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 투수 기록 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class PitchingRecordService(
    private val pitchingRecordRepository: PitchingRecordRepository,
    private val gamePlayerRepository: GamePlayerRepository
) {

    /**
     * 투수 기록을 생성합니다.
     */
    @Transactional
    fun createRecord(gamePlayerId: Long, isStartingPitcher: Boolean = false): PitchingRecord {
        val gamePlayer = gamePlayerRepository.findById(gamePlayerId)
            .orElseThrow { GamePlayerNotFoundException(gamePlayerId) }

        // 중복 체크
        pitchingRecordRepository.findByGamePlayer(gamePlayer)?.let {
            throw RecordAlreadyExistsException(gamePlayerId, "Pitching")
        }

        val pitchingRecord = PitchingRecord.create(gamePlayer, isStartingPitcher)
        return pitchingRecordRepository.save(pitchingRecord)
    }

    /**
     * GamePlayer ID로 투수 기록을 조회합니다.
     */
    fun getByGamePlayerId(gamePlayerId: Long): PitchingRecord {
        return pitchingRecordRepository.findByGamePlayerId(gamePlayerId)
            ?: throw PitchingRecordNotFoundException(gamePlayerId)
    }

    /**
     * 아웃을 기록합니다.
     */
    @Transactional
    fun recordOut(gamePlayerId: Long, isStrikeout: Boolean = false) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordOut(isStrikeout)
    }

    /**
     * 피안타를 기록합니다.
     */
    @Transactional
    fun recordHit(
        gamePlayerId: Long,
        isHomeRun: Boolean = false,
        runsScored: Int = 0,
        earnedRuns: Int = 0
    ) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordHit(isHomeRun, runsScored, earnedRuns)
    }

    /**
     * 볼넷을 기록합니다.
     */
    @Transactional
    fun recordWalk(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordWalk()
    }

    /**
     * 사구를 기록합니다.
     */
    @Transactional
    fun recordHitByPitch(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordHitByPitch()
    }

    /**
     * 실점을 기록합니다 (주루 중 득점 등).
     */
    @Transactional
    fun recordRun(gamePlayerId: Long, isEarned: Boolean = true) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordRun(isEarned)
    }

    /**
     * 와일드피치를 기록합니다.
     */
    @Transactional
    fun recordWildPitch(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordWildPitch()
    }

    /**
     * 보크를 기록합니다.
     */
    @Transactional
    fun recordBalk(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordBalk()
    }

    /**
     * 투구 수를 기록합니다.
     */
    @Transactional
    fun recordPitchCount(gamePlayerId: Long, totalPitches: Int, strikes: Int) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.recordPitchCount(totalPitches, strikes)
    }

    /**
     * 승리 결정을 부여합니다.
     */
    @Transactional
    fun assignWin(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.assignWin()
    }

    /**
     * 패배 결정을 부여합니다.
     */
    @Transactional
    fun assignLoss(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.assignLoss()
    }

    /**
     * 세이브 결정을 부여합니다.
     */
    @Transactional
    fun assignSave(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.assignSave()
    }

    /**
     * 홀드 결정을 부여합니다.
     */
    @Transactional
    fun assignHold(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.assignHold()
    }

    /**
     * 블론세이브를 기록합니다.
     */
    @Transactional
    fun assignBlownSave(gamePlayerId: Long) {
        val pitchingRecord = getByGamePlayerId(gamePlayerId)
        pitchingRecord.assignBlownSave()
    }

    /**
     * 경기의 선발 투수 기록을 조회합니다.
     */
    fun getStartingPitchersByGameId(gameId: Long): List<PitchingRecord> {
        return pitchingRecordRepository.findStartingPitchersByGameId(gameId)
    }

    /**
     * 경기의 구원 투수 기록을 조회합니다.
     */
    fun getReliefPitchersByGameId(gameId: Long): List<PitchingRecord> {
        return pitchingRecordRepository.findReliefPitchersByGameId(gameId)
    }

    /**
     * 경기 ID로 모든 투수 기록을 조회합니다.
     */
    fun getAllByGameId(gameId: Long): List<PitchingRecord> {
        return pitchingRecordRepository.findAllByGameId(gameId)
    }

    /**
     * 선수 ID로 모든 투수 기록을 조회합니다.
     */
    fun getAllByPlayerId(playerId: Long): List<PitchingRecord> {
        return pitchingRecordRepository.findAllByPlayerId(playerId)
    }
}
