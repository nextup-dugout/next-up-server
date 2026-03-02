package com.nextup.infrastructure.service.game.correction

import com.nextup.common.exception.BattingRecordNotFoundByIdException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PitchingRecordNotFoundByIdException
import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.RecordCorrection
import com.nextup.core.port.repository.AuditLogRepositoryPort
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.RecordCorrectionRepositoryPort
import com.nextup.core.service.game.correction.BattingCorrectionRequest
import com.nextup.core.service.game.correction.PitchingCorrectionRequest
import com.nextup.core.service.game.correction.RecordCorrectionDto
import com.nextup.core.service.game.correction.RecordCorrectionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기록 정정 서비스 구현체
 *
 * 관리자가 타격/투수 기록을 정정하는 유스케이스를 처리합니다.
 * 정정 시 AuditLog를 통해 변경 전/후 값을 기록합니다.
 */
@Service
@Transactional(readOnly = true)
class RecordCorrectionServiceImpl(
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val recordCorrectionRepository: RecordCorrectionRepositoryPort,
    private val auditLogRepository: AuditLogRepositoryPort,
    private val gameRepository: GameRepositoryPort,
) : RecordCorrectionService {
    /**
     * 타격 기록을 정정합니다.
     */
    @Transactional
    override fun correctBattingRecord(
        gameId: Long,
        recordId: Long,
        request: BattingCorrectionRequest,
    ): BattingRecord {
        // 경기 존재 확인
        gameRepository.findByIdOrNull(gameId)
            ?: throw GameNotFoundException(gameId)

        // 타격 기록 조회 (recordId = battingRecord.id)
        val battingRecord =
            battingRecordRepository.findByIdOrNull(recordId)
                ?: throw BattingRecordNotFoundByIdException(recordId)

        // Rich Domain Model: Entity의 비즈니스 메서드 호출
        val oldValue = battingRecord.correctField(request.fieldName, request.newValue)

        // 정정 이력 저장
        val correction =
            RecordCorrection.create(
                gameId = gameId,
                adminUserId = request.adminUserId,
                correctionType = CorrectionType.BATTING,
                targetRecordId = recordId,
                fieldName = request.fieldName,
                oldValue = oldValue,
                newValue = request.newValue,
                reason = request.reason,
            )
        recordCorrectionRepository.save(correction)

        // AuditLog 저장
        val auditLog =
            AuditLog.create(
                adminUserId = request.adminUserId,
                action = "CORRECT_BATTING_RECORD",
                targetEntity = "BattingRecord",
                targetId = recordId,
                details =
                    "gameId=$gameId, field=${request.fieldName}, " +
                        "oldValue=$oldValue, newValue=${request.newValue}, reason=${request.reason}",
            )
        auditLogRepository.save(auditLog)

        return battingRecord
    }

    /**
     * 투수 기록을 정정합니다.
     */
    @Transactional
    override fun correctPitchingRecord(
        gameId: Long,
        recordId: Long,
        request: PitchingCorrectionRequest,
    ): PitchingRecord {
        // 경기 존재 확인
        gameRepository.findByIdOrNull(gameId)
            ?: throw GameNotFoundException(gameId)

        // 투수 기록 조회 (recordId = pitchingRecord.id)
        val pitchingRecord =
            pitchingRecordRepository.findByIdOrNull(recordId)
                ?: throw PitchingRecordNotFoundByIdException(recordId)

        // Rich Domain Model: Entity의 비즈니스 메서드 호출
        val oldValue = pitchingRecord.correctField(request.fieldName, request.newValue)

        // 정정 이력 저장
        val correction =
            RecordCorrection.create(
                gameId = gameId,
                adminUserId = request.adminUserId,
                correctionType = CorrectionType.PITCHING,
                targetRecordId = recordId,
                fieldName = request.fieldName,
                oldValue = oldValue,
                newValue = request.newValue,
                reason = request.reason,
            )
        recordCorrectionRepository.save(correction)

        // AuditLog 저장
        val auditLog =
            AuditLog.create(
                adminUserId = request.adminUserId,
                action = "CORRECT_PITCHING_RECORD",
                targetEntity = "PitchingRecord",
                targetId = recordId,
                details =
                    "gameId=$gameId, field=${request.fieldName}, " +
                        "oldValue=$oldValue, newValue=${request.newValue}, reason=${request.reason}",
            )
        auditLogRepository.save(auditLog)

        return pitchingRecord
    }

    /**
     * 경기의 기록 정정 이력을 조회합니다.
     */
    override fun getCorrectionHistory(gameId: Long): List<RecordCorrectionDto> {
        gameRepository.findByIdOrNull(gameId)
            ?: throw GameNotFoundException(gameId)

        return recordCorrectionRepository.findAllByGameId(gameId).map { it.toDto() }
    }

    private fun RecordCorrection.toDto(): RecordCorrectionDto =
        RecordCorrectionDto(
            id = this.id,
            gameId = this.gameId,
            adminUserId = this.adminUserId,
            correctionType = this.correctionType,
            targetRecordId = this.targetRecordId,
            fieldName = this.fieldName,
            oldValue = this.oldValue,
            newValue = this.newValue,
            reason = this.reason,
        )
}
