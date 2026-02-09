package com.nextup.core.domain.election

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 선거 엔티티
 *
 * 팀 내에서 진행되는 선거/투표를 나타냅니다.
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
    }
}
