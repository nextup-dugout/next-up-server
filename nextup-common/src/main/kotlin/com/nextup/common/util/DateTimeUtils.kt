package com.nextup.common.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    val KST: ZoneId = ZoneId.of("Asia/Seoul")

    fun nowInKst(): LocalDateTime = LocalDateTime.now(KST)

    fun todayInKst(): LocalDate = LocalDate.now(KST)

    fun toKstLocalDateTime(instant: Instant): LocalDateTime = LocalDateTime.ofInstant(instant, KST)

    fun formatDate(
        date: LocalDate,
        pattern: String = "yyyy-MM-dd",
    ): String = date.format(DateTimeFormatter.ofPattern(pattern))

    fun formatDateTime(
        dateTime: LocalDateTime,
        pattern: String = "yyyy-MM-dd HH:mm:ss",
    ): String = dateTime.format(DateTimeFormatter.ofPattern(pattern))
}
