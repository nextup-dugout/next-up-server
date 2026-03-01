package com.nextup.api.mapper.game

import com.nextup.api.dto.game.PitchingRecordResponse
import com.nextup.core.domain.game.PitchingRecord

/**
 * PitchingRecord Entity를 PitchingRecordResponse DTO로 변환하는 Extension Function
 */
fun PitchingRecord.toResponse(): PitchingRecordResponse =
    PitchingRecordResponse(
        // 메타 정보
        id = this.id,
        gamePlayerId = this.gamePlayer.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        // 기본 투구 기록
        inningsPitchedOuts = this.inningsPitchedOuts,
        earnedRuns = this.earnedRuns,
        runsAllowed = this.runsAllowed,
        hitsAllowed = this.hitsAllowed,
        walksAllowed = this.walksAllowed,
        strikeouts = this.strikeouts,
        homeRunsAllowed = this.homeRunsAllowed,
        hitBatsmen = this.hitBatsmen,
        wildPitches = this.wildPitches,
        balks = this.balks,
        battersFaced = this.battersFaced,
        decision = this.decision.name, // Enum → String
        isStartingPitcher = this.isStartingPitcher,
        pitchesThrown = this.pitchesThrown, // nullable
        strikesThrown = this.strikesThrown, // nullable
        // 계산 속성
        completeInnings = this.completeInnings,
        remainingOuts = this.remainingOuts,
        inningsPitched = this.inningsPitched.toPlainString(),
        inningsPitchedDisplay = this.inningsPitchedDisplay,
        earnedRunAverage = this.earnedRunAverage?.toPlainString(),
        whip = this.whip.toPlainString(),
        strikeoutsPer9 = this.strikeoutsPer9.toPlainString(),
        walksPer9 = this.walksPer9.toPlainString(),
        strikeoutToWalkRatio = this.strikeoutToWalkRatio.toPlainString(),
        strikePercentage = this.strikePercentage?.toPlainString(), // nullable
        unearnedRuns = this.unearnedRuns,
        isQualifiedForWin = this.isQualifiedForWin,
    )

/**
 * List<PitchingRecord>를 List<PitchingRecordResponse>로 변환
 */
fun List<PitchingRecord>.toResponse(): List<PitchingRecordResponse> = this.map { it.toResponse() }
