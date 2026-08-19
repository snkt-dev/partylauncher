package org.snkt.partylauncher.util

import java.nio.file.Path
import java.nio.file.Paths

enum class OSType(val mojangName: String) {
    WINDOWS("windows"),
    MACOS("osx"),
    LINUX("linux"),
    UNKNOWN("unknown")
}

enum class ArchType(val mojangName: String) {
    X64("x64"),
    ARM64("arm64"),
    X86("x86"),
    UNKNOWN("unknown")
}

object OSUtils {
    val currentOS: OSType by lazy {
        val os = System.getProperty("os.name", "").lowercase()
        when {
            os.contains("win") -> OSType.WINDOWS
            os.contains("mac") || os.contains("darwin") -> OSType.MACOS
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> OSType.LINUX
            else -> OSType.UNKNOWN
        }
    }

    val currentArch: ArchType by lazy {
        val arch = System.getProperty("os.arch", "").lowercase()
        when {
            arch.contains("aarch64") || arch.contains("arm64") -> ArchType.ARM64
            arch.contains("64") -> ArchType.X64
            arch.contains("86") || arch.contains("32") -> ArchType.X86
            else -> ArchType.UNKNOWN
        }
    }

    /**
     * Resolves the base data directory for the launcher depending on the OS or local directory.
     * Default: `./launcher-data` in project/app directory, keeping it self-contained.
     */
    fun getLauncherDataDir(): Path {
        val path = Paths.get("launcher-data").toAbsolutePath().normalize()
        return path
    }

    fun getInstancesDir(): Path = getLauncherDataDir().resolve("instances")
    fun getMinecraftCacheDir(): Path = getLauncherDataDir().resolve("minecraft")
    fun getVersionsDir(): Path = getMinecraftCacheDir().resolve("versions")
    fun getLibrariesDir(): Path = getMinecraftCacheDir().resolve("libraries")
    fun getAssetsDir(): Path = getMinecraftCacheDir().resolve("assets")
    fun getCacheDir(): Path = getLauncherDataDir().resolve("cache")
    fun getAuthDir(): Path = getLauncherDataDir().resolve("auth")
    fun getLogsDir(): Path = Paths.get("logs").toAbsolutePath().normalize()
}
