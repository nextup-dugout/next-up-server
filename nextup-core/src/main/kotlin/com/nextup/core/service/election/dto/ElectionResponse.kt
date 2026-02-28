package com.nextup.core.service.election.dto

import com.nextup.core.domain.election.ActingOwnerPermissions
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.election.ElectionType
import java.time.Instant

/**
 * 선거 응답 DTO
 */
data class ElectionResponse(
    val id: Long,
    val teamId: Long,
    val title: String,
    val description: String?,
    val electionType: ElectionType,
    val startAt: Instant,
    val endAt: Instant,
    val status: ElectionStatus,
    val isVotingOpen: Boolean,
    val createdAt: Instant,
    /** 긴급 선거 발동 멤버 ID (EMERGENCY 타입만 사용) */
    val triggeredByMemberId: Long? = null,
    /** 임시 구단주 멤버 ID (EMERGENCY 타입만 사용) */
    val actingOwnerMemberId: Long? = null,
    /** 임시 구단주 권한 (EMERGENCY 타입만 사용) */
    val actingOwnerPermissions: ActingOwnerPermissions? = null,
    /** 정규 선거 마감 기한 (EMERGENCY 타입만 사용) */
    val regularElectionDeadline: Instant? = null,
)

/**
 * Election을 ElectionResponse로 변환합니다.
 */
fun Election.toResponse(): ElectionResponse =
    ElectionResponse(
        id = this.id,
        teamId = this.teamId,
        title = this.title,
        description = this.description,
        electionType = this.electionType,
        startAt = this.startAt,
        endAt = this.endAt,
        status = this.status,
        isVotingOpen = this.isVotingOpen(),
        createdAt = this.createdAt,
        triggeredByMemberId = this.triggeredByMemberId,
        actingOwnerMemberId = this.actingOwnerMemberId,
        actingOwnerPermissions = this.actingOwnerPermissions,
        regularElectionDeadline = this.regularElectionDeadline,
    )

/**
 * Election 리스트를 ElectionResponse 리스트로 변환합니다.
 */
fun List<Election>.toResponse(): List<ElectionResponse> = this.map { it.toResponse() }
