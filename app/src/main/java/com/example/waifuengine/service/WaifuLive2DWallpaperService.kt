package com.example.waifuengine.service

import android.R
import android.app.Presentation
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.util.Base64
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import androidx.webkit.WebViewAssetLoader
import com.example.waifuengine.data.WaifuPreferences
import com.example.waifuengine.live2d.Live2DUrlHelper
import java.io.File
import java.util.Calendar

class WaifuLive2DWallpaperService : WallpaperService() {

    private val prefs by lazy { WaifuPreferences(this) }

    override fun onCreateEngine(): Engine {
        return Live2DWallpaperEngine()
    }

    // --- MOTEUR EXCLUSIF LIVE2D (WEBGL CUBISM VIA VIRTUAL DISPLAY) ---
    inner class Live2DWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var presentation: Presentation? = null
        private var virtualDisplay: VirtualDisplay? = null

        private var rootLayout: FrameLayout? = null
        private var webView: WebView? = null
        private var bgVideoView: VideoView? = null
        private var bgImageView: ImageView? = null

        private var musicPlayer: MediaPlayer? = null
        private var assetLoader: WebViewAssetLoader? = null
        private var targetUrl = ""
        private var lastTouchTime = 0L

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            prefs.registerOnChangeListener(this)

            assetLoader = try {
                WebViewAssetLoader.Builder()
                    .setDomain("appassets.androidview.local")
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this@WaifuLive2DWallpaperService))
                    .addPathHandler("/files/", WebViewAssetLoader.InternalStoragePathHandler(this@WaifuLive2DWallpaperService, filesDir))
                    .build()
            } catch (_: Exception) { null }

            updateTargetUrl()
            loadMusic()
        }

        private fun updateTargetUrl() {
            targetUrl = Live2DUrlHelper.convertToAssetUrl(this@WaifuLive2DWallpaperService, prefs.skelPath)
        }

        private fun getBgImageBase64(imagePath: String): String {
            if (imagePath.isEmpty()) return ""
            val file = File(imagePath)
            if (!file.exists()) return ""
            return try {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val ext = file.extension.lowercase()
                val mime = if (ext == "png") "image/png" else "image/jpeg"
                "data:$mime;base64,$base64"
            } catch (_: Exception) {
                ""
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event == null) return

            val x = event.x
            val y = event.y

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val now = System.currentTimeMillis()
                    if (now - lastTouchTime >= 16) {
                        lastTouchTime = now
                        webView?.evaluateJavascript("setPointerPosition($x, $y)", null)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val customClickAnim = prefs.clickAnimation
                    if (customClickAnim.isNotEmpty()) {
                        val cleanAnim = customClickAnim.replace("'", "\\'")
                        webView?.evaluateJavascript("playNamedAnimation('$cleanAnim')", null)
                    } else {
                        webView?.evaluateJavascript("playActionAnimation()", null)
                    }
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (width <= 0 || height <= 0) return

            if (virtualDisplay == null) {
                createVirtualDisplayView(holder.surface, width, height)
            } else {
                virtualDisplay?.surface = holder.surface
                virtualDisplay?.resize(width, height, resources.displayMetrics.densityDpi)
                applyBackgroundScaleAndOffset()
                reloadBackground()
            }
        }

        private fun createVirtualDisplayView(surface: Surface, width: Int, height: Int) {
            val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager

            virtualDisplay = displayManager.createVirtualDisplay(
                "WaifuLive2DDisplay",
                width, height, resources.displayMetrics.densityDpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )

            val display = virtualDisplay?.display ?: return
            val displayContext = createDisplayContext(display)

            val windowContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    displayContext.createWindowContext(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION, null)
                } catch (_: Exception) {
                    displayContext
                }
            } else {
                displayContext
            }

            presentation = object : Presentation(windowContext, display, R.style.Theme_Translucent_NoTitleBar_Fullscreen) {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    window?.setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
                    window?.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                    window?.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                    window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                    rootLayout = FrameLayout(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }

                    bgVideoView = VideoView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        visibility = View.GONE
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            val vol = if (prefs.isVideoMuted) 0f else 0.8f
                            mp.setVolume(vol, vol)
                            mp.start()
                        }
                    }

                    bgImageView = ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        visibility = View.GONE
                    }

                    webView = WebView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        webChromeClient = WebChromeClient()

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val url = request?.url ?: return null
                                return try { assetLoader?.shouldInterceptRequest(url) } catch (_: Exception) { null }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.onResume()
                                view?.resumeTimers()
                                if (targetUrl.isNotEmpty()) {
                                    val cleanPath = targetUrl.replace("'", "\\'")
                                    evaluateJavascript("loadModelFromAndroid('$cleanPath')", null)
                                    evaluateJavascript("setCustomScale(${prefs.scale})", null)
                                    evaluateJavascript("setCustomOffset(${prefs.offsetX}, ${prefs.offsetY})", null)
                                    reloadBackground()
                                }
                            }
                        }
                        loadUrl("https://appassets.androidview.local/assets/live2d/index.html")
                    }

                    rootLayout?.addView(bgVideoView)
                    rootLayout?.addView(bgImageView)
                    rootLayout?.addView(webView)

                    rootLayout?.let { setContentView(it) }
                    reloadBackground()
                }
            }

            try {
                presentation?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun reloadBackground() {
            if (isScheduleDisabled()) {
                rootLayout?.setBackgroundColor(AndroidColor.BLACK)
                bgVideoView?.visibility = View.GONE
                bgImageView?.visibility = View.GONE

                if (prefs.scheduleFallbackType == WaifuPreferences.TYPE_IMAGE && prefs.scheduleFallbackImagePath.isNotEmpty()) {
                    val base64 = getBgImageBase64(prefs.scheduleFallbackImagePath)
                    if (base64.isNotEmpty()) {
                        webView?.evaluateJavascript("setCustomBackground('IMAGE', '$base64', 1.0, 0, 0)", null)
                    } else {
                        webView?.evaluateJavascript("setCustomBackground('COLOR', '#000000', 1.0, 0, 0)", null)
                    }
                } else {
                    webView?.evaluateJavascript("setCustomBackground('COLOR', '#000000', 1.0, 0, 0)", null)
                }
                return
            }

            bgImageView?.visibility = View.GONE
            bgVideoView?.visibility = View.GONE
            bgVideoView?.stopPlayback()

            when (prefs.bgType) {
                WaifuPreferences.TYPE_COLOR -> {
                    val colorHex = String.format("#%06X", 0xFFFFFF and prefs.bgColor)
                    rootLayout?.setBackgroundColor(prefs.bgColor)
                    webView?.evaluateJavascript("setCustomBackground('COLOR', '$colorHex', 1.0, 0, 0)", null)
                }
                WaifuPreferences.TYPE_IMAGE -> {
                    val base64 = getBgImageBase64(prefs.bgImagePath)
                    if (base64.isNotEmpty()) {
                        rootLayout?.setBackgroundColor(AndroidColor.TRANSPARENT)
                        webView?.evaluateJavascript(
                            "setCustomBackground('IMAGE', '$base64', ${prefs.bgScale}, ${prefs.bgOffsetX}, ${prefs.bgOffsetY})",
                            null
                        )
                    } else {
                        val colorHex = String.format("#%06X", 0xFFFFFF and prefs.bgColor)
                        webView?.evaluateJavascript("setCustomBackground('COLOR', '$colorHex', 1.0, 0, 0)", null)
                    }
                }
                WaifuPreferences.TYPE_VIDEO -> {
                    if (prefs.bgVideoPath.isNotEmpty() && File(prefs.bgVideoPath).exists()) {
                        rootLayout?.setBackgroundColor(AndroidColor.BLACK)
                        bgVideoView?.setVideoPath(prefs.bgVideoPath)
                        bgVideoView?.visibility = View.VISIBLE
                        bgVideoView?.start()
                    }
                }
                else -> {
                    val colorHex = String.format("#%06X", 0xFFFFFF and prefs.bgColor)
                    webView?.evaluateJavascript("setCustomBackground('COLOR', '$colorHex', 1.0, 0, 0)", null)
                }
            }

            applyBackgroundScaleAndOffset()
            rootLayout?.requestLayout()
            rootLayout?.invalidate()
        }

        private fun applyBackgroundScaleAndOffset() {
            val scale = prefs.bgScale
            val tx = prefs.bgOffsetX
            val ty = prefs.bgOffsetY

            bgImageView?.scaleX = scale
            bgImageView?.scaleY = scale
            bgImageView?.translationX = tx
            bgImageView?.translationY = ty

            bgVideoView?.scaleX = scale
            bgVideoView?.scaleY = scale
            bgVideoView?.translationX = tx
            bgVideoView?.translationY = ty

            if (prefs.bgType == WaifuPreferences.TYPE_IMAGE && prefs.bgImagePath.isNotEmpty()) {
                val base64 = getBgImageBase64(prefs.bgImagePath)
                if (base64.isNotEmpty()) {
                    webView?.evaluateJavascript(
                        "setCustomBackground('IMAGE', '$base64', ${prefs.bgScale}, ${prefs.bgOffsetX}, ${prefs.bgOffsetY})",
                        null
                    )
                }
            }
        }

        @Synchronized
        private fun loadMusic() {
            try {
                musicPlayer?.let { mp ->
                    if (mp.isPlaying) mp.stop()
                    mp.release()
                }
                musicPlayer = null

                if (prefs.isMusicEnabled || prefs.isRagebaitEnabled) {
                    val musicPath = prefs.musicPath
                    val hasCustomMusic = musicPath.isNotEmpty() && File(musicPath).exists()

                    musicPlayer = MediaPlayer().apply {
                        if (hasCustomMusic) {
                            setDataSource(musicPath)
                        } else {
                            val afd = assets.openFd("rickroll.mp3")
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                        }

                        isLooping = true
                        val vol = prefs.musicVolume
                        setVolume(vol, vol)
                        setOnPreparedListener { mp ->
                            if (!isScheduleDisabled() || prefs.isRagebaitEnabled) {
                                mp.start()
                            }
                        }
                        prepareAsync()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun isScheduleDisabled(): Boolean {
            if (!prefs.isScheduleEnabled) return false
            val now = Calendar.getInstance()
            val dayKey = now.get(Calendar.DAY_OF_WEEK).toString()
            val daySched = prefs.getDaySchedule(dayKey, "")
            if (!daySched.isEnabled) return false

            val currentMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val startMin = daySched.startHour * 60 + daySched.startMinute
            val endMin = daySched.endHour * 60 + daySched.endMinute

            return if (startMin <= endMin) {
                currentMin in startMin..endMin
            } else {
                currentMin >= startMin || currentMin <= endMin
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                webView?.onResume()
                webView?.resumeTimers()

                val customWakeupAnim = prefs.wakeupAnimation
                if (customWakeupAnim.isNotEmpty()) {
                    val cleanAnim = customWakeupAnim.replace("'", "\\'")
                    webView?.evaluateJavascript("playNamedAnimation('$cleanAnim')", null)
                }

                if (!isScheduleDisabled()) {
                    bgVideoView?.start()
                    if (musicPlayer?.isPlaying == false && (prefs.isMusicEnabled || prefs.isRagebaitEnabled)) {
                        musicPlayer?.start()
                    }
                }
            } else {
                webView?.onPause()
                webView?.pauseTimers()
                bgVideoView?.pause()
                if (!prefs.isRagebaitEnabled) {
                    musicPlayer?.pause()
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnChangeListener(this)
            try {
                musicPlayer?.release()
                musicPlayer = null
                presentation?.dismiss()
                presentation = null
                virtualDisplay?.release()
                virtualDisplay = null
            } catch (_: Exception) {}
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                WaifuPreferences.KEY_SKEL -> {
                    updateTargetUrl()
                    if (targetUrl.isNotEmpty()) {
                        val cleanPath = targetUrl.replace("'", "\\'")
                        webView?.evaluateJavascript("loadModelFromAndroid('$cleanPath')", null)
                    }
                }
                WaifuPreferences.KEY_SCALE, WaifuPreferences.KEY_OFFSET_X, WaifuPreferences.KEY_OFFSET_Y -> {
                    webView?.evaluateJavascript("setCustomScale(${prefs.scale})", null)
                    webView?.evaluateJavascript("setCustomOffset(${prefs.offsetX}, ${prefs.offsetY})", null)
                }
                WaifuPreferences.KEY_BG_TYPE, WaifuPreferences.KEY_BG_IMAGE, WaifuPreferences.KEY_BG_VIDEO, WaifuPreferences.KEY_BG_COLOR -> {
                    reloadBackground()
                }
                WaifuPreferences.KEY_BG_SCALE, WaifuPreferences.KEY_BG_OFFSET_X, WaifuPreferences.KEY_BG_OFFSET_Y -> {
                    applyBackgroundScaleAndOffset()
                }
                WaifuPreferences.KEY_MUSIC_PATH, WaifuPreferences.KEY_MUSIC_ENABLED, WaifuPreferences.KEY_RAGEBAIT_ENABLED -> {
                    loadMusic()
                }
                WaifuPreferences.KEY_MUSIC_VOLUME -> {
                    musicPlayer?.setVolume(prefs.musicVolume, prefs.musicVolume)
                }
            }
        }
    }
}
