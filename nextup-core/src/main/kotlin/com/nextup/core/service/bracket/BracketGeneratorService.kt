package com.nextup.core.service.bracket

import com.nextup.core.domain.competition.BracketEntry
import java.time.LocalDateTime

/**
 * 대진표 생성 서비스 인터페이스
 */
interface BracketGeneratorService {
    /**
     * 단일 토너먼트 대진표를 생성합니다.
     */
    fun generateSingleElimination(
        competitionId: Long,
        seededTeamIds: List<Long>,
    ): List<BracketEntry>

    /**
     * 더블 토너먼트 대진표를 생성합니다.
     */
    fun generateDoubleElimination(
        competitionId: Long,
        seededTeamIds: List<Long>,
    ): List<BracketEntry>

    /**
     * 대진표를 조회합니다.
     */
    fun getBracket(competitionId: Long): List<BracketEntry>

    /**
     * 승자를 진행시킵니다.
     */
    fun advanceWinner(
        bracketEntryId: Long,
        winnerTeamId: Long,
    ): BracketEntry

    /**
     * 대진표 엔트리에 해당하는 경기를 생성하고 연결합니다.
     *
     * @param bracketEntryId 대진표 엔트리 ID
     * @param scheduledAt 경기 예정 일시
     * @param location 경기 장소 (선택)
     * @param fieldName 구장 이름 (선택)
     * @return 경기가 연결된 대진표 엔트리
     */
    fun createGameForBracketEntry(
        bracketEntryId: Long,
        scheduledAt: LocalDateTime,
        location: String? = null,
        fieldName: String? = null,
    ): BracketEntry
}
