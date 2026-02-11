package com.nextup.core.domain.team

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 팀 가입 신청 엔티티
 *
 * 팀 가입 신청 및 승인 프로세스를 관리합니다.
 */
@Entity
@Table(
    name = "team_join_requests",
    indexes = [
        Index(name = "idx_tjr_team_id", columnList = "team_id"),
        Index(name = "idx_tjr_user_id", columnList = "user_id"),
        Index(name = "idx_tjr_status", columnList = "status"),
        Index(name = "idx_tjr_team_status", columnList = "team_id, status"),
    ],
)
class TeamJoinRequest private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Column(name = "desired_uniform_number", nullable = false)
    val desiredUniformNumber: Int,
    @Column(name = "request_message", length = 1000)
    val requestMessage: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: JoinRequestStatus = JoinRequestStatus.PENDING,
    @Column(name = "requested_at", nullable = false)
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "processed_at")
    var processedAt: LocalDateTime? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    var processedBy: User? = null,
    @Column(name = "response_message", length = 500)
    var responseMessage: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 승인 대기 중인지 확인합니다.
     */
    val isPending: Boolean
        get() = status == JoinRequestStatus.PENDING

    /**
     * 가입 신청을 승인합니다.
     *
     * @param approver 승인자
     * @param message 승인 메시지
     * @throws IllegalStateException 대기 중인 요청이 아닌 경우
     */
    fun approve(
        approver: User,
        message: String? = null,
    ) {
        check(this.status == JoinRequestStatus.PENDING) {
            "대기 중인 요청만 승인할 수 있습니다."
        }
        this.status = JoinRequestStatus.APPROVED
        this.processedAt = LocalDateTime.now()
        this.processedBy = approver
        this.responseMessage = message
    }

    /**
     * 가입 신청을 거부합니다.
     *
     * @param rejecter 거부자
     * @param message 거부 메시지
     * @throws IllegalStateException 대기 중인 요청이 아닌 경우
     */
    fun reject(
        rejecter: User,
        message: String? = null,
    ) {
        check(this.status == JoinRequestStatus.PENDING) {
            "대기 중인 요청만 거부할 수 있습니다."
        }
        this.status = JoinRequestStatus.REJECTED
        this.processedAt = LocalDateTime.now()
        this.processedBy = rejecter
        this.responseMessage = message
    }

    /**
     * 가입 신청을 취소합니다 (본인만 가능).
     *
     * @throws IllegalStateException 대기 중인 요청이 아닌 경우
     */
    fun cancel() {
        check(this.status == JoinRequestStatus.PENDING) {
            "대기 중인 요청만 취소할 수 있습니다."
        }
        this.status = JoinRequestStatus.REJECTED
        this.processedAt = LocalDateTime.now()
        this.responseMessage = "신청자가 취소함"
    }

    companion object {
        /**
         * 팀 가입 신청을 생성합니다.
         *
         * @param team 팀
         * @param user 사용자
         * @param player 선수
         * @param desiredUniformNumber 희망 등번호
         * @param requestMessage 신청 메시지
         * @return 생성된 TeamJoinRequest
         */
        fun create(
            team: Team,
            user: User,
            player: Player,
            desiredUniformNumber: Int,
            requestMessage: String? = null,
        ): TeamJoinRequest {
            require(desiredUniformNumber in 1..99) {
                "등번호는 1~99 범위여야 합니다."
            }

            return TeamJoinRequest(
                team = team,
                user = user,
                player = player,
                desiredUniformNumber = desiredUniformNumber,
                requestMessage = requestMessage,
                status = JoinRequestStatus.PENDING,
                requestedAt = LocalDateTime.now(),
            )
        }
    }
}
