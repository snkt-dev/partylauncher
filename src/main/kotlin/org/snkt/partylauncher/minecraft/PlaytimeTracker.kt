package org.snkt.partylauncher.minecraft

import org.snkt.partylauncher.config.LauncherConfig

object PlaytimeTracker {

    /**
     * Formats playtime in seconds to human-readable Russian string.
     * Examples: "14 ч 35 мин", "45 мин", "< 1 мин"
     */
    fun formatPlaytime(totalSeconds: Long): String {
        if (totalSeconds < 60) return "< 1 мин"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60

        return when {
            hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
            hours > 0 -> "$hours ч"
            else -> "$minutes мин"
        }
    }

    /**
     * Retrieves playtime for a specific player UUID.
     */
    fun getPlaytimeSeconds(config: LauncherConfig, playerUuid: String): Long {
        return config.playtimeSecondsByUuid[playerUuid] ?: 0L
    }

    /**
     * Returns a new LauncherConfig with added playtime seconds for the player UUID.
     */
    fun addPlaytime(config: LauncherConfig, playerUuid: String, additionalSeconds: Long): LauncherConfig {
        if (additionalSeconds <= 0) return config
        val current = getPlaytimeSeconds(config, playerUuid)
        val updatedMap = config.playtimeSecondsByUuid.toMutableMap().apply {
            put(playerUuid, current + additionalSeconds)
        }
        return config.copy(playtimeSecondsByUuid = updatedMap)
    }
}
