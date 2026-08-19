package org.snkt.partylauncher.minecraft.models

import kotlinx.serialization.Serializable

@Serializable
data class FabricProfile(
    val id: String,
    val inheritsFrom: String,
    val mainClass: String = "net.fabricmc.loader.impl.launch.knot.KnotClient",
    val libraries: List<FabricLibraryEntry> = emptyList()
)

@Serializable
data class FabricLibraryEntry(
    val name: String,
    val url: String = "https://maven.fabricmc.net/"
) {
    /**
     * Resolves the relative path for the Maven artifact.
     */
    fun getArtifactRelativePath(): String {
        val parts = name.split(":")
        if (parts.size < 3) return name

        val group = parts[0].replace('.', '/')
        val artifact = parts[1]
        val versionWithExt = parts[2]
        val classifier = if (parts.size >= 4) parts[3] else null

        val version = versionWithExt.substringBefore('@')
        val extension = if (versionWithExt.contains('@')) versionWithExt.substringAfter('@') else "jar"

        return if (classifier != null) {
            "$group/$artifact/$version/$artifact-$version-$classifier.$extension"
        } else {
            "$group/$artifact/$version/$artifact-$version.$extension"
        }
    }

    /**
     * Resolves full download URL.
     */
    fun getDownloadUrl(): String {
        val baseUrl = url.trimEnd('/')
        return "$baseUrl/${getArtifactRelativePath()}"
    }
}
