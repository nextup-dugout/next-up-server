package com.nextup.core.domain.election

/**
 * 선거 유형
 */
enum class ElectionType {
    /**
     * 구단주 선출
     */
    OWNER_ELECTION,

    /**
     * 주장 선출
     */
    CAPTAIN_ELECTION,

    /**
     * 일반 투표
     */
    GENERAL,

    /**
     * 비상대책위원회 긴급 선거
     *
     * 구단주 부재 시 MANAGER가 발동하는 긴급 모드.
     * 임시 구단주(Acting Owner)를 지정하고, 14일 이내 정규 선거를 자동 생성합니다.
     */
    EMERGENCY,
}
