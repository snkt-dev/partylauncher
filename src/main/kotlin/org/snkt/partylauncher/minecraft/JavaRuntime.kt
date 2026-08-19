package org.snkt.partylauncher.minecraft

import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.OSType
import org.snkt.partylauncher.util.OSUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

interface JavaRuntimeProvider {
    suspend fun getRuntime(minecraftVersion: String, customPath: String? = null): Path
}

data class JavaInstallation(
    val executablePath: Path,
    val majorVersion: Int,
    val vendor: String = ""
)

object JavaRuntime : JavaRuntimeProvider {

    /**
     * Determines the minimum required Java major version based on the Minecraft version.
     */
    fun getRequiredJavaVersion(minecraftVersion: String): Int {
        val parts = minecraftVersion.split(".")
        if (parts.size >= 2) {
            val minor = parts[1].toIntOrNull() ?: 21
            val patch = if (parts.size >= 3) parts[2].toIntOrNull() ?: 0 else 0

            return when {
                minor >= 21 || (minor == 20 && patch >= 5) -> 21 // 1.20.5+ requires Java 21
                minor >= 18 -> 17                                // 1.18 - 1.20.4 requires Java 17
                minor >= 17 -> 16                                // 1.17 requires Java 16
                else -> 8                                        // <= 1.16.5 requires Java 8
            }
        }
        return 21
    }

    /**
     * Resolves a compatible Java runtime executable path.
     */
    override suspend fun getRuntime(minecraftVersion: String, customPath: String?): Path {
        val requiredVersion = getRequiredJavaVersion(minecraftVersion)
        AppLogger.info("JavaRuntime", "Resolving Java runtime for Minecraft $minecraftVersion (Requires Java $requiredVersion+)...")

        // 1. Check custom path from settings if provided
        if (!customPath.isNullOrBlank()) {
            val custom = Paths.get(customPath)
            val exec = if (Files.isDirectory(custom)) resolveExecutable(custom) else custom
            if (Files.exists(exec)) {
                val detected = inspectJavaExecutable(exec)
                if (detected != null && detected.majorVersion >= requiredVersion) {
                    AppLogger.info("JavaRuntime", "Using configured custom Java: $exec (Version ${detected.majorVersion})")
                    return exec
                }
            }
            AppLogger.warn("JavaRuntime", "Configured custom Java path invalid or incompatible: $customPath")
        }

        // 2. Discover available Java installations on the machine
        val candidates = discoverSystemJavaInstallations()
        val searched = candidates.map { it.executablePath.toString() }

        // Find best match (highest version >= requiredVersion)
        val matching = candidates
            .filter { it.majorVersion >= requiredVersion }
            .maxByOrNull { it.majorVersion }

        if (matching != null) {
            AppLogger.info("JavaRuntime", "Selected Java ${matching.majorVersion} at ${matching.executablePath}")
            return matching.executablePath
        }

        // 3. Fallback: try current JVM if running with adequate version
        val currentJvm = getCurrentJvmExecutable()
        if (currentJvm != null) {
            val detected = inspectJavaExecutable(currentJvm)
            if (detected != null && detected.majorVersion >= requiredVersion) {
                AppLogger.info("JavaRuntime", "Falling back to running JVM Java ${detected.majorVersion} at $currentJvm")
                return currentJvm
            }
        }

        throw LauncherError.JavaNotFound(requiredVersion, searched)
    }

    /**
     * Discovers installed Java environments on the current OS.
     */
    fun discoverSystemJavaInstallations(): List<JavaInstallation> {
        val list = mutableListOf<JavaInstallation>()

        // 1. Check java.home
        System.getProperty("java.home")?.let { home ->
            val exec = resolveExecutable(Paths.get(home))
            inspectJavaExecutable(exec)?.let { list.add(it) }
        }

        // 2. Check JAVA_HOME environment variable
        System.getenv("JAVA_HOME")?.let { home ->
            val exec = resolveExecutable(Paths.get(home))
            inspectJavaExecutable(exec)?.let { list.add(it) }
        }

        // 3. OS-specific directories
        val pathsToScan = when (OSUtils.currentOS) {
            OSType.MACOS -> listOf(
                Paths.get("/Library/Java/JavaVirtualMachines"),
                Paths.get(System.getProperty("user.home"), "Library/Java/JavaVirtualMachines"),
                Paths.get("/usr/local/opt/openjdk/bin"),
                Paths.get("/opt/homebrew/opt/openjdk/bin")
            )
            OSType.WINDOWS -> listOf(
                Paths.get("C:\\Program Files\\Java"),
                Paths.get("C:\\Program Files\\Eclipse Adoptium"),
                Paths.get("C:\\Program Files\\Microsoft"),
                Paths.get("C:\\Program Files\\BellSoft"),
                Paths.get(System.getenv("LOCALAPPDATA") ?: "", "Programs\\Eclipse Adoptium")
            )
            OSType.LINUX -> listOf(
                Paths.get("/usr/lib/jvm"),
                Paths.get("/usr/java"),
                Paths.get("/opt/java")
            )
            else -> emptyList()
        }

        for (dir in pathsToScan) {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try {
                    Files.list(dir).use { stream ->
                        stream.forEach { candidateDir ->
                            val exec = resolveExecutable(candidateDir)
                            inspectJavaExecutable(exec)?.let { list.add(it) }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore access errors
                }
            }
        }

        // Remove duplicates by executable path
        return list.distinctBy { it.executablePath.toAbsolutePath().normalize() }
    }

    private fun resolveExecutable(homeOrBin: Path): Path {
        val execName = if (OSUtils.currentOS == OSType.WINDOWS) "java.exe" else "java"

        // If it's already an executable file
        if (Files.isRegularFile(homeOrBin) && homeOrBin.fileName.toString().startsWith("java")) {
            return homeOrBin
        }

        // Case 1: path/bin/java
        val inBin = homeOrBin.resolve("bin").resolve(execName)
        if (Files.exists(inBin)) return inBin

        // Case 2: macOS JDK Home: path/Contents/Home/bin/java
        val inMacHome = homeOrBin.resolve("Contents/Home/bin").resolve(execName)
        if (Files.exists(inMacHome)) return inMacHome

        // Case 3: directly in directory
        val direct = homeOrBin.resolve(execName)
        if (Files.exists(direct)) return direct

        return inBin
    }

    private fun getCurrentJvmExecutable(): Path? {
        val home = System.getProperty("java.home") ?: return null
        val exec = resolveExecutable(Paths.get(home))
        return if (Files.exists(exec)) exec else null
    }

    fun inspectJavaExecutable(executable: Path): JavaInstallation? {
        if (!Files.exists(executable) || !Files.isExecutable(executable)) return null

        return try {
            val process = ProcessBuilder(executable.toString(), "-version")
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            process.waitFor()

            parseJavaVersion(output, executable)
        } catch (e: Exception) {
            null
        }
    }

    fun parseJavaVersion(versionOutput: String, executable: Path): JavaInstallation? {
        val pattern = Pattern.compile("version\\s+\"([0-9._]+)\"|openjdk\\s+([0-9._]+)|java\\s+([0-9._]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(versionOutput)

        if (matcher.find()) {
            val rawVersion = matcher.group(1) ?: matcher.group(2) ?: matcher.group(3) ?: return null
            val major = parseMajorVersion(rawVersion)
            return JavaInstallation(executable, major, versionOutput.lines().firstOrNull() ?: "")
        }

        return null
    }

    fun parseMajorVersion(versionString: String): Int {
        val clean = versionString.trim('"')
        return if (clean.startsWith("1.")) {
            clean.split(".")[1].toIntOrNull() ?: 8
        } else {
            clean.split(".")[0].toIntOrNull() ?: 8
        }
    }
}
