package com.nextup.common.exception

class RecruitmentNotFoundException(
    id: Long,
) : NotFoundException(
        code = "RECRUITMENT_NOT_FOUND",
        message = "모집 공고를 찾을 수 없습니다: $id",
    )
