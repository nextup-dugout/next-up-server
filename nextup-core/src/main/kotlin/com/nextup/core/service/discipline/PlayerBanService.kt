package com.nextup.core.service.discipline

import com.nextup.core.domain.discipline.PlayerBan

/**
 * 선수 제재(BAN) 서비스 인터페이스
 *
 * 기존 DisciplineService에서 WARNING/SUSPENSION을 제거하고
 * BAN만 남긴 최소 서비스입니다.
 */
interface PlayerBanService {
    /**
     * 영구 제재를 발급합니다.
     */
    fun issueBan(
        playerId: Long,
        competitionId: Long,
        reason: String,
        issuedBy: String,
    ): PlayerBan

    /**
     * ID로 제재를 조회합니다.
     */
    fun getById(id: Long): PlayerBan

    /**
     * 모든 제재를 조회합니다.
     */
    fun getAll(): List<PlayerBan>

    /**
     * 선수의 모든 제재를 조회합니다.
     */
    fun getBansByPlayer(playerId: Long): List<PlayerBan>

    /**
     * 대회의 모든 제재를 조회합니다.
     */
    fun getBansByCompetition(competitionId: Long): List<PlayerBan>

    /**
     * 선수의 대회별 제재를 조회합니다.
     */
    fun getBansByPlayerAndCompetition(
        playerId: Long,
        competitionId: Long,
    ): List<PlayerBan>

    /**
     * 선수가 출장 가능한지 확인합니다.
     * BAN이 있으면 출장 불가입니다.
     */
    fun canPlayerPlay(
        playerId: Long,
        competitionId: Long,
    ): Boolean

    /**
     * 제재를 삭제합니다.
     */
    fun deleteBan(id: Long)
}
