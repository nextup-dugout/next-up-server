package com.nextup.core.service.player

import com.nextup.common.exception.*
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.domain.player.PlayerTeamStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.PlayerTeamHistoryRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 선수-팀 소속 관계 관리 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 * 사회인 야구에서는 "이적" 개념이 없으므로, 팀 변경은 탈퇴 + 새 팀 가입으로 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class PlayerTeamService(
    private val playerTeamHistoryRepository: PlayerTeamHistoryRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
) {
    // ========== CREATE ==========

    /**
     * 선수를 팀에 소속시킵니다.
     *
     * @param playerId 선수 ID
     * @param teamId 팀 ID
     * @param startDate 소속 시작일
     * @param position 포지션
     * @param uniformNumber 등번호 (선택)
     * @return 생성된 소속 이력
     * @throws PlayerNotFoundException 선수를 찾을 수 없는 경우
     * @throws TeamNotFoundException 팀을 찾을 수 없는 경우
     * @throws PlayerAlreadyInLeagueException 선수가 이미 해당 리그에 소속된 경우
     */
    @Transactional
    fun registerAffiliation(
        playerId: Long,
        teamId: Long,
        startDate: LocalDate,
        position: Position,
        uniformNumber: Int? = null,
    ): PlayerTeamHistory {
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        val team =
            teamRepository.findByIdWithLeague(teamId)
                ?: throw TeamNotFoundException(teamId)

        val leagueId = team.league.id

        // 같은 리그 내 중복 검증
        if (playerTeamHistoryRepository.existsActiveByPlayerIdAndLeagueId(playerId, leagueId)) {
            throw PlayerAlreadyInLeagueException(playerId, leagueId)
        }

        val affiliation =
            PlayerTeamHistory(
                player = player,
                team = team,
                startDate = startDate,
                position = position,
                uniformNumber = uniformNumber,
                status = PlayerTeamStatus.ACTIVE,
            )

        return playerTeamHistoryRepository.save(affiliation)
    }

    // ========== UPDATE ==========

    /**
     * 선수의 소속을 종료합니다 (탈퇴 처리).
     *
     * @param affiliationId 소속 이력 ID
     * @param endDate 종료일
     * @return 업데이트된 소속 이력
     * @throws PlayerTeamHistoryNotFoundException 소속 이력을 찾을 수 없는 경우
     */
    @Transactional
    fun endAffiliation(
        affiliationId: Long,
        endDate: LocalDate,
    ): PlayerTeamHistory {
        val affiliation = findAffiliationById(affiliationId)
        affiliation.deactivate(endDate)
        return affiliation
    }

    /**
     * 등번호를 변경합니다.
     *
     * @param affiliationId 소속 이력 ID
     * @param uniformNumber 새 등번호
     * @return 업데이트된 소속 이력
     */
    @Transactional
    fun changeUniformNumber(
        affiliationId: Long,
        uniformNumber: Int,
    ): PlayerTeamHistory {
        val affiliation = findAffiliationById(affiliationId)
        affiliation.changeUniformNumber(uniformNumber)
        return affiliation
    }

    /**
     * 포지션을 변경합니다.
     *
     * @param affiliationId 소속 이력 ID
     * @param position 새 포지션
     * @return 업데이트된 소속 이력
     */
    @Transactional
    fun changePosition(
        affiliationId: Long,
        position: Position,
    ): PlayerTeamHistory {
        val affiliation = findAffiliationById(affiliationId)
        affiliation.changePosition(position)
        return affiliation
    }

    // ========== READ ==========

    /**
     * 선수의 활성 소속 목록을 조회합니다.
     *
     * @param playerId 선수 ID
     * @return 활성 소속 이력 목록
     */
    fun getActiveAffiliationsByPlayer(playerId: Long): List<PlayerTeamHistory> {
        // 선수 존재 여부 확인
        playerRepository.findByIdOrNull(playerId)
            ?: throw PlayerNotFoundException(playerId)

        return playerTeamHistoryRepository.findActiveByPlayerId(playerId)
    }

    /**
     * 팀의 활성 선수 로스터를 조회합니다.
     *
     * @param teamId 팀 ID
     * @return 팀 로스터 (활성 소속 이력 목록)
     */
    fun getTeamRoster(teamId: Long): List<PlayerTeamHistory> {
        // 팀 존재 여부 확인
        teamRepository.findByIdWithLeague(teamId)
            ?: throw TeamNotFoundException(teamId)

        return playerTeamHistoryRepository.findActiveByTeamId(teamId)
    }

    /**
     * 특정 날짜 기준 팀의 선수 목록을 조회합니다.
     *
     * @param teamId 팀 ID
     * @param date 기준 날짜
     * @return 해당 날짜의 팀 로스터
     */
    fun getTeamRosterAtDate(
        teamId: Long,
        date: LocalDate,
    ): List<PlayerTeamHistory> {
        // 팀 존재 여부 확인
        teamRepository.findByIdWithLeague(teamId)
            ?: throw TeamNotFoundException(teamId)

        return playerTeamHistoryRepository.findByTeamIdAtDate(teamId, date)
    }

    /**
     * 선수의 모든 소속 이력을 조회합니다.
     *
     * @param playerId 선수 ID
     * @return 소속 이력 목록
     */
    fun getPlayerHistory(playerId: Long): List<PlayerTeamHistory> {
        // 선수 존재 여부 확인
        playerRepository.findByIdOrNull(playerId)
            ?: throw PlayerNotFoundException(playerId)

        return playerTeamHistoryRepository.findByPlayerIdWithDetails(playerId)
    }

    // ========== 유틸리티 ==========

    /**
     * ID로 소속 이력을 조회합니다.
     */
    private fun findAffiliationById(id: Long): PlayerTeamHistory =
        playerTeamHistoryRepository.findByIdOrNull(id)
            ?: throw PlayerTeamHistoryNotFoundException(id)
}
