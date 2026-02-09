package com.nextup.core.domain.game

import com.nextup.common.exception.DuplicatePlayerInLineupException
import com.nextup.common.exception.InvalidDhRuleException
import com.nextup.common.exception.NoCatcherInLineupException
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
     * @throws DuplicatePlayerInLineupException 동일 선수가 중복 등록된 경우
     * @throws NoCatcherInLineupException 포수가 없는 경우
     * @throws InvalidDhRuleException DH 규칙 위반 시
     */
    fun validate(entries: List<LineupEntry>) {
        val starters = entries.filter { it.isStarter }
        validateNoDuplicatePlayers(entries)
        validateCatcherExists(starters)
        validateDhRule(starters)
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
}
