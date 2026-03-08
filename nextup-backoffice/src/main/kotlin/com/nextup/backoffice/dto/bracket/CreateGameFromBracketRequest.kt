package com.nextup.backoffice.dto.bracket

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateGameFromBracketRequest(
    @field:NotNull
    val scheduledAt: LocalDateTime,
    val location: String? = null,
    val fieldName: String? = null,
)
