package org.snkt.partylauncher.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AccountStorageTest {

    @Test
    fun testSaveAndLoadAccountEncrypted(@TempDir tempDir: Path) {
        val storage = EncryptedFileAccountStorage(tempDir)

        val originalSession = MinecraftSession(
            username = "Steve",
            uuid = "12345678123412341234123456789abc",
            xuid = "2535400000000000",
            minecraftAccessToken = "secret_access_token_jwt",
            microsoftRefreshToken = "secret_refresh_token",
            expiresAtEpochMs = System.currentTimeMillis() + 3600000L,
            skinUrl = "http://textures.minecraft.net/texture/abc"
        )

        assertFalse(storage.hasAccount())
        storage.saveAccount(originalSession)

        assertTrue(storage.hasAccount())
        val loaded = storage.loadAccount()
        assertNotNull(loaded)
        assertEquals("Steve", loaded?.username)
        assertEquals("12345678123412341234123456789abc", loaded?.uuid)
        assertEquals("12345678-1234-1234-1234-123456789abc", loaded?.formattedUuid)
        assertEquals("secret_access_token_jwt", loaded?.minecraftAccessToken)
        assertEquals("secret_refresh_token", loaded?.microsoftRefreshToken)
        assertFalse(loaded?.isExpired() ?: true)

        // Test remove
        storage.removeAccount()
        assertFalse(storage.hasAccount())
        assertNull(storage.loadAccount())
    }
}
