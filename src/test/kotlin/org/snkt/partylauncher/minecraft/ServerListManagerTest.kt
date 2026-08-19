package org.snkt.partylauncher.minecraft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ServerListManagerTest {

    @Test
    fun testEnsureServerInList(@TempDir tempDir: File) {
        val serverName = "Party Server"
        val serverIp = "play.partyserver.ru:25565"

        // 1. Initial creation
        ServerListManager.ensureServerInList(tempDir, serverName, serverIp)

        val serversFile = File(tempDir, "servers.dat")
        assertTrue(serversFile.exists(), "servers.dat should be created")

        val servers = ServerListManager.readServerList(serversFile)
        assertEquals(1, servers.size)
        assertEquals(serverName, servers[0].name)
        assertEquals(serverIp, servers[0].ip)

        // 2. Updating name of existing server
        ServerListManager.ensureServerInList(tempDir, "Party Server Updated", serverIp)
        val updatedServers = ServerListManager.readServerList(serversFile)
        assertEquals(1, updatedServers.size)
        assertEquals("Party Server Updated", updatedServers[0].name)
        assertEquals(serverIp, updatedServers[0].ip)

        // 3. Adding second server
        ServerListManager.ensureServerInList(tempDir, "Another Server", "127.0.0.1:25565")
        val twoServers = ServerListManager.readServerList(serversFile)
        assertEquals(2, twoServers.size)
        assertEquals("Another Server", twoServers[0].name)
        assertEquals("127.0.0.1:25565", twoServers[0].ip)
        assertEquals("Party Server Updated", twoServers[1].name)
    }
}
