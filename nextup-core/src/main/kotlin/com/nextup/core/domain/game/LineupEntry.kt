package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import jakarta.persistence.*

/**
 * 라인업 엔트리 엔티티
 *
 * LineupSubmission에 포함되는 개별 선수 출전 정보를 나타냅니다.
 * 확인 완료 후 GamePlayer로 변환됩니다.
 */
@Entity
@Table(
    name = "lineup_entries",
    indexes = [
        Index(name = "idx_lineup_entries_submission", columnList = "submission_id"),
        Index(name = "idx_lineup_entries_player", columnList = "player_id"),
        Index(name = "idx_lineup_entries_batting_order", columnList = "submission_id, batting_order"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_lineup_entries_submission_player",
            columnNames = ["submission_id", "player_id"],
        ),
        UniqueConstraint(
            name = "uk_lineup_entries_submission_batting_order",
            columnNames = ["submission_id", "batting_order"],
        ),
    ],
)
class LineupEntry(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    val submission: LineupSubmission,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val position: Position,
    @Column(name = "batting_order")
    val battingOrder: Int?,
    @Column(name = "back_number")
    val backNumber: Int?,
    @Column(name = "is_starter", nullable = false)
    val isStarter: Boolean = true,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity()
