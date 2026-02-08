package com.nextup.core.service.discipline

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.DisciplineNotFoundException
import com.nextup.common.exception.InvalidDisciplineStateException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.discipline.Discipline
import com.nextup.core.domain.discipline.DisciplineStatus
import com.nextup.core.domain.discipline.DisciplineType
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.DisciplineRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 징계 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class DisciplineService(
    private val disciplineRepository: DisciplineRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
) {
    /**
     * 경고 징계를 발급합니다.
     */
    @Transactional
    fun issueWarning(
        playerId: Long,
        competitionId: Long,
        reason: String,
        issuedBy: String,
        expiresAt: LocalDateTime? = null,
    ): Discipline {
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val discipline =
            Discipline.createWarning(
                player = player,
                competition = competition,
                reason = reason,
                issuedBy = issuedBy,
                expiresAt = expiresAt,
            )

        return disciplineRepository.save(discipline)
    }

    /**
     * 출장 정지 징계를 발급합니다.
     */
    @Transactional
    fun issueSuspension(
        playerId: Long,
        competitionId: Long,
        reason: String,
        suspensionGames: Int,
        issuedBy: String,
    ): Discipline {
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val discipline =
            Discipline.createSuspension(
                player = player,
                competition = competition,
                reason = reason,
                suspensionGames = suspensionGames,
                issuedBy = issuedBy,
            )

        return disciplineRepository.save(discipline)
    }

    /**
     * 영구 제재 징계를 발급합니다.
     */
    @Transactional
    fun issueBan(
        playerId: Long,
        competitionId: Long,
        reason: String,
        issuedBy: String,
    ): Discipline {
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        val competition =
            competitionRepository.findByIdOrNull(competitionId)
                ?: throw CompetitionNotFoundException(competitionId)

        val discipline =
            Discipline.createBan(
                player = player,
                competition = competition,
                reason = reason,
                issuedBy = issuedBy,
            )

        return disciplineRepository.save(discipline)
    }

    /**
     * 징계를 취소합니다.
     */
    @Transactional
    fun cancelDiscipline(disciplineId: Long): Discipline {
        val discipline = getById(disciplineId)

        try {
            discipline.cancel()
        } catch (e: IllegalArgumentException) {
            throw InvalidDisciplineStateException(
                e.message ?: "Cannot cancel discipline"
            )
        }

        return discipline
    }

    /**
     * ID로 징계를 조회합니다.
     */
    fun getById(id: Long): Discipline =
        disciplineRepository.findByIdOrNull(id)
            ?: throw DisciplineNotFoundException(id)

    /**
     * 모든 징계를 조회합니다.
     */
    fun getAll(): List<Discipline> = disciplineRepository.findAll()

    /**
     * 선수의 모든 징계를 조회합니다.
     */
    fun getDisciplinesByPlayer(playerId: Long): List<Discipline> = disciplineRepository.findByPlayerId(playerId)

    /**
     * 선수의 대회별 징계를 조회합니다.
     */
    fun getDisciplinesByPlayerAndCompetition(
        playerId: Long,
        competitionId: Long,
    ): List<Discipline> = disciplineRepository.findByPlayerIdAndCompetitionId(playerId, competitionId)

    /**
     * 대회의 모든 징계를 조회합니다.
     */
    fun getDisciplinesByCompetition(competitionId: Long): List<Discipline> =
        disciplineRepository.findByCompetitionId(competitionId)

    /**
     * 특정 상태의 징계를 조회합니다.
     */
    fun getDisciplinesByStatus(status: DisciplineStatus): List<Discipline> = disciplineRepository.findByStatus(status)

    /**
     * 선수의 활성 징계를 조회합니다.
     */
    fun getActiveDisciplines(
        playerId: Long,
        competitionId: Long,
    ): List<Discipline> =
        disciplineRepository.findActiveByPlayerIdAndCompetitionId(playerId, competitionId)
            .filter { it.isEffective() }

    /**
     * 선수가 출장 가능한지 확인합니다.
     */
    fun canPlayerPlay(
        playerId: Long,
        competitionId: Long,
    ): Boolean {
        val activeDisciplines = getActiveDisciplines(playerId, competitionId)

        // BAN이 있으면 출장 불가
        if (activeDisciplines.any { it.type == DisciplineType.BAN }) {
            return false
        }

        // 유효한 SUSPENSION이 있으면 출장 불가
        if (
            activeDisciplines.any {
                it.type == DisciplineType.SUSPENSION && it.isEffective()
            }
        ) {
            return false
        }

        return true
    }

    /**
     * 출장 정지 징계의 경기를 소화합니다.
     */
    @Transactional
    fun incrementServedGames(disciplineId: Long): Discipline {
        val discipline = getById(disciplineId)

        try {
            discipline.incrementServedGames()
        } catch (e: IllegalArgumentException) {
            throw InvalidDisciplineStateException(
                e.message ?: "Cannot increment served games"
            )
        }

        return discipline
    }
}
