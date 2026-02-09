package com.nextup.core.domain.attendance

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import jakarta.persistence.*

/**
 * 출석 투표 응답 엔티티
 *
 * 선수의 특정 출석 투표에 대한 응답을 관리합니다.
 */
@Entity
@Table(
    name = "poll_votes",
    indexes = [
        Index(name = "idx_pv_poll_id", columnList = "poll_id"),
        Index(name = "idx_pv_player_id", columnList = "player_id"),
        Index(name = "idx_pv_vote_type", columnList = "vote_type"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_pv_poll_player",
            columnNames = ["poll_id", "player_id"],
        ),
    ],
)
class AttendanceVote private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    val poll: AttendancePoll,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 20)
    var voteType: VoteType,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 투표를 변경합니다.
     */
    fun changeVote(newVoteType: VoteType) {
        check(poll.canVote()) { "투표를 변경할 수 없습니다. 마감되었거나 기한이 지났습니다." }
        this.voteType = newVoteType
    }

    /**
     * 참석 의사를 표시했는지 확인합니다.
     */
    fun isAttending(): Boolean = voteType == VoteType.ATTEND

    /**
     * 불참 의사를 표시했는지 확인합니다.
     */
    fun isAbsent(): Boolean = voteType == VoteType.ABSENT

    companion object {
        /**
         * 출석 투표를 생성합니다.
         *
         * @param poll 출석 투표
         * @param player 선수
         * @param voteType 투표 유형
         * @return 생성된 AttendanceVote
         */
        fun create(
            poll: AttendancePoll,
            player: Player,
            voteType: VoteType,
        ): AttendanceVote {
            check(poll.canVote()) { "투표가 마감되었거나 기한이 지났습니다." }

            return AttendanceVote(
                poll = poll,
                player = player,
                voteType = voteType,
            )
        }
    }
}
