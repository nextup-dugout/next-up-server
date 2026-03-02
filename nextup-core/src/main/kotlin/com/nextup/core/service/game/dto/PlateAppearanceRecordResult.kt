package com.nextup.core.service.game.dto

import com.nextup.core.domain.game.Game

/**
 * 타석 결과 기록 후 반환 DTO
 *
 * 경기 상태와 함께 경고 메시지를 포함합니다.
 *
 * @property game 업데이트된 경기 엔티티
 * @property warnings 경고 메시지 목록 (투구 수 경고, 타순 위반 경고 등)
 */
data class PlateAppearanceRecordResult(
    val game: Game,
    val warnings: List<String> = emptyList(),
)
