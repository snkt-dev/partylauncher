package org.snkt.partylauncher.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.OSUtils
import java.nio.file.Files
import java.nio.file.Path

class ConfigStorage(
    private val configFile: Path = OSUtils.getLauncherDataDir().resolve("config.json")
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadConfig(): LauncherConfig {
        return try {
            if (Files.exists(configFile)) {
                val content = Files.readString(configFile)
                json.decodeFromString<LauncherConfig>(content)
            } else {
                val defaultConfig = LauncherConfig()
                saveConfig(defaultConfig)
                defaultConfig
            }
        } catch (e: Exception) {
            AppLogger.warn("ConfigStorage", "Failed to load config, returning default: ${e.message}")
            LauncherConfig()
        }
    }

    fun saveConfig(config: LauncherConfig) {
        try {
            configFile.parent?.let { Files.createDirectories(it) }
            val content = json.encodeToString(config)
            Files.writeString(configFile, content)
            AppLogger.info("ConfigStorage", "Saved launcher configuration to $configFile")
        } catch (e: Exception) {
            AppLogger.error("ConfigStorage", "Failed to save configuration: ${e.message}", e)
        }
    }
}
