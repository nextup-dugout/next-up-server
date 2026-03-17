package com.nextup.api.mapper.game

import com.nextup.api.dto.game.FieldingRecordResponse
import com.nextup.core.domain.game.FieldingRecord

/**
 * FieldingRecord Entity를 FieldingRecordResponse DTO로 변환하는 Extension Function
 */
fun FieldingRecord.toResponse(): FieldingRecordResponse =
    FieldingRecordResponse(
        id = this.id,
        gamePlayerId = this.gamePlayer.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        putOuts = this.putOuts,
        assists = this.assists,
        errors = this.errors,
        doublePlays = this.doublePlays,
        passedBalls = this.passedBalls,
        totalChances = this.totalChances,
        fieldingPercentage = this.fieldingPercentage?.toPlainString(),
    )

/**
 * List<FieldingRecord>를 List<FieldingRecordResponse>로 변환
 */
fun List<FieldingRecord>.toFieldingResponse(): List<FieldingRecordResponse> = this.map { it.toResponse() }
