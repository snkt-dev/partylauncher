package org.snkt.partylauncher.build

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.instance.InstanceManager
import org.snkt.partylauncher.util.FileUtils
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BuildInstallerTest {

    @Test
    fun testZipSlipProtectionThrowsError(@TempDir tempDir: Path) {
        val maliciousZip = tempDir.resolve("malicious.zip")
        val targetDir = tempDir.resolve("target")
        Files.createDirectories(targetDir)

        // Create zip with malicious traversal entry
        ZipOutputStream(FileOutputStream(maliciousZip.toFile())).use { zos ->
            val entry = ZipEntry("../../escaped_file.txt")
            zos.putNextEntry(entry)
            zos.write("malicious payload".toByteArray())
            zos.closeEntry()
        }

        var exceptionThrown = false
        try {
            FileUtils.safeExtractZip(maliciousZip, targetDir)
        } catch (e: LauncherError.InvalidBuild) {
            exceptionThrown = true
            assertTrue(e.technicalDetails?.contains("Path Traversal") == true || e.userMessage.contains("Path Traversal"))
        }

        assertTrue(exceptionThrown, "SafeExtractZip must block path traversal entries")
    }

    @Test
    fun testBuildInstallationPreservesUserFiles(@TempDir tempDir: Path) = runBlocking {
        val instancesDir = tempDir.resolve("instances")
        val instanceManager = InstanceManager(instancesDir)
        val installer = BuildInstaller(instanceManager)

        val instanceId = "test-instance"
        val instanceDir = instanceManager.ensureInstanceStructure(instanceId)

        // Existing user files
        val userSave = instanceDir.resolve("saves").resolve("World1").resolve("level.dat")
        Files.createDirectories(userSave.parent)
        Files.writeString(userSave, "user save data")

        val userOptions = instanceDir.resolve("options.txt")
        Files.writeString(userOptions, "fov:90.0")

        // Old mod that should be cleaned
        val oldMod = instanceDir.resolve("mods").resolve("old-mod-1.0.jar")
        Files.writeString(oldMod, "old mod")

        // Create new build zip
        val buildZip = tempDir.resolve("build.zip")
        ZipOutputStream(FileOutputStream(buildZip.toFile())).use { zos ->
            // New mod
            zos.putNextEntry(ZipEntry("mods/new-mod-2.0.jar"))
            zos.write("new mod jar".toByteArray())
            zos.closeEntry()

            // Config file
            zos.putNextEntry(ZipEntry("config/mod-config.json"))
            zos.write("{\"key\":\"value\"}".toByteArray())
            zos.closeEntry()
        }

        val manifest = BuildManifest(
            version = "2.0.0",
            minecraft = "1.21.4",
            loader = "fabric",
            loaderVersion = "0.16.9",
            file = "build.zip",
            sha256 = "dummy"
        )

        val result = installer.installBuild(buildZip, manifest, instanceId)
        assertTrue(result.isSuccess)

        // Verify new mod installed
        assertTrue(Files.exists(instanceDir.resolve("mods").resolve("new-mod-2.0.jar")))
        assertTrue(Files.exists(instanceDir.resolve("config").resolve("mod-config.json")))

        // Verify old mod removed during clean install
        assertFalse(Files.exists(oldMod))

        // Verify user files preserved
        assertTrue(Files.exists(userSave))
        assertEquals("user save data", Files.readString(userSave))
        assertTrue(Files.exists(userOptions))
        assertEquals("fov:90.0", Files.readString(userOptions))

        // Verify instance.json updated
        val config = instanceManager.loadInstanceConfig(instanceId)
        assertEquals("2.0.0", config?.buildVersion)
        assertEquals("1.21.4", config?.minecraftVersion)
    }
}
