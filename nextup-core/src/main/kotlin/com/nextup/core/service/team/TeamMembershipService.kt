package com.nextup.core.service.team

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamJoinRequest
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.domain.team.TeamMemberStatus

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
     * 팀의 가입 신청 목록을 조회합니다.
     *
     * @param teamId 팀 ID
     * @param status 필터링할 상태 (null이면 전체 조회)
     * @return 가입 신청 목록
     */
    fun getJoinRequests(
        teamId: Long,
        status: JoinRequestStatus?,
    ): List<TeamJoinRequest>

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

    /**
     * 팀 정보를 수정합니다.
     *
     * @param teamId 팀 ID
     * @param name 새 팀 이름 (null이면 변경 없음)
     * @param city 새 도시 (null이면 변경 없음)
     * @param abbreviation 새 약자 (null이면 변경 없음)
     * @return 수정된 Team
     * @throws TeamNotFoundException 팀을 찾을 수 없는 경우
     */
    fun updateTeam(
        teamId: Long,
        name: String?,
        city: String?,
        abbreviation: String?,
    ): Team

    /**
     * League를 포함한 팀 상세 정보를 조회합니다.
     *
     * @param teamId 팀 ID
     * @return Team (League 페치 포함)
     * @throws TeamNotFoundException 팀을 찾을 수 없는 경우
     */
    fun getTeamWithLeague(teamId: Long): Team

    /**
     * 이름/도시 필터로 활성 팀 목록을 조회합니다.
     *
     * @param name 팀 이름 필터 (null이면 전체)
     * @param city 도시 필터 (null이면 전체)
     * @return 활성 팀 목록
     */
    fun getActiveTeamsByFilter(
        name: String?,
        city: String?,
    ): List<Team>

    /**
     * 팀을 생성하고 생성자를 OWNER로 등록합니다. (필드 기반 시그니처)
     *
     * @param userId 생성자 사용자 ID
     * @param leagueId 소속 리그 ID
     * @param name 팀 이름
     * @param city 도시
     * @param abbreviation 약자
     * @param foundedYear 창단 연도
     * @param uniformNumber OWNER의 등번호
     * @return 생성된 Team
     */
    fun createTeamWithOwner(
        userId: Long,
        leagueId: Long,
        name: String,
        city: String,
        abbreviation: String?,
        foundedYear: Int,
        uniformNumber: Int,
    ): Team

    /**
     * 사용자의 활성 팀 멤버십 목록을 조회합니다. (ProfileController용)
     *
     * @param userId 사용자 ID
     * @return 활성 TeamMember 목록
     */
    fun getActiveTeamsByUserId(userId: Long): List<TeamMember>

    /**
     * 팀 멤버 목록을 페이징으로 조회합니다. (관리자용)
     *
     * @param teamId 팀 ID
     * @param status 상태 필터 (null이면 전체)
     * @param pageCommand 페이징 요청
     * @return 페이징된 TeamMember 결과
     */
    fun getMembersByTeamIdPaged(
        teamId: Long,
        status: TeamMemberStatus?,
        pageCommand: PageCommand,
    ): PageResult<TeamMember>

    /**
     * 멤버 상태를 변경합니다. (관리자용)
     *
     * @param memberId 멤버 ID
     * @param status 새 상태 (ACTIVE 또는 SUSPENDED만 허용)
     * @param reason 사유 (SUSPENDED 시 필요)
     * @return 변경된 TeamMember
     * @throws TeamMemberNotFoundException 멤버를 찾을 수 없는 경우
     * @throws InvalidInputException 허용되지 않는 상태인 경우
     */
    fun updateMemberStatus(
        memberId: Long,
        status: TeamMemberStatus,
        reason: String?,
    ): TeamMember

    /**
     * 멤버를 삭제합니다. (관리자용)
     *
     * @param memberId 멤버 ID
     */
    fun deleteMemberByAdmin(memberId: Long)

    /**
     * L-10: 관리자에 의한 강제 강퇴입니다.
     *
     * OWNER를 포함한 모든 역할의 멤버를 강제로 강퇴할 수 있습니다.
     *
     * @param memberId 강퇴 대상 멤버 ID
     * @param reason 강퇴 사유
     * @param addToBlacklist 블랙리스트 추가 여부
     * @throws TeamMemberNotFoundException 멤버를 찾을 수 없는 경우
     */
    fun forceKickMember(
        memberId: Long,
        reason: String,
        addToBlacklist: Boolean,
    )
}
