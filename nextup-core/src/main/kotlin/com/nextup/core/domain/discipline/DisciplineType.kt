package com.nextup.core.domain.discipline

/**
 * 징계 유형
 */
enum class DisciplineType(
    val displayName: String,
) {
    WARNING("경고"),
    SUSPENSION("출장 정지"),
    BAN("영구 제재"),
}
