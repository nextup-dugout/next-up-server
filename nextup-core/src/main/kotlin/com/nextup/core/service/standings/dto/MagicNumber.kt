package com.nextup.core.service.standings.dto

/**
 * 매직넘버 데이터 클래스
 *
 * 특정 팀이 목표 순위를 확정하기 위해 필요한 남은 승수를 나타냅니다.
 *
 * @param teamId 팀 ID
 * @param targetRank 목표 순위
 * @param magicNumber 매직넘버 (0 이하이면 이미 확정)
 * @param isClinched 순위 확정 여부 (magicNumber <= 0)
 * @param isEliminated 수학적으로 해당 순위 진입 불가 여부
 */
data class MagicNumber(
    val teamId: Long,
    val targetRank: Int,
    val magicNumber: Int,
    val isClinched: Boolean,
    val isEliminated: Boolean,
)
