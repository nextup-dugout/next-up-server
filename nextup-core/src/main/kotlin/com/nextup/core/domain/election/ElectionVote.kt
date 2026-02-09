package com.nextup.core.domain.election

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 투표 엔티티
 *
 * 선거에 대한 투표 기록을 나타냅니다.
 */
@Entity
@Table(
    name = "election_votes",
    indexes = [
        Index(name = "idx_election_votes_election", columnList = "election_id"),
        Index(name = "idx_election_votes_candidate", columnList = "candidate_id"),
        Index(name = "idx_election_votes_voter", columnList = "voter_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_election_votes_election_voter",
            columnNames = ["election_id", "voter_id"],
        ),
    ],
)
class ElectionVote private constructor(
    @Column(name = "election_id", nullable = false)
    val electionId: Long,
    @Column(name = "voter_id", nullable = false)
    val voterId: Long,
    @Column(name = "candidate_id", nullable = false)
    val candidateId: Long,
    @Column(name = "voted_at", nullable = false)
    val votedAt: Instant,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ElectionVote) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "ElectionVote(id=$id, electionId=$electionId, voterId=$voterId, candidateId=$candidateId)"

    companion object {
        /**
         * ElectionVote를 생성합니다.
         *
         * @param electionId 선거 ID
         * @param voterId 투표자 ID
         * @param candidateId 후보자 ID
         * @param votedAt 투표 시간
         * @return 생성된 ElectionVote
         */
        fun create(
            electionId: Long,
            voterId: Long,
            candidateId: Long,
            votedAt: Instant = Instant.now(),
        ): ElectionVote =
            ElectionVote(
                electionId = electionId,
                voterId = voterId,
                candidateId = candidateId,
                votedAt = votedAt,
            )
    }
}
