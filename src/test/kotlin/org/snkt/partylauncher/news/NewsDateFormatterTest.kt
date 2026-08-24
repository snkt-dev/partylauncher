package org.snkt.partylauncher.news

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.snkt.partylauncher.news.util.NewsDateFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NewsDateFormatterTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testFormatToday() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 8, 24), LocalTime.of(15, 30), zoneId).toInstant()
        val itemTime = ZonedDateTime.of(LocalDate.of(2026, 8, 24), LocalTime.of(14, 38), zoneId).toInstant().toEpochMilli()

        val formatted = NewsDateFormatter.format(itemTime, zoneId, now)
        assertEquals("Сегодня, 14:38", formatted)
    }

    @Test
    fun testFormatYesterday() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 8, 24), LocalTime.of(15, 30), zoneId).toInstant()
        val itemTime = ZonedDateTime.of(LocalDate.of(2026, 8, 23), LocalTime.of(19, 45), zoneId).toInstant().toEpochMilli()

        val formatted = NewsDateFormatter.format(itemTime, zoneId, now)
        assertEquals("Вчера, 19:45", formatted)
    }

    @Test
    fun testFormatOlderDates() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 8, 24), LocalTime.of(15, 30), zoneId).toInstant()
        val itemTime = ZonedDateTime.of(LocalDate.of(2026, 8, 15), LocalTime.of(10, 0), zoneId).toInstant().toEpochMilli()

        val formatted = NewsDateFormatter.format(itemTime, zoneId, now)
        assertTrue(formatted.contains("15") && formatted.contains("10:00"), "Expected date containing 15 and 10:00 but got $formatted")
    }

    @Test
    fun testFormatZeroTimestamp() {
        val formatted = NewsDateFormatter.format(0L)
        assertEquals("Недавно", formatted)
    }
}
