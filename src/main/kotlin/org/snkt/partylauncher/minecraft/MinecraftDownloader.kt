package org.snkt.partylauncher.minecraft

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.core.ProgressInfo
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.minecraft.models.AssetObject
import org.snkt.partylauncher.minecraft.models.FabricProfile
import org.snkt.partylauncher.minecraft.models.MojangAssetIndex
import org.snkt.partylauncher.minecraft.models.MojangVersionDetails
import org.snkt.partylauncher.minecraft.models.MojangVersionManifest
import org.snkt.partylauncher.util.FileUtils
import org.snkt.partylauncher.util.OSUtils
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicInteger

class MinecraftDownloader(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 20000
            socketTimeoutMillis = 60000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 PartyLauncher/1.0")
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    },
    private val fabricClient: FabricMetaClient = FabricMetaClient(httpClient)
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Installs or verifies all required Minecraft components (Version JSON, Client JAR, Libraries, Assets, Fabric).
     * @return Resolved launch components data needed by MinecraftLauncher
     */
    suspend fun prepareMinecraft(
        minecraftVersion: String,
        loader: String = "fabric",
        loaderVersion: String = "",
        onProgress: (ProgressInfo) -> Unit = {}
    ): Result<MinecraftInstallationResult> = withContext(Dispatchers.IO) {
        try {
            AppLogger.info("MinecraftDownloader", "Preparing Minecraft $minecraftVersion ($loader $loaderVersion)...")

            // 1. Fetch Mojang Version Manifest
            onProgress(ProgressInfo(title = "Получение манифеста версий Mojang...", currentItem = 1, totalItems = 5))
            val versionDetails = fetchVersionDetails(minecraftVersion)

            // 2. Download Minecraft Client JAR with streaming progress and retries
            onProgress(ProgressInfo(title = "Проверка и загрузка Minecraft client.jar...", currentItem = 2, totalItems = 5))
            val clientJar = downloadClientJar(versionDetails, onProgress)

            // 3. Resolve and Download Fabric Profile (if requested)
            val fabricProfile = if (loader.equals("fabric", ignoreCase = true) && loaderVersion.isNotBlank()) {
                onProgress(ProgressInfo(title = "Получение метаданных Fabric Loader...", currentItem = 3, totalItems = 5))
                fabricClient.fetchFabricProfile(minecraftVersion, loaderVersion).getOrThrow()
            } else null

            // 4. Download Mojang & Fabric Libraries
            onProgress(ProgressInfo(title = "Проверка и загрузка библиотек...", currentItem = 4, totalItems = 5))
            val libraryJars = downloadLibraries(versionDetails, fabricProfile, onProgress)

            // 5. Download Assets (indexes & objects)
            onProgress(ProgressInfo(title = "Проверка и загрузка ресурсов (assets)...", currentItem = 5, totalItems = 5))
            val assetIndexId = downloadAssets(versionDetails, onProgress)

            val mainClass = fabricProfile?.mainClass ?: versionDetails.mainClass
            AppLogger.info("MinecraftDownloader", "Minecraft preparation complete! Main class: $mainClass, Total libs: ${libraryJars.size}")

            Result.success(
                MinecraftInstallationResult(
                    minecraftVersion = minecraftVersion,
                    clientJar = clientJar,
                    libraryJars = libraryJars,
                    mainClass = mainClass,
                    assetIndexId = assetIndexId,
                    versionDetails = versionDetails
                )
            )
        } catch (e: Exception) {
            AppLogger.error("MinecraftDownloader", "Failed to prepare Minecraft: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchVersionDetails(minecraftVersion: String): MojangVersionDetails {
        val versionsDir = OSUtils.getVersionsDir().resolve(minecraftVersion)
        Files.createDirectories(versionsDir)
        val localVersionJson = versionsDir.resolve("$minecraftVersion.json")

        if (Files.exists(localVersionJson)) {
            try {
                val content = Files.readString(localVersionJson)
                return json.decodeFromString<MojangVersionDetails>(content)
            } catch (e: Exception) {
                AppLogger.warn("MinecraftDownloader", "Failed to read cached version JSON, re-fetching...")
            }
        }

        val manifestUrl = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
        val response = httpClient.get(manifestUrl)
        if (!response.status.isSuccess()) {
            throw LauncherError.MinecraftInstallationFailed("Не удалось получить манифест версий Mojang")
        }

        val manifest = json.decodeFromString<MojangVersionManifest>(response.bodyAsText())
        val versionEntry = manifest.versions.find { it.id == minecraftVersion }
            ?: throw LauncherError.MinecraftInstallationFailed("Версия Minecraft $minecraftVersion не найдена в манифесте Mojang")

        val detailsResponse = httpClient.get(versionEntry.url)
        if (!detailsResponse.status.isSuccess()) {
            throw LauncherError.MinecraftInstallationFailed("Не удалось загрузить детали версии $minecraftVersion")
        }

        val detailsJson = detailsResponse.bodyAsText()
        Files.writeString(localVersionJson, detailsJson)
        return json.decodeFromString<MojangVersionDetails>(detailsJson)
    }

    private suspend fun downloadClientJar(
        versionDetails: MojangVersionDetails,
        onProgress: (ProgressInfo) -> Unit = {}
    ): Path {
        val clientArtifact = versionDetails.downloads["client"]
            ?: throw LauncherError.MinecraftInstallationFailed("Client JAR artifact definition is missing in version JSON")

        val versionsDir = OSUtils.getVersionsDir().resolve(versionDetails.id)
        Files.createDirectories(versionsDir)
        val clientJarPath = versionsDir.resolve("${versionDetails.id}.jar")
        val partPath = versionsDir.resolve("${versionDetails.id}.jar.part")

        if (Files.exists(clientJarPath)) {
            if (clientArtifact.sha1.isBlank() || FileUtils.computeSha1(clientJarPath).equals(clientArtifact.sha1, ignoreCase = true)) {
                AppLogger.info("MinecraftDownloader", "Existing valid client.jar found for ${versionDetails.id}")
                return clientJarPath
            }
        }

        var lastException: Exception? = null
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                AppLogger.info("MinecraftDownloader", "Downloading client.jar for ${versionDetails.id} (attempt $attempt/$maxRetries) from ${clientArtifact.url}")
                Files.deleteIfExists(partPath)

                httpClient.prepareGet(clientArtifact.url).execute { response ->
                    if (!response.status.isSuccess()) {
                        throw LauncherError.DownloadFailed("client.jar", "HTTP ${response.status.value}: ${response.status.description}")
                    }

                    val totalBytes = response.contentLength() ?: clientArtifact.size ?: -1L
                    val channel: ByteReadChannel = response.bodyAsChannel()

                    FileOutputStream(partPath.toFile()).use { output ->
                        val buffer = ByteArray(65536)
                        var downloadedBytes = 0L
                        var lastTime = System.currentTimeMillis()
                        var bytesSinceLastTime = 0L
                        var currentSpeed = 0L

                        while (!channel.isClosedForRead) {
                            currentCoroutineContext().ensureActive()
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read <= 0) break

                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            bytesSinceLastTime += read

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastTime
                            if (elapsed >= 400) {
                                currentSpeed = (bytesSinceLastTime * 1000) / elapsed
                                lastTime = now
                                bytesSinceLastTime = 0L

                                onProgress(
                                    ProgressInfo(
                                        title = "Скачивание client.jar (${versionDetails.id})...",
                                        currentBytes = downloadedBytes,
                                        totalBytes = totalBytes,
                                        speedBytesPerSec = currentSpeed,
                                        currentItem = 2,
                                        totalItems = 5
                                    )
                                )
                            }
                        }
                    }
                }

                // Verify SHA-1 hash
                if (clientArtifact.sha1.isNotBlank()) {
                    val actualHash = FileUtils.computeSha1(partPath)
                    if (!actualHash.equals(clientArtifact.sha1, ignoreCase = true)) {
                        Files.deleteIfExists(partPath)
                        throw LauncherError.HashMismatch("client.jar", clientArtifact.sha1, actualHash)
                    }
                }

                Files.move(partPath, clientJarPath, StandardCopyOption.REPLACE_EXISTING)
                AppLogger.info("MinecraftDownloader", "client.jar successfully downloaded and verified for ${versionDetails.id}")
                return clientJarPath
            } catch (e: Exception) {
                lastException = e
                Files.deleteIfExists(partPath)
                AppLogger.warn("MinecraftDownloader", "Attempt $attempt failed to download client.jar: ${e.message}")
                if (attempt < maxRetries) {
                    delay(1500L * attempt)
                }
            }
        }

        throw lastException ?: LauncherError.DownloadFailed("client.jar", "Failed after $maxRetries attempts")
    }

    private suspend fun downloadLibraries(
        versionDetails: MojangVersionDetails,
        fabricProfile: FabricProfile?,
        onProgress: (ProgressInfo) -> Unit
    ): List<Path> = coroutineScope {
        val librariesDir = OSUtils.getLibrariesDir()
        Files.createDirectories(librariesDir)

        val resolvedPaths = mutableListOf<Path>()
        val downloadTasks = mutableListOf<suspend () -> Unit>()

        // 1. Mojang Libraries
        for (lib in versionDetails.libraries) {
            if (!lib.isAllowedOnCurrentSystem()) continue

            val relPath = lib.getArtifactRelativePath()
            val localPath = librariesDir.resolve(relPath).normalize()
            resolvedPaths.add(localPath)

            val artifact = lib.downloads?.artifact
            val downloadUrl = artifact?.url ?: if (lib.url != null) "${lib.url.trimEnd('/')}/$relPath" else "https://libraries.minecraft.net/$relPath"
            val expectedSha1 = artifact?.sha1

            if (!isLibraryValid(localPath, expectedSha1)) {
                downloadTasks.add {
                    downloadFileWithRetry(downloadUrl, localPath, expectedSha1)
                }
            }
        }

        // 2. Fabric Libraries
        fabricProfile?.libraries?.forEach { lib ->
            val relPath = lib.getArtifactRelativePath()
            val localPath = librariesDir.resolve(relPath).normalize()
            resolvedPaths.add(localPath)

            val downloadUrl = lib.getDownloadUrl()
            if (!isLibraryValid(localPath, null)) {
                downloadTasks.add {
                    downloadFileWithRetry(downloadUrl, localPath, null)
                }
            }
        }

        if (downloadTasks.isNotEmpty()) {
            AppLogger.info("MinecraftDownloader", "Downloading ${downloadTasks.size} missing libraries...")
            val semaphore = Semaphore(8)
            val completed = AtomicInteger(0)
            val total = downloadTasks.size

            downloadTasks.map { task ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        task()
                        val current = completed.incrementAndGet()
                        onProgress(
                            ProgressInfo(
                                title = "Загрузка библиотек ($current/$total)...",
                                currentItem = current,
                                totalItems = total
                            )
                        )
                    }
                }
            }.awaitAll()
        }

        resolvedPaths
    }

    private fun isLibraryValid(path: Path, expectedSha1: String?): Boolean {
        if (!Files.exists(path) || Files.size(path) == 0L) return false
        if (expectedSha1.isNullOrBlank()) return true
        return try {
            FileUtils.computeSha1(path).equals(expectedSha1, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun downloadAssets(
        versionDetails: MojangVersionDetails,
        onProgress: (ProgressInfo) -> Unit
    ): String = coroutineScope {
        val assetIndexRef = versionDetails.assetIndex ?: return@coroutineScope versionDetails.assets
        val assetsDir = OSUtils.getAssetsDir()
        val indexesDir = assetsDir.resolve("indexes")
        val objectsDir = assetsDir.resolve("objects")

        Files.createDirectories(indexesDir)
        Files.createDirectories(objectsDir)

        val indexFile = indexesDir.resolve("${assetIndexRef.id}.json")
        val indexJsonContent = if (Files.exists(indexFile)) {
            Files.readString(indexFile)
        } else {
            AppLogger.info("MinecraftDownloader", "Downloading asset index ${assetIndexRef.id} from ${assetIndexRef.url}")
            val response = httpClient.get(assetIndexRef.url)
            val text = response.bodyAsText()
            Files.writeString(indexFile, text)
            text
        }

        val assetIndex = json.decodeFromString<MojangAssetIndex>(indexJsonContent)
        val missingAssets = mutableListOf<Pair<String, AssetObject>>()

        for ((_, obj) in assetIndex.objects) {
            val localPath = objectsDir.resolve(obj.relativeStoragePath)
            if (!Files.exists(localPath) || Files.size(localPath) != obj.size) {
                missingAssets.add(obj.mojangDownloadUrl to obj)
            }
        }

        if (missingAssets.isNotEmpty()) {
            AppLogger.info("MinecraftDownloader", "Downloading ${missingAssets.size} missing assets...")
            val semaphore = Semaphore(16)
            val completed = AtomicInteger(0)
            val total = missingAssets.size

            missingAssets.map { (url, obj) ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val target = objectsDir.resolve(obj.relativeStoragePath)
                        downloadFileWithRetry(url, target, obj.hash)
                        val current = completed.incrementAndGet()
                        if (current % 25 == 0 || current == total) {
                            onProgress(
                                ProgressInfo(
                                    title = "Загрузка ресурсов ($current/$total)...",
                                    currentItem = current,
                                    totalItems = total
                                )
                            )
                        }
                    }
                }
            }.awaitAll()
        }

        assetIndexRef.id
    }

    private suspend fun downloadFileWithRetry(url: String, target: Path, expectedSha1: String?, maxRetries: Int = 3) {
        var lastErr: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                target.parent?.let { Files.createDirectories(it) }
                val temp = target.parent.resolve("${target.fileName}.tmp")

                val response = httpClient.get(url)
                if (!response.status.isSuccess()) {
                    throw LauncherError.DownloadFailed(target.fileName.toString(), "HTTP ${response.status.value}")
                }

                val bytes = response.bodyAsBytes()
                Files.write(temp, bytes)

                if (!expectedSha1.isNullOrBlank()) {
                    val actual = FileUtils.computeSha1(temp)
                    if (!actual.equals(expectedSha1, ignoreCase = true)) {
                        Files.deleteIfExists(temp)
                        throw LauncherError.HashMismatch(target.fileName.toString(), expectedSha1, actual)
                    }
                }

                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
                return
            } catch (e: Exception) {
                lastErr = e
                if (attempt < maxRetries) {
                    delay(500L * attempt)
                }
            }
        }
        AppLogger.warn("MinecraftDownloader", "Failed to download $url to $target after $maxRetries attempts: ${lastErr?.message}")
        throw lastErr ?: Exception("Failed to download $url")
    }
}

data class MinecraftInstallationResult(
    val minecraftVersion: String,
    val clientJar: Path,
    val libraryJars: List<Path>,
    val mainClass: String,
    val assetIndexId: String,
    val versionDetails: MojangVersionDetails
)
