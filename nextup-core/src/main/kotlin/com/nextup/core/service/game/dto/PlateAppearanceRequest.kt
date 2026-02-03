package com.nextup.core.service.game.dto

import com.nextup.core.domain.game.PlateAppearanceResult

/**
 * 타석 결과 입력 요청 DTO
 *
 * @property batterId 타자 ID (GamePlayer ID)
 * @property pitcherId 투수 ID (GamePlayer ID)
 * @property result 타석 결과
 * @property runnerMovements 주자 이동 목록 (옵션)
 * @property rbis 타점 수 (옵션, 자동 계산 가능)
 * @property balls 최종 볼 카운트 (옵션)
 * @property strikes 최종 스트라이크 카운트 (옵션)
 */
data class PlateAppearanceRequest(
    val batterId: Long,
    val pitcherId: Long,
    val result: PlateAppearanceResult,
    val runnerMovements: List<RunnerMovement> = emptyList(),
    val rbis: Int? = null,
    val balls: Int = 0,
    val strikes: Int = 0
) {
    init {
        require(balls in 0..4) { "볼 카운트는 0-4 사이여야 합니다: $balls" }
        require(strikes in 0..3) { "스트라이크 카운트는 0-3 사이여야 합니다: $strikes" }
        rbis?.let {
            require(it >= 0) { "타점은 0 이상이어야 합니다: $it" }
        }
    }

    /**
     * 득점한 주자 수를 계산합니다.
     */
    fun calculateRunsScored(): Int {
        return runnerMovements.count { it.isScored }
    }

    /**
     * 실제 타점을 계산합니다 (rbis가 null이면 자동 계산).
     */
    fun getActualRbis(): Int {
        return rbis ?: calculateRunsScored()
    }
}
