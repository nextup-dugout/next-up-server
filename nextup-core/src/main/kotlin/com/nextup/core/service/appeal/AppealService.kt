package com.nextup.core.service.appeal

import com.nextup.common.exception.AppealNotFoundException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.appeal.Appeal
import com.nextup.core.domain.appeal.AppealStatus
import com.nextup.core.port.repository.AppealRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.appeal.dto.CreateAppealRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 이의 제기 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class AppealService(
    private val appealRepository: AppealRepositoryPort,
    private val gameRepository: GameRepositoryPort,
) {
    /**
     * 이의 제기를 생성합니다.
     */
    @Transactional
    fun createAppeal(request: CreateAppealRequest): Appeal {
        val game =
            gameRepository.findByIdOrNull(request.gameId)
                ?: throw GameNotFoundException(request.gameId)

        val appeal =
            Appeal.create(
                game = game,
                appealerId = request.appealerId,
                appealerName = request.appealerName,
                type = request.type,
                title = request.title,
                description = request.description,
            )

        return appealRepository.save(appeal)
    }

    /**
     * 경기별 이의 제기 목록을 조회합니다.
     */
    fun getAppealsByGame(gameId: Long): List<Appeal> = appealRepository.findByGameId(gameId)

    /**
     * 신청자별 이의 제기 목록을 조회합니다.
     */
    fun getAppealsByAppealer(appealerId: Long): List<Appeal> = appealRepository.findByAppealerId(appealerId)

    /**
     * 대기 중인 모든 이의 제기를 조회합니다.
     */
    fun getAllPendingAppeals(): List<Appeal> = appealRepository.findByStatus(AppealStatus.PENDING)

    /**
     * 모든 이의 제기를 조회합니다.
     */
    fun getAllAppeals(): List<Appeal> = appealRepository.findAll()

    /**
     * 특정 상태의 이의 제기를 조회합니다.
     */
    fun getAppealsByStatus(status: AppealStatus): List<Appeal> = appealRepository.findByStatus(status)

    /**
     * ID로 이의 제기를 조회합니다.
     */
    fun getById(id: Long): Appeal =
        appealRepository.findByIdOrNull(id)
            ?: throw AppealNotFoundException(id)

    /**
     * 이의 제기를 승인합니다.
     */
    @Transactional
    fun approveAppeal(
        appealId: Long,
        reviewerId: Long,
        comment: String?,
    ): Appeal {
        val appeal = getById(appealId)

        try {
            appeal.approve(reviewerId, comment)
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_APPEAL_STATE",
                e.message ?: "Cannot approve appeal",
            )
        }

        return appeal
    }

    /**
     * 이의 제기를 반려합니다.
     */
    @Transactional
    fun rejectAppeal(
        appealId: Long,
        reviewerId: Long,
        comment: String,
    ): Appeal {
        val appeal = getById(appealId)

        try {
            appeal.reject(reviewerId, comment)
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_APPEAL_STATE",
                e.message ?: "Cannot reject appeal",
            )
        }

        return appeal
    }
}
