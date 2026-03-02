package com.nextup.scorer.dto.fielding

import com.nextup.core.domain.game.FieldingRecord

/**
 * 수비 기록 응답 DTO (Scorer 모듈)
 */
data class FieldingRecordResponse(
    val id: Long,
    val gamePlayerId: Long,
    val putOuts: Int,
    val assists: Int,
    val errors: Int,
    val doublePlays: Int,
    val passedBalls: Int,
    val totalChances: Int,
    val fieldingPercentage: String?,
)

fun FieldingRecord.toScorerResponse(): FieldingRecordResponse =
    FieldingRecordResponse(
        id = this.id,
        gamePlayerId = this.gamePlayer.id,
        putOuts = this.putOuts,
        assists = this.assists,
        errors = this.errors,
        doublePlays = this.doublePlays,
        passedBalls = this.passedBalls,
        totalChances = this.totalChances,
        fieldingPercentage = this.fieldingPercentage?.toPlainString(),
    )

fun List<FieldingRecord>.toScorerResponse(): List<FieldingRecordResponse> = this.map { it.toScorerResponse() }
