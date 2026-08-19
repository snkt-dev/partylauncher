package org.snkt.partylauncher.build

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.core.ProgressInfo
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.OSUtils
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class BuildDownloader(
    private val client: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 30000
        }
    }
) {

    /**
     * Downloads a file streamingly into the cache directory, verifying its SHA-256 upon completion.
     * @param url Download URL
     * @param version Build version string (used in file naming)
     * @param expectedSha256 Expected SHA-256 hash
     * @param onProgress Progress update callback
     * @return Path to the verified downloaded file
     */
    suspend fun downloadBuild(
        url: String,
        version: String,
        expectedSha256: String,
        onProgress: (ProgressInfo) -> Unit = {}
    ): Result<Path> = withContext(Dispatchers.IO) {
        val cacheDir = OSUtils.getCacheDir()
        Files.createDirectories(cacheDir)

        val finalFile = cacheDir.resolve("build-$version.zip")
        val partFile = cacheDir.resolve("build-$version.zip.part")

        // If file already exists and hash matches, reuse it!
        if (Files.exists(finalFile) && HashVerifier.verifySha256(finalFile, expectedSha256)) {
            AppLogger.info("BuildDownloader", "Existing valid build archive found at $finalFile")
            return@withContext Result.success(finalFile)
        }

        Files.deleteIfExists(partFile)

        AppLogger.info("BuildDownloader", "Downloading build $version from $url")

        try {
            client.prepareGet(url).execute { response ->
                if (!response.status.isSuccess()) {
                    throw LauncherError.DownloadFailed(
                        "Сборка $version",
                        "HTTP ${response.status.value}: ${response.status.description}"
                    ).let { Exception(it.userMessage) }
                }

                val totalBytes = response.contentLength() ?: -1L
                val channel: ByteReadChannel = response.bodyAsChannel()

                FileOutputStream(partFile.toFile()).use { output ->
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
                        if (elapsed >= 500) {
                            currentSpeed = (bytesSinceLastTime * 1000) / elapsed
                            lastTime = now
                            bytesSinceLastTime = 0L

                            onProgress(
                                ProgressInfo(
                                    title = "Скачивание сборки $version",
                                    currentBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed
                                )
                            )
                        }
                    }

                    // Final progress report
                    onProgress(
                        ProgressInfo(
                            title = "Скачивание сборки $version завершено",
                            currentBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            speedBytesPerSec = 0L
                        )
                    )
                }
            }

            AppLogger.info("BuildDownloader", "Download finished. Verifying SHA-256...")
            if (!HashVerifier.verifySha256(partFile, expectedSha256)) {
                Files.deleteIfExists(partFile)
                val actual = try { org.snkt.partylauncher.util.FileUtils.computeSha256(partFile) } catch (e: Exception) { "unknown" }
                val error = LauncherError.HashMismatch(partFile.fileName.toString(), expectedSha256, actual)
                return@withContext Result.failure(Exception(error.userMessage))
            }

            // Move .part to finalFile
            Files.move(partFile, finalFile, StandardCopyOption.REPLACE_EXISTING)
            AppLogger.info("BuildDownloader", "Saved verified build archive to $finalFile")
            Result.success(finalFile)
        } catch (e: Exception) {
            Files.deleteIfExists(partFile)
            AppLogger.error("BuildDownloader", "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
