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
    @Enumerated(EnumType.STRING)
    @Column(name = "absence_reason", length = 20)
    var absenceReason: AbsenceReason? = null,
    @Column(name = "reason_detail", length = 500)
    var reasonDetail: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 투표를 변경합니다.
     *
     * @param newVoteType 새로운 투표 유형
     * @param absenceReason 불참 사유 (불참/미정 시 선택 가능)
     * @param reasonDetail 상세 사유 (OTHER 선택 시 입력 가능)
     */
    fun changeVote(
        newVoteType: VoteType,
        absenceReason: AbsenceReason? = null,
        reasonDetail: String? = null,
    ) {
        check(poll.canVote()) { "투표를 변경할 수 없습니다. 마감되었거나 기한이 지났습니다." }
        validateAbsenceReason(newVoteType, absenceReason, reasonDetail)
        this.voteType = newVoteType
        this.absenceReason = absenceReason
        this.reasonDetail = reasonDetail
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
         * @param absenceReason 불참 사유 (불참/미정 시 선택 가능)
         * @param reasonDetail 상세 사유 (OTHER 선택 시 입력 가능)
         * @return 생성된 AttendanceVote
         */
        fun create(
            poll: AttendancePoll,
            player: Player,
            voteType: VoteType,
            absenceReason: AbsenceReason? = null,
            reasonDetail: String? = null,
        ): AttendanceVote {
            check(poll.canVote()) { "투표가 마감되었거나 기한이 지났습니다." }
            validateAbsenceReason(voteType, absenceReason, reasonDetail)

            return AttendanceVote(
                poll = poll,
                player = player,
                voteType = voteType,
                absenceReason = absenceReason,
                reasonDetail = reasonDetail,
            )
        }

        private fun validateAbsenceReason(
            voteType: VoteType,
            absenceReason: AbsenceReason?,
            reasonDetail: String?,
        ) {
            if (voteType == VoteType.ATTEND) {
                require(absenceReason == null) { "참석 투표 시 불참 사유를 입력할 수 없습니다." }
                require(reasonDetail == null) { "참석 투표 시 상세 사유를 입력할 수 없습니다." }
            }
            if (reasonDetail != null) {
                require(absenceReason == AbsenceReason.OTHER) {
                    "상세 사유는 기타(OTHER) 사유 선택 시에만 입력할 수 있습니다."
                }
            }
        }
    }
}
