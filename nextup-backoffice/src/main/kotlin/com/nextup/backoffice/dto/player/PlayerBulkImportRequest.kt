package com.nextup.backoffice.dto.player

import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 선수 벌크 임포트 요청 DTO
 */
data class PlayerBulkImportRequest(
    @field:NotEmpty(message = "임포트할 선수 목록이 비어 있습니다.")
    @field:Valid
    val players: List<PlayerImportItem>,
)

data class PlayerImportItem(
    @field:NotBlank(message = "선수 이름은 필수입니다.")
    val name: String,
    @field:NotNull(message = "주 포지션은 필수입니다.")
    val primaryPosition: Position,
    val birthDate: LocalDate? = null,
    val birthPlace: String? = null,
    val nationality: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val throwingHand: ThrowingHand? = null,
    val battingHand: BattingHand? = null,
    val debutYear: Int? = null,
) {
    fun toEntity(): Player =
        Player(
            name = name,
            primaryPosition = primaryPosition,
            birthDate = birthDate,
            birthPlace = birthPlace,
            nationality = nationality,
            height = height,
            weight = weight,
            throwingHand = throwingHand,
            battingHand = battingHand,
            debutYear = debutYear,
        )
}
