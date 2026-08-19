package org.snkt.partylauncher.minecraft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.snkt.partylauncher.config.LauncherConfig

class PlaytimeTrackerTest {

    @Test
    fun testFormatPlaytime() {
        assertEquals("< 1 мин", PlaytimeTracker.formatPlaytime(0))
        assertEquals("< 1 мин", PlaytimeTracker.formatPlaytime(45))
        assertEquals("1 мин", PlaytimeTracker.formatPlaytime(60))
        assertEquals("45 мин", PlaytimeTracker.formatPlaytime(45 * 60))
        assertEquals("1 ч", PlaytimeTracker.formatPlaytime(3600))
        assertEquals("2 ч 15 мин", PlaytimeTracker.formatPlaytime(2 * 3600 + 15 * 60))
        assertEquals("14 ч 35 мин", PlaytimeTracker.formatPlaytime(14 * 3600 + 35 * 60))
    }

    @Test
    fun testAddPlaytime() {
        var config = LauncherConfig()
        val uuid = "123e4567-e89b-12d3-a456-426614174000"

        assertEquals(0L, PlaytimeTracker.getPlaytimeSeconds(config, uuid))

        config = PlaytimeTracker.addPlaytime(config, uuid, 120)
        assertEquals(120L, PlaytimeTracker.getPlaytimeSeconds(config, uuid))

        config = PlaytimeTracker.addPlaytime(config, uuid, 60)
        assertEquals(180L, PlaytimeTracker.getPlaytimeSeconds(config, uuid))
    }
}
