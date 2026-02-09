package com.nextup.common.exception

/**
 * 대진표 엔트리를 찾을 수 없을 때 발생하는 예외
 */
class BracketEntryNotFoundException(
    bracketEntryId: Long,
) : NotFoundException(
        "BRACKET_ENTRY_NOT_FOUND",
        "대진표 엔트리를 찾을 수 없습니다: $bracketEntryId",
    )
