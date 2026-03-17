package com.nextup.core.service.mercenary

import com.nextup.common.exception.MercenaryAlreadyAppliedException
import com.nextup.common.exception.MercenaryApplicationNotFoundException
import com.nextup.common.exception.MercenaryMaxCountReachedException
import com.nextup.common.exception.MercenaryRequestClosedException
import com.nextup.common.exception.MercenaryRequestNotFoundException
import com.nextup.core.domain.mercenary.MercenaryApplication
import com.nextup.core.domain.mercenary.MercenaryParticipation
import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus
import com.nextup.core.port.repository.MercenaryApplicationRepositoryPort
import com.nextup.core.port.repository.MercenaryParticipationRepositoryPort
import com.nextup.core.port.repository.MercenaryRequestRepositoryPort
import com.nextup.core.service.mercenary.dto.ApplyMercenaryDto
import com.nextup.core.service.mercenary.dto.CreateMercenaryRequestDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 용병 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class MercenaryService(
    private val mercenaryRequestRepository: MercenaryRequestRepositoryPort,
    private val mercenaryApplicationRepository: MercenaryApplicationRepositoryPort,
    private val mercenaryParticipationRepository: MercenaryParticipationRepositoryPort,
) {
    /**
     * 용병 요청을 생성합니다.
     */
    @Transactional
    fun createRequest(dto: CreateMercenaryRequestDto): MercenaryRequest {
        val mercenaryRequest =
            MercenaryRequest.create(
                requestingTeamId = dto.requestingTeamId,
                gameId = dto.gameId,
                positions = dto.positions,
                maxCount = dto.maxCount,
                deadline = dto.deadline,
                description = dto.description,
            )

        return mercenaryRequestRepository.save(mercenaryRequest)
    }

    /**
     * OPEN 상태의 용병 요청 목록을 조회합니다.
     */
    fun getOpenRequests(): List<MercenaryRequest> = mercenaryRequestRepository.findByStatus(MercenaryRequestStatus.OPEN)

    /**
     * 용병 요청을 ID로 조회합니다.
     */
    fun getRequestById(id: Long): MercenaryRequest =
        mercenaryRequestRepository.findByIdOrNull(id)
            ?: throw MercenaryRequestNotFoundException(id)

    /**
     * 용병 요청에 지원합니다.
     */
    @Transactional
    fun apply(dto: ApplyMercenaryDto): MercenaryApplication {
        val request =
            mercenaryRequestRepository.findByIdOrNull(dto.requestId)
                ?: throw MercenaryRequestNotFoundException(dto.requestId)

        if (!request.canAcceptApplication()) {
            throw MercenaryRequestClosedException(dto.requestId)
        }

        if (mercenaryApplicationRepository.existsByRequestIdAndPlayerId(dto.requestId, dto.playerId)) {
            throw MercenaryAlreadyAppliedException(dto.requestId, dto.playerId)
        }

        val application =
            MercenaryApplication.create(
                requestId = dto.requestId,
                playerId = dto.playerId,
                preferredPositions = dto.preferredPositions,
                message = dto.message,
            )

        return mercenaryApplicationRepository.save(application)
    }

    /**
     * 용병 지원을 수락합니다.
     */
    @Transactional
    fun acceptApplication(
        requestId: Long,
        applicationId: Long,
    ): MercenaryApplication {
        val request =
            mercenaryRequestRepository.findByIdOrNull(requestId)
                ?: throw MercenaryRequestNotFoundException(requestId)

        val application =
            mercenaryApplicationRepository.findByIdOrNull(applicationId)
                ?: throw MercenaryApplicationNotFoundException(applicationId)

        val acceptedCount = mercenaryApplicationRepository.countAcceptedByRequestId(requestId)
        if (acceptedCount >= request.maxCount) {
            throw MercenaryMaxCountReachedException(requestId)
        }

        application.accept()

        // 참가 기록 생성
        val participation =
            MercenaryParticipation.create(
                gameId = request.gameId,
                playerId = application.playerId,
                teamId = request.requestingTeamId,
            )
        mercenaryParticipationRepository.save(participation)

        // 최대 인원 도달 시 자동 마감
        if (acceptedCount + 1 >= request.maxCount) {
            request.close()
        }

        return application
    }

    /**
     * 용병 지원을 거절합니다.
     */
    @Transactional
    fun rejectApplication(
        requestId: Long,
        applicationId: Long,
    ): MercenaryApplication {
        mercenaryRequestRepository.findByIdOrNull(requestId)
            ?: throw MercenaryRequestNotFoundException(requestId)

        val application =
            mercenaryApplicationRepository.findByIdOrNull(applicationId)
                ?: throw MercenaryApplicationNotFoundException(applicationId)

        application.reject()

        return application
    }

    /**
     * 용병 요청에 대한 지원 목록을 조회합니다.
     */
    fun getApplicationsByRequest(requestId: Long): List<MercenaryApplication> {
        mercenaryRequestRepository.findByIdOrNull(requestId)
            ?: throw MercenaryRequestNotFoundException(requestId)

        return mercenaryApplicationRepository.findByRequestId(requestId)
    }

    /**
     * 선수의 용병 참가 이력을 조회합니다.
     */
    fun getParticipationsByPlayer(playerId: Long): List<MercenaryParticipation> =
        mercenaryParticipationRepository.findByPlayerId(playerId)

    /**
     * 용병 요청을 취소합니다.
     */
    @Transactional
    fun cancelRequest(requestId: Long): MercenaryRequest {
        val request =
            mercenaryRequestRepository.findByIdOrNull(requestId)
                ?: throw MercenaryRequestNotFoundException(requestId)

        try {
            request.cancel()
        } catch (e: IllegalStateException) {
            throw com.nextup.common.exception.InvalidStateException(
                "INVALID_MERCENARY_REQUEST_STATE",
                e.message ?: "용병 요청을 취소할 수 없습니다",
            )
        }

        return request
    }
}
