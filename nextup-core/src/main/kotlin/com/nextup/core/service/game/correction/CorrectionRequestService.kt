package com.nextup.core.service.game.correction

import com.nextup.common.exception.CorrectionRequestNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.game.CorrectionRequest
import com.nextup.core.domain.game.CorrectionRequestStatus
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.port.repository.CorrectionRequestRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기록 정정 요청 서비스
 *
 * 기록원이 기록 정정을 요청하고, 관리자가 승인/반려하는 워크플로우를 관리합니다.
 * 승인 시 기존 RecordCorrectionService를 통해 실제 정정을 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class CorrectionRequestService(
    private val correctionRequestRepository: CorrectionRequestRepositoryPort,
    private val recordCorrectionService: RecordCorrectionService,
) {
    /**
     * 기록 정정 요청을 생성합니다.
     */
    @Transactional
    fun createRequest(
        gameId: Long,
        requesterUserId: Long,
        correctionType: CorrectionType,
        targetRecordId: Long,
        fieldName: String,
        newValue: String,
        reason: String,
    ): CorrectionRequest {
        val request =
            CorrectionRequest.create(
                gameId = gameId,
                requesterUserId = requesterUserId,
                correctionType = correctionType,
                targetRecordId = targetRecordId,
                fieldName = fieldName,
                newValue = newValue,
                reason = reason,
            )

        return correctionRequestRepository.save(request)
    }

    /**
     * 기록 정정 요청을 승인하고 실제 정정을 수행합니다.
     */
    @Transactional
    fun approve(
        requestId: Long,
        reviewerUserId: Long,
        comment: String?,
    ): CorrectionRequest {
        val request = getById(requestId)

        try {
            request.approve(reviewerUserId, comment)
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_CORRECTION_REQUEST_STATE",
                e.message ?: "정정 요청을 승인할 수 없습니다",
            )
        }

        // 실제 기록 정정 수행
        when (request.correctionType) {
            CorrectionType.BATTING ->
                recordCorrectionService.correctBattingRecord(
                    gameId = request.gameId,
                    recordId = request.targetRecordId,
                    request =
                        BattingCorrectionRequest(
                            adminUserId = reviewerUserId,
                            fieldName = request.fieldName,
                            newValue = request.newValue,
                            reason = request.reason,
                        ),
                )
            CorrectionType.PITCHING ->
                recordCorrectionService.correctPitchingRecord(
                    gameId = request.gameId,
                    recordId = request.targetRecordId,
                    request =
                        PitchingCorrectionRequest(
                            adminUserId = reviewerUserId,
                            fieldName = request.fieldName,
                            newValue = request.newValue,
                            reason = request.reason,
                        ),
                )
            CorrectionType.FIELDING ->
                recordCorrectionService.correctFieldingRecord(
                    gameId = request.gameId,
                    recordId = request.targetRecordId,
                    request =
                        FieldingCorrectionRequest(
                            adminUserId = reviewerUserId,
                            fieldName = request.fieldName,
                            newValue = request.newValue,
                            reason = request.reason,
                        ),
                )
        }

        return request
    }

    /**
     * 기록 정정 요청을 반려합니다.
     */
    @Transactional
    fun reject(
        requestId: Long,
        reviewerUserId: Long,
        comment: String,
    ): CorrectionRequest {
        val request = getById(requestId)

        try {
            request.reject(reviewerUserId, comment)
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_CORRECTION_REQUEST_STATE",
                e.message ?: "정정 요청을 반려할 수 없습니다",
            )
        }

        return request
    }

    /**
     * ID로 정정 요청을 조회합니다.
     */
    fun getById(id: Long): CorrectionRequest =
        correctionRequestRepository.findByIdOrNull(id)
            ?: throw CorrectionRequestNotFoundException(id)

    /**
     * 대기 중인 정정 요청 목록을 조회합니다.
     */
    fun getPendingRequests(): List<CorrectionRequest> =
        correctionRequestRepository.findByStatus(CorrectionRequestStatus.PENDING)

    /**
     * 경기별 정정 요청 목록을 조회합니다.
     */
    fun getByGameId(gameId: Long): List<CorrectionRequest> = correctionRequestRepository.findByGameId(gameId)
}
