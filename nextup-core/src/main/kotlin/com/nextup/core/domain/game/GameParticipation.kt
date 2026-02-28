package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.attendance.AbsenceReason
import com.nextup.core.domain.team.TeamMember
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 출석 투표 엔티티
 *
 * 경기별 출석 투표를 관리합니다.
 */
@Entity
@Table(
    name = "attendance_votes",
    indexes = [
        Index(name = "idx_av_game_id", columnList = "game_id"),
        Index(name = "idx_av_member_id", columnList = "member_id"),
        Index(name = "idx_av_game_status", columnList = "game_id, status"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_av_game_member", columnNames = ["game_id", "member_id"]),
    ],
)
class GameParticipation private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: TeamMember,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AttendanceStatus = AttendanceStatus.UNDECIDED,
    @Enumerated(EnumType.STRING)
    @Column(name = "absence_reason", length = 20)
    var absenceReason: AbsenceReason? = null,
    @Column(name = "reason_detail", length = 500)
    var reasonDetail: String? = null,
    @Column(name = "responded_at")
    var respondedAt: LocalDateTime? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 응답했는지 확인합니다.
     */
    val hasResponded: Boolean
        get() = respondedAt != null

    /**
     * 참석 투표인지 확인합니다.
     */
    val isAttending: Boolean
        get() = status == AttendanceStatus.ATTENDING

    /**
     * 투표합니다.
     *
     * @param newStatus 투표 상태
     * @param absenceReason 불참 사유 (불참/미정 시 선택 가능)
     * @param reasonDetail 상세 사유 (OTHER 선택 시 입력 가능)
     * @throws IllegalStateException 투표 권한이 없는 경우
     */
    fun vote(
        newStatus: AttendanceStatus,
        absenceReason: AbsenceReason? = null,
        reasonDetail: String? = null,
    ) {
        check(this.member.canVote) {
            "투표 권한이 없는 회원입니다."
        }
        validateAbsenceReason(newStatus, absenceReason, reasonDetail)
        this.status = newStatus
        this.absenceReason = absenceReason
        this.reasonDetail = reasonDetail
        this.respondedAt = LocalDateTime.now()
    }

    /**
     * 투표를 변경합니다.
     *
     * @param newStatus 새로운 투표 상태
     * @param absenceReason 불참 사유 (불참/미정 시 선택 가능)
     * @param reasonDetail 상세 사유 (OTHER 선택 시 입력 가능)
     * @throws IllegalStateException 아직 투표하지 않은 경우
     */
    fun changeVote(
        newStatus: AttendanceStatus,
        absenceReason: AbsenceReason? = null,
        reasonDetail: String? = null,
    ) {
        check(this.hasResponded) {
            "아직 투표하지 않았습니다."
        }
        validateAbsenceReason(newStatus, absenceReason, reasonDetail)
        this.status = newStatus
        this.absenceReason = absenceReason
        this.reasonDetail = reasonDetail
        this.respondedAt = LocalDateTime.now()
    }

    private fun validateAbsenceReason(
        status: AttendanceStatus,
        absenceReason: AbsenceReason?,
        reasonDetail: String?,
    ) {
        if (status == AttendanceStatus.ATTENDING) {
            require(absenceReason == null) { "참석 투표 시 불참 사유를 입력할 수 없습니다." }
            require(reasonDetail == null) { "참석 투표 시 상세 사유를 입력할 수 없습니다." }
        }
        if (reasonDetail != null) {
            require(absenceReason == AbsenceReason.OTHER) {
                "상세 사유는 기타(OTHER) 사유 선택 시에만 입력할 수 있습니다."
            }
        }
    }

    companion object {
        /**
         * 경기에 대한 출석 투표를 생성합니다.
         *
         * @param game 경기
         * @param member 팀 멤버
         * @return 생성된 GameParticipation
         */
        fun createForGame(
            game: Game,
            member: TeamMember,
        ): GameParticipation =
            GameParticipation(
                game = game,
                member = member,
                status = AttendanceStatus.UNDECIDED,
            )
    }
}
