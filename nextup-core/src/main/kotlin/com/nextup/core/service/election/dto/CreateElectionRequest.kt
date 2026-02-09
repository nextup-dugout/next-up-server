package com.nextup.core.service.election.dto

import com.nextup.core.domain.election.ElectionType
import java.time.Instant

/**
 * 선거 생성 요청 DTO
 */
data class CreateElectionRequest(
    val teamId: Long,
    val title: String,
    val description: String?,
    val electionType: ElectionType,
    val startAt: Instant,
    val endAt: Instant,
)
