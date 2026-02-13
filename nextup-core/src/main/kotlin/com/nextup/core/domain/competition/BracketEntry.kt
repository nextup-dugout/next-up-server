package com.nextup.core.domain.competition

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*

/**
 * 대진표 엔트리 엔티티
 *
 * 토너먼트 대회의 각 매치를 나타냅니다.
 */
@Entity
@Table(
    name = "bracket_entries",
    indexes = [
        Index(name = "idx_bracket_entries_competition", columnList = "competition_id"),
    ],
)
class BracketEntry(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    val competition: Competition,
    @Column(nullable = false)
    val roundNumber: Int,
    @Column(nullable = false)
    val matchNumber: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team1_id")
    val team1: Team?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team2_id")
    val team2: Team?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    var winner: Team? = null,
    @Column(nullable = false, length = 20)
    val bracketType: String = "WINNERS",
    @Column(nullable = true)
    val seed1: Int? = null,
    @Column(nullable = true)
    val seed2: Int? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 승자를 기록합니다.
     */
    fun recordWinner(winnerTeam: Team) {
        require(winner == null) { "이미 승자가 결정된 경기입니다" }
        require(winnerTeam == team1 || winnerTeam == team2) { "참가팀만 승자로 지정할 수 있습니다" }
        winner = winnerTeam
    }

    /**
     * 부전승 경기인지 확인합니다.
     */
    fun isBye(): Boolean = team1 == null || team2 == null

    /**
     * 경기가 완료되었는지 확인합니다.
     */
    fun isCompleted(): Boolean = winner != null || isBye()
}
