package org.snkt.partylauncher.minecraft

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.snkt.partylauncher.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

@Serializable
data class ServerStatus(
    val isOnline: Boolean,
    val pingMs: Long,
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val motd: String,
    val version: String,
    val serverAddress: String
) {
    val pingColorType: PingQuality
        get() = when {
            !isOnline || pingMs < 0 -> PingQuality.OFFLINE
            pingMs < 80 -> PingQuality.GOOD
            pingMs < 160 -> PingQuality.MEDIUM
            else -> PingQuality.POOR
        }

    enum class PingQuality {
        GOOD, MEDIUM, POOR, OFFLINE
    }
}

object ServerPinger {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Pings a Minecraft server using the Server List Ping (SLP) protocol over TCP.
     */
    suspend fun ping(serverAddress: String, timeoutMs: Int = 4000): ServerStatus = withContext(Dispatchers.IO) {
        val (host, port) = parseAddress(serverAddress)
        val startTime = System.currentTimeMillis()

        try {
            Socket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress(host, port), timeoutMs)

                val out = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())

                // 1. Send Handshake Packet (ID: 0x00, Protocol: 767, Host, Port, NextState: 1)
                val handshakeBytes = ByteArrayOutputStream()
                val handshakeData = DataOutputStream(handshakeBytes)
                writeVarInt(handshakeData, 0x00) // Packet ID
                writeVarInt(handshakeData, 767)  // Protocol version (1.21.x)
                writeString(handshakeData, host)
                handshakeData.writeShort(port)
                writeVarInt(handshakeData, 1)    // Next state: status

                val handshakePacket = handshakeBytes.toByteArray()
                writeVarInt(out, handshakePacket.size)
                out.write(handshakePacket)

                // 2. Send Status Request Packet (ID: 0x00, empty payload)
                val statusBytes = ByteArrayOutputStream()
                val statusData = DataOutputStream(statusBytes)
                writeVarInt(statusData, 0x00)
                val statusPacket = statusBytes.toByteArray()
                writeVarInt(out, statusPacket.size)
                out.write(statusPacket)
                out.flush()

                // 3. Read Status Response Packet
                readVarInt(input) // Total packet length
                val packetId = readVarInt(input)
                if (packetId != 0x00) {
                    throw IllegalStateException("Unexpected packet ID: $packetId")
                }

                val jsonResponse = readString(input)
                val pingMs = System.currentTimeMillis() - startTime

                parseJsonResponse(jsonResponse, pingMs, serverAddress)
            }
        } catch (e: Exception) {
            AppLogger.warn("ServerPinger", "Failed to ping server '$serverAddress': ${e.message}")
            ServerStatus(
                isOnline = false,
                pingMs = -1,
                onlinePlayers = 0,
                maxPlayers = 0,
                motd = "Сервер недоступен",
                version = "",
                serverAddress = serverAddress
            )
        }
    }

    fun parseAddress(address: String): Pair<String, Int> {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return "127.0.0.1" to 25565
        return if (trimmed.contains(":")) {
            val parts = trimmed.split(":")
            val host = parts[0].ifEmpty { "127.0.0.1" }
            val port = parts.getOrNull(1)?.toIntOrNull() ?: 25565
            host to port
        } else {
            trimmed to 25565
        }
    }

    fun parseJsonResponse(jsonStr: String, pingMs: Long, address: String): ServerStatus {
        return try {
            val element = json.parseToJsonElement(jsonStr).jsonObject
            val players = element["players"]?.jsonObject
            val online = players?.get("online")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val max = players?.get("max")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

            val versionObj = element["version"]?.jsonObject
            val versionName = versionObj?.get("name")?.jsonPrimitive?.content ?: ""

            val descElement = element["description"]
            val motd = extractMotd(descElement)

            ServerStatus(
                isOnline = true,
                pingMs = pingMs,
                onlinePlayers = online,
                maxPlayers = max,
                motd = motd,
                version = versionName,
                serverAddress = address
            )
        } catch (e: Exception) {
            AppLogger.warn("ServerPinger", "Failed to parse JSON response from server: ${e.message}")
            ServerStatus(
                isOnline = true,
                pingMs = pingMs,
                onlinePlayers = 0,
                maxPlayers = 0,
                motd = "Minecraft Server",
                version = "",
                serverAddress = address
            )
        }
    }

    private fun extractMotd(element: kotlinx.serialization.json.JsonElement?): String {
        if (element == null) return "Minecraft Server"
        return when {
            element is JsonObject -> {
                val text = element["text"]?.jsonPrimitive?.content ?: ""
                val extra = element["extra"]?.jsonArray?.mapNotNull {
                    if (it is JsonObject) it["text"]?.jsonPrimitive?.content else it.jsonPrimitive.content
                }?.joinToString("") ?: ""
                cleanColorCodes(text + extra).trim().ifEmpty { "Minecraft Server" }
            }
            else -> cleanColorCodes(element.jsonPrimitive.content).trim().ifEmpty { "Minecraft Server" }
        }
    }

    private fun cleanColorCodes(text: String): String {
        return text.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "")
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                out.writeByte(v)
                return
            }
            out.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }

    private fun readVarInt(input: DataInputStream): Int {
        var value = 0
        var size = 0
        var b: Int
        while (true) {
            b = input.readByte().toInt()
            value = value or ((b and 0x7F) shl (size++ * 7))
            if (size > 5) throw IllegalArgumentException("VarInt is too big")
            if ((b and 0x80) != 0x80) break
        }
        return value
    }

    private fun writeString(out: DataOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val length = readVarInt(input)
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
