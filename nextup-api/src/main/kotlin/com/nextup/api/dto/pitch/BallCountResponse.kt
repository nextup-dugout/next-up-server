package com.nextup.api.dto.pitch

/**
 * 볼카운트 응답 DTO
 */
data class BallCountResponse(
    val balls: Int,
    val strikes: Int,
    val countDisplay: String,
) {
    companion object {
        fun of(
            balls: Int,
            strikes: Int,
        ): BallCountResponse =
            BallCountResponse(
                balls = balls,
                strikes = strikes,
                countDisplay = "${balls}B-${strikes}S",
            )
    }
}
