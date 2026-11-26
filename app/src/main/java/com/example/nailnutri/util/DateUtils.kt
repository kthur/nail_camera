package com.example.nailnutri.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Thread-safe date formatting utility compatible with minSdk 24+.
 * Replaces ad-hoc SimpleDateFormat allocations across analyzers and UI screens.
 */
object DateUtils {

    private val dateTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    private val fullDateTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private val koreanSessionFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("M월 d일 세션", Locale.KOREAN)
    }

    fun nowFormatted(): String =
        requireNotNull(dateTimeFormatter.get()).format(Date())

    fun nowKoreanSessionLabel(): String =
        requireNotNull(koreanSessionFormatter.get()).format(Date())

    fun formatTimestamp(timestampMs: Long, includeSeconds: Boolean = false): String {
        val fmt = if (includeSeconds) {
            requireNotNull(fullDateTimeFormatter.get())
        } else {
            requireNotNull(dateTimeFormatter.get())
        }
        return fmt.format(Date(timestampMs))
    }
}
