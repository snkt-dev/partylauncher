package org.snkt.partylauncher.build

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.core.ProgressInfo
import org.snkt.partylauncher.instance.InstanceConfig
import org.snkt.partylauncher.instance.InstanceManager
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.FileUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class BuildInstaller(
    private val instanceManager: InstanceManager = InstanceManager()
) {

    /**
     * Installs or updates a build into the specified instance from a verified ZIP archive.
     */
    suspend fun installBuild(
        zipFile: Path,
        manifest: BuildManifest,
        instanceId: String,
        onProgress: (ProgressInfo) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val instanceDir = instanceManager.ensureInstanceStructure(instanceId)
        val tempExtractDir = instanceDir.resolve(".update-temp")

        AppLogger.info("BuildInstaller", "Installing build ${manifest.version} into instance '$instanceId'")

        try {
            onProgress(ProgressInfo(title = "Распаковка сборки...", currentItem = 1, totalItems = 4))
            FileUtils.deleteDirectoryRecursively(tempExtractDir)
            Files.createDirectories(tempExtractDir)

            // Safe extract with Zip Slip protection
            FileUtils.safeExtractZip(zipFile, tempExtractDir)

            onProgress(ProgressInfo(title = "Обновление файлов модов и конфигурации...", currentItem = 2, totalItems = 4))

            // Directories commonly managed by the build that should be cleaned/replaced
            val managedDirs = listOf("mods", "shaderpacks")
            for (dirName in managedDirs) {
                val targetSubDir = instanceDir.resolve(dirName)
                val tempSubDir = tempExtractDir.resolve(dirName)
                if (Files.exists(tempSubDir)) {
                    FileUtils.deleteDirectoryRecursively(targetSubDir)
                    Files.createDirectories(targetSubDir)
                }
            }

            onProgress(ProgressInfo(title = "Копирование новых файлов...", currentItem = 3, totalItems = 4))

            // Copy extracted files into instance, respecting protected user files
            Files.walk(tempExtractDir).use { stream ->
                stream.forEach { sourcePath ->
                    val relativePath = tempExtractDir.relativize(sourcePath).toString()
                    if (relativePath.isNotEmpty() && !instanceManager.isProtectedUserPath(relativePath)) {
                        val targetPath = instanceDir.resolve(relativePath)
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(targetPath)
                        } else {
                            targetPath.parent?.let { Files.createDirectories(it) }
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
            }

            // Cleanup temp directory
            FileUtils.deleteDirectoryRecursively(tempExtractDir)

            onProgress(ProgressInfo(title = "Сохранение конфигурации инстанса...", currentItem = 4, totalItems = 4))

            // Save instance.json
            val newConfig = InstanceConfig(
                id = instanceId,
                buildVersion = manifest.version,
                minecraftVersion = manifest.minecraft,
                loader = manifest.loader,
                loaderVersion = manifest.loaderVersion
            )
            instanceManager.saveInstanceConfig(newConfig)

            AppLogger.info("BuildInstaller", "Build ${manifest.version} installed successfully in '$instanceId'")
            Result.success(Unit)
        } catch (e: Exception) {
            FileUtils.deleteDirectoryRecursively(tempExtractDir)
            AppLogger.error("BuildInstaller", "Failed to install build: ${e.message}", e)
            Result.failure(e)
        }
    }
}
