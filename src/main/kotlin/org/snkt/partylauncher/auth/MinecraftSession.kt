package org.snkt.partylauncher.auth

import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.util.UUID

@Serializable
data class MinecraftSession(
    val username: String,
    val uuid: String,
    val xuid: String? = null,
    val minecraftAccessToken: String,
    val microsoftRefreshToken: String? = null,
    val authManagerJson: String? = null,
    val expiresAtEpochMs: Long = 0L,
    val skinUrl: String? = null,
    val isOffline: Boolean = false
) {
    val formattedUuid: String
        get() = if (uuid.contains("-")) uuid else {
            if (uuid.length == 32) {
                "${uuid.substring(0, 8)}-${uuid.substring(8, 12)}-${uuid.substring(12, 16)}-${uuid.substring(16, 20)}-${uuid.substring(20, 32)}"
            } else uuid
        }

    fun isExpired(): Boolean {
        if (isOffline) return false
        // Expired if less than 2 minutes remaining
        return System.currentTimeMillis() >= (expiresAtEpochMs - 120_000)
    }

    companion object {
        /**
         * Creates an offline Minecraft session using standard Mojang/Bukkit offline UUID generation.
         */
        fun createOffline(username: String): MinecraftSession {
            val cleanName = username.trim()
            val offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$cleanName".toByteArray(StandardCharsets.UTF_8)).toString()
            return MinecraftSession(
                username = cleanName,
                uuid = offlineUuid,
                xuid = null,
                minecraftAccessToken = "0",
                microsoftRefreshToken = null,
                authManagerJson = null,
                expiresAtEpochMs = Long.MAX_VALUE,
                skinUrl = null,
                isOffline = true
            )
        }
    }
}
