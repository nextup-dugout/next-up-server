package com.nextup.core.domain.mercenary

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Position
import jakarta.persistence.*
import java.time.Instant

/**
 * 용병 요청 엔티티
 *
 * 팀이 특정 경기에 용병을 구하기 위해 생성하는 요청입니다.
 * Rich Domain Model 원칙에 따라 비즈니스 로직을 Entity 내부에 캡슐화합니다.
 */
@Entity
@Table(
    name = "mercenary_requests",
    indexes = [
        Index(name = "idx_mr_requesting_team_id", columnList = "requesting_team_id"),
        Index(name = "idx_mr_game_id", columnList = "game_id"),
        Index(name = "idx_mr_status", columnList = "status"),
        Index(name = "idx_mr_deadline", columnList = "deadline"),
    ],
)
class MercenaryRequest private constructor(
    @Column(name = "requesting_team_id", nullable = false)
    val requestingTeamId: Long,
    @Column(name = "game_id", nullable = false)
    val gameId: Long,
    @ElementCollection(targetClass = Position::class)
    @CollectionTable(
        name = "mercenary_request_positions",
        joinColumns = [JoinColumn(name = "mercenary_request_id")],
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 30)
    val positions: MutableSet<Position> = mutableSetOf(),
    @Column(name = "max_count", nullable = false)
    val maxCount: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MercenaryRequestStatus = MercenaryRequestStatus.OPEN,
    @Column(nullable = false)
    val deadline: Instant,
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Version
    var version: Long = 0
        protected set

    /**
     * 마감 시한이 지났는지 확인합니다.
     */
    fun isExpired(): Boolean = Instant.now().isAfter(deadline)

    /**
     * 지원을 받을 수 있는 상태인지 확인합니다.
     */
    fun canAcceptApplication(): Boolean = status == MercenaryRequestStatus.OPEN && !isExpired()

    /**
     * 용병 요청을 마감합니다.
     */
    fun close() {
        check(status == MercenaryRequestStatus.OPEN) {
            "OPEN 상태의 요청만 마감할 수 있습니다"
        }
        this.status = MercenaryRequestStatus.CLOSED
    }

    /**
     * 용병 요청을 취소합니다.
     */
    fun cancel() {
        check(status == MercenaryRequestStatus.OPEN) {
            "OPEN 상태의 요청만 취소할 수 있습니다"
        }
        this.status = MercenaryRequestStatus.CANCELLED
    }

    companion object {
        /**
         * 용병 요청을 생성합니다.
         *
         * @param requestingTeamId 요청 팀 ID
         * @param gameId 경기 ID
         * @param positions 필요 포지션 목록
         * @param maxCount 최대 모집 인원
         * @param deadline 마감 시한
         * @param description 설명
         * @return 생성된 MercenaryRequest
         */
        fun create(
            requestingTeamId: Long,
            gameId: Long,
            positions: Set<Position>,
            maxCount: Int,
            deadline: Instant,
            description: String? = null,
        ): MercenaryRequest {
            require(positions.isNotEmpty()) {
                "필요 포지션을 최소 1개 이상 지정해야 합니다"
            }
            require(maxCount > 0) {
                "최대 모집 인원은 1명 이상이어야 합니다"
            }
            require(deadline.isAfter(Instant.now())) {
                "마감 시한은 현재 시간 이후여야 합니다"
            }

            return MercenaryRequest(
                requestingTeamId = requestingTeamId,
                gameId = gameId,
                positions = positions.toMutableSet(),
                maxCount = maxCount,
                deadline = deadline,
                description = description,
            )
        }
    }
}
