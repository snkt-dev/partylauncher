package org.snkt.partylauncher.minecraft

import org.snkt.partylauncher.logging.AppLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

data class ServerEntry(
    val name: String,
    val ip: String,
    val acceptTextures: Byte = 1,
    val icon: String? = null
)

object ServerListManager {

    private const val TAG_END: Byte = 0
    private const val TAG_BYTE: Byte = 1
    private const val TAG_SHORT: Byte = 2
    private const val TAG_INT: Byte = 3
    private const val TAG_LONG: Byte = 4
    private const val TAG_FLOAT: Byte = 5
    private const val TAG_DOUBLE: Byte = 6
    private const val TAG_BYTE_ARRAY: Byte = 7
    private const val TAG_STRING: Byte = 8
    private const val TAG_LIST: Byte = 9
    private const val TAG_COMPOUND: Byte = 10
    private const val TAG_INT_ARRAY: Byte = 11
    private const val TAG_LONG_ARRAY: Byte = 12

    /**
     * Ensures the configured server exists in the instance's servers.dat file.
     */
    fun ensureServerInList(instanceDir: File, serverName: String, serverAddress: String) {
        try {
            val serversFile = File(instanceDir, "servers.dat")
            val existingServers = if (serversFile.exists() && serversFile.length() > 0) {
                readServerList(serversFile)
            } else {
                mutableListOf()
            }

            val normalizedTargetIp = serverAddress.trim()
            val existingIndex = existingServers.indexOfFirst {
                it.ip.trim().equals(normalizedTargetIp, ignoreCase = true)
            }

            if (existingIndex >= 0) {
                // Update name if changed
                val existing = existingServers[existingIndex]
                if (existing.name != serverName) {
                    existingServers[existingIndex] = existing.copy(name = serverName)
                    writeServerList(serversFile, existingServers)
                    AppLogger.info("ServerListManager", "Updated server '$serverName' ($serverAddress) in servers.dat")
                }
            } else {
                // Add to top of server list
                existingServers.add(0, ServerEntry(name = serverName, ip = normalizedTargetIp))
                writeServerList(serversFile, existingServers)
                AppLogger.info("ServerListManager", "Added server '$serverName' ($serverAddress) to servers.dat")
            }
        } catch (e: Exception) {
            AppLogger.warn("ServerListManager", "Could not sync servers.dat: ${e.message}")
        }
    }

    /**
     * Reads servers from servers.dat.
     */
    fun readServerList(file: File): MutableList<ServerEntry> {
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return mutableListOf()

        val isGzip = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
        val inputStream = if (isGzip) {
            GZIPInputStream(ByteArrayInputStream(bytes))
        } else {
            ByteArrayInputStream(bytes)
        }

        val dis = DataInputStream(inputStream)
        val rootType = dis.readByte()
        if (rootType != TAG_COMPOUND) return mutableListOf()
        dis.readUTF() // Root name ("")

        val servers = mutableListOf<ServerEntry>()

        while (true) {
            val tagType = dis.readByte()
            if (tagType == TAG_END) break
            val tagName = dis.readUTF()

            if (tagType == TAG_LIST && tagName == "servers") {
                val elemType = dis.readByte()
                val count = dis.readInt()
                if (elemType == TAG_COMPOUND) {
                    for (i in 0 until count) {
                        servers.add(readServerCompound(dis))
                    }
                } else {
                    skipList(dis, elemType, count)
                }
            } else {
                skipTagPayload(dis, tagType)
            }
        }

        return servers
    }

    private fun readServerCompound(dis: DataInputStream): ServerEntry {
        var name = "Minecraft Server"
        var ip = "127.0.0.1"
        var acceptTextures: Byte = 1
        var icon: String? = null

        while (true) {
            val tagType = dis.readByte()
            if (tagType == TAG_END) break
            val tagName = dis.readUTF()

            when (tagName) {
                "name" -> name = if (tagType == TAG_STRING) dis.readUTF() else { skipTagPayload(dis, tagType); name }
                "ip" -> ip = if (tagType == TAG_STRING) dis.readUTF() else { skipTagPayload(dis, tagType); ip }
                "acceptTextures" -> acceptTextures = if (tagType == TAG_BYTE) dis.readByte() else { skipTagPayload(dis, tagType); acceptTextures }
                "icon" -> icon = if (tagType == TAG_STRING) dis.readUTF() else { skipTagPayload(dis, tagType); icon }
                else -> skipTagPayload(dis, tagType)
            }
        }

        return ServerEntry(name = name, ip = ip, acceptTextures = acceptTextures, icon = icon)
    }

    /**
     * Writes list of servers to servers.dat in uncompressed NBT format.
     */
    fun writeServerList(file: File, servers: List<ServerEntry>) {
        file.parentFile?.mkdirs()
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Root TAG_Compound ("")
        dos.writeByte(TAG_COMPOUND.toInt())
        dos.writeUTF("")

        // TAG_List ("servers", TAG_COMPOUND)
        dos.writeByte(TAG_LIST.toInt())
        dos.writeUTF("servers")
        dos.writeByte(TAG_COMPOUND.toInt())
        dos.writeInt(servers.size)

        for (server in servers) {
            // TAG_String ("name")
            dos.writeByte(TAG_STRING.toInt())
            dos.writeUTF("name")
            dos.writeUTF(server.name)

            // TAG_String ("ip")
            dos.writeByte(TAG_STRING.toInt())
            dos.writeUTF("ip")
            dos.writeUTF(server.ip)

            // TAG_Byte ("acceptTextures")
            dos.writeByte(TAG_BYTE.toInt())
            dos.writeUTF("acceptTextures")
            dos.writeByte(server.acceptTextures.toInt())

            if (server.icon != null) {
                dos.writeByte(TAG_STRING.toInt())
                dos.writeUTF("icon")
                dos.writeUTF(server.icon)
            }

            // End of server compound
            dos.writeByte(TAG_END.toInt())
        }

        // End of root compound
        dos.writeByte(TAG_END.toInt())
        dos.flush()

        FileOutputStream(file).use { it.write(baos.toByteArray()) }
    }

    private fun skipTagPayload(dis: DataInputStream, tagType: Byte) {
        when (tagType) {
            TAG_BYTE -> dis.readByte()
            TAG_SHORT -> dis.readShort()
            TAG_INT -> dis.readInt()
            TAG_LONG -> dis.readLong()
            TAG_FLOAT -> dis.readFloat()
            TAG_DOUBLE -> dis.readDouble()
            TAG_BYTE_ARRAY -> {
                val len = dis.readInt()
                dis.skipBytes(len)
            }
            TAG_STRING -> dis.readUTF()
            TAG_LIST -> {
                val elemType = dis.readByte()
                val count = dis.readInt()
                skipList(dis, elemType, count)
            }
            TAG_COMPOUND -> {
                while (true) {
                    val innerType = dis.readByte()
                    if (innerType == TAG_END) break
                    dis.readUTF()
                    skipTagPayload(dis, innerType)
                }
            }
            TAG_INT_ARRAY -> {
                val len = dis.readInt()
                dis.skipBytes(len * 4)
            }
            TAG_LONG_ARRAY -> {
                val len = dis.readInt()
                dis.skipBytes(len * 8)
            }
        }
    }

    private fun skipList(dis: DataInputStream, elemType: Byte, count: Int) {
        for (i in 0 until count) {
            skipTagPayload(dis, elemType)
        }
    }
}
