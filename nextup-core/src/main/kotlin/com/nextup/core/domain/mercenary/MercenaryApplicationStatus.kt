package com.nextup.core.domain.mercenary

enum class MercenaryApplicationStatus(
    val displayName: String,
) {
    PENDING("대기중"),
    ACCEPTED("수락"),
    REJECTED("거절"),
}
