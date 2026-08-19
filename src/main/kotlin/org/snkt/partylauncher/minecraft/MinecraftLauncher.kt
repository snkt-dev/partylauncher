package org.snkt.partylauncher.minecraft

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.snkt.partylauncher.auth.MinecraftSession
import org.snkt.partylauncher.config.LauncherConfig
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.instance.InstanceManager
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.OSType
import org.snkt.partylauncher.util.OSUtils
import org.snkt.partylauncher.util.ProcessUtils
import java.io.File
import java.nio.file.Path

class MinecraftLauncher(
    private val javaRuntimeProvider: JavaRuntimeProvider = JavaRuntime,
    private val instanceManager: InstanceManager = InstanceManager()
) {

    /**
     * Constructs the command arguments list for launching Minecraft.
     */
    fun buildLaunchCommand(
        javaExecutable: Path,
        installation: MinecraftInstallationResult,
        session: MinecraftSession,
        instanceDir: Path,
        config: LauncherConfig
    ): List<String> {
        val command = mutableListOf<String>()

        // 1. Java Executable
        command.add(javaExecutable.toAbsolutePath().toString())

        // 2. JVM Memory & GC Arguments
        command.add("-Xms${config.minMemoryMb}M")
        command.add("-Xmx${config.maxMemoryMb}M")
        command.add("-XX:+UnlockExperimentalVMOptions")
        command.add("-XX:+UseG1GC")
        command.add("-XX:G1NewSizePercent=20")
        command.add("-XX:G1ReservePercent=20")
        command.add("-XX:MaxGCPauseMillis=50")
        command.add("-XX:G1HeapRegionSize=32M")

        // 3. System Properties & Launcher Branding
        command.add("-Dminecraft.launcher.brand=PartyLauncher")
        command.add("-Dminecraft.launcher.version=1.0.0")

        if (OSUtils.currentOS == OSType.MACOS) {
            command.add("-XstartOnFirstThread")
            command.add("-Dapple.awt.application.name=Minecraft")
        }

        // 4. Classpath
        val classpathEntries = mutableListOf<String>()
        installation.libraryJars.forEach {
            classpathEntries.add(it.toAbsolutePath().toString())
        }
        classpathEntries.add(installation.clientJar.toAbsolutePath().toString())

        val classpathString = classpathEntries.joinToString(File.pathSeparator)
        command.add("-cp")
        command.add(classpathString)

        // 5. Main Class
        command.add(installation.mainClass)

        // 6. Game Arguments
        command.add("--username")
        command.add(session.username)

        command.add("--version")
        command.add(installation.minecraftVersion)

        command.add("--gameDir")
        command.add(instanceDir.toAbsolutePath().toString())

        command.add("--assetsDir")
        command.add(OSUtils.getAssetsDir().toAbsolutePath().toString())

        command.add("--assetIndex")
        command.add(installation.assetIndexId)

        command.add("--uuid")
        command.add(session.formattedUuid)

        command.add("--accessToken")
        command.add(session.minecraftAccessToken)

        command.add("--userType")
        command.add("msa")

        command.add("--versionType")
        command.add("PartyLauncher")

        session.xuid?.let {
            if (it.isNotBlank()) {
                command.add("--xuid")
                command.add(it)
            }
        }

        return command
    }

    /**
     * Launches the Minecraft client process and attaches logging.
     */
    suspend fun launch(
        installation: MinecraftInstallationResult,
        session: MinecraftSession,
        instanceId: String,
        config: LauncherConfig,
        scope: CoroutineScope,
        onProcessExit: (exitCode: Int) -> Unit = {}
    ): Result<Process> = withContext(Dispatchers.IO) {
        val instanceDir = instanceManager.getInstanceDir(instanceId)
        AppLogger.info("MinecraftLauncher", "Preparing to launch Minecraft for instance '$instanceId' in $instanceDir")

        try {
            val javaExecutable = javaRuntimeProvider.getRuntime(installation.minecraftVersion, config.customJavaPath)
            val command = buildLaunchCommand(javaExecutable, installation, session, instanceDir, config)

            val safeCommandLine = command.joinToString(" ") { arg ->
                if (arg.contains(" ") || arg.contains(";")) "\"$arg\"" else arg
            }
            AppLogger.info("MinecraftLauncher", "Starting process: $safeCommandLine")

            val processBuilder = ProcessBuilder(command)
                .directory(instanceDir.toFile())

            val process = processBuilder.start()
            AppLogger.info("MinecraftLauncher", "Minecraft process spawned successfully (PID: ${process.pid()})")

            // Attach asynchronous stream loggers
            ProcessUtils.attachProcessLogger(process, "Minecraft", scope)

            // Monitor process exit in background
            scope.launch(Dispatchers.IO) {
                val exitCode = process.waitFor()
                AppLogger.info("MinecraftLauncher", "Minecraft process exited with code $exitCode")
                onProcessExit(exitCode)
            }

            Result.success(process)
        } catch (e: Exception) {
            AppLogger.error("MinecraftLauncher", "Failed to start Minecraft process: ${e.message}", e)
            Result.failure(LauncherError.MinecraftLaunchFailed(null, e.message, e))
        }
    }
}
