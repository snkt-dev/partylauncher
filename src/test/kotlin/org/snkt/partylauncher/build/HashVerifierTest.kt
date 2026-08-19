package org.snkt.partylauncher.build

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.snkt.partylauncher.util.FileUtils
import java.nio.file.Files
import java.nio.file.Path

class HashVerifierTest {

    @Test
    fun testSha256VerificationMatches(@TempDir tempDir: Path) {
        val testFile = tempDir.resolve("test.txt")
        Files.writeString(testFile, "Hello Minecraft!")

        // SHA-256 of "Hello Minecraft!" is "4004da773950ef377df48fe75fe249fbfad7df69062024b423dc2a5bbca7d2d3"
        val expectedSha = FileUtils.computeSha256(testFile)

        assertTrue(HashVerifier.verifySha256(testFile, expectedSha))
        assertTrue(HashVerifier.verifySha256(testFile, expectedSha.uppercase()))
    }

    @Test
    fun testSha256VerificationFailsOnMismatch(@TempDir tempDir: Path) {
        val testFile = tempDir.resolve("corrupted.txt")
        Files.writeString(testFile, "Corrupted content")

        val wrongSha = "0000000000000000000000000000000000000000000000000000000000000000"
        assertFalse(HashVerifier.verifySha256(testFile, wrongSha))
    }

    @Test
    fun testNonExistentFileReturnsFalse(@TempDir tempDir: Path) {
        val nonExistent = tempDir.resolve("does_not_exist.txt")
        assertFalse(HashVerifier.verifySha256(nonExistent, "dummy"))
    }
}
