package com.nextup.core.domain.discipline

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.player.Player
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 징계 엔티티
 *
 * 선수에 대한 징계 정보를 관리합니다.
 * - WARNING: 경고 (누적될 수 있음)
 * - SUSPENSION: 출장 정지 (특정 경기 수만큼)
 * - BAN: 영구 제재
 */
@Entity
@Table(
    name = "disciplines",
    indexes = [
        Index(name = "idx_disciplines_player_id", columnList = "player_id"),
        Index(name = "idx_disciplines_competition_id", columnList = "competition_id"),
        Index(name = "idx_disciplines_status", columnList = "status"),
        Index(
            name = "idx_disciplines_player_competition_status",
            columnList = "player_id, competition_id, status"
        ),
    ],
)
class Discipline private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    val competition: Competition,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: DisciplineType,
    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String,
    @Column(name = "suspension_games")
    val suspensionGames: Int? = null,
    @Column(nullable = false, name = "issued_at")
    val issuedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "expires_at")
    val expiresAt: LocalDateTime? = null,
    @Column(nullable = false, name = "issued_by", length = 255)
    val issuedBy: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DisciplineStatus = DisciplineStatus.ACTIVE,
    @Column(name = "served_games")
    var servedGames: Int = 0,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 징계를 취소합니다.
     */
    fun cancel() {
        require(status == DisciplineStatus.ACTIVE) {
            "활성 상태의 징계만 취소할 수 있습니다."
        }
        status = DisciplineStatus.CANCELLED
    }

    /**
     * 징계가 현재 유효한지 확인합니다.
     */
    fun isEffective(): Boolean {
        if (status != DisciplineStatus.ACTIVE) return false

        // 만료일이 설정된 경우 만료 확인
        expiresAt?.let {
            if (LocalDateTime.now().isAfter(it)) {
                return false
            }
        }

        // SUSPENSION의 경우 이행 경기 수 확인
        if (type == DisciplineType.SUSPENSION) {
            suspensionGames?.let {
                if (servedGames >= it) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * 출장 정지 징계의 경기를 하나 소화합니다.
     */
    fun incrementServedGames() {
        require(type == DisciplineType.SUSPENSION) {
            "출장 정지 징계만 경기를 소화할 수 있습니다."
        }
        require(status == DisciplineStatus.ACTIVE) {
            "활성 상태의 징계만 경기를 소화할 수 있습니다."
        }

        servedGames++

        // 모든 경기를 소화한 경우 이행 완료 처리
        suspensionGames?.let {
            if (servedGames >= it) {
                markServed()
            }
        }
    }

    /**
     * 징계를 이행 완료 처리합니다.
     */
    fun markServed() {
        require(type == DisciplineType.SUSPENSION) {
            "출장 정지 징계만 이행 완료 처리할 수 있습니다."
        }
        require(status == DisciplineStatus.ACTIVE) {
            "활성 상태의 징계만 이행 완료 처리할 수 있습니다."
        }

        status = DisciplineStatus.SERVED
    }

    companion object {
        /**
         * 경고 징계를 생성합니다.
         */
        fun createWarning(
            player: Player,
            competition: Competition,
            reason: String,
            issuedBy: String,
            expiresAt: LocalDateTime? = null,
        ): Discipline {
            require(reason.isNotBlank()) { "징계 사유는 필수입니다." }
            require(issuedBy.isNotBlank()) { "징계 발급자는 필수입니다." }

            return Discipline(
                player = player,
                competition = competition,
                type = DisciplineType.WARNING,
                reason = reason,
                issuedBy = issuedBy,
                expiresAt = expiresAt,
            )
        }

        /**
         * 출장 정지 징계를 생성합니다.
         */
        fun createSuspension(
            player: Player,
            competition: Competition,
            reason: String,
            suspensionGames: Int,
            issuedBy: String,
        ): Discipline {
            require(reason.isNotBlank()) { "징계 사유는 필수입니다." }
            require(issuedBy.isNotBlank()) { "징계 발급자는 필수입니다." }
            require(suspensionGames > 0) { "출장 정지 경기 수는 1 이상이어야 합니다." }

            return Discipline(
                player = player,
                competition = competition,
                type = DisciplineType.SUSPENSION,
                reason = reason,
                suspensionGames = suspensionGames,
                issuedBy = issuedBy,
            )
        }

        /**
         * 영구 제재 징계를 생성합니다.
         */
        fun createBan(
            player: Player,
            competition: Competition,
            reason: String,
            issuedBy: String,
        ): Discipline {
            require(reason.isNotBlank()) { "징계 사유는 필수입니다." }
            require(issuedBy.isNotBlank()) { "징계 발급자는 필수입니다." }

            return Discipline(
                player = player,
                competition = competition,
                type = DisciplineType.BAN,
                reason = reason,
                issuedBy = issuedBy,
            )
        }
    }
}
