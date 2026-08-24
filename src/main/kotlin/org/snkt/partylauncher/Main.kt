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

fun main() {
    // Fix Windows font rendering, fractional DPI blur, and Skiko flickering / tearing
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    if (osName.contains("win")) {
        // Enable subpixel LCD font antialiasing (ClearType)
        System.setProperty("awt.useSystemAAFontSettings", "lcd")
        System.setProperty("swing.aatext", "true")
        System.setProperty("sun.java2d.dpiaware", "true")
        System.setProperty("sun.java2d.uiScale.enabled", "true")

        // Enable VSync and hardware render stability
        if (System.getProperty("skiko.vsync") == null) {
            System.setProperty("skiko.vsync", "true")
        }
        // Force OpenGL on Windows if default Direct3D causes tearing / font flicker
        if (System.getProperty("skiko.renderApi") == null) {
            System.setProperty("skiko.renderApi", "OPENGL")
        }
    }

    application {
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

        val windowState = WindowState(size = DpSize(1180.dp, 740.dp))
        val appPainter = iconImage?.let { BitmapPainter(it.toComposeImageBitmap()) }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "BeachParty Launcher",
            icon = appPainter
        ) {
            window.minimumSize = Dimension(980, 640)
            App()
        }
    }
}
