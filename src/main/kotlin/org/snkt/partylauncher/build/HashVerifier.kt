package org.snkt.partylauncher.build

import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.FileUtils
import java.nio.file.Files
import java.nio.file.Path

object HashVerifier {

    /**
     * Verifies if the SHA-256 hash of a file matches the expected hash.
     * Ignores case and whitespace.
     */
    fun verifySha256(file: Path, expectedHash: String): Boolean {
        if (!Files.exists(file)) {
            AppLogger.error("HashVerifier", "File does not exist for SHA-256 verification: $file")
            return false
        }
        return try {
            val actualHash = FileUtils.computeSha256(file).trim().lowercase()
            val expected = expectedHash.trim().lowercase()
            val matches = actualHash == expected

            if (matches) {
                AppLogger.info("HashVerifier", "SHA-256 verified successfully for ${file.fileName} ($actualHash)")
            } else {
                AppLogger.error(
                    "HashVerifier",
                    "SHA-256 verification FAILED for ${file.fileName}! Expected: $expected, Actual: $actualHash"
                )
            }
            matches
        } catch (e: Exception) {
            AppLogger.error("HashVerifier", "Error computing SHA-256 for $file: ${e.message}", e)
            false
        }
    }
}
