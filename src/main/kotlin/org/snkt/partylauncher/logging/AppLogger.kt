package org.snkt.partylauncher.logging

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object AppLogger {
    val globalBuffer = LogBuffer()
    private val rootLogger = LoggerFactory.getLogger("PartyLauncher")

    private val sensitivePatterns = listOf(
        Pattern.compile("(?i)(access_token|refresh_token|password|client_secret|identityToken|UserTokens|XBL3\\.0\\s+x=[^;]+;)([=:\"'\\s]+)([^\"'\\s&,]{6,})"),
        Pattern.compile("(?i)(Bearer\\s+)([A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*)"),
        Pattern.compile("(?i)(--accessToken\\s+)([^\\s]+)"),
        Pattern.compile("(?i)(--xuid\\s+)([^\\s]+)")
    )

    fun maskSensitiveData(message: String): String {
        var result = message
        for (pattern in sensitivePatterns) {
            val matcher = pattern.matcher(result)
            val sb = StringBuilder()
            while (matcher.find()) {
                val prefix = matcher.group(1)
                val sep = if (matcher.groupCount() >= 3) matcher.group(2) else ""
                matcher.appendReplacement(sb, "$prefix$sep***REDACTED***")
            }
            matcher.appendTail(sb)
            result = sb.toString()
        }
        return result
    }

    fun debug(tag: String, message: String) {
        val safe = maskSensitiveData(message)
        rootLogger.debug("[$tag] $safe")
        globalBuffer.log(LogLevel.DEBUG, "[$tag] $safe")
    }

    fun info(tag: String, message: String) {
        val safe = maskSensitiveData(message)
        rootLogger.info("[$tag] $safe")
        globalBuffer.log(LogLevel.INFO, "[$tag] $safe")
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        val safe = maskSensitiveData(message)
        if (throwable != null) {
            rootLogger.warn("[$tag] $safe", throwable)
        } else {
            rootLogger.warn("[$tag] $safe")
        }
        globalBuffer.log(LogLevel.WARN, "[$tag] $safe")
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val safe = maskSensitiveData(message)
        if (throwable != null) {
            rootLogger.error("[$tag] $safe", throwable)
        } else {
            rootLogger.error("[$tag] $safe")
        }
        globalBuffer.log(LogLevel.ERROR, "[$tag] $safe")
    }
}
