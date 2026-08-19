package org.snkt.partylauncher.logging

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedDeque

data class LogEntry(
    val time: String,
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * Thread-safe circular buffer for storing and streaming log entries to the UI console.
 */
class LogBuffer(private val capacity: Int = 1000) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val entries = ConcurrentLinkedDeque<LogEntry>()
    private val _logFlow = MutableSharedFlow<LogEntry>(replay = 50, extraBufferCapacity = 500)
    val logFlow: SharedFlow<LogEntry> = _logFlow.asSharedFlow()

    fun log(level: LogLevel, rawMessage: String) {
        val sanitized = AppLogger.maskSensitiveData(rawMessage)
        val entry = LogEntry(
            time = LocalTime.now().format(timeFormatter),
            level = level,
            message = sanitized
        )
        entries.add(entry)
        while (entries.size > capacity) {
            entries.pollFirst()
        }
        _logFlow.tryEmit(entry)
    }

    fun getAllEntries(): List<LogEntry> = entries.toList()

    fun clear() {
        entries.clear()
    }
}
