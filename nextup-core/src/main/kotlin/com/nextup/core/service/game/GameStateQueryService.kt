package com.nextup.core.service.game

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam

/**
 * 경기 상태 조회 서비스 인터페이스
 *
 * 기록원 재접속 시 현재 경기 상태를 복원하기 위한 조회 기능을 제공합니다.
 */
interface GameStateQueryService {
    /**
     * 경기를 조회합니다.
     *
     * @param gameId 경기 ID
     * @return Game 엔티티
     * @throws GameNotFoundException 경기가 존재하지 않는 경우
     */
    fun getGame(gameId: Long): Game

    /**
     * 경기의 현재 출전 중인 라인업을 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 현재 출전 중인 GamePlayer 목록
     */
    fun getCurrentLineup(gameId: Long): List<GamePlayer>

    /**
     * 경기의 GameTeam 목록을 조회합니다.
     *
     * @param gameId 경기 ID
     * @return GameTeam 목록 (홈팀, 원정팀)
     */
    fun getGameTeams(gameId: Long): List<GameTeam>
}
