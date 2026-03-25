package com.nextup.infrastructure.service.discipline

import com.nextup.common.exception.PlayerBanNotFoundException
import com.nextup.core.domain.discipline.PlayerBan
import com.nextup.core.port.repository.PlayerBanRepositoryPort
import com.nextup.core.service.discipline.PlayerBanService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 선수 제재(BAN) 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class PlayerBanServiceImpl(
    private val playerBanRepository: PlayerBanRepositoryPort,
) : PlayerBanService {
    @Transactional
    override fun issueBan(
        playerId: Long,
        competitionId: Long,
        reason: String,
        issuedBy: String,
    ): PlayerBan {
        val ban =
            PlayerBan.create(
                playerId = playerId,
                competitionId = competitionId,
                reason = reason,
                issuedBy = issuedBy,
            )

        return playerBanRepository.save(ban)
    }

    override fun getById(id: Long): PlayerBan =
        playerBanRepository.findByIdOrNull(id)
            ?: throw PlayerBanNotFoundException(id)

    override fun getAll(): List<PlayerBan> = playerBanRepository.findAll()

    override fun getBansByPlayer(playerId: Long): List<PlayerBan> = playerBanRepository.findByPlayerId(playerId)

    override fun getBansByCompetition(competitionId: Long): List<PlayerBan> =
        playerBanRepository.findByCompetitionId(competitionId)

    override fun getBansByPlayerAndCompetition(
        playerId: Long,
        competitionId: Long,
    ): List<PlayerBan> = playerBanRepository.findByPlayerIdAndCompetitionId(playerId, competitionId)

    override fun canPlayerPlay(
        playerId: Long,
        competitionId: Long,
    ): Boolean = !playerBanRepository.existsByPlayerIdAndCompetitionId(playerId, competitionId)

    @Transactional
    override fun deleteBan(id: Long) {
        val ban = getById(id)
        playerBanRepository.delete(ban)
    }
}
