package com.nextup.api.mapper.stats

import com.nextup.api.dto.stats.CareerFieldingStatsResponse
import com.nextup.api.dto.stats.SeasonFieldingStatsResponse
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.SeasonFieldingStats

/**
 * SeasonFieldingStats Entity를 SeasonFieldingStatsResponse DTO로 변환하는 Extension Function
 */
fun SeasonFieldingStats.toResponse(): SeasonFieldingStatsResponse =
    SeasonFieldingStatsResponse(
        // 메타 정보
        id = this.id,
        playerId = this.player.id,
        year = this.year,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        // 출전 정보
        gamesPlayed = this.gamesPlayed,
        // 기본 수비 기록
        putOuts = this.putOuts,
        assists = this.assists,
        errors = this.errors,
        doublePlays = this.doublePlays,
        passedBalls = this.passedBalls,
        // 계산 속성
        totalChances = this.totalChances,
        fieldingPercentage = this.fieldingPercentage?.toPlainString(),
    )

/**
 * CareerFieldingStats Entity를 CareerFieldingStatsResponse DTO로 변환하는 Extension Function
 */
fun CareerFieldingStats.toResponse(): CareerFieldingStatsResponse =
    CareerFieldingStatsResponse(
        // 메타 정보
        id = this.id,
        playerId = this.player.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        // 시즌 및 출전 정보
        seasonsPlayed = this.seasonsPlayed,
        gamesPlayed = this.gamesPlayed,
        // 기본 수비 기록
        putOuts = this.putOuts,
        assists = this.assists,
        errors = this.errors,
        doublePlays = this.doublePlays,
        passedBalls = this.passedBalls,
        // 계산 속성
        totalChances = this.totalChances,
        fieldingPercentage = this.fieldingPercentage?.toPlainString(),
    )

/**
 * List<SeasonFieldingStats>를 List<SeasonFieldingStatsResponse>로 변환
 */
fun List<SeasonFieldingStats>.toSeasonFieldingResponse(): List<SeasonFieldingStatsResponse> =
    this.map { it.toResponse() }
