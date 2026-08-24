package org.snkt.partylauncher.news.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NewsDateFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("ru"))
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("ru"))

    /**
     * Formats timestamp in epoch milliseconds according to:
     * - "Сегодня, HH:mm" (if today)
     * - "Вчера, HH:mm" (if yesterday)
     * - "d MMM yyyy, HH:mm" or "d MMM, HH:mm" (if older than 2 days)
     */
    fun format(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault(), nowInstant: Instant = Instant.now()): String {
        if (epochMs <= 0L) return "Недавно"

        val itemDateTime = Instant.ofEpochMilli(epochMs).atZone(zoneId)
        val itemDate = itemDateTime.toLocalDate()

        val nowDate = nowInstant.atZone(zoneId).toLocalDate()

        val timeStr = itemDateTime.format(timeFormatter)

        return when (itemDate) {
            nowDate -> "Сегодня, $timeStr"
            nowDate.minusDays(1) -> "Вчера, $timeStr"
            else -> {
                if (itemDate.year == nowDate.year) {
                    itemDateTime.format(shortDateFormatter)
                } else {
                    itemDateTime.format(fullDateFormatter)
                }
            }
        }
    }
}
