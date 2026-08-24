package org.snkt.partylauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.ui.theme.BackgroundDark
import org.snkt.partylauncher.ui.theme.PrimaryGreen
import org.snkt.partylauncher.ui.theme.TextMuted
import org.snkt.partylauncher.util.OSUtils
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

private object ImageCacheManager {
    val memoryCache = ConcurrentHashMap<String, ImageBitmap>()

    private val httpClient = HttpClient(CIO) {
        followRedirects = true
        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 20000
            requestTimeoutMillis = 20000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            header(HttpHeaders.Accept, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
        }
    }

    private fun decodeImageBytes(bytes: ByteArray): ImageBitmap? {
        // 1. Decode via Skia (supports WebP, PNG, JPEG, GIF, BMP)
        try {
            val skiaImage = SkiaImage.makeFromEncoded(bytes)
            return skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            // fallback to ImageIO
        }

        // 2. Decode via ImageIO
        try {
            val awtImage = ImageIO.read(ByteArrayInputStream(bytes))
            if (awtImage != null) {
                return awtImage.toComposeImageBitmap()
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    suspend fun loadImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null

        // 1. Check memory cache
        memoryCache[url]?.let { return@withContext it }

        // 2. Check disk cache
        val diskCacheDir = OSUtils.getCacheDir().resolve("img_cache")
        Files.createDirectories(diskCacheDir)
        val urlHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val diskFile = diskCacheDir.resolve("$urlHash.img")

        if (Files.exists(diskFile) && Files.size(diskFile) > 0L) {
            try {
                val bytes = Files.readAllBytes(diskFile)
                val bitmap = decodeImageBytes(bytes)
                if (bitmap != null) {
                    memoryCache[url] = bitmap
                    return@withContext bitmap
                } else {
                    Files.deleteIfExists(diskFile)
                }
            } catch (e: Exception) {
                Files.deleteIfExists(diskFile)
            }
        }

        // 3. Download from network
        try {
            AppLogger.info("AsyncImage", "Loading image: $url")
            val response = httpClient.get(url)
            if (response.status.isSuccess()) {
                val bytes = response.bodyAsBytes()
                val bitmap = decodeImageBytes(bytes)
                if (bitmap != null) {
                    Files.write(diskFile, bytes)
                    memoryCache[url] = bitmap
                    AppLogger.info("AsyncImage", "Successfully loaded & cached image (${bytes.size} bytes): $url")
                    return@withContext bitmap
                } else {
                    AppLogger.warn("AsyncImage", "Could not decode image format for $url (${bytes.size} bytes)")
                }
            } else {
                AppLogger.warn("AsyncImage", "HTTP error ${response.status.value} for image $url")
            }
        } catch (e: Exception) {
            AppLogger.warn("AsyncImage", "Network error loading image $url: ${e.message}")
        }

        null
    }
}

@Composable
fun AsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(ImageCacheManager.memoryCache[url]) }
    var isLoading by remember(url) { mutableStateOf(bitmap == null && url.isNotBlank()) }
    var isError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.isBlank()) {
            isLoading = false
            isError = true
            return@LaunchedEffect
        }

        if (bitmap == null) {
            isLoading = true
            isError = false
            val loaded = ImageCacheManager.loadImage(url)
            bitmap = loaded
            isLoading = false
            isError = (loaded == null)
        }
    }

    Box(
        modifier = modifier.background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    painter = BitmapPainter(bitmap!!),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = PrimaryGreen.copy(alpha = 0.7f)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = contentDescription,
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
