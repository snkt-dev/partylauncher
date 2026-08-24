package org.snkt.partylauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.snkt.partylauncher.ui.theme.PrimaryGreen
import javax.imageio.ImageIO

@Composable
fun AppLogo(
    size: Dp = 40.dp,
    cornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember {
        try {
            object {}.javaClass.getResourceAsStream("/icon.png")?.use { ImageIO.read(it) }?.toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(PrimaryGreen.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap, filterQuality = androidx.compose.ui.graphics.FilterQuality.High),
                contentDescription = "PartyLauncher Logo",
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = "PartyLauncher Logo",
                tint = PrimaryGreen,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}
