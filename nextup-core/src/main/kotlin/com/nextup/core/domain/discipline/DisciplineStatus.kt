package com.nextup.core.domain.discipline

/**
 * 징계 상태
 */
enum class DisciplineStatus(
    val displayName: String,
) {
    ACTIVE("활성"),
    SERVED("이행 완료"),
    CANCELLED("취소"),
}
