package com.nextup.core.domain.game

/**
 * 투수 교체 제한 경고 (한 타자 최소 대면 규칙)
 *
 * 사회인 야구의 유연성을 위해 규칙 위반 시 예외가 아닌 경고를 반환합니다.
 * 호출 측에서 이 경고를 무시하거나 사용자에게 확인을 요청할 수 있습니다.
 *
 * @param currentBattersFaced 현재 투수의 대면 타자 수
 * @param minBattersFaced 최소 대면 타자 수 요건
 */
data class PitcherChangeWarning(
    val currentBattersFaced: Int,
    val minBattersFaced: Int,
) {
    val message: String
        get() =
            "현재 투수가 ${currentBattersFaced}명의 타자만 대면했습니다. " +
                "최소 ${minBattersFaced}명의 타자를 대면해야 합니다. " +
                "사회인 야구 규칙에 따라 교체는 허용되나 경고가 발생합니다."
}
