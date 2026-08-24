package org.snkt.partylauncher.minecraft

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.minecraft.models.FabricProfile
import org.snkt.partylauncher.util.OSUtils
import java.nio.file.Files

class FabricMetaClient(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Fetches the Fabric profile JSON for a specific Minecraft version and Fabric loader version,
     * caching it locally for offline support.
     */
    suspend fun fetchFabricProfile(gameVersion: String, loaderVersion: String): Result<FabricProfile> = withContext(Dispatchers.IO) {
        val versionsDir = OSUtils.getVersionsDir().resolve(gameVersion)
        Files.createDirectories(versionsDir)
        val cachedProfileFile = versionsDir.resolve("fabric-$loaderVersion.json")

        // 1. Return cached profile if available and offline/valid
        if (Files.exists(cachedProfileFile)) {
            try {
                val content = Files.readString(cachedProfileFile)
                val profile = json.decodeFromString<FabricProfile>(content)
                AppLogger.info("FabricMeta", "Using cached Fabric profile for MC $gameVersion (Loader $loaderVersion)")
                return@withContext Result.success(profile)
            } catch (e: Exception) {
                AppLogger.warn("FabricMeta", "Failed to parse cached Fabric profile, re-fetching...")
            }
        }

        val url = "https://meta.fabricmc.net/v2/versions/loader/$gameVersion/$loaderVersion/profile/json"
        AppLogger.info("FabricMeta", "Fetching Fabric profile for MC $gameVersion (Loader $loaderVersion) from $url")

        try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                val error = LauncherError.MinecraftInstallationFailed(
                    "Загрузка профиля Fabric",
                    "HTTP ${response.status.value}: ${response.status.description} ($url)"
                )
                return@withContext Result.failure(Exception(error.userMessage))
            }

            val body = response.bodyAsText()
            Files.writeString(cachedProfileFile, body)
            val profile = json.decodeFromString<FabricProfile>(body)
            AppLogger.info("FabricMeta", "Fabric profile loaded with ${profile.libraries.size} libraries. Main class: ${profile.mainClass}")
            Result.success(profile)
        } catch (e: Exception) {
            AppLogger.error("FabricMeta", "Failed to fetch Fabric profile: ${e.message}", e)
            Result.failure(e)
        }
    }
}
