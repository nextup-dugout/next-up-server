package com.nextup.backoffice.service

import com.nextup.backoffice.dto.player.PlayerBulkImportResponse
import com.nextup.backoffice.dto.player.PlayerImportFailure
import com.nextup.backoffice.dto.player.PlayerImportItem
import com.nextup.backoffice.dto.player.PlayerImportResult
import com.nextup.backoffice.dto.player.toImportResult
import com.nextup.core.port.repository.PlayerRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 선수 벌크 임포트 서비스
 *
 * CSV 또는 JSON 형식으로 전달된 선수 목록을 일괄 등록합니다.
 * 각 항목별로 개별 검증을 수행하며, 일부 실패해도 성공한 항목은 저장됩니다.
 */
@Service
@Transactional
class PlayerBulkImportService(
    private val playerRepository: PlayerRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(PlayerBulkImportService::class.java)

    /**
     * 선수 목록을 일괄 등록합니다.
     *
     * @param items 임포트할 선수 항목 목록
     * @return 임포트 결과 (성공/실패 항목 포함)
     */
    fun importPlayers(items: List<PlayerImportItem>): PlayerBulkImportResponse {
        val importedPlayers = mutableListOf<PlayerImportResult>()
        val failures = mutableListOf<PlayerImportFailure>()

        items.forEachIndexed { index, item ->
            runCatching {
                validateItem(item, index)
                val player = item.toEntity()
                val saved = playerRepository.save(player)
                importedPlayers.add(saved.toImportResult())
                logger.info("선수 임포트 성공 - row={}, name={}, id={}", index, saved.name, saved.id)
            }.onFailure { e ->
                logger.warn("선수 임포트 실패 - row={}, name={}, reason={}", index, item.name, e.message)
                failures.add(
                    PlayerImportFailure(
                        rowIndex = index,
                        name = item.name,
                        reason = e.message ?: "알 수 없는 오류",
                    ),
                )
            }
        }

        logger.info(
            "선수 벌크 임포트 완료 - total={}, success={}, failure={}",
            items.size,
            importedPlayers.size,
            failures.size,
        )

        return PlayerBulkImportResponse(
            totalRequested = items.size,
            successCount = importedPlayers.size,
            failureCount = failures.size,
            importedPlayers = importedPlayers,
            failures = failures,
        )
    }

    private fun validateItem(
        item: PlayerImportItem,
        index: Int,
    ) {
        require(item.name.isNotBlank()) { "row[$index]: 선수 이름은 필수입니다." }
        item.height?.let {
            require(it in 100..250) { "row[$index]: 키는 100~250cm 범위여야 합니다. 입력값: $it" }
        }
        item.weight?.let {
            require(it in 30..200) { "row[$index]: 몸무게는 30~200kg 범위여야 합니다. 입력값: $it" }
        }
        item.debutYear?.let {
            require(it in 1900..2100) { "row[$index]: 데뷔 연도가 유효하지 않습니다. 입력값: $it" }
        }
    }
}
