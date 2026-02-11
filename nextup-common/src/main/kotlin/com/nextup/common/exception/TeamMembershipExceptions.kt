package com.nextup.common.exception

/**
 * 팀 멤버를 찾을 수 없을 때 발생하는 예외
 */
class TeamMemberNotFoundException(
    memberId: Long,
) : NotFoundException(
        "TEAM_MEMBER_NOT_FOUND",
        "Team member not found: $memberId",
    )

/**
 * 이미 팀에 소속되어 있을 때 발생하는 예외
 */
class AlreadyTeamMemberException(
    userId: Long,
    teamId: Long,
) : BusinessException(
        "ALREADY_TEAM_MEMBER",
        "User $userId is already a member of team $teamId",
    )

/**
 * 팀 가입 신청을 찾을 수 없을 때 발생하는 예외
 */
class TeamJoinRequestNotFoundException(
    requestId: Long,
) : NotFoundException(
        "TEAM_JOIN_REQUEST_NOT_FOUND",
        "Team join request not found: $requestId",
    )

/**
 * 블랙리스트에 등록된 사용자일 때 발생하는 예외
 */
class BlacklistedUserException(
    userId: Long,
    teamId: Long,
    reason: String? = null,
) : BusinessException(
        "BLACKLISTED_USER",
        "User $userId is blacklisted from team $teamId" + (reason?.let { ": $it" } ?: ""),
    )

/**
 * 등번호가 이미 사용 중일 때 발생하는 예외
 */
class UniformNumberAlreadyTakenException(
    teamId: Long,
    uniformNumber: Int,
    currentOwner: String? = null,
) : BusinessException(
        "UNIFORM_NUMBER_ALREADY_TAKEN",
        "Uniform number $uniformNumber is already taken in team $teamId" +
            (currentOwner?.let { " by $it" } ?: ""),
    )

/**
 * 팀 역할 권한이 부족할 때 발생하는 예외
 */
class InsufficientTeamRoleException(
    requiredRole: String,
    currentRole: String,
) : BusinessException(
        "INSUFFICIENT_TEAM_ROLE",
        "Insufficient team role. Required: $requiredRole, Current: $currentRole",
    )

/**
 * OWNER가 탈퇴할 수 없을 때 발생하는 예외
 */
class OwnerCannotLeaveException(
    message: String = "OWNER는 다른 OWNER를 지정한 후 탈퇴할 수 있습니다.",
) : BusinessException(
        "OWNER_CANNOT_LEAVE",
        message,
    )

/**
 * 유효하지 않은 등번호일 때 발생하는 예외
 */
class InvalidUniformNumberException(
    uniformNumber: Int,
) : InvalidInputException(
        "INVALID_UNIFORM_NUMBER",
        "Invalid uniform number: $uniformNumber. Must be between 1 and 99.",
    )

/**
 * 중복된 가입 신청이 있을 때 발생하는 예외
 */
class DuplicateJoinRequestException(
    userId: Long,
    teamId: Long,
    existingRequestId: Long,
) : BusinessException(
        "DUPLICATE_JOIN_REQUEST",
        "User $userId already has a pending join request for team $teamId (request ID: $existingRequestId)",
    )

/**
 * 출석 투표를 찾을 수 없을 때 발생하는 예외
 */
class AttendanceVoteNotFoundException(
    voteId: Long,
) : NotFoundException(
        "ATTENDANCE_VOTE_NOT_FOUND",
        "Attendance vote not found: $voteId",
    )

/**
 * 출석 투표가 마감되었을 때 발생하는 예외
 */
class VoteClosedException(
    gameId: Long,
    message: String = "경기가 이미 시작되어 투표가 마감되었습니다.",
) : BusinessException(
        "VOTE_CLOSED",
        "Vote closed for game $gameId: $message",
    )

/**
 * 이미 정규팀에 소속되어 있을 때 발생하는 예외
 */
class AlreadyInTeamException(
    userId: Long,
    currentTeamId: Long,
    currentTeamName: String,
) : BusinessException(
        "ALREADY_IN_TEAM",
        "User $userId is already a member of team '$currentTeamName' (ID: $currentTeamId)",
    )
