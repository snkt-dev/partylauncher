package org.snkt.partylauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.snkt.partylauncher.auth.MinecraftSession
import org.snkt.partylauncher.build.BuildManifest
import org.snkt.partylauncher.config.LauncherConfig
import org.snkt.partylauncher.core.AppState
import org.snkt.partylauncher.core.ProgressInfo
import org.snkt.partylauncher.instance.InstanceConfig
import org.snkt.partylauncher.logging.LogEntry
import org.snkt.partylauncher.minecraft.ServerStatus
import org.snkt.partylauncher.news.model.NewsItem
import org.snkt.partylauncher.ui.components.AppLogo
import org.snkt.partylauncher.ui.components.BuildInfoCard
import org.snkt.partylauncher.ui.components.LogViewer
import org.snkt.partylauncher.ui.components.NewsSidebar
import org.snkt.partylauncher.ui.components.ProgressBarWithSpeed
import org.snkt.partylauncher.ui.components.UserProfileBadge
import org.snkt.partylauncher.ui.theme.AccentCyan
import org.snkt.partylauncher.ui.theme.BackgroundDark
import org.snkt.partylauncher.ui.theme.BorderDark
import org.snkt.partylauncher.ui.theme.GoldAccent
import org.snkt.partylauncher.ui.theme.GoldDarkText
import org.snkt.partylauncher.ui.theme.PrimaryGreen
import org.snkt.partylauncher.ui.theme.StatusError
import org.snkt.partylauncher.ui.theme.StatusSuccess
import org.snkt.partylauncher.ui.theme.StatusWarning
import org.snkt.partylauncher.ui.theme.SurfaceCard
import org.snkt.partylauncher.ui.theme.TextMuted
import org.snkt.partylauncher.ui.theme.TextPrimary
import org.snkt.partylauncher.ui.theme.TextSecondary
import java.awt.Desktop
import java.net.URI

@Composable
fun MainScreen(
    session: MinecraftSession,
    config: LauncherConfig,
    appState: AppState,
    remoteManifest: BuildManifest?,
    installedConfig: InstanceConfig?,
    progress: ProgressInfo,
    logs: List<LogEntry>,
    serverStatus: ServerStatus?,
    playtime: String,
    newsList: List<NewsItem>,
    isNewsLoading: Boolean,
    onPlayOrUpdate: () -> Unit,
    onCheckUpdates: () -> Unit,
    onRefreshNews: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    onClearLogs: () -> Unit
) {
    val needsUpdate = remoteManifest != null && (installedConfig == null || installedConfig.buildVersion != remoteManifest.version)
    val isBusy = appState == AppState.DOWNLOADING_BUILD ||
            appState == AppState.VERIFYING_BUILD ||
            appState == AppState.INSTALLING_BUILD ||
            appState == AppState.DOWNLOADING_MINECRAFT ||
            appState == AppState.LAUNCHING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Launcher Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(size = 40.dp, cornerRadius = 10.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "BeachParty Launcher",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Лаунчер для сервера Minecraft",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Right side: User Badge & Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserProfileBadge(
                    session = session,
                    playtime = playtime,
                    onLogout = onLogout,
                    modifier = Modifier.height(44.dp)
                )

                // Golden "Поддержать" button
                Button(
                    onClick = {
                        if (config.donationUrl.isNotBlank()) {
                            try {
                                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                    Desktop.getDesktop().browse(URI(config.donationUrl))
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Paid,
                        contentDescription = "Поддержать",
                        tint = GoldDarkText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Поддержать",
                        color = GoldDarkText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onCheckUpdates,
                    enabled = !isBusy,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Проверить обновления",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    enabled = !isBusy,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Body: 2 Columns (Main Game Actions on Left, News Feed on Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Left Column: Server Status, Build Info, Launch Controls, Logs
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Server Status Card (Live Ping & Online)
                ServerStatusCard(
                    config = config,
                    serverStatus = serverStatus
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Build Info Card
                BuildInfoCard(
                    serverName = config.instanceName,
                    remoteManifest = remoteManifest,
                    installedConfig = installedConfig
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Banner
                StatusBanner(
                    appState = appState,
                    needsUpdate = needsUpdate,
                    remoteVersion = remoteManifest?.version,
                    installedVersion = installedConfig?.buildVersion
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar (if busy)
                if (isBusy) {
                    ProgressBarWithSpeed(progress = progress)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onPlayOrUpdate,
                        enabled = !isBusy && appState != AppState.RUNNING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (needsUpdate) AccentCyan else PrimaryGreen,
                            disabledContainerColor = SurfaceCard
                        )
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = appState.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        } else if (appState == AppState.RUNNING) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = PrimaryGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Игра запущена",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        } else if (needsUpdate) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ОБНОВИТЬ И ИГРАТЬ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundDark
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ИГРАТЬ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom: Log Viewer Console
                LogViewer(
                    logs = logs,
                    onClearLogs = onClearLogs
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column: News Sidebar (LazyColumn)
            NewsSidebar(
                newsList = newsList,
                isLoading = isNewsLoading,
                onRefresh = onRefreshNews,
                modifier = Modifier.width(340.dp)
            )
        }
    }
}

@Composable
private fun ServerStatusCard(
    config: LauncherConfig,
    serverStatus: ServerStatus?
) {
    val isOnline = serverStatus?.isOnline == true
    val pingMs = serverStatus?.pingMs ?: -1
    val online = serverStatus?.onlinePlayers ?: 0
    val max = serverStatus?.maxPlayers ?: 0

    val pingColor = when {
        !isOnline -> StatusError
        pingMs < 80 -> StatusSuccess
        pingMs < 160 -> StatusWarning
        else -> StatusError
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Server IP & Status Dot
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) StatusSuccess else StatusError)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.serverName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOnline) "ОНЛАЙН" else "ОФЛАЙН",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) StatusSuccess else StatusError,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = config.serverAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Right: Online Counter & Ping
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Online players badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = if (isOnline) PrimaryGreen else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOnline) "$online / $max" else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOnline) TextPrimary else TextMuted
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Ping badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = pingColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOnline && pingMs >= 0) "$pingMs ms" else "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = pingColor
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    appState: AppState,
    needsUpdate: Boolean,
    remoteVersion: String?,
    installedVersion: String?
) {
    val (statusColor, statusText, statusIcon) = when {
        appState == AppState.RUNNING -> Triple(StatusSuccess, "Minecraft запущен и работает", Icons.Default.SportsEsports)
        appState == AppState.CHECKING_BUILD -> Triple(AccentCyan, "Проверка обновлений сборки...", Icons.Default.Refresh)
        appState == AppState.DOWNLOADING_BUILD -> Triple(AccentCyan, "Скачивание файлов сборки...", Icons.Default.Download)
        appState == AppState.VERIFYING_BUILD -> Triple(AccentCyan, "Проверка целостности SHA-256...", Icons.Default.CheckCircle)
        appState == AppState.INSTALLING_BUILD -> Triple(AccentCyan, "Установка файлов сборки...", Icons.Default.SystemUpdate)
        appState == AppState.DOWNLOADING_MINECRAFT -> Triple(AccentCyan, "Загрузка компонентов Minecraft...", Icons.Default.Download)
        appState == AppState.LAUNCHING -> Triple(PrimaryGreen, "Запуск процесса Minecraft...", Icons.Default.PlayArrow)
        needsUpdate -> Triple(StatusWarning, "Доступно обновление сборки (v$remoteVersion)", Icons.Default.SystemUpdate)
        installedVersion != null -> Triple(StatusSuccess, "Установлена актуальная версия (v$installedVersion)", Icons.Default.CheckCircle)
        else -> Triple(StatusWarning, "Сборка еще не установлена", Icons.Default.Download)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}
