package org.snkt.partylauncher.config

import kotlinx.serialization.Serializable

@Serializable
data class LauncherConfig(
    val buildServerUrl: String = "https://example.com/minecraft",
    val instanceId: String = "my-server",
    val instanceName: String = "My Minecraft Server",
    val serverAddress: String = "127.0.0.1:25565",
    val serverName: String = "Party Minecraft Server",
    val minMemoryMb: Int = 1024,
    val maxMemoryMb: Int = 4096,
    val customJavaPath: String? = null,
    val windowWidth: Int = 1024,
    val windowHeight: Int = 768,
    val autoCheckUpdates: Boolean = true,
    val closeOnLaunch: Boolean = false,
    val playtimeSecondsByUuid: Map<String, Long> = emptyMap()
)
