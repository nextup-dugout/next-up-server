package com.nextup.core.service.election.dto

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
    )

/**
 * Election 리스트를 ElectionResponse 리스트로 변환합니다.
 */
fun List<Election>.toResponse(): List<ElectionResponse> = this.map { it.toResponse() }
