package org.snkt.partylauncher.auth

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService
import org.snkt.partylauncher.auth.models.DeviceCodeResponse
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.logging.AppLogger
import java.util.function.Consumer

class MicrosoftAuthService {

    /**
     * Executes the official Minecraft authentication via Device Code Flow using MinecraftAuth.
     */
    suspend fun loginWithDeviceCode(
        onDeviceCodeReceived: (DeviceCodeResponse) -> Unit
    ): Result<MinecraftSession> = withContext(Dispatchers.IO) {
        AppLogger.info("MicrosoftAuth", "Starting Microsoft Device Code Login Flow...")
        try {
            val httpClient = MinecraftAuth.createHttpClient()

            val authCallback = Consumer<MsaDeviceCode> { msaCode ->
                AppLogger.info("MicrosoftAuth", "Received device code: ${msaCode.userCode} for URL: ${msaCode.directVerificationUri ?: msaCode.verificationUri}")
                val response = DeviceCodeResponse(
                    userCode = msaCode.userCode,
                    deviceCode = msaCode.deviceCode,
                    verificationUri = msaCode.directVerificationUri ?: msaCode.verificationUri,
                    expiresIn = ((msaCode.expireTimeMs - System.currentTimeMillis()) / 1000).coerceAtLeast(60),
                    interval = (msaCode.intervalMs / 1000).coerceAtLeast(3)
                )
                onDeviceCodeReceived(response)
            }

            val authManager = JavaAuthManager.create(httpClient)
                .login(::DeviceCodeMsaAuthService, authCallback)

            AppLogger.info("MicrosoftAuth", "OAuth successful, retrieving Minecraft profile and entitlements...")

            val profile = authManager.minecraftProfile.getUpToDate()
                ?: throw LauncherError.AuthenticationFailed("Minecraft profile not found for this account")

            val token = authManager.minecraftToken.getUpToDate()
                ?: throw LauncherError.AuthenticationFailed("Failed to obtain Minecraft token")

            val entitlements = try { authManager.minecraftEntitlements.getUpToDate() } catch (e: Exception) { null }
            if (entitlements != null && entitlements.items.isEmpty()) {
                AppLogger.error("MicrosoftAuth", "Account does not own Minecraft Java Edition.")
                return@withContext Result.failure(LauncherError.MinecraftNotOwned)
            }

            val xuid = try {
                authManager.xboxUserProfile.getUpToDate()?.id
            } catch (e: Exception) {
                null
            }

            val serializedJson = JavaAuthManager.toJson(authManager).toString()

            val session = MinecraftSession(
                username = profile.name,
                uuid = profile.id.toString(),
                xuid = xuid,
                minecraftAccessToken = token.token,
                microsoftRefreshToken = null,
                authManagerJson = serializedJson,
                expiresAtEpochMs = token.expireTimeMs,
                skinUrl = null
            )

            AppLogger.info("MicrosoftAuth", "Successfully authenticated player '${session.username}' (UUID: ${session.formattedUuid})")
            Result.success(session)
        } catch (e: LauncherError) {
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.error("MicrosoftAuth", "Authentication failed: ${e.message}", e)
            Result.failure(LauncherError.AuthenticationFailed(e.message ?: "Authentication failed", cause = e))
        }
    }

    /**
     * Refreshes the session using the serialized JavaAuthManager JSON state.
     */
    suspend fun refreshSession(savedAuthManagerJson: String): Result<MinecraftSession> = withContext(Dispatchers.IO) {
        AppLogger.info("MicrosoftAuth", "Refreshing saved Minecraft session...")
        try {
            val httpClient = MinecraftAuth.createHttpClient()
            val jsonObject = JsonParser.parseString(savedAuthManagerJson).asJsonObject
            val authManager = JavaAuthManager.fromJson(httpClient, jsonObject)

            val token = authManager.minecraftToken.getUpToDate()
                ?: throw LauncherError.AuthenticationFailed("Failed to refresh Minecraft token")
            val profile = authManager.minecraftProfile.getUpToDate()
                ?: throw LauncherError.AuthenticationFailed("Failed to refresh Minecraft profile")

            val xuid = try {
                authManager.xboxUserProfile.getUpToDate()?.id
            } catch (e: Exception) {
                null
            }

            val newJson = JavaAuthManager.toJson(authManager).toString()

            val session = MinecraftSession(
                username = profile.name,
                uuid = profile.id.toString(),
                xuid = xuid,
                minecraftAccessToken = token.token,
                authManagerJson = newJson,
                expiresAtEpochMs = token.expireTimeMs,
                skinUrl = null
            )

            AppLogger.info("MicrosoftAuth", "Session successfully refreshed for '${session.username}'")
            Result.success(session)
        } catch (e: Exception) {
            AppLogger.error("MicrosoftAuth", "Failed to refresh session: ${e.message}", e)
            Result.failure(e)
        }
    }
}
