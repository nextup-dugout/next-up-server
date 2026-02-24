package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import jakarta.persistence.*
import java.time.Instant

/**
 * 라인업 제출 엔티티
 *
 * 경기 전 감독이 작성하고 기록원이 확인하는 라인업 제출 워크플로우를 나타냅니다.
 * 확인 완료 후 GamePlayer로 변환됩니다.
 */
@Entity
@Table(
    name = "lineup_submissions",
    indexes = [
        Index(name = "idx_lineup_submissions_game", columnList = "game_id"),
        Index(name = "idx_lineup_submissions_team", columnList = "team_id"),
        Index(name = "idx_lineup_submissions_status", columnList = "status"),
        Index(name = "idx_lineup_submissions_submitted_by", columnList = "submitted_by_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_lineup_submissions_game_team",
            columnNames = ["game_id", "team_id"],
        ),
    ],
)
class LineupSubmission private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    val submittedBy: User,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LineupSubmissionStatus = LineupSubmissionStatus.DRAFT
        protected set

    @Column(name = "submitted_at")
    var submittedAt: Instant? = null
        protected set

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    var confirmedBy: User? = null
        protected set

    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_id")
    var rejectedBy: User? = null
        protected set

    @OneToMany(
        mappedBy = "submission",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    private val _entries: MutableList<LineupEntry> = mutableListOf()

    val entries: List<LineupEntry> get() = _entries.toList()

    /**
     * 라인업을 기록원에게 제출합니다.
     *
     * 제출 전 라인업 검증을 수행합니다:
     * - 동일 선수 중복 등록 검증
     * - 포수(C) 필수 검증
     * - DH 규칙 검증
     * - 참석자만 라인업 등록 검증 (attendingPlayerIds 제공 시)
     *
     * @param attendingPlayerIds 참석(ATTENDING) 상태인 선수 ID 목록 (nullable, null이면 참석 검증 생략)
     * @throws com.nextup.common.exception.DuplicatePlayerInLineupException 동일 선수 중복 시
     * @throws com.nextup.common.exception.NoCatcherInLineupException 포수 미지정 시
     * @throws com.nextup.common.exception.InvalidDhRuleException DH 규칙 위반 시
     * @throws com.nextup.common.exception.NonAttendingPlayerInLineupException 참석하지 않는 선수 포함 시
     */
    fun submit(attendingPlayerIds: Set<Long>? = null) {
        require(status.canSubmit()) {
            "제출 가능한 상태가 아닙니다. 현재 상태: ${status.displayName}"
        }
        // 라인업 비즈니스 규칙 검증
        LineupValidator.validate(_entries, attendingPlayerIds)
        this.status = LineupSubmissionStatus.SUBMITTED
        this.submittedAt = Instant.now()
        // 재제출 시 이전 반려 정보 초기화
        this.rejectionReason = null
        this.rejectedBy = null
    }

    /**
     * 기록원이 라인업을 확인하고 승인합니다.
     */
    fun confirm(scorer: User) {
        require(status.canConfirm()) {
            "제출된 상태의 라인업만 확인할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        this.status = LineupSubmissionStatus.CONFIRMED
        this.confirmedAt = Instant.now()
        this.confirmedBy = scorer
    }

    /**
     * 양 팀 모두 제출 완료 시 라인업을 교환 상태로 전환합니다.
     *
     * SUBMITTED → EXCHANGED 전환.
     * 교환되면 상대팀이 이 라인업을 조회할 수 있습니다.
     *
     * @throws IllegalArgumentException SUBMITTED 상태가 아닐 때
     */
    fun exchange() {
        require(status.canExchange()) {
            "제출된 상태의 라인업만 교환할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        this.status = LineupSubmissionStatus.EXCHANGED
    }

    /**
     * 기록원이 라인업을 반려합니다.
     */
    fun reject(
        scorer: User,
        reason: String,
    ) {
        require(status.canReject()) {
            "제출된 상태의 라인업만 반려할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        require(reason.isNotBlank()) { "반려 사유는 필수입니다." }
        this.status = LineupSubmissionStatus.REJECTED
        this.rejectionReason = reason
        this.rejectedBy = scorer
    }

    /**
     * 라인업 엔트리를 추가합니다.
     */
    fun addEntry(entry: LineupEntry) {
        require(status.canEdit()) {
            "수정 가능한 상태가 아닙니다. 현재 상태: ${status.displayName}"
        }
        _entries.add(entry)
    }

    /**
     * 모든 라인업 엔트리를 제거합니다.
     */
    fun clearEntries() {
        require(status.canEdit()) {
            "수정 가능한 상태가 아닙니다. 현재 상태: ${status.displayName}"
        }
        _entries.clear()
    }

    companion object {
        /**
         * 라인업 제출을 생성합니다.
         */
        fun create(
            game: Game,
            team: Team,
            submittedBy: User,
        ): LineupSubmission =
            LineupSubmission(
                game = game,
                team = team,
                submittedBy = submittedBy,
            )
    }
}
