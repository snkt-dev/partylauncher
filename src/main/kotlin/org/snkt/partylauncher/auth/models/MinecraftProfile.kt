package org.snkt.partylauncher.auth.models

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftProfile(
    val id: String,
    val name: String,
    val skins: List<MinecraftSkin> = emptyList(),
    val capes: List<MinecraftCape> = emptyList()
)

@Serializable
data class MinecraftSkin(
    val id: String,
    val state: String,
    val url: String,
    val variant: String? = null
)

@Serializable
data class MinecraftCape(
    val id: String,
    val state: String,
    val url: String,
    val alias: String? = null
)

@Serializable
data class MinecraftEntitlements(
    val items: List<EntitlementItem> = emptyList(),
    val signature: String? = null,
    val keyId: String? = null
)

@Serializable
data class EntitlementItem(
    val name: String,
    val signature: String? = null
)
