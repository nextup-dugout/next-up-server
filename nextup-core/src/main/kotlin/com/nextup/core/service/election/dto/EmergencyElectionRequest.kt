package com.nextup.core.service.election.dto

import java.time.Instant

/**
 * 긴급 선거 발동 요청 DTO
 *
 * MANAGER 역할 멤버가 구단주 부재 시 긴급 선거를 발동할 때 사용합니다.
 */
data class TriggerEmergencyElectionRequest(
    /** 팀 ID */
    val teamId: Long,
    /** 긴급 선거를 발동하는 MANAGER 멤버 ID */
    val requesterId: Long,
    /** 선거 제목 */
    val title: String,
    /** 선거 설명 */
    val description: String? = null,
    /** 긴급 선거 시작 시간 */
    val startAt: Instant,
    /** 긴급 선거 종료 시간 */
    val endAt: Instant,
)

/**
 * 임시 구단주 지정 요청 DTO
 *
 * 긴급 선거 상태에서 임시 구단주를 지정할 때 사용합니다.
 */
data class DesignateActingOwnerRequest(
    /** 긴급 선거 ID */
    val electionId: Long,
    /** 임시 구단주로 지정할 MANAGER 멤버 ID */
    val actingOwnerMemberId: Long,
)
