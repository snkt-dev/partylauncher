package org.snkt.partylauncher.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.snkt.partylauncher.config.LauncherConfig
import org.snkt.partylauncher.ui.theme.BackgroundDark
import org.snkt.partylauncher.ui.theme.BorderDark
import org.snkt.partylauncher.ui.theme.PrimaryGreen
import org.snkt.partylauncher.ui.theme.SurfaceCard
import org.snkt.partylauncher.ui.theme.TextMuted
import org.snkt.partylauncher.ui.theme.TextPrimary
import org.snkt.partylauncher.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    currentConfig: LauncherConfig,
    onSave: (LauncherConfig) -> Unit,
    onClose: () -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentConfig.buildServerUrl) }
    var serverAddress by remember { mutableStateOf(currentConfig.serverAddress) }
    var serverName by remember { mutableStateOf(currentConfig.serverName) }
    var instanceName by remember { mutableStateOf(currentConfig.instanceName) }
    var customJavaPath by remember { mutableStateOf(currentConfig.customJavaPath ?: "") }
    var maxMemoryGb by remember { mutableFloatStateOf((currentConfig.maxMemoryMb / 1024f).coerceIn(1f, 16f)) }
    var minMemoryGb by remember { mutableFloatStateOf((currentConfig.minMemoryMb / 1024f).coerceIn(1f, 8f)) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .width(580.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp)),
            color = SurfaceCard
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Настройки лаунчера",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Server Address (IP:Port)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Адрес игрового сервера (IP / Host)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Используется для мониторинга онлайна, пинга и добавления в список серверов игры",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { serverAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("например: play.example.com или 127.0.0.1:25565", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = BorderDark
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

//                // Server Display Name
//                Text(
//                    text = "Название сервера в мультиплеере",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = TextPrimary
//                )
//                Spacer(modifier = Modifier.height(6.dp))
//                OutlinedTextField(
//                    value = serverName,
//                    onValueChange = { serverName = it },
//                    modifier = Modifier.fillMaxWidth(),
//                    placeholder = { Text("Party Minecraft Server", color = TextMuted) },
//                    singleLine = true,
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = PrimaryGreen,
//                        unfocusedBorderColor = BorderDark
//                    )
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))

                // Build Server URL
                Text(
                    text = "URL сервера обновлений сборки",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/minecraft", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = BorderDark
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // RAM Allocation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Выделение оперативной памяти (RAM)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${maxMemoryGb.roundToInt()} GB (${(maxMemoryGb * 1024).roundToInt()} MB)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = maxMemoryGb,
                    onValueChange = {
                        maxMemoryGb = it
                        if (minMemoryGb > maxMemoryGb) minMemoryGb = maxMemoryGb
                    },
                    valueRange = 1f..16f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGreen,
                        activeTrackColor = PrimaryGreen,
                        inactiveTrackColor = BorderDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 GB", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("4 GB (Рекомендуется)", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("16 GB", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Java Path
                Text(
                    text = "Пользовательский путь к Java (необязательно)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Оставьте пустым для автоопределения установленной Java в системе",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customJavaPath,
                    onValueChange = { customJavaPath = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например: /usr/bin/java или C:\\Program Files\\Java\\...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = BorderDark
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Отмена", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val newConfig = currentConfig.copy(
                                serverAddress = serverAddress.trim().ifBlank { "127.0.0.1:25565" },
                                serverName = serverName.trim().ifBlank { "Party Minecraft Server" },
                                buildServerUrl = serverUrl.trim(),
                                instanceName = instanceName.trim().ifBlank { "Minecraft Server" },
                                customJavaPath = customJavaPath.trim().ifBlank { null },
                                minMemoryMb = (minMemoryGb * 1024).roundToInt(),
                                maxMemoryMb = (maxMemoryGb * 1024).roundToInt()
                            )
                            onSave(newConfig)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp), tint = BackgroundDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
