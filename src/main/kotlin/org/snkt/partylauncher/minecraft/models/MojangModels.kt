package org.snkt.partylauncher.minecraft.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.snkt.partylauncher.util.ArchType
import org.snkt.partylauncher.util.OSType
import org.snkt.partylauncher.util.OSUtils

@Serializable
data class MojangVersionManifest(
    val latest: LatestVersions,
    val versions: List<VersionEntry>
)

@Serializable
data class LatestVersions(
    val release: String,
    val snapshot: String
)

@Serializable
data class VersionEntry(
    val id: String,
    val type: String,
    val url: String,
    val time: String? = null,
    val releaseTime: String? = null,
    val sha1: String? = null
)

@Serializable
data class MojangVersionDetails(
    val id: String,
    val mainClass: String = "net.minecraft.client.main.Main",
    val minecraftArguments: String? = null,
    val arguments: VersionArguments? = null,
    val assetIndex: AssetIndexRef? = null,
    val assets: String = "legacy",
    val downloads: Map<String, DownloadArtifact> = emptyMap(),
    val libraries: List<LibraryEntry> = emptyList(),
    val javaVersion: JavaVersionReq? = null
)

@Serializable
data class JavaVersionReq(
    val component: String = "jre-legacy",
    val majorVersion: Int = 8
)

@Serializable
data class VersionArguments(
    val game: List<JsonElement> = emptyList(),
    val jvm: List<JsonElement> = emptyList()
)

@Serializable
data class AssetIndexRef(
    val id: String,
    val sha1: String,
    val size: Long = 0,
    val totalSize: Long = 0,
    val url: String
)

@Serializable
data class DownloadArtifact(
    val sha1: String = "",
    val size: Long = 0,
    val url: String = "",
    val path: String? = null
)

@Serializable
data class LibraryEntry(
    val name: String,
    val downloads: LibraryDownloads? = null,
    val rules: List<RuleEntry>? = null,
    val natives: Map<String, String>? = null,
    val url: String? = null
) {
    /**
     * Checks if this library is allowed on the current OS and architecture.
     */
    fun isAllowedOnCurrentSystem(): Boolean {
        if (rules == null || rules.isEmpty()) return true
        var allowed = false
        for (rule in rules) {
            val applies = rule.appliesToCurrentSystem()
            if (applies) {
                allowed = (rule.action == "allow")
            }
        }
        return allowed
    }

    /**
     * Resolves the relative path for this library in the libraries repository.
     */
    fun getArtifactRelativePath(): String {
        downloads?.artifact?.path?.let { return it }

        // Parse Maven coordinate: group:artifact:version[:classifier][@extension]
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
}

@Serializable
data class LibraryDownloads(
    val artifact: DownloadArtifact? = null,
    val classifiers: Map<String, DownloadArtifact>? = null
)

@Serializable
data class RuleEntry(
    val action: String = "allow",
    val os: OsRule? = null
) {
    fun appliesToCurrentSystem(): Boolean {
        if (os == null) return true
        if (os.name != null && os.name != OSUtils.currentOS.mojangName) {
            return false
        }
        if (os.arch != null) {
            val match = when (OSUtils.currentArch) {
                ArchType.X64 -> os.arch == "x64" || os.arch == "x86_64"
                ArchType.ARM64 -> os.arch == "arm64" || os.arch == "aarch64"
                ArchType.X86 -> os.arch == "x86"
                ArchType.UNKNOWN -> true
            }
            if (!match) return false
        }
        return true
    }
}

@Serializable
data class OsRule(
    val name: String? = null,
    val version: String? = null,
    val arch: String? = null
)

@Serializable
data class MojangAssetIndex(
    val objects: Map<String, AssetObject> = emptyMap()
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long
) {
    val prefix2: String
        get() = if (hash.length >= 2) hash.substring(0, 2) else "00"

    val mojangDownloadUrl: String
        get() = "https://resources.download.minecraft.net/$prefix2/$hash"

    val relativeStoragePath: String
        get() = "$prefix2/$hash"
}
