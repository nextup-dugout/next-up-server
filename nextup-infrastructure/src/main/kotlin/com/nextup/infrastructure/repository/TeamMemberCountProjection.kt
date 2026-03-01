package com.nextup.infrastructure.repository

/**
 * 팀별 멤버 수 집계 프로젝션
 *
 * TeamMemberRepository.countByTeamIdsAndStatus 배치 쿼리 결과 매핑용
 */
interface TeamMemberCountProjection {
    fun getTeamId(): Long

    fun getMemberCount(): Long
}
