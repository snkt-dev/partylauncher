package org.snkt.partylauncher.util

import org.snkt.partylauncher.core.LauncherError
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipFile

object FileUtils {

    /**
     * Computes the SHA-256 hash of a file using streaming reads.
     */
    fun computeSha256(file: Path): String {
        if (!Files.exists(file)) {
            throw IllegalArgumentException("File does not exist: $file")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(65536)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes SHA-1 hash of a file or stream.
     */
    fun computeSha1(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val buffer = ByteArray(65536)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun computeSha1(file: Path): String {
        if (!Files.exists(file)) {
            throw IllegalArgumentException("File does not exist: $file")
        }
        return Files.newInputStream(file).use { computeSha1(it) }
    }

    /**
     * Safely extracts a ZIP archive into the target directory with strict Zip Slip protection.
     */
    fun safeExtractZip(zipFile: Path, targetDir: Path) {
        val normalizedTarget = targetDir.toAbsolutePath().normalize()
        Files.createDirectories(normalizedTarget)

        ZipFile(zipFile.toFile()).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryTarget = normalizedTarget.resolve(entry.name).normalize()

                // Crucial Zip Slip protection: Ensure entry target path is inside targetDir
                if (!entryTarget.startsWith(normalizedTarget)) {
                    throw LauncherError.InvalidBuild(
                        "Обнаружена попытка Path Traversal в архиве!",
                        "Zip entry '${entry.name}' resolves outside target directory: $entryTarget"
                    )
                }

                if (entry.isDirectory) {
                    Files.createDirectories(entryTarget)
                } else {
                    entryTarget.parent?.let { Files.createDirectories(it) }
                    zip.getInputStream(entry).use { input ->
                        Files.copy(input, entryTarget, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    fun deleteDirectoryRecursively(directory: Path) {
        if (!Files.exists(directory)) return

        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach { path ->
                try {
                    Files.deleteIfExists(path)
                } catch (e: Exception) {
                    // Best effort delete
                }
            }
    }

    /**
     * Recursively copies a directory tree into a target directory.
     */
    fun copyDirectoryRecursively(source: Path, target: Path) {
        if (!Files.exists(source)) return
        val normalizedSource = source.toAbsolutePath().normalize()
        val normalizedTarget = target.toAbsolutePath().normalize()

        Files.walk(normalizedSource).use { stream ->
            stream.forEach { sourcePath ->
                val relativePath = normalizedSource.relativize(sourcePath)
                val targetPath = normalizedTarget.resolve(relativePath)
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath)
                } else {
                    targetPath.parent?.let { Files.createDirectories(it) }
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
