package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.RecordCorrection
import com.nextup.core.port.repository.RecordCorrectionRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RecordCorrectionRepository :
    JpaRepository<RecordCorrection, Long>,
    RecordCorrectionRepositoryPort {
    /**
     * 경기 ID로 정정 이력을 조회합니다.
     */
    @Query("SELECT rc FROM RecordCorrection rc WHERE rc.gameId = :gameId ORDER BY rc.createdAt DESC")
    override fun findAllByGameId(
        @Param("gameId") gameId: Long,
    ): List<RecordCorrection>

    /**
     * 경기 ID와 정정 유형으로 정정 이력을 조회합니다.
     */
    @Query(
        """
        SELECT rc FROM RecordCorrection rc
        WHERE rc.gameId = :gameId
        AND rc.correctionType = :correctionType
        ORDER BY rc.createdAt DESC
    """,
    )
    override fun findAllByGameIdAndCorrectionType(
        @Param("gameId") gameId: Long,
        @Param("correctionType") correctionType: CorrectionType,
    ): List<RecordCorrection>

    /**
     * 특정 기록의 정정 이력을 조회합니다.
     */
    @Query(
        """
        SELECT rc FROM RecordCorrection rc
        WHERE rc.correctionType = :correctionType
        AND rc.targetRecordId = :targetRecordId
        ORDER BY rc.createdAt DESC
    """,
    )
    override fun findAllByTargetRecordId(
        @Param("correctionType") correctionType: CorrectionType,
        @Param("targetRecordId") targetRecordId: Long,
    ): List<RecordCorrection>

    /**
     * ID로 정정 이력을 조회합니다.
     */
    override fun findByIdOrNull(id: Long): RecordCorrection? = findById(id).orElse(null)
}
