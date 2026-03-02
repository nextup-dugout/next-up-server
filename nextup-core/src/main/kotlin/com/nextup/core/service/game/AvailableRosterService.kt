package com.nextup.core.service.game

import com.nextup.core.service.game.dto.AvailableRosterDto

/**
 * 라인업 제출용 로스터 조회 서비스 인터페이스
 *
 * 특정 경기의 팀 소속 선수 목록을 대회 등록/징계 상태와 함께 반환합니다.
 */
interface AvailableRosterService {
    /**
     * 특정 경기에 출전 가능한 팀의 선수 목록을 조회합니다.
     *
     * @param gameId 경기 ID
     * @param teamId 팀 ID
     * @return 로스터 선수 목록 (출전 가능 여부 포함)
     */
    fun getAvailableRoster(
        gameId: Long,
        teamId: Long,
    ): AvailableRosterDto
}
