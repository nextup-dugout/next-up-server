package com.nextup.core.domain.game

import com.nextup.core.domain.player.Position
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * PositionHistoryEntry 리스트 ↔ CSV 문자열 변환기
 *
 * DB 컬럼(TEXT)에 "이닝:포지션,이닝:포지션,..." 형식으로 저장합니다.
 * 기존 CSV 형식과 완전 호환됩니다.
 */
@Converter
class PositionHistoryConverter : AttributeConverter<List<PositionHistoryEntry>, String?> {
    override fun convertToDatabaseColumn(attribute: List<PositionHistoryEntry>?): String? {
        if (attribute.isNullOrEmpty()) return null
        return attribute.joinToString(",") { "${it.inning}:${it.position.name}" }
    }

    override fun convertToEntityAttribute(dbData: String?): List<PositionHistoryEntry> {
        if (dbData.isNullOrBlank()) return emptyList()
        return dbData
            .split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val inning = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                    val position =
                        runCatching { Position.valueOf(parts[1].trim()) }.getOrNull()
                            ?: return@mapNotNull null
                    PositionHistoryEntry(inning, position)
                } else {
                    null
                }
            }
    }
}
