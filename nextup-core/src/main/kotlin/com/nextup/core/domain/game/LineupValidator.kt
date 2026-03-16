package com.nextup.core.domain.game

import com.nextup.common.exception.DuplicatePlayerInLineupException
import com.nextup.common.exception.InvalidDhRuleException
import com.nextup.common.exception.InvalidLineupBattingOrderCountException
import com.nextup.common.exception.NoCatcherInLineupException
import com.nextup.common.exception.NonAttendingPlayerInLineupException
import com.nextup.common.exception.UnregisteredPlayerInLineupException
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.PositionCategory

/**
 * 라인업 검증 도메인 서비스
 *
 * 라인업 제출 시 비즈니스 규칙을 검증합니다.
 */
object LineupValidator {
    /**
     * 라인업 엔트리 전체를 검증합니다.
     *
     * @param entries 검증할 라인업 엔트리 목록
     * @param attendingPlayerIds 참석(ATTENDING) 상태인 선수 ID 목록 (nullable, null이면 검증 생략)
     * @param registeredPlayerIds 대회에 등록된 선수 ID 목록 (nullable, null이면 검증 생략)
     * @throws DuplicatePlayerInLineupException 동일 선수가 중복 등록된 경우
     * @throws NoCatcherInLineupException 포수가 없는 경우
     * @throws InvalidDhRuleException DH 규칙 위반 시
     * @throws NonAttendingPlayerInLineupException 참석하지 않는 선수가 라인업에 포함된 경우
     * @throws UnregisteredPlayerInLineupException 리그에 등록되지 않은 선수가 라인업에 포함된 경우
     */
    fun validate(
        entries: List<LineupEntry>,
        attendingPlayerIds: Set<Long>? = null,
        registeredPlayerIds: Set<Long>? = null,
    ) {
        val starters = entries.filter { it.isStarter }
        validateNoDuplicatePlayers(entries)
        validateCatcherExists(starters)
        validateDhRule(starters)
        validateBattingOrderCount(starters)
        if (attendingPlayerIds != null) {
            validateOnlyAttendingPlayers(entries, attendingPlayerIds)
        }
        if (registeredPlayerIds != null) {
            validateLeagueRegisteredPlayers(entries, registeredPlayerIds)
        }
    }

    /**
     * 동일 선수 중복 등록 검증
     *
     * 같은 playerId가 2번 이상 등록되면 예외를 발생시킵니다.
     */
    private fun validateNoDuplicatePlayers(entries: List<LineupEntry>) {
        val playerIds = entries.map { it.player.id }
        val duplicates = playerIds.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw DuplicatePlayerInLineupException(duplicates)
        }
    }

    /**
     * 포수(C) 필수 검증
     *
     * 선발 라인업에 포수가 최소 1명 있어야 합니다.
     */
    private fun validateCatcherExists(starters: List<LineupEntry>) {
        val hasCatcher = starters.any { it.position.category == PositionCategory.CATCHER }
        if (!hasCatcher) {
            throw NoCatcherInLineupException()
        }
    }

    /**
     * DH 규칙 검증
     *
     * DH 지정 시:
     * - DH가 있으면 투수는 타순에 없어야 합니다 (DH가 투수 대신 타격)
     * - DH 없이 투수가 타순에 있는 것은 허용됩니다 (투수 직접 타격)
     */
    private fun validateDhRule(starters: List<LineupEntry>) {
        val hasDh = starters.any { it.position == Position.DESIGNATED_HITTER }
        if (!hasDh) {
            return
        }

        val pitchersWithBattingOrder =
            starters.filter {
                it.position.category == PositionCategory.PITCHER && it.battingOrder != null
            }

        if (pitchersWithBattingOrder.isNotEmpty()) {
            throw InvalidDhRuleException(
                "DH가 지정된 경우 투수는 타순에 배치할 수 없습니다.",
            )
        }
    }

    /**
     * 타순 인원 수 검증 (M-7: DH 해제 후 타순 인원 검증)
     *
     * 선발 라인업의 타순에 배치된 선수가 정확히 9명이어야 합니다.
     * DH가 없는 경우 투수 포함 9명, DH가 있는 경우 투수 제외 DH 포함 9명.
     * DH 해제 후 타순 인원이 9명이 아닌 경우(8명 등) 예외를 발생시킵니다.
     *
     * @param starters 선발 라인업 엔트리 목록
     * @throws InvalidLineupBattingOrderCountException 타순 인원이 9명이 아닌 경우
     */
    private fun validateBattingOrderCount(starters: List<LineupEntry>) {
        val battersInOrder = starters.filter { it.battingOrder != null }
        if (battersInOrder.size != REQUIRED_BATTING_ORDER_COUNT) {
            throw InvalidLineupBattingOrderCountException(
                expected = REQUIRED_BATTING_ORDER_COUNT,
                actual = battersInOrder.size,
            )
        }
    }

    /** 타순에 필요한 선수 수 */
    private const val REQUIRED_BATTING_ORDER_COUNT = 9

    /**
     * 참석(ATTENDING) 선수만 라인업에 포함되었는지 검증
     *
     * AttendanceVote에서 ATTENDING 상태인 선수만 라인업에 등록 가능합니다.
     */
    private fun validateOnlyAttendingPlayers(
        entries: List<LineupEntry>,
        attendingPlayerIds: Set<Long>,
    ) {
        val lineupPlayerIds = entries.map { it.player.id }.toSet()
        val nonAttendingPlayerIds = lineupPlayerIds - attendingPlayerIds

        if (nonAttendingPlayerIds.isNotEmpty()) {
            throw NonAttendingPlayerInLineupException(nonAttendingPlayerIds)
        }
    }

    /**
     * 리그에 등록된 선수만 라인업에 포함되었는지 검증 (부정선수 체크)
     *
     * 대회에 등록(ACTIVE 상태)된 선수만 라인업에 등록 가능합니다.
     *
     * @param entries 검증할 라인업 엔트리 목록
     * @param registeredPlayerIds 대회에 등록된(ACTIVE) 선수 ID 목록
     * @throws UnregisteredPlayerInLineupException 미등록 선수가 라인업에 포함된 경우
     */
    fun validateLeagueRegisteredPlayers(
        entries: List<LineupEntry>,
        registeredPlayerIds: Set<Long>,
    ) {
        val lineupPlayerIds = entries.map { it.player.id }.toSet()
        val unregisteredPlayerIds = lineupPlayerIds - registeredPlayerIds

        if (unregisteredPlayerIds.isNotEmpty()) {
            throw UnregisteredPlayerInLineupException(unregisteredPlayerIds)
        }
    }
}
