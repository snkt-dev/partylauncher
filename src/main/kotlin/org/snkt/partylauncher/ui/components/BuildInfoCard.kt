package org.snkt.partylauncher.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.snkt.partylauncher.build.BuildManifest
import org.snkt.partylauncher.instance.InstanceConfig
import org.snkt.partylauncher.ui.theme.AccentCyan
import org.snkt.partylauncher.ui.theme.BorderDark
import org.snkt.partylauncher.ui.theme.PrimaryGreen
import org.snkt.partylauncher.ui.theme.SurfaceCard
import org.snkt.partylauncher.ui.theme.TextMuted
import org.snkt.partylauncher.ui.theme.TextPrimary
import org.snkt.partylauncher.ui.theme.TextSecondary

@Composable
fun BuildInfoCard(
    serverName: String,
    remoteManifest: BuildManifest?,
    installedConfig: InstanceConfig?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = serverName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoChip(
                icon = Icons.Default.Layers,
                label = "Версия сборки",
                value = remoteManifest?.version ?: installedConfig?.buildVersion ?: "—",
                accentColor = PrimaryGreen,
                modifier = Modifier.weight(1f)
            )

            InfoChip(
                icon = Icons.Default.SportsEsports,
                label = "Minecraft",
                value = remoteManifest?.minecraft ?: installedConfig?.minecraftVersion ?: "—",
                accentColor = AccentCyan,
                modifier = Modifier.weight(1f)
            )

            InfoChip(
                icon = Icons.Default.Code,
                label = "Загрузчик",
                value = "${(remoteManifest?.loader ?: installedConfig?.loader ?: "fabric").replaceFirstChar { it.uppercase() }} ${remoteManifest?.loaderVersion ?: installedConfig?.loaderVersion ?: ""}".trim(),
                accentColor = PrimaryGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BorderDark.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
