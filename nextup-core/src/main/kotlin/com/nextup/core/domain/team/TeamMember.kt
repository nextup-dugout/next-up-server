package com.nextup.core.domain.team

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 팀 멤버 엔티티
 *
 * 팀 내 회원의 역할, 등번호, 상태를 관리하는 핵심 Entity입니다.
 * Rich Domain Model 원칙에 따라 비즈니스 로직을 Entity 내부에 캡슐화합니다.
 */
@Entity
@Table(
    name = "team_members",
    indexes = [
        Index(name = "idx_tm_team_id", columnList = "team_id"),
        Index(name = "idx_tm_user_id", columnList = "user_id"),
        Index(name = "idx_tm_player_id", columnList = "player_id"),
        Index(name = "idx_tm_team_status", columnList = "team_id, status"),
        Index(name = "idx_tm_uniform", columnList = "team_id, uniform_number"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_tm_team_user", columnNames = ["team_id", "user_id"]),
    ],
)
class TeamMember private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: TeamMemberRole = TeamMemberRole.MEMBER,
    @Column(name = "uniform_number", nullable = false)
    val uniformNumber: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TeamMemberStatus = TeamMemberStatus.ACTIVE,
    @Column(name = "joined_at", nullable = false)
    val joinedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "left_at")
    var leftAt: LocalDateTime? = null,
    @Column(length = 500)
    var memo: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 활동 중인지 확인합니다.
     */
    val isActive: Boolean
        get() = status == TeamMemberStatus.ACTIVE

    /**
     * 경기에 참여 가능한지 확인합니다.
     */
    val canParticipateInGame: Boolean
        get() = status == TeamMemberStatus.ACTIVE

    /**
     * 투표 가능한지 확인합니다 (ACTIVE, SUSPENDED 모두 가능).
     */
    val canVote: Boolean
        get() = status in listOf(TeamMemberStatus.ACTIVE, TeamMemberStatus.SUSPENDED)

    /**
     * OWNER 역할인지 확인합니다.
     */
    fun isOwner(): Boolean = role == TeamMemberRole.OWNER

    /**
     * MANAGER 역할인지 확인합니다.
     */
    fun isManager(): Boolean = role == TeamMemberRole.MANAGER

    /**
     * 멤버 관리 권한이 있는지 확인합니다 (OWNER, MANAGER).
     */
    fun canManageMembers(): Boolean = role.canApproveJoin()

    /**
     * 역할을 변경합니다 (OWNER만 가능, 자기 자신 제외).
     *
     * @param newRole 새로운 역할
     * @param requester 요청자
     * @throws IllegalStateException 권한이 없거나 규칙 위반인 경우
     */
    fun changeRole(
        newRole: TeamMemberRole,
        requester: TeamMember,
    ) {
        check(requester.role.canChangeRole()) {
            "OWNER만 역할을 변경할 수 있습니다."
        }
        check(this.id != requester.id) {
            "자기 자신의 역할은 변경할 수 없습니다."
        }
        check(newRole != TeamMemberRole.OWNER) {
            "OWNER 역할은 이양(transferOwnership)을 통해서만 부여할 수 있습니다."
        }
        this.role = newRole
    }

    /**
     * 활동 정지합니다.
     *
     * @param reason 정지 사유
     * @param requester 요청자
     * @throws IllegalStateException 권한이 없거나 규칙 위반인 경우
     */
    fun suspend(
        reason: String? = null,
        requester: TeamMember,
    ) {
        check(requester.role.canKickMember()) {
            "OWNER만 활동 정지를 할 수 있습니다."
        }
        check(this.role != TeamMemberRole.OWNER) {
            "OWNER는 활동 정지할 수 없습니다."
        }
        this.status = TeamMemberStatus.SUSPENDED
        this.memo = reason
    }

    /**
     * 활동을 재개합니다.
     *
     * @param requester 요청자
     * @throws IllegalStateException 권한이 없거나 상태가 부적절한 경우
     */
    fun resume(requester: TeamMember) {
        check(requester.role.canKickMember()) {
            "OWNER만 활동 재개를 할 수 있습니다."
        }
        check(this.status == TeamMemberStatus.SUSPENDED) {
            "활동 정지 상태인 회원만 재개할 수 있습니다."
        }
        this.status = TeamMemberStatus.ACTIVE
    }

    /**
     * 강퇴합니다.
     *
     * @param reason 강퇴 사유
     * @param requester 요청자
     * @throws IllegalStateException 권한이 없거나 규칙 위반인 경우
     */
    fun kick(
        reason: String? = null,
        requester: TeamMember,
    ) {
        check(requester.role.canKickMember()) {
            "OWNER만 강퇴할 수 있습니다."
        }
        check(this.id != requester.id) {
            "자기 자신을 강퇴할 수 없습니다."
        }
        check(this.role != TeamMemberRole.OWNER) {
            "OWNER는 강퇴할 수 없습니다. 먼저 역할을 변경하세요."
        }
        this.status = TeamMemberStatus.KICKED
        this.leftAt = LocalDateTime.now()
        this.memo = reason
    }

    /**
     * 자진 탈퇴합니다.
     *
     * @throws IllegalStateException OWNER이거나 상태가 부적절한 경우
     */
    fun leave() {
        check(this.role != TeamMemberRole.OWNER) {
            "OWNER는 탈퇴할 수 없습니다. 먼저 다른 OWNER를 지정하세요."
        }
        check(this.status == TeamMemberStatus.ACTIVE) {
            "활동 중인 회원만 탈퇴할 수 있습니다."
        }
        this.status = TeamMemberStatus.LEFT
        this.leftAt = LocalDateTime.now()
    }

    /**
     * OWNER 역할을 이양합니다.
     *
     * @param newOwner 새로운 OWNER
     * @param requester 요청자 (본인이어야 함)
     * @throws IllegalStateException 규칙 위반인 경우
     */
    fun transferOwnership(
        newOwner: TeamMember,
        requester: TeamMember,
    ) {
        check(requester.id == this.id) {
            "본인만 OWNER 역할을 이양할 수 있습니다."
        }
        check(this.role == TeamMemberRole.OWNER) {
            "OWNER만 역할을 이양할 수 있습니다."
        }
        check(newOwner.status == TeamMemberStatus.ACTIVE) {
            "활동 중인 회원에게만 역할을 이양할 수 있습니다."
        }
        check(newOwner.team.id == this.team.id) {
            "같은 팀 회원에게만 역할을 이양할 수 있습니다."
        }

        this.role = TeamMemberRole.MANAGER
        newOwner.role = TeamMemberRole.OWNER
    }

    companion object {
        /**
         * 팀 멤버를 생성합니다.
         *
         * @param team 팀
         * @param user 사용자
         * @param player 선수
         * @param uniformNumber 등번호
         * @param role 역할 (기본값: MEMBER)
         * @return 생성된 TeamMember
         */
        fun create(
            team: Team,
            user: User,
            player: Player,
            uniformNumber: Int,
            role: TeamMemberRole = TeamMemberRole.MEMBER,
        ): TeamMember {
            require(uniformNumber in 1..99) {
                "등번호는 1~99 범위여야 합니다."
            }

            return TeamMember(
                team = team,
                user = user,
                player = player,
                role = role,
                uniformNumber = uniformNumber,
                status = TeamMemberStatus.ACTIVE,
                joinedAt = LocalDateTime.now(),
            )
        }
    }
}
