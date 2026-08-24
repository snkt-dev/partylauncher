package org.snkt.partylauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.snkt.partylauncher.core.AppState
import org.snkt.partylauncher.ui.components.ErrorDialog
import org.snkt.partylauncher.ui.screens.LoginScreen
import org.snkt.partylauncher.ui.screens.MainScreen
import org.snkt.partylauncher.ui.screens.SettingsScreen
import org.snkt.partylauncher.ui.state.LauncherViewModel
import org.snkt.partylauncher.ui.theme.LauncherTheme

@Composable
fun App(viewModel: LauncherViewModel = remember { LauncherViewModel() }) {
    val appState by viewModel.appState.collectAsState()
    val session by viewModel.session.collectAsState()
    val config by viewModel.config.collectAsState()
    val deviceCode by viewModel.deviceCode.collectAsState()
    val deviceCodeRemainingSeconds by viewModel.deviceCodeRemainingSeconds.collectAsState()
    val remoteManifest by viewModel.remoteManifest.collectAsState()
    val installedConfig by viewModel.installedInstanceConfig.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentError by viewModel.currentError.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val formattedPlaytime by viewModel.formattedPlaytime.collectAsState()
    val newsList by viewModel.newsList.collectAsState()
    val isNewsLoading by viewModel.isNewsLoading.collectAsState()

    LauncherTheme {
        if (session == null || appState == AppState.REQUIRES_LOGIN || appState == AppState.DEVICE_CODE_WAITING) {
            LoginScreen(
                appState = appState,
                deviceCode = deviceCode,
                remainingSeconds = deviceCodeRemainingSeconds,
                onLoginClick = { viewModel.startMicrosoftLogin() },
                onOfflineLogin = { viewModel.loginOffline(it) },
                onCancelClick = { viewModel.cancelLogin() }
            )
        } else {
            MainScreen(
                session = session!!,
                config = config,
                appState = appState,
                remoteManifest = remoteManifest,
                installedConfig = installedConfig,
                progress = progress,
                logs = logs,
                serverStatus = serverStatus,
                playtime = formattedPlaytime,
                newsList = newsList,
                isNewsLoading = isNewsLoading,
                onPlayOrUpdate = { viewModel.playOrUpdate() },
                onCheckUpdates = { viewModel.checkUpdates() },
                onRefreshNews = { viewModel.refreshNews() },
                onOpenSettings = { viewModel.openSettings() },
                onLogout = { viewModel.logout() },
                onClearLogs = { viewModel.clearLogs() },
                onCloseGame = { viewModel.stopGame() }
            )
        }

        // Settings Dialog
        if (isSettingsOpen) {
            SettingsScreen(
                currentConfig = config,
                onSave = { viewModel.saveSettings(it) },
                onClose = { viewModel.closeSettings() }
            )
        }

        // Error Dialog
        currentError?.let { error ->
            ErrorDialog(
                error = error,
                onDismiss = { viewModel.dismissError() }
            )
        }
    }
}
