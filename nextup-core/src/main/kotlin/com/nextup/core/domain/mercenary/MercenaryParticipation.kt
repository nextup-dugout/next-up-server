package com.nextup.core.domain.mercenary

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*

/**
 * 용병 참가 엔티티
 *
 * 용병으로 실제 경기에 참가한 기록을 나타냅니다.
 * 지원이 수락되면 자동으로 생성됩니다.
 */
@Entity
@Table(
    name = "mercenary_participations",
    indexes = [
        Index(name = "idx_mp_game_id", columnList = "game_id"),
        Index(name = "idx_mp_player_id", columnList = "player_id"),
        Index(name = "idx_mp_team_id", columnList = "team_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_mp_game_player",
            columnNames = ["game_id", "player_id"],
        ),
    ],
)
class MercenaryParticipation private constructor(
    @Column(name = "game_id", nullable = false)
    val gameId: Long,
    @Column(name = "player_id", nullable = false)
    val playerId: Long,
    @Column(name = "team_id", nullable = false)
    val teamId: Long,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        /**
         * 용병 참가 기록을 생성합니다.
         *
         * @param gameId 경기 ID
         * @param playerId 선수 ID
         * @param teamId 팀 ID
         * @return 생성된 MercenaryParticipation
         */
        fun create(
            gameId: Long,
            playerId: Long,
            teamId: Long,
        ): MercenaryParticipation =
            MercenaryParticipation(
                gameId = gameId,
                playerId = playerId,
                teamId = teamId,
            )
    }
}
