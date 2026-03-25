package com.nextup.api.dto.eventgame

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 이벤트 게임 생성 요청
 */
data class CreateEventGameApiRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    val title: String,
    @field:Size(max = 1000, message = "설명은 1000자 이하여야 합니다.")
    val description: String? = null,
    @field:NotNull(message = "경기 일시는 필수입니다.")
    val scheduledAt: LocalDateTime,
    @field:Size(max = 200, message = "장소는 200자 이하여야 합니다.")
    val location: String? = null,
    @field:Size(max = 100, message = "구장명은 100자 이하여야 합니다.")
    val fieldName: String? = null,
    @field:Min(value = 2, message = "최소 참가 인원은 2명 이상이어야 합니다.")
    @field:Max(value = 40, message = "최대 참가 인원은 40명 이하여야 합니다.")
    val maxParticipants: Int,
    @field:Min(value = 1, message = "이닝은 1 이상이어야 합니다.")
    @field:Max(value = 9, message = "이닝은 9 이하여야 합니다.")
    val innings: Int = 7,
    @field:Size(max = 50, message = "팀 이름은 50자 이하여야 합니다.")
    val teamAName: String = "Team A",
    @field:Size(max = 50, message = "팀 이름은 50자 이하여야 합니다.")
    val teamBName: String = "Team B",
)

/**
 * 이벤트 게임 참가 신청 요청
 */
data class JoinEventGameApiRequest(
    @field:Size(max = 500, message = "메시지는 500자 이하여야 합니다.")
    val message: String? = null,
)

/**
 * 팀 배정 요청
 */
data class AssignTeamApiRequest(
    @field:NotNull(message = "팀 배정은 필수입니다.")
    val team: String,
)

/**
 * 경기 종료 요청
 */
data class FinishEventGameApiRequest(
    @field:Min(value = 0, message = "점수는 0 이상이어야 합니다.")
    val teamAScore: Int,
    @field:Min(value = 0, message = "점수는 0 이상이어야 합니다.")
    val teamBScore: Int,
)

/**
 * 경기 취소 요청
 */
data class CancelEventGameApiRequest(
    @field:NotBlank(message = "취소 사유는 필수입니다.")
    val reason: String,
)
