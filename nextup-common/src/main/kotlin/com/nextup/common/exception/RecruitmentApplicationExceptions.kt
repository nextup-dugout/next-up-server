package com.nextup.common.exception

/**
 * 모집 지원을 찾을 수 없을 때 발생하는 예외
 */
class RecruitmentApplicationNotFoundException(
    id: Long,
) : NotFoundException(
        code = "RECRUITMENT_APPLICATION_NOT_FOUND",
        message = "모집 지원을 찾을 수 없습니다: $id",
    )

/**
 * 이미 지원한 모집 공고에 중복 지원할 때 발생하는 예외
 */
class DuplicateApplicationException(
    recruitmentId: Long,
    applicantId: Long,
) : BusinessException(
        code = "DUPLICATE_APPLICATION",
        message = "이미 해당 모집 공고에 지원했습니다: recruitmentId=$recruitmentId, applicantId=$applicantId",
    )

/**
 * 모집 공고가 마감되었거나 만료된 경우 발생하는 예외
 */
class RecruitmentNotOpenException(
    recruitmentId: Long,
) : InvalidStateException(
        code = "RECRUITMENT_NOT_OPEN",
        message = "모집 공고가 진행 중이 아닙니다: $recruitmentId",
    )

/**
 * 이미 해당 팀에 소속된 사용자가 지원할 때 발생하는 예외
 */
class AlreadyTeamMemberApplicationException(
    applicantId: Long,
    teamId: Long,
) : BusinessException(
        code = "ALREADY_TEAM_MEMBER",
        message = "이미 해당 팀의 멤버입니다: applicantId=$applicantId, teamId=$teamId",
    )
