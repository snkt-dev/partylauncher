package org.snkt.partylauncher

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.ui.App
import java.awt.Dimension
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() = application {
    AppLogger.info("Main", "Starting PartyLauncher application...")

    val iconImage = try {
        object {}.javaClass.getResourceAsStream("/icon.png")?.use { ImageIO.read(it) }
    } catch (e: Exception) {
        null
    }

    // Set macOS Dock and OS Taskbar icon
    try {
        if (iconImage != null && Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.iconImage = iconImage
            }
        }
    } catch (e: Exception) {
        AppLogger.warn("Main", "Could not set Taskbar icon: ${e.message}")
    }

    val windowState = WindowState(size = DpSize(1000.dp, 700.dp))
    val appPainter = iconImage?.let { BitmapPainter(it.toComposeImageBitmap()) }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "BeachParty Launcher",
        icon = appPainter
    ) {
        window.minimumSize = Dimension(880, 620)
        App()
    }
}
