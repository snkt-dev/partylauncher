package org.snkt.partylauncher.minecraft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerPingerTest {

    @Test
    fun testAddressParsing() {
        val (host1, port1) = ServerPinger.parseAddress("play.hypixel.net")
        assertEquals("play.hypixel.net", host1)
        assertEquals(25565, port1)

        val (host2, port2) = ServerPinger.parseAddress("127.0.0.1:25577")
        assertEquals("127.0.0.1", host2)
        assertEquals(25577, port2)

        val (host3, port3) = ServerPinger.parseAddress("   ")
        assertEquals("127.0.0.1", host3)
        assertEquals(25565, port3)
    }

    @Test
    fun testParseJsonResponse() {
        val rawJson = """
            {
                "version": {
                    "name": "1.21.1",
                    "protocol": 767
                },
                "players": {
                    "max": 100,
                    "online": 42
                },
                "description": {
                    "text": "§aWelcome to §bParty Minecraft!"
                }
            }
        """.trimIndent()

        val status = ServerPinger.parseJsonResponse(rawJson, 35L, "mc.test.com:25565")
        assertTrue(status.isOnline)
        assertEquals(35L, status.pingMs)
        assertEquals(42, status.onlinePlayers)
        assertEquals(100, status.maxPlayers)
        assertEquals("1.21.1", status.version)
        assertEquals("Welcome to Party Minecraft!", status.motd)
        assertEquals(ServerStatus.PingQuality.GOOD, status.pingColorType)
    }
}
