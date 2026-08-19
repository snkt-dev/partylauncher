package org.snkt.partylauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.snkt.partylauncher.core.ProgressInfo
import org.snkt.partylauncher.ui.theme.PrimaryGreen
import org.snkt.partylauncher.ui.theme.SurfaceCard
import org.snkt.partylauncher.ui.theme.TextPrimary
import org.snkt.partylauncher.ui.theme.TextSecondary

@Composable
fun ProgressBarWithSpeed(
    progress: ProgressInfo,
    modifier: Modifier = Modifier
) {
    val fraction = progress.progressFraction
    val animatedProgress by animateFloatAsState(
        targetValue = fraction ?: 0f,
        label = "progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = progress.title.ifBlank { "Обработка..." },
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            val detailsText = when {
                progress.totalBytes > 0 -> "${progress.formattedCurrent} / ${progress.formattedTotal}  ${progress.formattedSpeed}"
                progress.totalItems > 0 -> "${progress.currentItem} / ${progress.totalItems}"
                else -> ""
            }

            if (detailsText.isNotBlank()) {
                Text(
                    text = detailsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (fraction != null) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryGreen,
                trackColor = SurfaceCard
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryGreen,
                trackColor = SurfaceCard
            )
        }
    }
}
