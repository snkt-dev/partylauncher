package org.snkt.partylauncher.core

/**
 * Strongly-typed hierarchy of errors that can occur during launcher operations.
 */
sealed class LauncherError(
    val userMessage: String,
    val technicalDetails: String? = null,
    override val cause: Throwable? = null
) : Exception(userMessage, cause) {

    data class AuthenticationFailed(
        val reason: String,
        val details: String? = null,
        override val cause: Throwable? = null
    ) : LauncherError("Ошибка авторизации Microsoft: $reason", details, cause)

    data object MinecraftNotOwned : LauncherError(
        "На этом аккаунте Microsoft не найдена лицензия Minecraft Java Edition.",
        "Entitlements check returned no active Java Edition license for this Microsoft account."
    )

    data class ManifestUnavailable(
        val url: String,
        val details: String? = null,
        override val cause: Throwable? = null
    ) : LauncherError("Не удалось загрузить манифест сборки по адресу $url.", details, cause)

    data class DownloadFailed(
        val item: String,
        val details: String? = null,
        override val cause: Throwable? = null
    ) : LauncherError("Не удалось скачать '$item'. Проверьте подключение к интернету.", details, cause)

    data class HashMismatch(
        val fileName: String,
        val expectedHash: String,
        val actualHash: String
    ) : LauncherError(
        "Контрольная сумма SHA-256 для '$fileName' не совпадает. Файл поврежден и был удален.",
        "Expected SHA-256: $expectedHash, Actual: $actualHash"
    )

    data class InvalidBuild(
        val reason: String,
        val details: String? = null
    ) : LauncherError("Некорректная структура сборки: $reason", details)

    data class MinecraftInstallationFailed(
        val step: String,
        val details: String? = null,
        override val cause: Throwable? = null
    ) : LauncherError("Ошибка установки компонентов Minecraft ($step).", details, cause)

    data class JavaNotFound(
        val minVersion: Int,
        val searchedPaths: List<String> = emptyList()
    ) : LauncherError(
        "Подходящая версия Java (JRE/JDK $minVersion+) не найдена. Установите Java $minVersion+ или укажите путь в Настройках.",
        "Searched paths: ${searchedPaths.joinToString()}"
    )

    data class MinecraftLaunchFailed(
        val exitCode: Int?,
        val details: String? = null,
        override val cause: Throwable? = null
    ) : LauncherError(
        "Minecraft аварийно завершил работу${if (exitCode != null) " с кодом $exitCode" else ""}.",
        details,
        cause
    )

    data class Generic(
        val msg: String,
        val details: String? = null,
        override val cause: Throwable? = null
    ) : LauncherError(msg, details, cause)
}
