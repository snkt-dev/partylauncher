package org.snkt.partylauncher.instance

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.OSUtils
import java.nio.file.Files
import java.nio.file.Path

class InstanceManager(
    private val instancesDir: Path = OSUtils.getInstancesDir()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Returns the path to the instance directory.
     */
    fun getInstanceDir(instanceId: String): Path {
        return instancesDir.resolve(instanceId).toAbsolutePath().normalize()
    }

    /**
     * Initializes the instance directory structure.
     */
    fun ensureInstanceStructure(instanceId: String): Path {
        val dir = getInstanceDir(instanceId)
        Files.createDirectories(dir)
        Files.createDirectories(dir.resolve("mods"))
        Files.createDirectories(dir.resolve("config"))
        Files.createDirectories(dir.resolve("resourcepacks"))
        Files.createDirectories(dir.resolve("shaderpacks"))
        Files.createDirectories(dir.resolve("saves"))
        Files.createDirectories(dir.resolve("screenshots"))
        return dir
    }

    /**
     * Loads the instance configuration from instance.json if it exists.
     */
    fun loadInstanceConfig(instanceId: String): InstanceConfig? {
        val configFile = getInstanceDir(instanceId).resolve("instance.json")
        if (!Files.exists(configFile)) return null
        return try {
            val content = Files.readString(configFile)
            json.decodeFromString<InstanceConfig>(content)
        } catch (e: Exception) {
            AppLogger.warn("InstanceManager", "Failed to read instance.json: ${e.message}")
            null
        }
    }

    /**
     * Saves the instance configuration to instance.json.
     */
    fun saveInstanceConfig(config: InstanceConfig) {
        val dir = ensureInstanceStructure(config.id)
        val configFile = dir.resolve("instance.json")
        val content = json.encodeToString(config)
        Files.writeString(configFile, content)
        AppLogger.info("InstanceManager", "Saved instance config for '${config.id}' (build ${config.buildVersion})")
    }

    /**
     * Protected user file names and directories that must not be deleted or overwritten during updates.
     */
    fun isProtectedUserPath(relativePath: String): Boolean {
        val normalized = relativePath.replace('\\', '/').trimStart('/')
        val protectedPrefixes = listOf(
            "saves",
            "screenshots",
            "logs",
            "crash-reports",
            "options.txt",
            "optionsof.txt",
            "servers.dat",
            "hotbar.nbt"
        )
        return protectedPrefixes.any { normalized == it || normalized.startsWith("$it/") }
    }
}
