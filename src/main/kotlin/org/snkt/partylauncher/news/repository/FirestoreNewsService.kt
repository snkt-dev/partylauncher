package org.snkt.partylauncher.news.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.news.model.NewsItem
import org.snkt.partylauncher.util.OSUtils
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class FirestoreNewsService(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 20000
            requestTimeoutMillis = 20000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "PartyLauncher/1.0")
        }
    }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Fetches news from Firebase Firestore REST API or custom URL.
     * Caches news locally in cache directory for offline resilience.
     */
    suspend fun fetchNews(
        projectId: String,
        collection: String = "news",
        customUrl: String? = null
    ): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        val cacheFile = getCachePath()

        val endpointUrl = if (!customUrl.isNullOrBlank()) {
            customUrl
        } else {
            "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?pageSize=50"
        }

        try {
            AppLogger.info("FirestoreNewsService", "Fetching news from $endpointUrl")
            val response = httpClient.get(endpointUrl)

            if (!response.status.isSuccess()) {
                AppLogger.warn("FirestoreNewsService", "Firestore returned HTTP ${response.status.value}, falling back to cache...")
                val cached = loadFromCache(cacheFile)
                if (cached.isNotEmpty()) {
                    return@withContext Result.success(cached)
                }
                return@withContext Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
            }

            val bodyText = response.bodyAsText()
            val parsedList = parseNewsResponse(bodyText)

            if (parsedList.isNotEmpty()) {
                saveToCache(cacheFile, parsedList)
                AppLogger.info("FirestoreNewsService", "Successfully fetched ${parsedList.size} news items from Firestore")
            }

            Result.success(parsedList)
        } catch (e: Exception) {
            AppLogger.warn("FirestoreNewsService", "Failed to fetch news (${e.message}), attempting cache load...")
            val cached = loadFromCache(cacheFile)
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Parses Firestore REST API response or flat JSON list.
     */
    fun parseNewsResponse(jsonText: String): List<NewsItem> {
        val news = mutableListOf<NewsItem>()

        try {
            val rootElement = json.parseToJsonElement(jsonText)

            if (rootElement is JsonObject) {
                // Check if it's Firestore document list
                val documents = rootElement["documents"]?.let {
                    if (it is JsonArray) it.jsonArray else null
                }

                if (documents != null) {
                    for (doc in documents) {
                        if (doc is JsonObject) {
                            parseFirestoreDocument(doc)?.let { news.add(it) }
                        }
                    }
                } else if (rootElement.containsKey("news") && rootElement["news"] is JsonArray) {
                    for (item in rootElement["news"]!!.jsonArray) {
                        if (item is JsonObject) {
                            parseFlatOrFirestoreObject(item)?.let { news.add(it) }
                        }
                    }
                } else if (rootElement.containsKey("fields")) {
                    // Single Firestore document
                    parseFirestoreDocument(rootElement)?.let { news.add(it) }
                }
            } else if (rootElement is JsonArray) {
                // Flat JSON Array
                for (item in rootElement) {
                    if (item is JsonObject) {
                        parseFlatOrFirestoreObject(item)?.let { news.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.error("FirestoreNewsService", "Failed to parse news JSON: ${e.message}", e)
        }

        // Sort descending by timestamp (newest first)
        return news.sortedByDescending { it.timestampEpochMs }
    }

    private fun parseFirestoreDocument(docObj: JsonObject): NewsItem? {
        val docName = docObj["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val docId = docName.substringAfterLast('/', "news_${System.currentTimeMillis()}")
        val fields = docObj["fields"]?.jsonObject ?: return null

        val title = extractFirestoreString(fields, "title") ?: "Новость"
        val text = extractFirestoreString(fields, "text", "description", "content", "body") ?: ""
        val imageUrl = extractFirestoreString(fields, "image_url", "imageUrl", "image", "img") ?: ""
        val sourceUrl = extractFirestoreString(fields, "source_url", "sourceUrl", "url", "link") ?: ""

        val timestamp = extractFirestoreTimestamp(fields, docObj)

        return NewsItem(
            id = docId,
            title = title,
            text = text,
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            timestampEpochMs = timestamp
        )
    }

    private fun parseFlatOrFirestoreObject(obj: JsonObject): NewsItem? {
        if (obj.containsKey("fields")) {
            return parseFirestoreDocument(obj)
        }

        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "news_${System.currentTimeMillis()}"
        val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Новость"
        val text = obj["text"]?.jsonPrimitive?.contentOrNull
            ?: obj["description"]?.jsonPrimitive?.contentOrNull
            ?: obj["content"]?.jsonPrimitive?.contentOrNull
            ?: ""
        val imageUrl = obj["image_url"]?.jsonPrimitive?.contentOrNull
            ?: obj["imageUrl"]?.jsonPrimitive?.contentOrNull
            ?: obj["image"]?.jsonPrimitive?.contentOrNull
            ?: ""
        val sourceUrl = obj["source_url"]?.jsonPrimitive?.contentOrNull
            ?: obj["sourceUrl"]?.jsonPrimitive?.contentOrNull
            ?: obj["url"]?.jsonPrimitive?.contentOrNull
            ?: obj["link"]?.jsonPrimitive?.contentOrNull
            ?: ""

        val timestamp = obj["timestamp"]?.let { parseTimestampElement(it) }
            ?: obj["createdAt"]?.let { parseTimestampElement(it) }
            ?: System.currentTimeMillis()

        return NewsItem(
            id = id,
            title = title,
            text = text,
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            timestampEpochMs = timestamp
        )
    }

    private fun extractFirestoreString(fields: JsonObject, vararg keys: String): String? {
        for (k in keys) {
            val fieldVal = fields[k]
            if (fieldVal is JsonObject) {
                val str = fieldVal["stringValue"]?.jsonPrimitive?.contentOrNull
                if (!str.isNullOrBlank()) return str
            } else if (fieldVal != null) {
                val str = fieldVal.jsonPrimitive.contentOrNull
                if (!str.isNullOrBlank()) return str
            }
        }
        return null
    }

    private fun extractFirestoreTimestamp(fields: JsonObject, parentDoc: JsonObject): Long {
        val tsField = fields["timestamp"] ?: fields["createdAt"] ?: fields["date"]
        if (tsField is JsonObject) {
            val tsVal = tsField["timestampValue"]?.jsonPrimitive?.contentOrNull
            if (!tsVal.isNullOrBlank()) {
                try {
                    return Instant.parse(tsVal).toEpochMilli()
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val intVal = tsField["integerValue"]?.jsonPrimitive?.longOrNull
            if (intVal != null) {
                // If seconds vs ms
                return if (intVal < 100_000_000_000L) intVal * 1000L else intVal
            }
        }

        // Try parent createTime (standard Firestore ISO timestamp)
        val createTimeStr = parentDoc["createTime"]?.jsonPrimitive?.contentOrNull
        if (!createTimeStr.isNullOrBlank()) {
            try {
                return Instant.parse(createTimeStr).toEpochMilli()
            } catch (e: Exception) {
                // Ignore
            }
        }

        return System.currentTimeMillis()
    }

    private fun parseTimestampElement(element: JsonElement): Long {
        val primitive = element.jsonPrimitive
        val longVal = primitive.longOrNull
        if (longVal != null) {
            return if (longVal < 100_000_000_000L) longVal * 1000L else longVal
        }
        val strVal = primitive.contentOrNull
        if (!strVal.isNullOrBlank()) {
            try {
                return Instant.parse(strVal).toEpochMilli()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return System.currentTimeMillis()
    }

    private fun getCachePath(): Path {
        val cacheDir = OSUtils.getCacheDir()
        Files.createDirectories(cacheDir)
        return cacheDir.resolve("news_cache.json")
    }

    private fun saveToCache(path: Path, items: List<NewsItem>) {
        try {
            val content = json.encodeToString(items)
            Files.writeString(path, content)
        } catch (e: Exception) {
            AppLogger.warn("FirestoreNewsService", "Could not write news cache: ${e.message}")
        }
    }

    private fun loadFromCache(path: Path): List<NewsItem> {
        return try {
            if (Files.exists(path)) {
                val content = Files.readString(path)
                json.decodeFromString<List<NewsItem>>(content)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            AppLogger.warn("FirestoreNewsService", "Could not read news cache: ${e.message}")
            emptyList()
        }
    }
}
