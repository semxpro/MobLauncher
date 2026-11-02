package net.millida.launcher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ContentScale.Companion.Crop
import androidx.compose.ui.Alignment
import coil.compose.AsyncImage

@Composable
internal fun AssetImage(
    path: String,
    modifier: Modifier = Modifier,
    scale: ContentScale = Crop,
    alignment: Alignment = Alignment.Center,
    pixelated: Boolean = true,
) {
    AsyncImage(
        model = "file:///android_asset/$path",
        contentDescription = null,
        modifier = modifier,
        contentScale = scale,
        alignment = alignment,
        filterQuality = if (pixelated) FilterQuality.None else FilterQuality.Low,
    )
}
