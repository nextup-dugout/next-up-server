package com.nextup.core.service.team

import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamJoinRequest
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole

/**
 * 팀 멤버십 서비스 인터페이스
 *
 * 팀 가입, 승인, 강퇴, 역할 변경 등 팀 멤버십 관련 비즈니스 로직을 정의합니다.
 */
interface TeamMembershipService {
    /**
     * 팀 가입을 신청합니다.
     *
     * @param userId 사용자 ID
     * @param teamId 팀 ID
     * @param desiredUniformNumber 희망 등번호
     * @param message 신청 메시지
     * @return 생성된 TeamJoinRequest
     * @throws AlreadyInTeamException 이미 다른 팀에 소속된 경우
     * @throws BlacklistedUserException 블랙리스트에 등록된 경우
     * @throws DuplicateJoinRequestException 이미 대기 중인 신청이 있는 경우
     */
    fun requestJoin(
        userId: Long,
        teamId: Long,
        desiredUniformNumber: Int,
        message: String?,
    ): TeamJoinRequest

    /**
     * 가입 신청을 승인합니다.
     *
     * @param requestId 가입 신청 ID
     * @param processorUserId 처리자 사용자 ID
     * @param finalUniformNumber 최종 배정 등번호 (null이면 희망 등번호 사용)
     * @param responseMessage 승인 메시지
     * @return 생성된 TeamMember
     * @throws TeamJoinRequestNotFoundException 가입 신청을 찾을 수 없는 경우
     * @throws InsufficientTeamRoleException 승인 권한이 없는 경우
     * @throws UniformNumberAlreadyTakenException 등번호가 이미 사용 중인 경우
     */
    fun approveJoinRequest(
        requestId: Long,
        processorUserId: Long,
        finalUniformNumber: Int? = null,
        responseMessage: String? = null,
    ): TeamMember

    /**
     * 가입 신청을 거부합니다.
     *
     * @param requestId 가입 신청 ID
     * @param processorUserId 처리자 사용자 ID
     * @param reason 거부 사유
     * @return 거부된 TeamJoinRequest
     * @throws TeamJoinRequestNotFoundException 가입 신청을 찾을 수 없는 경우
     * @throws InsufficientTeamRoleException 거부 권한이 없는 경우
     */
    fun rejectJoinRequest(
        requestId: Long,
        processorUserId: Long,
        reason: String?,
    ): TeamJoinRequest

    /**
     * 회원을 강퇴합니다.
     *
     * @param memberId 회원 ID
     * @param kickerUserId 강퇴하는 사용자 ID
     * @param reason 강퇴 사유
     * @param addToBlacklist 블랙리스트 추가 여부
     * @throws TeamMemberNotFoundException 회원을 찾을 수 없는 경우
     * @throws InsufficientTeamRoleException 강퇴 권한이 없는 경우
     */
    fun kickMember(
        memberId: Long,
        kickerUserId: Long,
        reason: String,
        addToBlacklist: Boolean,
    )

    /**
     * 회원이 자진 탈퇴합니다.
     *
     * @param memberId 회원 ID
     * @throws TeamMemberNotFoundException 회원을 찾을 수 없는 경우
     * @throws OwnerCannotLeaveException OWNER가 다른 OWNER 없이 탈퇴하려는 경우
     */
    fun leaveMember(memberId: Long)

    /**
     * 회원의 역할을 변경합니다.
     *
     * @param memberId 회원 ID
     * @param newRole 새로운 역할
     * @param changerUserId 변경하는 사용자 ID
     * @return 변경된 TeamMember
     * @throws TeamMemberNotFoundException 회원을 찾을 수 없는 경우
     * @throws InsufficientTeamRoleException 역할 변경 권한이 없는 경우
     */
    fun changeRole(
        memberId: Long,
        newRole: TeamMemberRole,
        changerUserId: Long,
    ): TeamMember

    /**
     * 팀 로스터를 조회합니다.
     *
     * @param teamId 팀 ID
     * @return 팀 멤버 목록
     */
    fun getRoster(teamId: Long): List<TeamMember>

    /**
     * 팀 멤버를 조회합니다.
     *
     * @param teamId 팀 ID
     * @param userId 사용자 ID
     * @return TeamMember (없으면 null)
     */
    fun getMember(
        teamId: Long,
        userId: Long,
    ): TeamMember?

    /**
     * 멤버 ID로 팀 멤버를 조회합니다.
     *
     * @param memberId 멤버 ID
     * @return TeamMember (없으면 null)
     */
    fun getMemberById(memberId: Long): TeamMember?

    /**
     * 팀을 생성하고 생성자를 OWNER로 등록합니다.
     *
     * @param userId 생성자 사용자 ID
     * @param team 생성할 팀
     * @param uniformNumber OWNER의 등번호
     * @return 생성된 Team
     */
    fun createTeamWithOwner(
        userId: Long,
        team: Team,
        uniformNumber: Int,
    ): Team

    /**
     * 팀을 삭제합니다. OWNER만 가능하며 멤버가 1명일 때만 삭제 가능합니다.
     *
     * @param teamId 팀 ID
     * @param userId 요청자 사용자 ID
     * @throws TeamNotFoundException 팀을 찾을 수 없는 경우
     * @throws InsufficientTeamRoleException OWNER가 아닌 경우
     * @throws InvalidTeamStateException 멤버가 2명 이상인 경우
     */
    fun deleteTeam(
        teamId: Long,
        userId: Long,
    )

    /**
     * 팀의 활성 멤버 수를 반환합니다.
     *
     * @param teamId 팀 ID
     * @return 활성 멤버 수
     */
    fun getTeamMemberCount(teamId: Long): Int

    /**
     * 여러 팀의 활성 멤버 수를 배치로 반환합니다. (N+1 방지)
     *
     * @param teamIds 팀 ID 목록
     * @return 팀 ID → 활성 멤버 수 맵
     */
    fun getTeamMemberCounts(teamIds: List<Long>): Map<Long, Int>
}
