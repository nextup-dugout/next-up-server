package com.nextup.backoffice.dto.correction

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.service.game.correction.RecordCorrectionDto
import java.math.BigDecimal

/**
 * 타격 기록 정정 응답 DTO
 */
data class BattingRecordCorrectionResponse(
    val recordId: Long,
    val gamePlayerId: Long,
    val plateAppearances: Int,
    val atBats: Int,
    val hits: Int,
    val doubles: Int,
    val triples: Int,
    val homeRuns: Int,
    val runs: Int,
    val runsBattedIn: Int,
    val walks: Int,
    val intentionalWalks: Int,
    val hitByPitch: Int,
    val strikeouts: Int,
    val sacrificeBunts: Int,
    val sacrificeFlies: Int,
    val stolenBases: Int,
    val caughtStealing: Int,
    val groundedIntoDoublePlays: Int,
    val triplePlays: Int,
    val battingAverage: BigDecimal,
    val onBasePercentage: BigDecimal,
    val sluggingPercentage: BigDecimal,
    val ops: BigDecimal,
)

/**
 * 투수 기록 정정 응답 DTO
 */
data class PitchingRecordCorrectionResponse(
    val recordId: Long,
    val gamePlayerId: Long,
    val inningsPitchedOuts: Int,
    val inningsPitchedDisplay: String,
    val earnedRuns: Int,
    val runsAllowed: Int,
    val hitsAllowed: Int,
    val walksAllowed: Int,
    val strikeouts: Int,
    val homeRunsAllowed: Int,
    val hitBatsmen: Int,
    val wildPitches: Int,
    val balks: Int,
    val battersFaced: Int,
    val pitchesThrown: Int?,
    val strikesThrown: Int?,
    val isStartingPitcher: Boolean,
    val decision: String,
    val earnedRunAverage: BigDecimal?,
    val whip: BigDecimal,
)

/**
 * 수비 기록 정정 응답 DTO
 */
data class FieldingRecordCorrectionResponse(
    val recordId: Long,
    val gamePlayerId: Long,
    val putOuts: Int,
    val assists: Int,
    val errors: Int,
    val doublePlays: Int,
    val passedBalls: Int,
    val totalChances: Int,
    val fieldingPercentage: BigDecimal?,
)

/**
 * 기록 정정 이력 응답 DTO
 */
data class RecordCorrectionHistoryResponse(
    val id: Long,
    val gameId: Long,
    val adminUserId: Long,
    val correctionType: CorrectionType,
    val targetRecordId: Long,
    val fieldName: String,
    val oldValue: String,
    val newValue: String,
    val reason: String,
)

// Extension functions for DTO conversion

fun BattingRecord.toCorrectionResponse(): BattingRecordCorrectionResponse =
    BattingRecordCorrectionResponse(
        recordId = this.id,
        gamePlayerId = this.gamePlayer.id,
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
        triplePlays = this.triplePlays,
        battingAverage = this.battingAverage,
        onBasePercentage = this.onBasePercentage,
        sluggingPercentage = this.sluggingPercentage,
        ops = this.ops,
    )

fun FieldingRecord.toCorrectionResponse(): FieldingRecordCorrectionResponse =
    FieldingRecordCorrectionResponse(
        recordId = this.id,
        gamePlayerId = this.gamePlayer.id,
        putOuts = this.putOuts,
        assists = this.assists,
        errors = this.errors,
        doublePlays = this.doublePlays,
        passedBalls = this.passedBalls,
        totalChances = this.totalChances,
        fieldingPercentage = this.fieldingPercentage,
    )

fun PitchingRecord.toCorrectionResponse(): PitchingRecordCorrectionResponse =
    PitchingRecordCorrectionResponse(
        recordId = this.id,
        gamePlayerId = this.gamePlayer.id,
        inningsPitchedOuts = this.inningsPitchedOuts,
        inningsPitchedDisplay = this.inningsPitchedDisplay,
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
        pitchesThrown = this.pitchesThrown,
        strikesThrown = this.strikesThrown,
        isStartingPitcher = this.isStartingPitcher,
        decision = this.decision.name,
        earnedRunAverage = this.earnedRunAverage,
        whip = this.whip,
    )

fun RecordCorrectionDto.toHistoryResponse(): RecordCorrectionHistoryResponse =
    RecordCorrectionHistoryResponse(
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
