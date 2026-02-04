package com.nextup.core.domain.player

enum class ThrowingHand(
    val displayName: String,
) {
    LEFT("좌투"),
    RIGHT("우투"),
    SWITCH("양투"),
}

enum class BattingHand(
    val displayName: String,
) {
    LEFT("좌타"),
    RIGHT("우타"),
    SWITCH("양타"),
}
