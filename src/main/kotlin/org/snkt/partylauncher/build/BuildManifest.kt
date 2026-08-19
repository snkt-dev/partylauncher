package org.snkt.partylauncher.build

import kotlinx.serialization.Serializable

@Serializable
data class BuildManifest(
    val version: String,
    val minecraft: String,
    val loader: String = "fabric",
    val loaderVersion: String = "0.17.2",
    val file: String,
    val sha256: String
)
