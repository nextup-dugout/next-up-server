package com.nextup.api.mapper.game

import com.nextup.api.dto.game.BattingRecordResponse
import com.nextup.core.domain.game.BattingRecord

/**
 * BattingRecord Entity를 BattingRecordResponse DTO로 변환하는 Extension Function
 */
fun BattingRecord.toResponse(): BattingRecordResponse {
    return BattingRecordResponse(
        // 메타 정보
        id = this.id,
        gamePlayerId = this.gamePlayer.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,

        // 기본 타격 기록
        plateAppearances = this.plateAppearances,
        atBats = this.atBats,
        hits = this.hits,
        doubles = this.doubles,
        triples = this.triples,
        homeRuns = this.homeRuns,
        runs = this.runs,
        runsBattedIn = this.runsBattedIn,
        walks = this.walks,
        intentionalWalks = this.intentionalWalks,
        hitByPitch = this.hitByPitch,
        strikeouts = this.strikeouts,
        sacrificeBunts = this.sacrificeBunts,
        sacrificeFlies = this.sacrificeFlies,
        stolenBases = this.stolenBases,
        caughtStealing = this.caughtStealing,
        groundedIntoDoublePlays = this.groundedIntoDoublePlays,

        // 계산 속성
        singles = this.singles,
        totalBases = this.totalBases,
        extraBaseHits = this.extraBaseHits,
        sacrifices = this.sacrifices,
        totalWalks = this.totalWalks,

        // BigDecimal → String 변환 (소수점 3자리 고정)
        battingAverage = this.battingAverage.toPlainString(),
        onBasePercentage = this.onBasePercentage.toPlainString(),
        sluggingPercentage = this.sluggingPercentage.toPlainString(),
        ops = this.ops.toPlainString(),
        stolenBasePercentage = this.stolenBasePercentage.toPlainString()
    )
}

/**
 * List<BattingRecord>를 List<BattingRecordResponse>로 변환
 */
fun List<BattingRecord>.toResponse(): List<BattingRecordResponse> {
    return this.map { it.toResponse() }
}
