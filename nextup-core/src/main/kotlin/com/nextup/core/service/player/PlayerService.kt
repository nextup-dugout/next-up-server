package com.nextup.core.service.player

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.PlayerRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 선수 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class PlayerService(
    private val playerRepository: PlayerRepositoryPort,
) {
    /**
     * ID로 선수를 조회합니다.
     */
    fun getById(id: Long): Player =
        playerRepository.findByIdOrNull(id)
            ?: throw PlayerNotFoundException(id)

    /**
     * 모든 선수를 조회합니다.
     */
    fun getAll(): List<Player> = playerRepository.findAll()

    /**
     * 이름으로 선수를 조회합니다.
     */
    fun getByName(name: String): List<Player> = playerRepository.findByName(name)

    /**
     * 이름에 포함된 문자열로 선수를 검색합니다.
     */
    fun searchByName(name: String): List<Player> = playerRepository.findByNameContaining(name)

    /**
     * 활성 선수 목록을 조회합니다.
     */
    fun getActivePlayers(): List<Player> = playerRepository.findActivePlayers()

    /**
     * 이름(부분 일치), 팀 ID, 포지션으로 선수를 검색합니다.
     *
     * @param name 이름 부분 검색 (null이면 조건 무시)
     * @param teamId 팀 ID 필터 (null이면 조건 무시)
     * @param position 포지션 필터 (null이면 조건 무시)
     * @param pageCommand 페이징 정보
     * @return 검색 결과 페이지
     */
    fun search(
        name: String?,
        teamId: Long?,
        position: Position?,
        pageCommand: PageCommand,
    ): PageResult<Player> =
        playerRepository.search(
            name = name,
            teamId = teamId,
            position = position,
            pageCommand = pageCommand,
        )
}
