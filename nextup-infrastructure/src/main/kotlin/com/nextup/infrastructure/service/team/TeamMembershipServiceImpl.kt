package com.nextup.infrastructure.service.team

import com.nextup.common.exception.*
import com.nextup.core.domain.team.*
import com.nextup.core.port.repository.*
import com.nextup.core.service.team.TeamMembershipService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 팀 멤버십 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class TeamMembershipServiceImpl(
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val teamJoinRequestRepository: TeamJoinRequestRepositoryPort,
    private val teamBlacklistRepository: TeamBlacklistRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val playerRepository: PlayerRepositoryPort,
) : TeamMembershipService {
    @Transactional
    override fun requestJoin(
        userId: Long,
        teamId: Long,
        desiredUniformNumber: Int,
        message: String?,
    ): TeamJoinRequest {
        // 등번호 유효성 검증
        if (desiredUniformNumber !in 1..99) {
            throw InvalidUniformNumberException(desiredUniformNumber)
        }

        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(userId)
        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)
        // Player는 User와 1:1 관계이므로 User를 통해 조회
        val player =
            user.player
                ?: throw PlayerNotFoundException(userId)

        // 정규팀 1개 제한 검증
        val activeMember = teamMemberRepository.findActiveByUserId(userId)
        if (activeMember != null) {
            throw AlreadyInTeamException(userId, activeMember.team.id, activeMember.team.name)
        }

        // 블랙리스트 검증
        if (teamBlacklistRepository.existsActiveByTeamIdAndUserId(teamId, userId)) {
            val blacklist = teamBlacklistRepository.findByTeamIdAndUserId(teamId, userId)
            throw BlacklistedUserException(userId, teamId, blacklist?.reason)
        }

        // 중복 신청 검증
        val pendingRequest = teamJoinRequestRepository.findPendingByTeamIdAndUserId(teamId, userId)
        if (pendingRequest != null) {
            throw DuplicateJoinRequestException(userId, teamId, pendingRequest.id)
        }

        // 가입 신청 생성
        val joinRequest =
            TeamJoinRequest.create(
                team = team,
                user = user,
                player = player,
                desiredUniformNumber = desiredUniformNumber,
                requestMessage = message,
            )

        return teamJoinRequestRepository.save(joinRequest)
    }

    @Transactional
    override fun approveJoinRequest(
        requestId: Long,
        processorUserId: Long,
        finalUniformNumber: Int?,
        responseMessage: String?,
    ): TeamMember {
        val joinRequest =
            teamJoinRequestRepository.findByIdOrNull(requestId)
                ?: throw TeamJoinRequestNotFoundException(requestId)

        val processor =
            userRepository.findByIdOrNull(processorUserId)
                ?: throw UserNotFoundException(processorUserId)

        // 승인 권한 검증 (OWNER 또는 MANAGER)
        val processorMember = teamMemberRepository.findByTeamIdAndUserId(joinRequest.team.id, processorUserId)
        if (processorMember == null || !processorMember.canManageMembers()) {
            throw InsufficientTeamRoleException(
                "OWNER or MANAGER",
                processorMember?.role?.name ?: "NONE",
            )
        }

        // 등번호 결정 (관리자가 지정하지 않으면 신청자의 희망 등번호 사용)
        val uniformNumber = finalUniformNumber ?: joinRequest.desiredUniformNumber

        // 등번호 유효성 검증
        if (uniformNumber !in 1..99) {
            throw InvalidUniformNumberException(uniformNumber)
        }

        // 등번호 중복 검증 (ACTIVE 상태만)
        if (teamMemberRepository.existsByTeamIdAndUniformNumberAndStatus(
                joinRequest.team.id,
                uniformNumber,
                TeamMemberStatus.ACTIVE,
            )
        ) {
            throw UniformNumberAlreadyTakenException(joinRequest.team.id, uniformNumber)
        }

        // 가입 신청 승인 처리
        joinRequest.approve(processor, responseMessage)

        // TeamMember 생성
        val newMember =
            TeamMember.create(
                team = joinRequest.team,
                user = joinRequest.user,
                player = joinRequest.player,
                uniformNumber = uniformNumber,
                role = TeamMemberRole.MEMBER,
            )

        return teamMemberRepository.save(newMember)
    }

    @Transactional
    override fun rejectJoinRequest(
        requestId: Long,
        processorUserId: Long,
        reason: String?,
    ): TeamJoinRequest {
        val joinRequest =
            teamJoinRequestRepository.findByIdOrNull(requestId)
                ?: throw TeamJoinRequestNotFoundException(requestId)

        val processor =
            userRepository.findByIdOrNull(processorUserId)
                ?: throw UserNotFoundException(processorUserId)

        // 거부 권한 검증 (OWNER 또는 MANAGER)
        val processorMember = teamMemberRepository.findByTeamIdAndUserId(joinRequest.team.id, processorUserId)
        if (processorMember == null || !processorMember.canManageMembers()) {
            throw InsufficientTeamRoleException(
                "OWNER or MANAGER",
                processorMember?.role?.name ?: "NONE",
            )
        }

        // 가입 신청 거부 처리
        joinRequest.reject(processor, reason)

        return teamJoinRequestRepository.save(joinRequest)
    }

    @Transactional
    override fun kickMember(
        memberId: Long,
        kickerUserId: Long,
        reason: String,
        addToBlacklist: Boolean,
    ) {
        val member =
            teamMemberRepository.findByIdOrNull(memberId)
                ?: throw TeamMemberNotFoundException(memberId)

        val kicker =
            teamMemberRepository.findByTeamIdAndUserId(member.team.id, kickerUserId)
                ?: throw InsufficientTeamRoleException("OWNER", "NONE")

        // Entity의 비즈니스 로직으로 강퇴 처리 (권한 검증 포함)
        member.kick(reason, kicker)
        teamMemberRepository.save(member)

        // 블랙리스트 추가 옵션
        if (addToBlacklist) {
            val blacklist =
                TeamBlacklist.createPermanent(
                    team = member.team,
                    user = member.user,
                    player = member.player,
                    reason = reason,
                    registeredBy = kicker.user,
                )
            teamBlacklistRepository.save(blacklist)
        }
    }

    @Transactional
    override fun leaveMember(memberId: Long) {
        val member =
            teamMemberRepository.findByIdOrNull(memberId)
                ?: throw TeamMemberNotFoundException(memberId)

        // OWNER는 다른 OWNER가 있어야 탈퇴 가능
        if (member.isOwner()) {
            val ownerCount = teamMemberRepository.countOwnersByTeamId(member.team.id)
            if (ownerCount <= 1) {
                throw OwnerCannotLeaveException()
            }
        }

        // Entity의 비즈니스 로직으로 탈퇴 처리
        member.leave()
        teamMemberRepository.save(member)
    }

    @Transactional
    override fun changeRole(
        memberId: Long,
        newRole: TeamMemberRole,
        changerUserId: Long,
    ): TeamMember {
        val member =
            teamMemberRepository.findByIdOrNull(memberId)
                ?: throw TeamMemberNotFoundException(memberId)

        val changer =
            teamMemberRepository.findByTeamIdAndUserId(member.team.id, changerUserId)
                ?: throw InsufficientTeamRoleException("OWNER", "NONE")

        // Entity의 비즈니스 로직으로 역할 변경 처리 (권한 검증 포함)
        member.changeRole(newRole, changer)
        return teamMemberRepository.save(member)
    }

    override fun getRoster(teamId: Long): List<TeamMember> {
        // ACTIVE 상태의 멤버만 조회
        return teamMemberRepository.findByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE)
    }

    override fun getMember(
        teamId: Long,
        userId: Long,
    ): TeamMember? {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
    }

    @Transactional
    override fun createTeamWithOwner(
        userId: Long,
        team: Team,
        uniformNumber: Int,
    ): Team {
        // 등번호 유효성 검증
        if (uniformNumber !in 1..99) {
            throw InvalidUniformNumberException(uniformNumber)
        }

        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(userId)
        val player =
            user.player
                ?: throw PlayerNotFoundException(userId)

        // 이미 다른 팀에 소속된 경우 검증
        val activeMember = teamMemberRepository.findActiveByUserId(userId)
        if (activeMember != null) {
            throw AlreadyInTeamException(userId, activeMember.team.id, activeMember.team.name)
        }

        // 팀 이름 중복 검증
        val existingTeam = teamRepository.findByName(team.name)
        if (existingTeam != null) {
            throw TeamAlreadyExistsException(team.name)
        }

        // 팀 저장
        val savedTeam = teamRepository.save(team)

        // OWNER로 멤버 생성
        val ownerMember =
            TeamMember.create(
                team = savedTeam,
                user = user,
                player = player,
                uniformNumber = uniformNumber,
                role = TeamMemberRole.OWNER,
            )
        teamMemberRepository.save(ownerMember)

        return savedTeam
    }

    @Transactional
    override fun deleteTeam(
        teamId: Long,
        userId: Long,
    ) {
        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw TeamNotFoundException(teamId)

        // OWNER 권한 검증
        val member =
            teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                ?: throw InsufficientTeamRoleException("OWNER", "NONE")

        if (!member.isOwner()) {
            throw InsufficientTeamRoleException("OWNER", member.role.name)
        }

        // 활성 멤버 수 검증 (1명일 때만 삭제 가능)
        val activeMemberCount =
            teamMemberRepository.countByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE)
        if (activeMemberCount > 1) {
            throw InvalidTeamStateException(
                "팀 멤버가 ${activeMemberCount}명입니다. 멤버가 1명일 때만 삭제할 수 있습니다.",
            )
        }

        // 멤버 삭제 후 팀 삭제
        teamMemberRepository.delete(member)
        teamRepository.delete(team)
    }

    override fun getTeamMemberCount(teamId: Long): Int {
        return teamMemberRepository.countByTeamIdAndStatus(teamId, TeamMemberStatus.ACTIVE).toInt()
    }
}
