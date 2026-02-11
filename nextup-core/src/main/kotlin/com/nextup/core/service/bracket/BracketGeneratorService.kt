package com.nextup.core.service.bracket

import com.nextup.core.domain.competition.BracketEntry

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
}
