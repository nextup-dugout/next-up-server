package com.nextup.common.exception

/**
 * 조직 관리자를 찾을 수 없을 때 발생하는 예외
 */
class OrganizationAdminNotFoundException(
    organizationType: String,
    organizationId: Long,
    userId: Long
) : NotFoundException(
    "ORGANIZATION_ADMIN_NOT_FOUND",
    "Organization admin not found: type=$organizationType, orgId=$organizationId, userId=$userId"
)

/**
 * 조직 관리자 ID로 찾을 수 없을 때 발생하는 예외
 */
class OrganizationAdminNotFoundByIdException(adminId: Long) :
    NotFoundException(
        "ORGANIZATION_ADMIN_NOT_FOUND",
        "Organization admin not found: $adminId"
    )

/**
 * 조직 관리자가 이미 존재할 때 발생하는 예외
 */
class OrganizationAdminAlreadyExistsException(
    organizationType: String,
    organizationId: Long,
    userId: Long
) : BusinessException(
    "ORGANIZATION_ADMIN_ALREADY_EXISTS",
    "Organization admin already exists: type=$organizationType, orgId=$organizationId, userId=$userId"
)

/**
 * 조직을 찾을 수 없을 때 발생하는 예외
 */
class OrganizationNotFoundException(
    organizationType: String,
    organizationId: Long
) : NotFoundException(
    "ORGANIZATION_NOT_FOUND",
    "Organization not found: type=$organizationType, id=$organizationId"
)

/**
 * 유효하지 않은 조직 유형일 때 발생하는 예외
 */
class InvalidOrganizationTypeException(value: String) :
    BusinessException(
        "INVALID_ORGANIZATION_TYPE",
        "Invalid organization type: $value"
    )

/**
 * 조직에 대한 권한이 없을 때 발생하는 예외
 */
class UnauthorizedOrganizationAccessException(
    organizationType: String,
    organizationId: Long
) : BusinessException(
    "UNAUTHORIZED_ORGANIZATION_ACCESS",
    "Unauthorized access to organization: type=$organizationType, id=$organizationId"
)

/**
 * 같은 리그 내 여러 팀의 관리자가 될 수 없을 때 발생하는 예외
 */
class SameLeagueConflictException(
    leagueId: Long,
    existingTeamId: Long,
    newTeamId: Long
) : BusinessException(
    "SAME_LEAGUE_CONFLICT",
    "Cannot manage multiple teams in the same league: leagueId=$leagueId, existingTeamId=$existingTeamId, newTeamId=$newTeamId"
)

/**
 * 관리자 역할 수준이 부족할 때 발생하는 예외
 */
class InsufficientRoleLevelException(
    requiredRole: String,
    currentRole: String
) : BusinessException(
    "INSUFFICIENT_ROLE_LEVEL",
    "Insufficient role level: required=$requiredRole, current=$currentRole"
)

/**
 * 비활성화된 관리자일 때 발생하는 예외
 */
class OrganizationAdminDeactivatedException(adminId: Long) :
    InvalidStateException(
        "ORGANIZATION_ADMIN_DEACTIVATED",
        "Organization admin is deactivated: $adminId"
    )
