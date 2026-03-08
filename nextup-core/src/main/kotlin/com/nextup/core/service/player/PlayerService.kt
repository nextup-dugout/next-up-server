package com.nextup.core.service.player

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.common.exception.PlayerNotLinkedException
import com.nextup.common.exception.UserNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
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
    private val userRepository: UserRepositoryPort,
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

    /**
     * 인증된 사용자의 연결된 선수를 조회합니다.
     *
     * @param userId 인증된 사용자 ID
     * @return 연결된 선수
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws PlayerNotLinkedException 연결된 선수가 없는 경우
     */
    fun getLinkedPlayer(userId: Long): Player {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(userId)
        return user.player
            ?: throw PlayerNotLinkedException(userId)
    }

    /**
     * 인증된 사용자의 선수 프로필을 수정합니다.
     *
     * @param userId 인증된 사용자 ID
     * @param primaryPosition 주 포지션
     * @param throwingHand 투구 손
     * @param battingHand 타격 손
     * @param height 키 (cm)
     * @param weight 몸무게 (kg)
     * @return 수정된 선수
     */
    @Transactional
    fun updatePlayerProfile(
        userId: Long,
        primaryPosition: Position?,
        throwingHand: ThrowingHand?,
        battingHand: BattingHand?,
        height: Int?,
        weight: Int?,
    ): Player {
        val player = getLinkedPlayer(userId)
        player.updatePlayerProfile(
            primaryPosition = primaryPosition ?: player.primaryPosition,
            throwingHand = throwingHand,
            battingHand = battingHand,
            height = height,
            weight = weight,
        )
        return player
    }
}
