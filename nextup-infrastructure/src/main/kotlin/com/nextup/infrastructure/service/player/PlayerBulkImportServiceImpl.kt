package com.nextup.infrastructure.service.player

import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.service.player.PlayerBulkImportService
import com.nextup.core.service.player.PlayerImportError
import com.nextup.core.service.player.PlayerImportResult
import com.nextup.core.service.player.PlayerImportRow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 선수 벌크 임포트 서비스 구현체
 *
 * 각 행을 독립적으로 처리하며, 오류 행은 건너뛰고 결과 리포트를 반환합니다.
 */
@Service
@Transactional
class PlayerBulkImportServiceImpl(
    private val playerRepository: PlayerRepositoryPort,
) : PlayerBulkImportService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun importPlayers(rows: List<PlayerImportRow>): PlayerImportResult {
        val importedPlayers = mutableListOf<Player>()
        val errors = mutableListOf<PlayerImportError>()

        for (row in rows) {
            try {
                val player = parseAndCreatePlayer(row)
                val saved = playerRepository.save(player)
                importedPlayers.add(saved)
            } catch (ex: Exception) {
                log.warn("Player import failed at row {}: {}", row.rowNumber, ex.message)
                errors.add(
                    PlayerImportError(
                        rowNumber = row.rowNumber,
                        reason = ex.message ?: "알 수 없는 오류",
                    ),
                )
            }
        }

        return PlayerImportResult(
            successCount = importedPlayers.size,
            errorCount = errors.size,
            importedPlayers = importedPlayers,
            errors = errors,
        )
    }

    private fun parseAndCreatePlayer(row: PlayerImportRow): Player {
        require(row.name.isNotBlank()) { "선수 이름은 필수입니다 (행 ${row.rowNumber})" }

        val position =
            try {
                Position.valueOf(row.primaryPosition.uppercase())
            } catch (ex: IllegalArgumentException) {
                throw IllegalArgumentException("유효하지 않은 포지션입니다: '${row.primaryPosition}' (행 ${row.rowNumber})")
            }

        val birthDate =
            row.birthDate?.let {
                try {
                    LocalDate.parse(it)
                } catch (ex: DateTimeParseException) {
                    throw IllegalArgumentException(
                        "유효하지 않은 생년월일 형식입니다: '$it' (ISO 형식 예: 1990-01-15, 행 ${row.rowNumber})"
                    )
                }
            }

        val throwingHand =
            row.throwingHand?.let {
                try {
                    ThrowingHand.valueOf(it.uppercase())
                } catch (ex: IllegalArgumentException) {
                    throw IllegalArgumentException("유효하지 않은 투구 손입니다: '$it' (행 ${row.rowNumber})")
                }
            }

        val battingHand =
            row.battingHand?.let {
                try {
                    BattingHand.valueOf(it.uppercase())
                } catch (ex: IllegalArgumentException) {
                    throw IllegalArgumentException("유효하지 않은 타격 손입니다: '$it' (행 ${row.rowNumber})")
                }
            }

        return Player(
            name = row.name.trim(),
            primaryPosition = position,
            birthDate = birthDate,
            height = row.height,
            weight = row.weight,
            throwingHand = throwingHand,
            battingHand = battingHand,
        )
    }
}
