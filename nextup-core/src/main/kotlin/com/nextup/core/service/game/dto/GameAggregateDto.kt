package com.nextup.core.service.game.dto

/**
 * 경기 상세 통합 DTO
 *
 * 경기 상세 화면에 필요한 모든 데이터를 단일 구조로 제공합니다.
 */
data class GameAggregateDto(
    val gameDetail: GameDetailDto,
    val boxScore: BoxScoreDto?,
    val timeline: GameTimelineDto,
    val scoresheet: ScoresheetDto?,
)
