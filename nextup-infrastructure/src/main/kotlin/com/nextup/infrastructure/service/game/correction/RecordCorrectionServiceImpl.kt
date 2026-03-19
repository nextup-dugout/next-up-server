package com.nextup.infrastructure.service.game.correction

import com.nextup.common.exception.BattingRecordNotFoundByIdException
import com.nextup.common.exception.FieldingRecordNotFoundByIdException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PitchingRecordNotFoundByIdException
import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.domain.event.RecordCorrectedEvent
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.RecordCorrection
import com.nextup.core.port.repository.AuditLogRepositoryPort
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.RecordCorrectionRepositoryPort
import com.nextup.core.service.game.correction.BattingCorrectionRequest
import com.nextup.core.service.game.correction.FieldingCorrectionRequest
import com.nextup.core.service.game.correction.GameInningsCorrectionRequest
import com.nextup.core.service.game.correction.PitchingCorrectionRequest
import com.nextup.core.service.game.correction.RecordCorrectionDto
import com.nextup.core.service.game.correction.RecordCorrectionService
import org.springframework.context.ApplicationEventPublisher
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
    private val fieldingRecordRepository: FieldingRecordRepositoryPort,
    private val recordCorrectionRepository: RecordCorrectionRepositoryPort,
    private val auditLogRepository: AuditLogRepositoryPort,
    private val gameRepository: GameRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
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

        // 타석 결과와 관련된 GameEvent ID 조회 (H6: GameEvent 타임라인 정정)
        val gameEventId =
            gameEventRepository
                .findPlateAppearancesByGameIdAndBatterGamePlayerId(
                    gameId = gameId,
                    batterGamePlayerId = battingRecord.gamePlayer.id,
                ).lastOrNull()?.id

        // 시즌/커리어 스탯 재집계 이벤트 발행
        eventPublisher.publishEvent(
            RecordCorrectedEvent(
                gameId = gameId,
                correctionType = CorrectionType.BATTING,
                playerId = battingRecord.gamePlayer.player.id,
                fieldName = request.fieldName,
                oldValue = oldValue,
                newValue = request.newValue,
                gameEventId = gameEventId,
            ),
        )

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

        // 시즌/커리어 스탯 재집계 이벤트 발행
        eventPublisher.publishEvent(
            RecordCorrectedEvent(
                gameId = gameId,
                correctionType = CorrectionType.PITCHING,
                playerId = pitchingRecord.gamePlayer.player.id,
                fieldName = request.fieldName,
                oldValue = oldValue,
                newValue = request.newValue,
            ),
        )

        return pitchingRecord
    }

    /**
     * 수비 기록을 정정합니다.
     */
    @Transactional
    override fun correctFieldingRecord(
        gameId: Long,
        recordId: Long,
        request: FieldingCorrectionRequest,
    ): FieldingRecord {
        // 경기 존재 확인
        gameRepository.findByIdOrNull(gameId)
            ?: throw GameNotFoundException(gameId)

        // 수비 기록 조회 (recordId = fieldingRecord.id)
        val fieldingRecord =
            fieldingRecordRepository.findByIdOrNull(recordId)
                ?: throw FieldingRecordNotFoundByIdException(recordId)

        // Rich Domain Model: Entity의 비즈니스 메서드 호출
        val oldValue = fieldingRecord.correctField(request.fieldName, request.newValue)

        // 정정 이력 저장
        val correction =
            RecordCorrection.create(
                gameId = gameId,
                adminUserId = request.adminUserId,
                correctionType = CorrectionType.FIELDING,
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
                action = "CORRECT_FIELDING_RECORD",
                targetEntity = "FieldingRecord",
                targetId = recordId,
                details =
                    "gameId=$gameId, field=${request.fieldName}, " +
                        "oldValue=$oldValue, newValue=${request.newValue}, reason=${request.reason}",
            )
        auditLogRepository.save(auditLog)

        // 시즌/커리어 스탯 재집계 이벤트 발행
        eventPublisher.publishEvent(
            RecordCorrectedEvent(
                gameId = gameId,
                correctionType = CorrectionType.FIELDING,
                playerId = fieldingRecord.gamePlayer.player.id,
                fieldName = request.fieldName,
                oldValue = oldValue,
                newValue = request.newValue,
            ),
        )

        return fieldingRecord
    }

    /**
     * L-5: 경기 이닝 수를 축소 정정합니다.
     */
    @Transactional
    override fun correctGameTotalInnings(
        gameId: Long,
        request: GameInningsCorrectionRequest,
    ): Game {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        val oldTotalInnings = game.totalInnings

        // 해당 경기의 모든 투수 기록 조회하여 충돌 검증
        val pitchingRecords = pitchingRecordRepository.findAllByGameId(gameId)
        game.reduceTotalInnings(request.newTotalInnings, pitchingRecords)

        // 정정 이력 저장
        val correction =
            RecordCorrection.create(
                gameId = gameId,
                adminUserId = request.adminUserId,
                correctionType = CorrectionType.PITCHING,
                targetRecordId = gameId,
                fieldName = "totalInnings",
                oldValue = oldTotalInnings.toString(),
                newValue = request.newTotalInnings.toString(),
                reason = request.reason,
            )
        recordCorrectionRepository.save(correction)

        // AuditLog 저장
        val auditLog =
            AuditLog.create(
                adminUserId = request.adminUserId,
                action = "CORRECT_GAME_TOTAL_INNINGS",
                targetEntity = "Game",
                targetId = gameId,
                details =
                    "gameId=$gameId, field=totalInnings, " +
                        "oldValue=$oldTotalInnings, newValue=${request.newTotalInnings}, reason=${request.reason}",
            )
        auditLogRepository.save(auditLog)

        return game
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
