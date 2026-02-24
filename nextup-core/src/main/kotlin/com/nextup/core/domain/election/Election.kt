package com.nextup.core.domain.election

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 선거 엔티티
 *
 * 팀 내에서 진행되는 선거/투표를 나타냅니다.
 * EMERGENCY 타입의 경우 임시 구단주(Acting Owner) 지정 및 14일 이내 정규 선거 자동 생성을 지원합니다.
 */
@Entity
@Table(
    name = "elections",
    indexes = [
        Index(name = "idx_elections_team", columnList = "team_id"),
        Index(name = "idx_elections_status", columnList = "status"),
        Index(name = "idx_elections_start_at", columnList = "start_at"),
        Index(name = "idx_elections_end_at", columnList = "end_at"),
    ],
)
class Election private constructor(
    @Column(name = "team_id", nullable = false)
    val teamId: Long,
    @Column(nullable = false, length = 200)
    val title: String,
    @Column(length = 1000)
    val description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "election_type", nullable = false, length = 50)
    val electionType: ElectionType,
    @Column(name = "start_at", nullable = false)
    val startAt: Instant,
    @Column(name = "end_at", nullable = false)
    val endAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: ElectionStatus = ElectionStatus.SCHEDULED,
    /** 비상대책위원회 모드: 긴급 선거를 발동한 MANAGER 멤버 ID */
    @Column(name = "triggered_by_member_id")
    val triggeredByMemberId: Long? = null,
    /** 비상대책위원회 모드: 임시 구단주로 지정된 멤버 ID */
    @Column(name = "acting_owner_member_id")
    var actingOwnerMemberId: Long? = null,
    /** 비상대책위원회 모드: 임시 구단주 권한 */
    @Embedded
    var actingOwnerPermissions: ActingOwnerPermissions? = null,
    /** 비상대책위원회 모드: 정규 선거 마감 기한 (긴급 선거 발동 후 14일) */
    @Column(name = "regular_election_deadline")
    val regularElectionDeadline: Instant? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 투표가 진행 중인지 확인합니다.
     */
    fun isVotingOpen(): Boolean {
        val now = Instant.now()
        return status == ElectionStatus.IN_PROGRESS && now.isAfter(startAt) && now.isBefore(endAt)
    }

    /**
     * 선거를 시작합니다.
     *
     * @throws IllegalStateException 이미 시작되었거나 취소된 경우
     */
    fun start() {
        require(status == ElectionStatus.SCHEDULED) {
            "Cannot start election: current status is $status"
        }
        status = ElectionStatus.IN_PROGRESS
    }

    /**
     * 선거를 완료합니다.
     *
     * @throws IllegalStateException 진행 중이 아닌 경우
     */
    fun complete() {
        require(status == ElectionStatus.IN_PROGRESS) {
            "Cannot complete election: current status is $status"
        }
        status = ElectionStatus.COMPLETED
    }

    /**
     * 선거를 취소합니다.
     *
     * @throws IllegalStateException 이미 완료되었거나 취소된 경우
     */
    fun cancel() {
        require(status != ElectionStatus.COMPLETED && status != ElectionStatus.CANCELLED) {
            "Cannot cancel election: current status is $status"
        }
        status = ElectionStatus.CANCELLED
    }

    /**
     * 긴급 선거인지 확인합니다.
     */
    fun isEmergency(): Boolean = electionType == ElectionType.EMERGENCY

    /**
     * 임시 구단주가 지정되었는지 확인합니다.
     */
    fun hasActingOwner(): Boolean = actingOwnerMemberId != null

    /**
     * 임시 구단주를 지정합니다.
     *
     * 긴급 선거(EMERGENCY) 상태에서만 가능하며, 대상은 MANAGER 역할의 멤버여야 합니다.
     *
     * @param memberId 임시 구단주로 지정할 멤버 ID
     * @throws IllegalStateException 긴급 선거가 아니거나 이미 임시 구단주가 지정된 경우
     */
    fun designateActingOwner(memberId: Long) {
        require(isEmergency()) {
            "임시 구단주 지정은 긴급 선거(EMERGENCY)에서만 가능합니다."
        }
        require(status == ElectionStatus.IN_PROGRESS || status == ElectionStatus.SCHEDULED) {
            "Cannot designate acting owner: current status is $status"
        }
        require(!hasActingOwner()) {
            "이미 임시 구단주(memberId=$actingOwnerMemberId)가 지정되어 있습니다."
        }
        actingOwnerMemberId = memberId
        actingOwnerPermissions = ActingOwnerPermissions.default()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Election) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "Election(id=$id, teamId=$teamId, title='$title', type=$electionType, status=$status)"

    companion object {
        /** 정규 선거 마감 기한 (긴급 선거 발동 후 일수) */
        const val REGULAR_ELECTION_DEADLINE_DAYS = 14L

        /**
         * Election을 생성합니다.
         *
         * @param teamId 팀 ID
         * @param title 선거 제목
         * @param description 선거 설명
         * @param electionType 선거 유형
         * @param startAt 시작 시간
         * @param endAt 종료 시간
         * @return 생성된 Election
         * @throws IllegalArgumentException 제목이 비어있거나 종료 시간이 시작 시간보다 이전인 경우
         */
        fun create(
            teamId: Long,
            title: String,
            description: String? = null,
            electionType: ElectionType,
            startAt: Instant,
            endAt: Instant,
        ): Election {
            require(title.isNotBlank()) { "Title cannot be blank" }
            require(endAt.isAfter(startAt)) {
                "End time must be after start time"
            }

            return Election(
                teamId = teamId,
                title = title,
                description = description,
                electionType = electionType,
                startAt = startAt,
                endAt = endAt,
            )
        }

        /**
         * 긴급 선거(비상대책위원회 모드)를 생성합니다.
         *
         * MANAGER 역할 멤버가 구단주 부재 시 긴급 선거를 발동합니다.
         * 발동 즉시 IN_PROGRESS 상태로 시작하며, 14일 이내 정규 선거를 자동 생성해야 합니다.
         *
         * @param teamId 팀 ID
         * @param triggeredByMemberId 발동한 MANAGER 멤버 ID
         * @param title 선거 제목
         * @param description 선거 설명
         * @param startAt 시작 시간
         * @param endAt 종료 시간
         * @return 생성된 긴급 선거 (IN_PROGRESS 상태)
         * @throws IllegalArgumentException 제목이 비어있거나 종료 시간이 시작 시간보다 이전인 경우
         */
        fun createEmergency(
            teamId: Long,
            triggeredByMemberId: Long,
            title: String,
            description: String? = null,
            startAt: Instant,
            endAt: Instant,
        ): Election {
            require(title.isNotBlank()) { "Title cannot be blank" }
            require(endAt.isAfter(startAt)) {
                "End time must be after start time"
            }

            val now = Instant.now()
            val regularElectionDeadline =
                now.plus(REGULAR_ELECTION_DEADLINE_DAYS, ChronoUnit.DAYS)

            return Election(
                teamId = teamId,
                title = title,
                description = description,
                electionType = ElectionType.EMERGENCY,
                startAt = startAt,
                endAt = endAt,
                status = ElectionStatus.IN_PROGRESS,
                triggeredByMemberId = triggeredByMemberId,
                regularElectionDeadline = regularElectionDeadline,
            )
        }
    }
}
