package com.nextup.core.domain.competition

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * 대회 경기 규칙 설정 (Value Object)
 *
 * Competition 단위로 설정되며, 사회인 야구 대회별로 다른 규칙을 지원합니다.
 * 기본값은 일반적인 9이닝 야구 규칙을 따릅니다.
 */
@Embeddable
data class GameRules(
    /** 기본 이닝 수 (3~12 사이) */
    @Column(name = "default_innings")
    val defaultInnings: Int = 9,
    /** 콜드게임(머시 룰) 활성화 여부 */
    @Column(name = "mercy_rule_enabled")
    val mercyRuleEnabled: Boolean = false,
    /** 콜드게임 적용 득점 차이 (mercyRuleEnabled가 true일 때만 유효) */
    @Column(name = "mercy_run_difference")
    val mercyRunDifference: Int? = null,
    /** 콜드게임 적용 최소 이닝 (mercyRuleEnabled가 true일 때만 유효) */
    @Column(name = "mercy_minimum_inning")
    val mercyMinimumInning: Int? = null,
    /** 최대 연장 이닝 수 (null이면 무제한) */
    @Column(name = "max_extra_innings")
    val maxExtraInnings: Int? = null,
    /** 동점 경기 처리 방법 */
    @Enumerated(EnumType.STRING)
    @Column(name = "tied_game_result")
    val tiedGameResult: TiedGameResult = TiedGameResult.DRAW,
    /** 타이브레이커(무사 2루 연장) 활성화 여부 */
    @Column(name = "tiebreaker_enabled")
    val tiebreakerEnabled: Boolean = false,
    /** 몰수승 점수 */
    @Column(name = "forfeit_score")
    val forfeitScore: Int = 7,
    /** 선발 투수 승리 자격 최소 아웃 수 (기본 15 = 5이닝) */
    @Column(name = "starter_win_qualification_outs")
    val starterWinQualificationOuts: Int = 15,
    /** 규정 타석 배수 (팀당 경기 수 * 배수) */
    @Column(name = "qualification_pa_multiplier")
    val qualificationPAMultiplier: Double = 3.1,
    /** 규정 이닝 배수 (팀당 경기 수 * 배수) */
    @Column(name = "qualification_ip_multiplier")
    val qualificationIPMultiplier: Double = 1.0,
    /** 시간 제한 (분, null이면 무제한) */
    @Column(name = "time_limit_minutes")
    val timeLimitMinutes: Int? = null,
    /** 투구 수 제한 (null이면 무제한, 사회인 야구 투구 수 보호 규칙) */
    @Column(name = "pitch_count_limit")
    val pitchCountLimit: Int? = null,
    /** 투구 수 경고 임박 기준 (제한 대비 몇 구 전에 경고할지, 기본 10구) */
    @Column(name = "pitch_count_warning_threshold")
    val pitchCountWarningThreshold: Int = 10,
) {
    init {
        require(defaultInnings in 3..12) { "기본 이닝은 3~12 사이여야 합니다." }
        require(forfeitScore > 0) { "몰수승 점수는 양수여야 합니다." }
        require(starterWinQualificationOuts > 0) { "선발 승리 자격 아웃 수는 양수여야 합니다." }
        require(qualificationPAMultiplier > 0.0) { "규정 타석 배수는 양수여야 합니다." }
        require(qualificationIPMultiplier > 0.0) { "규정 이닝 배수는 양수여야 합니다." }

        if (mercyRuleEnabled) {
            val diff =
                requireNotNull(mercyRunDifference) { "콜드게임 활성화 시 득점 차이를 설정해야 합니다." }
            require(diff > 0) { "콜드게임 적용 득점 차이는 양수여야 합니다." }
            val minInning =
                requireNotNull(mercyMinimumInning) { "콜드게임 활성화 시 최소 이닝을 설정해야 합니다." }
            require(minInning in 1..defaultInnings) {
                "콜드게임 최소 이닝은 1 이상 기본 이닝($defaultInnings) 이하여야 합니다."
            }
        }

        maxExtraInnings?.let {
            require(it > 0) { "최대 연장 이닝 수는 양수여야 합니다." }
        }

        timeLimitMinutes?.let {
            require(it > 0) { "시간 제한은 양수여야 합니다." }
        }

        pitchCountLimit?.let {
            require(it > 0) { "투구 수 제한은 양수여야 합니다." }
        }

        require(pitchCountWarningThreshold > 0) { "투구 수 경고 임박 기준은 양수여야 합니다." }
    }
}
