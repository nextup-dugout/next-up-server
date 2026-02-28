package com.nextup.core.domain.competition

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.team.Team
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * 대회 등록 선수 엔티티
 *
 * 리그 대회에 등록된 선수를 나타냅니다.
 * 라인업 검증 시 부정선수 체크에 사용됩니다.
 */
@Entity
@Table(
    name = "competition_players",
    indexes = [
        Index(name = "idx_competition_players_competition", columnList = "competition_id"),
        Index(name = "idx_competition_players_team", columnList = "team_id"),
        Index(name = "idx_competition_players_player", columnList = "player_id"),
        Index(name = "idx_competition_players_status", columnList = "status"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_competition_players_competition_player",
            columnNames = ["competition_id", "player_id"],
        ),
    ],
)
class CompetitionPlayer private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    val competition: Competition,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Column(name = "registered_at", nullable = false)
    val registeredAt: Instant = Instant.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CompetitionPlayerStatus = CompetitionPlayerStatus.ACTIVE
        protected set

    /**
     * 선수를 출전 정지 처리합니다.
     */
    fun suspend() {
        require(status == CompetitionPlayerStatus.ACTIVE) {
            "활성 상태의 선수만 출전 정지할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        this.status = CompetitionPlayerStatus.SUSPENDED
    }

    /**
     * 선수를 출전 정지에서 복귀시킵니다.
     */
    fun reinstate() {
        require(status == CompetitionPlayerStatus.SUSPENDED) {
            "출전 정지 상태의 선수만 복귀시킬 수 있습니다. 현재 상태: ${status.displayName}"
        }
        this.status = CompetitionPlayerStatus.ACTIVE
    }

    /**
     * 선수 등록을 취소합니다.
     */
    fun withdraw() {
        require(status != CompetitionPlayerStatus.WITHDRAWN) {
            "이미 등록 취소된 선수입니다."
        }
        this.status = CompetitionPlayerStatus.WITHDRAWN
    }

    /**
     * 선수가 대회에 출전 가능한 상태인지 확인합니다.
     */
    val isEligible: Boolean
        get() = status == CompetitionPlayerStatus.ACTIVE

    companion object {
        /**
         * 대회 등록 선수를 생성합니다.
         */
        fun register(
            competition: Competition,
            team: Team,
            player: Player,
        ): CompetitionPlayer =
            CompetitionPlayer(
                competition = competition,
                team = team,
                player = player,
                registeredAt = Instant.now(),
            )
    }
}

/**
 * 대회 등록 선수 상태
 */
enum class CompetitionPlayerStatus(
    val displayName: String,
) {
    ACTIVE("활성"),
    SUSPENDED("출전 정지"),
    WITHDRAWN("등록 취소"),
}
