package com.nextup.common.util

object StringUtils {

    fun String.toSlug(): String =
        this.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')

    fun String.truncate(maxLength: Int, suffix: String = "..."): String =
        if (this.length <= maxLength) this
        else this.take(maxLength - suffix.length) + suffix

    fun String?.orEmpty(): String = this ?: ""

    fun String?.isNullOrBlankOrEmpty(): Boolean =
        this == null || this.isBlank() || this.isEmpty()
}
