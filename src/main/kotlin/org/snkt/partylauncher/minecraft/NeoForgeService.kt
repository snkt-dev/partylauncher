package org.snkt.partylauncher.minecraft

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.core.ProgressInfo
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.minecraft.models.LibraryEntry
import org.snkt.partylauncher.minecraft.models.MojangVersionDetails
import org.snkt.partylauncher.util.OSUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class NeoForgeService(
    private val httpClient: HttpClient,
    private val minecraftDir: Path = OSUtils.getMinecraftCacheDir()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun prepareNeoForge(
        minecraftVersion: String,
        loaderVersion: String,
        javaExecutable: Path,
        onProgress: (ProgressInfo) -> Unit
    ): Result<NeoForgeProfile> = withContext(Dispatchers.IO) {
        try {
            val versionId = "neoforge-$loaderVersion"
            val versionsDir = minecraftDir.resolve("versions").resolve(versionId)
            val versionJsonPath = versionsDir.resolve("$versionId.json")

            // Check if NeoForge is already installed and valid (must have libraries)
            if (Files.exists(versionJsonPath)) {
                try {
                    val content = Files.readString(versionJsonPath)
                    val details = json.decodeFromString<MojangVersionDetails>(content)
                    if (details.libraries.isNotEmpty() && details.mainClass.isNotBlank()) {
                        AppLogger.info("NeoForgeService", "Found existing valid NeoForge profile for $versionId")
                        return@withContext Result.success(parseNeoForgeProfile(details))
                    }
                } catch (e: Exception) {
                    AppLogger.warn("NeoForgeService", "Corrupted NeoForge version JSON, will reinstall: ${e.message}")
                }
            }

            // 1. Download installer JAR
            onProgress(ProgressInfo(title = "Скачивание установщика NeoForge $loaderVersion...", currentItem = 3, totalItems = 5))
            val installerCacheDir = OSUtils.getCacheDir().resolve("installers")
            Files.createDirectories(installerCacheDir)
            val installerJarPath = installerCacheDir.resolve("neoforge-$loaderVersion-installer.jar")

            val installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$loaderVersion/neoforge-$loaderVersion-installer.jar"
            if (!Files.exists(installerJarPath) || Files.size(installerJarPath) == 0L) {
                AppLogger.info("NeoForgeService", "Downloading NeoForge installer from $installerUrl")
                val response = httpClient.get(installerUrl)
                if (!response.status.isSuccess()) {
                    throw LauncherError.DownloadFailed("NeoForge installer", "HTTP ${response.status.value}")
                }
                val tempPath = installerJarPath.parent.resolve("${installerJarPath.fileName}.tmp")
                Files.write(tempPath, response.bodyAsBytes())
                Files.move(tempPath, installerJarPath, StandardCopyOption.REPLACE_EXISTING)
            }

            // 2. Ensure launcher_profiles.json exists in minecraftDir
            val launcherProfilesPath = minecraftDir.resolve("launcher_profiles.json")
            if (!Files.exists(launcherProfilesPath)) {
                Files.createDirectories(minecraftDir)
                Files.writeString(launcherProfilesPath, "{\"profiles\":{}}")
            }

            // 3. Run client installation
            onProgress(ProgressInfo(title = "Установка компонентов NeoForge $loaderVersion...", currentItem = 3, totalItems = 5))
            AppLogger.info("NeoForgeService", "Running NeoForge installer into $minecraftDir using $javaExecutable")

            val processBuilder = ProcessBuilder(
                javaExecutable.toAbsolutePath().toString(),
                "-jar",
                installerJarPath.toAbsolutePath().toString(),
                "--installClient",
                minecraftDir.toAbsolutePath().toString()
            ).redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                AppLogger.error("NeoForgeService", "NeoForge installation failed (exit $exitCode): $output")
                throw LauncherError.MinecraftInstallationFailed("Не удалось установить NeoForge (код $exitCode)")
            }

            AppLogger.info("NeoForgeService", "NeoForge installer finished successfully for $versionId")

            if (!Files.exists(versionJsonPath)) {
                throw LauncherError.MinecraftInstallationFailed("NeoForge profile JSON was not created at $versionJsonPath")
            }

            val content = Files.readString(versionJsonPath)
            val details = json.decodeFromString<MojangVersionDetails>(content)
            Result.success(parseNeoForgeProfile(details))
        } catch (e: Exception) {
            AppLogger.error("NeoForgeService", "Failed to prepare NeoForge: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseNeoForgeProfile(details: MojangVersionDetails): NeoForgeProfile {
        val jvmArgs = mutableListOf<String>()
        val gameArgs = mutableListOf<String>()

        details.arguments?.let { args ->
            // JVM args
            args.jvm.forEach { elem ->
                when {
                    elem is JsonPrimitive && elem.isString -> jvmArgs.add(elem.content)
                    elem is JsonObject -> {
                        val value = elem["value"]
                        when {
                            value is JsonPrimitive && value.isString -> jvmArgs.add(value.content)
                            value is JsonArray -> value.forEach { item ->
                                if (item is JsonPrimitive && item.isString) jvmArgs.add(item.content)
                            }
                        }
                    }
                }
            }

            // Game args
            args.game.forEach { elem ->
                when {
                    elem is JsonPrimitive && elem.isString -> gameArgs.add(elem.content)
                    elem is JsonObject -> {
                        val value = elem["value"]
                        when {
                            value is JsonPrimitive && value.isString -> gameArgs.add(value.content)
                            value is JsonArray -> value.forEach { item ->
                                if (item is JsonPrimitive && item.isString) gameArgs.add(item.content)
                            }
                        }
                    }
                }
            }
        }

        return NeoForgeProfile(
            versionId = details.id,
            mainClass = details.mainClass,
            jvmArgs = jvmArgs,
            gameArgs = gameArgs,
            libraries = details.libraries
        )
    }
}

data class NeoForgeProfile(
    val versionId: String,
    val mainClass: String,
    val jvmArgs: List<String>,
    val gameArgs: List<String>,
    val libraries: List<LibraryEntry>
)
