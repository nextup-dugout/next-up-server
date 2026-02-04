package com.nextup.core.domain.player

enum class ContractType(
    val displayName: String,
) {
    REGULAR("정규 계약"),
    MINOR_LEAGUE("마이너리그"),
    LOAN("임대"),
    FREE_AGENT("FA 계약"),
    ROOKIE("신인"),
    FOREIGN("외국인 선수"),
}
