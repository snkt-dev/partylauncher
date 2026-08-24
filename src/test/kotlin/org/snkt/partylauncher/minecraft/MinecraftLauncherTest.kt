package org.snkt.partylauncher.minecraft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.snkt.partylauncher.auth.MinecraftSession
import org.snkt.partylauncher.config.LauncherConfig
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.minecraft.models.MojangVersionDetails
import java.nio.file.Path
import java.nio.file.Paths

class MinecraftLauncherTest {

    @Test
    fun testBuildLaunchCommandContainsAllArguments(@TempDir tempDir: Path) {
        val launcher = MinecraftLauncher()
        val javaExec = Paths.get("/usr/bin/java")
        val clientJar = tempDir.resolve("1.21.4.jar")
        val lib1 = tempDir.resolve("lib1.jar")
        val lib2 = tempDir.resolve("lib2.jar")

        val installation = MinecraftInstallationResult(
            minecraftVersion = "1.21.4",
            clientJar = clientJar,
            libraryJars = listOf(lib1, lib2),
            mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient",
            assetIndexId = "1.21.4",
            versionDetails = MojangVersionDetails(id = "1.21.4")
        )

        val session = MinecraftSession(
            username = "Alex",
            uuid = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            xuid = "1234567890",
            minecraftAccessToken = "secret_mc_token_value",
            expiresAtEpochMs = System.currentTimeMillis() + 3600000L
        )

        val config = LauncherConfig(
            minMemoryMb = 2048,
            maxMemoryMb = 6144
        )

        val command = launcher.buildLaunchCommand(
            javaExecutable = javaExec,
            installation = installation,
            session = session,
            instanceDir = tempDir,
            config = config
        )

        // Check java executable
        assertEquals(javaExec.toAbsolutePath().toString(), command[0])

        // Check memory flags
        assertTrue(command.contains("-Xms2048M"))
        assertTrue(command.contains("-Xmx6144M"))

        // Check main class
        assertTrue(command.contains("net.fabricmc.loader.impl.launch.knot.KnotClient"))

        // Check game args
        val usernameIndex = command.indexOf("--username")
        assertTrue(usernameIndex != -1)
        assertEquals("Alex", command[usernameIndex + 1])

        val versionIndex = command.indexOf("--version")
        assertTrue(versionIndex != -1)
        assertEquals("1.21.4", command[versionIndex + 1])

        val uuidIndex = command.indexOf("--uuid")
        assertTrue(uuidIndex != -1)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", command[uuidIndex + 1])

        val tokenIndex = command.indexOf("--accessToken")
        assertTrue(tokenIndex != -1)
        assertEquals("secret_mc_token_value", command[tokenIndex + 1])

        val xuidIndex = command.indexOf("--xuid")
        assertTrue(xuidIndex != -1)
        assertEquals("1234567890", command[xuidIndex + 1])
    }

    @Test
    fun testNeoForgeLaunchCommandWithJvmAndGameArgs(@TempDir tempDir: Path) {
        val launcher = MinecraftLauncher()
        val javaExec = Paths.get("/usr/bin/java")
        val clientJar = tempDir.resolve("1.21.1.jar")

        val installation = MinecraftInstallationResult(
            minecraftVersion = "1.21.1",
            clientJar = clientJar,
            libraryJars = emptyList(),
            mainClass = "cpw.mods.bootstraplauncher.BootstrapLauncher",
            assetIndexId = "1.21.1",
            versionDetails = MojangVersionDetails(id = "1.21.1"),
            extraJvmArgs = listOf(
                "-DlibraryDirectory=\${library_directory}",
                "-p",
                "\${library_directory}/bootstraplauncher.jar\${classpath_separator}\${library_directory}/securejar.jar",
                "--add-modules",
                "ALL-MODULE-PATH"
            ),
            extraGameArgs = listOf(
                "--fml.neoForgeVersion",
                "21.1.248",
                "--launchTarget",
                "forgeclient"
            )
        )

        val session = MinecraftSession(
            username = "Steve",
            uuid = "11111111-2222-3333-4444-555555555555",
            minecraftAccessToken = "dummy_token",
            expiresAtEpochMs = System.currentTimeMillis() + 3600000L
        )

        val config = LauncherConfig(minMemoryMb = 4096, maxMemoryMb = 8192)
        val command = launcher.buildLaunchCommand(javaExec, installation, session, tempDir, config)

        // Check main class is NeoForge BootstrapLauncher
        assertTrue(command.contains("cpw.mods.bootstraplauncher.BootstrapLauncher"))

        // Check NeoForge extra JVM args
        assertTrue(command.contains("--add-modules"))
        assertTrue(command.contains("ALL-MODULE-PATH"))
        assertTrue(command.any { it.startsWith("-DlibraryDirectory=") })

        // Check NeoForge extra Game args
        assertTrue(command.contains("--fml.neoForgeVersion"))
        assertTrue(command.contains("21.1.248"))
        assertTrue(command.contains("--launchTarget"))
        assertTrue(command.contains("forgeclient"))
    }

    @Test
    fun testSensitiveDataMaskingInLogs() {
        val rawCommand = "--username Alex --accessToken eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeakThis --xuid 2535400000000000"
        val masked = AppLogger.maskSensitiveData(rawCommand)

        assertTrue(!masked.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertTrue(!masked.contains("2535400000000000"))
        assertTrue(masked.contains("***REDACTED***"))
        assertTrue(masked.contains("Alex"))
    }
}
