package com.nextup.core.domain.match

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*

/**
 * 매칭 응답 엔티티
 *
 * 매칭 요청에 대한 다른 팀의 응답을 관리합니다.
 */
@Entity
@Table(
    name = "match_responses",
    indexes = [
        Index(name = "idx_match_responses_match_request_id", columnList = "match_request_id"),
        Index(name = "idx_match_responses_respond_team_id", columnList = "respond_team_id"),
        Index(name = "idx_match_responses_status", columnList = "status"),
    ],
)
class MatchResponse private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_request_id", nullable = false)
    val matchRequest: MatchRequest,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respond_team_id", nullable = false)
    val respondTeam: Team,
    @Column(columnDefinition = "TEXT")
    val message: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MatchResponseStatus = MatchResponseStatus.PENDING,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 응답을 수락합니다.
     */
    fun accept() {
        require(status == MatchResponseStatus.PENDING) {
            "PENDING 상태의 응답만 수락할 수 있습니다"
        }
        this.status = MatchResponseStatus.ACCEPTED
    }

    /**
     * 응답을 거절합니다.
     */
    fun reject() {
        require(status == MatchResponseStatus.PENDING) {
            "PENDING 상태의 응답만 거절할 수 있습니다"
        }
        this.status = MatchResponseStatus.REJECTED
    }

    companion object {
        /**
         * 매칭 응답을 생성합니다.
         */
        fun create(
            matchRequest: MatchRequest,
            respondTeam: Team,
            message: String?,
        ): MatchResponse {
            require(matchRequest.status == MatchRequestStatus.OPEN) {
                "OPEN 상태의 요청에만 응답할 수 있습니다"
            }

            return MatchResponse(
                matchRequest = matchRequest,
                respondTeam = respondTeam,
                message = message,
            )
        }
    }
}
