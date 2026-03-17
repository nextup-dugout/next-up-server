package com.nextup.core.service.game

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 선수 게임 로그 서비스
 *
 * 선수의 경기별 기록(타격/투수/수비)을 통합하여 제공합니다.
 */
@Service
@Transactional(readOnly = true)
class GameLogService(
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val fieldingRecordRepository: FieldingRecordRepositoryPort,
) {
    /**
     * 선수의 최근 경기 기록을 조회합니다.
     *
     * @param playerId 선수 ID
     * @param limit 조회할 경기 수 (기본값: 10)
     * @return 경기별 기록 리스트 (최근 경기 순)
     */
    fun getGameLog(
        playerId: Long,
        limit: Int = 10,
    ): List<GameLogEntry> {
        // GamePlayer를 통해 참여한 경기 목록을 가져옴
        val gamePlayers = gamePlayerRepository.findAllByPlayerId(playerId)
        if (gamePlayers.isEmpty()) return emptyList()

        // 각 기록을 gamePlayerId로 인덱싱
        val battingByGamePlayer =
            battingRecordRepository.findAllByPlayerId(playerId)
                .associateBy { it.gamePlayer.id }
        val pitchingByGamePlayer =
            pitchingRecordRepository.findAllByPlayerId(playerId)
                .associateBy { it.gamePlayer.id }
        val fieldingByGamePlayer =
            fieldingRecordRepository.findAllByPlayerId(playerId)
                .associateBy { it.gamePlayer.id }

        // GamePlayer 기준으로 게임 로그 조립 (최근 경기 순으로 정렬)
        return gamePlayers
            .sortedByDescending { it.gameTeam.game.scheduledAt }
            .take(limit)
            .map { gamePlayer ->
                GameLogEntry(
                    gamePlayer = gamePlayer,
                    battingRecord = battingByGamePlayer[gamePlayer.id],
                    pitchingRecord = pitchingByGamePlayer[gamePlayer.id],
                    fieldingRecord = fieldingByGamePlayer[gamePlayer.id],
                )
            }
    }
}

/**
 * 게임 로그 항목
 *
 * 하나의 경기에서 선수의 모든 기록을 묶어 제공합니다.
 */
data class GameLogEntry(
    val gamePlayer: GamePlayer,
    val battingRecord: BattingRecord?,
    val pitchingRecord: PitchingRecord?,
    val fieldingRecord: FieldingRecord?,
)
