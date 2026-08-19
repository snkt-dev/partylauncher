package org.snkt.partylauncher.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.snkt.partylauncher.logging.AppLogger
import java.io.BufferedReader
import java.io.InputStreamReader

object ProcessUtils {

    /**
     * Reads process stdout and stderr asynchronously, passing masked log messages to AppLogger.
     */
    fun attachProcessLogger(process: Process, tag: String = "Minecraft", scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { AppLogger.info(tag, it) }
                    }
                }
            } catch (e: Exception) {
                AppLogger.debug(tag, "Process stdout stream closed: ${e.message}")
            }
        }

        scope.launch(Dispatchers.IO) {
            try {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { AppLogger.warn(tag, it) }
                    }
                }
            } catch (e: Exception) {
                AppLogger.debug(tag, "Process stderr stream closed: ${e.message}")
            }
        }
    }
}
