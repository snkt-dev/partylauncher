package org.snkt.partylauncher.instance

import kotlinx.serialization.Serializable

@Serializable
data class InstanceConfig(
    val id: String,
    val buildVersion: String,
    val minecraftVersion: String,
    val loader: String = "fabric",
    val loaderVersion: String = ""
)
