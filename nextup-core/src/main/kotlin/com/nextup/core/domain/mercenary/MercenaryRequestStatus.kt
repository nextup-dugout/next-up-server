package com.nextup.core.domain.mercenary

enum class MercenaryRequestStatus(
    val displayName: String,
) {
    OPEN("모집중"),
    CLOSED("모집완료"),
    CANCELLED("취소"),
}
