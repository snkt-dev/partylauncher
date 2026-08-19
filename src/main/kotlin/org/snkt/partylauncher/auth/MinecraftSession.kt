package org.snkt.partylauncher.auth

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftSession(
    val username: String,
    val uuid: String,
    val xuid: String? = null,
    val minecraftAccessToken: String,
    val microsoftRefreshToken: String? = null,
    val authManagerJson: String? = null,
    val expiresAtEpochMs: Long = 0L,
    val skinUrl: String? = null
) {
    val formattedUuid: String
        get() = if (uuid.contains("-")) uuid else {
            if (uuid.length == 32) {
                "${uuid.substring(0, 8)}-${uuid.substring(8, 12)}-${uuid.substring(12, 16)}-${uuid.substring(16, 20)}-${uuid.substring(20, 32)}"
            } else uuid
        }

    fun isExpired(): Boolean {
        // Expired if less than 2 minutes remaining
        return System.currentTimeMillis() >= (expiresAtEpochMs - 120_000)
    }
}
