package org.snkt.partylauncher.minecraft

import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.snkt.partylauncher.util.OSUtils
import java.nio.file.Files
import java.nio.file.Path

class NeoForgeServiceTest {

    @Test
    fun testParseExistingNeoForgeVersionJson(@TempDir tempDir: Path) = runBlocking {
        val service = NeoForgeService(HttpClient())

        val versionId = "neoforge-21.1.248"
        val versionsDir = OSUtils.getVersionsDir().resolve(versionId)
        Files.createDirectories(versionsDir)
        val jsonFile = versionsDir.resolve("$versionId.json")

        val jsonContent = """
        {
          "id": "neoforge-21.1.248",
          "mainClass": "cpw.mods.bootstraplauncher.BootstrapLauncher",
          "arguments": {
            "game": [
              "--fml.neoForgeVersion",
              "21.1.248",
              "--launchTarget",
              "forgeclient"
            ],
            "jvm": [
              "-DlibraryDirectory=${'$'}{library_directory}",
              "-p",
              "${'$'}{library_directory}/bootstraplauncher.jar",
              "--add-modules",
              "ALL-MODULE-PATH"
            ]
          },
          "libraries": [
            {
              "name": "net.neoforged.fancymodloader:loader:4.0.43",
              "downloads": {
                "artifact": {
                  "sha1": "fb10b7bf2f568a9676ad8b426b19c23badbbd98a",
                  "size": 505633,
                  "url": "https://maven.neoforged.net/releases/net/neoforged/fancymodloader/loader/4.0.43/loader-4.0.43.jar",
                  "path": "net/neoforged/fancymodloader/loader/4.0.43/loader-4.0.43.jar"
                }
              }
            }
          ]
        }
        """.trimIndent()

        Files.writeString(jsonFile, jsonContent)

        val result = service.prepareNeoForge(
            minecraftVersion = "1.21.1",
            loaderVersion = "21.1.248",
            javaExecutable = tempDir.resolve("dummy_java"),
            onProgress = {}
        )

        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()

        assertEquals("cpw.mods.bootstraplauncher.BootstrapLauncher", profile.mainClass)
        assertTrue(profile.jvmArgs.contains("--add-modules"))
        assertTrue(profile.jvmArgs.contains("ALL-MODULE-PATH"))
        assertTrue(profile.gameArgs.contains("--fml.neoForgeVersion"))
        assertTrue(profile.gameArgs.contains("21.1.248"))
        assertTrue(profile.gameArgs.contains("forgeclient"))
        assertEquals(1, profile.libraries.size)
        assertEquals("net.neoforged.fancymodloader:loader:4.0.43", profile.libraries[0].name)
    }
}
