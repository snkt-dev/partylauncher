package org.snkt.partylauncher.core

/**
 * State machine representing the launcher lifecycle.
 */
enum class AppState(val description: String) {
    STARTING("Запуск приложения..."),
    CHECKING_AUTH("Проверка учетной записи Microsoft..."),
    REQUIRES_LOGIN("Требуется вход через Microsoft"),
    DEVICE_CODE_WAITING("Ожидание подтверждения входа..."),
    CHECKING_BUILD("Проверка обновлений сборки..."),
    DOWNLOADING_BUILD("Скачивание сборки..."),
    VERIFYING_BUILD("Проверка контрольной суммы SHA-256..."),
    INSTALLING_BUILD("Установка файлов сборки..."),
    CHECKING_MINECRAFT("Проверка файлов Minecraft..."),
    DOWNLOADING_MINECRAFT("Скачивание компонентов Minecraft..."),
    READY("Готово к игре"),
    LAUNCHING("Запуск Minecraft..."),
    RUNNING("Minecraft запущен"),
    ERROR("Произошла ошибка")
}

/**
 * Detailed progress information for downloading and installation operations.
 */
data class ProgressInfo(
    val title: String = "",
    val currentBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSec: Long = 0L,
    val currentItem: Int = 0,
    val totalItems: Int = 0,
    val itemDescription: String = ""
) {
    val progressFraction: Float?
        get() = when {
            totalBytes > 0 -> (currentBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            totalItems > 0 -> (currentItem.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
            else -> null
        }

    val formattedCurrent: String
        get() = formatBytes(currentBytes)

    val formattedTotal: String
        get() = if (totalBytes > 0) formatBytes(totalBytes) else "—"

    val formattedSpeed: String
        get() = if (speedBytesPerSec > 0) "${formatBytes(speedBytesPerSec)}/с" else ""

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.0f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }
}
