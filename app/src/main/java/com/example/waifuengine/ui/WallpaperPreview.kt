package com.example.waifuengine.ui

import android.graphics.BitmapFactory
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.example.waifuengine.data.WaifuPreferences
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun WallpaperPreviewBackground(
    bgType: String,
    pickedColor: Color,
    bgImagePath: String,
    bgVideoPath: String,
    bgScale: Float,
    bgOffsetX: Float,
    bgOffsetY: Float,
    isVideoMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (bgType) {
            WaifuPreferences.TYPE_COLOR -> {
                Box(modifier = Modifier.fillMaxSize().background(pickedColor))
            }
            WaifuPreferences.TYPE_IMAGE -> {
                if (bgImagePath.isNotEmpty() && File(bgImagePath).exists()) {
                    val bitmap = rememberSampledImageBitmap(bgImagePath)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Fond d'écran",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = bgScale
                                    scaleY = bgScale
                                    translationX = bgOffsetX
                                    translationY = bgOffsetY
                                }
                        )
                    }
                }
            }
            WaifuPreferences.TYPE_VIDEO -> {
                if (bgVideoPath.isNotEmpty() && File(bgVideoPath).exists()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                tag = bgVideoPath
                                setVideoPath(bgVideoPath)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    val vol = if (isVideoMuted) 0f else 0.8f
                                    mp.setVolume(vol, vol)
                                    mp.start()
                                }
                            }
                        },
                        update = { view ->
                            if (view.tag != bgVideoPath) {
                                view.tag = bgVideoPath
                                view.setVideoPath(bgVideoPath)
                            }
                            if (!view.isPlaying) view.start()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = bgScale
                                scaleY = bgScale
                                translationX = bgOffsetX
                                translationY = bgOffsetY
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSampledImageBitmap(path: String): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            decodeSampledBitmap(path, 1080, 1920)?.asImageBitmap()
        }
    }
    return bitmap
}

private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(path, options)
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
        inSampleSize = max(1, inSampleSize)
    }
    return inSampleSize
}
