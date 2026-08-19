package org.snkt.partylauncher.auth

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.util.OSUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

interface AccountStorage {
    fun saveAccount(session: MinecraftSession)
    fun loadAccount(): MinecraftSession?
    fun removeAccount()
    fun hasAccount(): Boolean
}

class EncryptedFileAccountStorage(
    private val authDir: Path = OSUtils.getAuthDir()
) : AccountStorage {

    private val sessionFile = authDir.resolve("session.enc")
    private val keySaltFile = authDir.resolve(".key_salt")
    private val json = Json { ignoreUnknownKeys = true }

    private val gcmTagLength = 128
    private val ivLength = 12

    private fun getSecretKey(): SecretKey {
        Files.createDirectories(authDir)
        val salt = if (Files.exists(keySaltFile)) {
            Files.readAllBytes(keySaltFile)
        } else {
            val randomSalt = ByteArray(32)
            SecureRandom().nextBytes(randomSalt)
            Files.write(keySaltFile, randomSalt)
            randomSalt
        }

        val userSecret = (System.getProperty("user.name") + ":" + System.getProperty("os.name") + ":PartyLauncherSecretV1").toCharArray()
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(userSecret, salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    override fun saveAccount(session: MinecraftSession) {
        try {
            Files.createDirectories(authDir)
            val plainText = json.encodeToString(session).toByteArray(StandardCharsets.UTF_8)
            val key = getSecretKey()

            val iv = ByteArray(ivLength)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
            val cipherText = cipher.doFinal(plainText)

            // Format: [12 bytes IV] + [Ciphertext + AuthTag]
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            val base64 = Base64.getEncoder().encodeToString(combined)
            Files.writeString(sessionFile, base64)
            AppLogger.info("AccountStorage", "Account session encrypted and saved for '${session.username}'")
        } catch (e: Exception) {
            AppLogger.error("AccountStorage", "Failed to save account session: ${e.message}", e)
        }
    }

    override fun loadAccount(): MinecraftSession? {
        if (!Files.exists(sessionFile)) return null
        return try {
            val base64 = Files.readString(sessionFile).trim()
            if (base64.isEmpty()) return null

            val combined = Base64.getDecoder().decode(base64)
            if (combined.size <= ivLength) return null

            val iv = ByteArray(ivLength)
            System.arraycopy(combined, 0, iv, 0, ivLength)
            val cipherText = ByteArray(combined.size - ivLength)
            System.arraycopy(combined, ivLength, cipherText, 0, cipherText.size)

            val key = getSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagLength, iv))
            val plainText = cipher.doFinal(cipherText)

            val sessionJson = String(plainText, StandardCharsets.UTF_8)
            val session = json.decodeFromString<MinecraftSession>(sessionJson)
            AppLogger.info("AccountStorage", "Loaded saved account session for '${session.username}'")
            session
        } catch (e: Exception) {
            AppLogger.warn("AccountStorage", "Could not decrypt/load saved session: ${e.message}")
            removeAccount()
            null
        }
    }

    override fun removeAccount() {
        try {
            Files.deleteIfExists(sessionFile)
            AppLogger.info("AccountStorage", "Removed saved account session")
        } catch (e: Exception) {
            AppLogger.error("AccountStorage", "Failed to remove session file: ${e.message}", e)
        }
    }

    override fun hasAccount(): Boolean {
        return Files.exists(sessionFile)
    }
}
