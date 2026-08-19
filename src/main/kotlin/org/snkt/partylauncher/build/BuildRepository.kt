package org.snkt.partylauncher.build

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.logging.AppLogger

class BuildRepository(
    private val client: HttpClient = HttpClient(CIO) {
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
     * Fetches the build manifest from the specified remote server URL.
     */
    suspend fun fetchManifest(serverBaseUrl: String): Result<BuildManifest> {
        val trimmed = serverBaseUrl.trim().trimEnd('/')
        val manifestUrl = if (trimmed.endsWith(".json")) trimmed else "$trimmed/manifest.json"

        AppLogger.info("BuildRepository", "Fetching manifest from $manifestUrl")

        return try {
            val response = client.get(manifestUrl)
            if (!response.status.isSuccess()) {
                val error = LauncherError.ManifestUnavailable(
                    manifestUrl,
                    "HTTP ${response.status.value}: ${response.status.description}"
                )
                AppLogger.error("BuildRepository", error.userMessage)
                return Result.failure(Exception(error.userMessage))
            }

            val body = response.bodyAsText()
            val manifest = json.decodeFromString<BuildManifest>(body)
            AppLogger.info("BuildRepository", "Manifest loaded: Build v${manifest.version}, Minecraft ${manifest.minecraft}, Loader ${manifest.loader} ${manifest.loaderVersion}")
            Result.success(manifest)
        } catch (e: Exception) {
            val error = LauncherError.ManifestUnavailable(manifestUrl, e.message, e)
            AppLogger.error("BuildRepository", error.userMessage, e)
            Result.failure(e)
        }
    }

    /**
     * Resolves the full download URL for the build file (handling relative paths).
     */
    fun resolveBuildFileUrl(serverBaseUrl: String, filePath: String): String {
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath
        }
        val trimmed = serverBaseUrl.trim().trimEnd('/')
        val cleanPath = filePath.trimStart('/')
        return "$trimmed/$cleanPath"
    }
}
