package com.nextup.core.domain.mercenary

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Position
import jakarta.persistence.*

/**
 * 용병 지원 엔티티
 *
 * 선수가 용병 요청에 지원하는 것을 나타냅니다.
 * Rich Domain Model 원칙에 따라 비즈니스 로직을 Entity 내부에 캡슐화합니다.
 */
@Entity
@Table(
    name = "mercenary_applications",
    indexes = [
        Index(name = "idx_ma_request_id", columnList = "request_id"),
        Index(name = "idx_ma_player_id", columnList = "player_id"),
        Index(name = "idx_ma_status", columnList = "status"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_ma_request_player",
            columnNames = ["request_id", "player_id"],
        ),
    ],
)
class MercenaryApplication private constructor(
    @Column(name = "request_id", nullable = false)
    val requestId: Long,
    @Column(name = "player_id", nullable = false)
    val playerId: Long,
    @ElementCollection(targetClass = Position::class)
    @CollectionTable(
        name = "mercenary_application_positions",
        joinColumns = [JoinColumn(name = "mercenary_application_id")],
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 30)
    val preferredPositions: MutableSet<Position> = mutableSetOf(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MercenaryApplicationStatus = MercenaryApplicationStatus.PENDING,
    @Column(columnDefinition = "TEXT")
    val message: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 지원을 수락합니다.
     */
    fun accept() {
        check(status == MercenaryApplicationStatus.PENDING) {
            "PENDING 상태의 지원만 수락할 수 있습니다"
        }
        this.status = MercenaryApplicationStatus.ACCEPTED
    }

    /**
     * 지원을 거절합니다.
     */
    fun reject() {
        check(status == MercenaryApplicationStatus.PENDING) {
            "PENDING 상태의 지원만 거절할 수 있습니다"
        }
        this.status = MercenaryApplicationStatus.REJECTED
    }

    companion object {
        /**
         * 용병 지원을 생성합니다.
         *
         * @param requestId 용병 요청 ID
         * @param playerId 지원 선수 ID
         * @param preferredPositions 선호 포지션 목록
         * @param message 지원 메시지
         * @return 생성된 MercenaryApplication
         */
        fun create(
            requestId: Long,
            playerId: Long,
            preferredPositions: Set<Position>,
            message: String? = null,
        ): MercenaryApplication {
            require(preferredPositions.isNotEmpty()) {
                "선호 포지션을 최소 1개 이상 지정해야 합니다"
            }

            return MercenaryApplication(
                requestId = requestId,
                playerId = playerId,
                preferredPositions = preferredPositions.toMutableSet(),
                message = message,
            )
        }
    }
}
