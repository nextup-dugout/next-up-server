package com.nextup.core.domain.election

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*

/**
 * 후보자 엔티티
 *
 * 선거에 출마한 후보자를 나타냅니다.
 */
@Entity
@Table(
    name = "candidates",
    indexes = [
        Index(name = "idx_candidates_election", columnList = "election_id"),
        Index(name = "idx_candidates_member", columnList = "member_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_candidates_election_member",
            columnNames = ["election_id", "member_id"],
        ),
    ],
)
class Candidate private constructor(
    @Column(name = "election_id", nullable = false)
    val electionId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "member_name", nullable = false, length = 100)
    val memberName: String,
    @Column(length = 1000)
    val statement: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Candidate) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "Candidate(id=$id, electionId=$electionId, memberId=$memberId, memberName='$memberName')"

    companion object {
        /**
         * Candidate를 생성합니다.
         *
         * @param electionId 선거 ID
         * @param memberId 회원 ID
         * @param memberName 회원 이름
         * @param statement 공약/소견
         * @return 생성된 Candidate
         * @throws IllegalArgumentException 회원 이름이 비어있는 경우
         */
        fun create(
            electionId: Long,
            memberId: Long,
            memberName: String,
            statement: String? = null,
        ): Candidate {
            require(memberName.isNotBlank()) { "Member name cannot be blank" }

            return Candidate(
                electionId = electionId,
                memberId = memberId,
                memberName = memberName,
                statement = statement,
            )
        }
    }
}
