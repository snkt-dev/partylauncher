package org.snkt.partylauncher.config

import kotlinx.serialization.Serializable

@Serializable
data class LauncherConfig(
    val buildServerUrl: String = "https://pub-e51ac0b19bed440f8e136dfed81a44f3.r2.dev",
    val instanceId: String = "partybeach",
    val instanceName: String = "Текущая сборка",
    val serverAddress: String = "213.152.43.46:25843",
    val serverName: String = "BeachParty",
    val minMemoryMb: Int = 1024,
    val maxMemoryMb: Int = 4096,
    val customJavaPath: String? = null,
    val windowWidth: Int = 1024,
    val windowHeight: Int = 768,
    val autoCheckUpdates: Boolean = true,
    val closeOnLaunch: Boolean = false,
    val playtimeSecondsByUuid: Map<String, Long> = emptyMap(),
    val firestoreProjectId: String = "beachparty-7d3a5",
    val firestoreNewsCollection: String = "news",
    val customNewsUrl: String? = null,
    val donationUrl: String = "https://boosty.to/sharklab/donate"
)
