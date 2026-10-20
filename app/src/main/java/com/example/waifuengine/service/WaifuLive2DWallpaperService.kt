package com.example.waifuengine.service

import android.R
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Presentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.webkit.WebViewAssetLoader
import com.example.waifuengine.data.WaifuPreferences
import com.example.waifuengine.live2d.Live2DUrlHelper
import com.example.waifuengine.spine.SpineModelHelper
import com.example.waifuengine.spine.SpineUrlHelper
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WaifuLive2DWallpaperService : WallpaperService() {

    private val prefs by lazy { WaifuPreferences(this) }

    override fun onCreateEngine(): Engine {
        return Live2DWallpaperEngine()
    }

    // --- MOTEUR FOND D'ECRAN UNIFIE (LIVE2D & SPINE AUTO-SWITCH) ---
    inner class Live2DWallpaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var presentation: Presentation? = null
        private var virtualDisplay: VirtualDisplay? = null

        private var rootLayout: FrameLayout? = null
        private var webView: WebView? = null
        private var bgVideoView: VideoView? = null
        private var bgImageView: ImageView? = null
        private var clockTextView: TextView? = null

        private var musicPlayer: MediaPlayer? = null
        private var assetLoader: WebViewAssetLoader? = null
        private var targetUrl = ""
        private var targetModelUrlSpine = ""
        private var targetAtlasUrlSpine = ""
        private var currentLoadedEngine = ""
        private var isEngineReady = false
        private var lastTouchTime = 0L

        private var isCurrentlyPaused: Boolean? = null
        private var isReceiverRegistered = false

        private val clockHandler = Handler(Looper.getMainLooper())
        private val clockRunnable = object : Runnable {
            override fun run() {
                if (isVisible && isScreenInteractive() && !isKeyguardLocked()) {
                    updateClockText()
                    syncAudioAndVideoState()
                    val delay = if (prefs.clockShowSeconds) 1000L else 2000L
                    clockHandler.postDelayed(this, delay)
                }
            }
        }

        private val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        // ENERGY: Arrêter tout traitement GPU/CPU quand l'écran est éteint
                        webView?.evaluateJavascript("pauseRendering()", null)
                        webView?.onPause()
                        clockHandler.removeCallbacks(clockRunnable)
                        syncAudioAndVideoState()
                    }
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                        // Reprendre le rendu quand l'écran se rallume
                        if (isVisible) {
                            webView?.onResume()
                            webView?.evaluateJavascript("resumeRendering()", null)
                            if (prefs.isClockEnabled && !isKeyguardLocked()) {
                                clockHandler.post(clockRunnable)
                            }
                        }
                        checkAndApplySchedule()
                        syncAudioAndVideoState()
                    }
                    else -> {
                        checkAndApplySchedule()
                        syncAudioAndVideoState()
                    }
                }
            }
        }

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

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction("android.intent.action.KEYGUARD_CHANGED")
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(timeReceiver, filter, RECEIVER_EXPORTED)
                } else {
                    @SuppressLint("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(timeReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            updateTargetUrls()
            loadMusic()
        }

        private fun isCurrentModelSpine(): Boolean {
            return prefs.engineType.equals(WaifuPreferences.ENGINE_SPINE, ignoreCase = true) ||
                   prefs.skelPath.endsWith(".skel", ignoreCase = true) ||
                   SpineModelHelper.isSpineFile(prefs.skelPath)
        }

        private fun updateTargetUrls() {
            if (isCurrentModelSpine()) {
                val skelPath = prefs.skelPath
                targetModelUrlSpine = SpineUrlHelper.convertToAssetUrl(this@WaifuLive2DWallpaperService, skelPath)
                val atlasPref = prefs.spineAtlasPath
                val atlasFile = if (atlasPref.isNotEmpty() && File(atlasPref).exists()) {
                    File(atlasPref)
                } else {
                    val skelFile = File(skelPath)
                    val parent = skelFile.parentFile
                    if (parent != null && parent.exists()) SpineModelHelper.findAtlasFile(parent) else null
                }
                targetAtlasUrlSpine = if (atlasFile != null) {
                    SpineUrlHelper.convertToAssetUrl(this@WaifuLive2DWallpaperService, atlasFile.absolutePath)
                } else ""
            } else {
                targetUrl = Live2DUrlHelper.convertToAssetUrl(this@WaifuLive2DWallpaperService, prefs.skelPath)
            }
        }

        private fun loadActiveModelInWebView() {
            val wv = webView ?: return
            if (!isEngineReady) return
            val pauseActive = isSchedulePauseActive()
            updateTargetUrls()

            if (isCurrentModelSpine()) {
                if (targetModelUrlSpine.isNotEmpty()) {
                    val quotedModel = JSONObject.quote(targetModelUrlSpine)
                    val quotedAtlas = if (targetAtlasUrlSpine.isNotEmpty()) JSONObject.quote(targetAtlasUrlSpine) else "null"
                    wv.evaluateJavascript("loadModelFromAndroid($quotedModel, $quotedAtlas)", null)
                    wv.evaluateJavascript("setCustomScale(${prefs.scale})", null)
                    wv.evaluateJavascript("setCustomOffset(${prefs.offsetX}, ${prefs.offsetY})", null)
                    wv.evaluateJavascript("setPointerTrackingEnabled(${prefs.isPointerTrackingEnabled})", null)
                    wv.evaluateJavascript("setModelVisible(${!pauseActive})", null)
                }
            } else {
                if (targetUrl.isNotEmpty()) {
                    val quotedPath = JSONObject.quote(targetUrl)
                    wv.evaluateJavascript("loadModelFromAndroid($quotedPath)", null)
                    wv.evaluateJavascript("setCustomScale(${prefs.scale})", null)
                    wv.evaluateJavascript("setCustomOffset(${prefs.offsetX}, ${prefs.offsetY})", null)
                    wv.evaluateJavascript("setPointerTrackingEnabled(${prefs.isPointerTrackingEnabled})", null)
                    wv.evaluateJavascript("setModelVisible(${!pauseActive})", null)
                }
            }
        }

        private fun ensureCorrectEngineLoaded(forceReloadModel: Boolean = false) {
            val wv = webView ?: return
            val neededEngine = if (isCurrentModelSpine()) WaifuPreferences.ENGINE_SPINE else WaifuPreferences.ENGINE_LIVE2D
            if (currentLoadedEngine != neededEngine) {
                currentLoadedEngine = neededEngine
                isEngineReady = false
                val url = if (neededEngine == WaifuPreferences.ENGINE_SPINE) {
                    "https://appassets.androidview.local/assets/spine/index.html"
                } else {
                    "https://appassets.androidview.local/assets/live2d/index.html"
                }
                wv.loadUrl(url)
            } else if (forceReloadModel) {
                loadActiveModelInWebView()
            }
        }

        // ENERGY: Cache pour éviter le ré-encodage Base64 de l'image de fond à chaque reload
        private var cachedBgBase64Path: String = ""
        private var cachedBgBase64Data: String = ""

        private fun getBgImageBase64(imagePath: String): String {
            if (imagePath.isEmpty()) return ""
            // Utiliser le cache si le chemin n'a pas changé
            if (imagePath == cachedBgBase64Path && cachedBgBase64Data.isNotEmpty()) {
                return cachedBgBase64Data
            }
            val file = File(imagePath)
            if (!file.exists()) return ""
            return try {
                val bytes = file.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mime = when (file.extension.lowercase()) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    else -> "image/jpeg"
                }
                val result = "data:$mime;base64,$base64"
                cachedBgBase64Path = imagePath
                cachedBgBase64Data = result
                result
            } catch (_: Exception) {
                ""
            }
        }

        private var surfaceWidth = 1080
        private var surfaceHeight = 1920

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event == null || isSchedulePauseActive()) return

            val x = event.x
            val y = event.y

            // VERIFICATION DE LA ZONE TACTILE PERSONNALISÉE
            if (prefs.isTouchZoneCustom && surfaceWidth > 0 && surfaceHeight > 0) {
                val normX = x / surfaceWidth.toFloat()
                val normY = y / surfaceHeight.toFloat()
                val left = prefs.touchZoneLeft
                val top = prefs.touchZoneTop
                val right = left + prefs.touchZoneWidth
                val bottom = top + prefs.touchZoneHeight

                if (normX !in left..right || normY !in top..bottom) {
                    return
                }
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val now = System.currentTimeMillis()
                    if (now - lastTouchTime >= 16 && prefs.isPointerTrackingEnabled) {
                        lastTouchTime = now
                        webView?.evaluateJavascript("setPointerPosition($x, $y)", null)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val customClickAnim = prefs.clickAnimation
                    if (customClickAnim.isNotBlank() && !customClickAnim.equals("NONE", ignoreCase = true)) {
                        val cleanList = customClickAnim.replace("'", "\\'")
                        webView?.evaluateJavascript("playNamedAnimation('$cleanList')", null)
                    }
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (width <= 0 || height <= 0) return

            surfaceWidth = width
            surfaceHeight = height

            if (virtualDisplay == null) {
                createVirtualDisplayView(holder.surface, width, height)
            } else {
                virtualDisplay?.surface = holder.surface
                virtualDisplay?.resize(width, height, resources.displayMetrics.densityDpi)
                applyBackgroundScaleAndOffset()
                reloadBackground()
                applyClockZOrderAndVisibility()
                webView?.evaluateJavascript("resizeToContainer()", null)
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
                            if (!isSchedulePauseActive()) {
                                mp.start()
                            }
                        }
                    }

                    bgImageView = ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        visibility = View.GONE
                    }

                    clockTextView = TextView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.CENTER
                        }
                        visibility = View.GONE
                    }

                    webView = WebView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        webChromeClient = WebChromeClient()

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val url = request?.url ?: return null
                                val defaultResponse = try { assetLoader?.shouldInterceptRequest(url) } catch (_: Exception) { null }
                                if (defaultResponse != null) return defaultResponse

                                if (url.host == "appassets.androidview.local") {
                                    val path = url.path.orEmpty()
                                    if (path.startsWith("/files/")) {
                                        val relPath = Uri.decode(path.removePrefix("/files/").trimStart('/'))
                                        val file = File(filesDir, relPath)
                                        if (file.exists() && file.isFile) {
                                            val ext = file.extension.lowercase()
                                            val mimeType = when (ext) {
                                                "json", "model3.json" -> "application/json"
                                                "png" -> "image/png"
                                                "jpg", "jpeg" -> "image/jpeg"
                                                "webp" -> "image/webp"
                                                "js" -> "application/javascript"
                                                "css" -> "text/css"
                                                "html" -> "text/html"
                                                else -> "application/octet-stream"
                                            }
                                            return try {
                                                val stream = java.io.FileInputStream(file)
                                                val headers = mapOf("Access-Control-Allow-Origin" to "*")
                                                WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, stream)
                                            } catch (_: Exception) { null }
                                        }
                                    } else if (path.startsWith("/assets/")) {
                                        val relPath = Uri.decode(path.removePrefix("/assets/").trimStart('/'))
                                        val ext = relPath.substringAfterLast('.', "").lowercase()
                                        val mimeType = when (ext) {
                                            "json", "model3.json" -> "application/json"
                                            "png" -> "image/png"
                                            "jpg", "jpeg" -> "image/jpeg"
                                            "webp" -> "image/webp"
                                            "js" -> "application/javascript"
                                            "css" -> "text/css"
                                            "html" -> "text/html"
                                            else -> "application/octet-stream"
                                        }
                                        return try {
                                            val stream = assets.open(relPath)
                                            val headers = mapOf("Access-Control-Allow-Origin" to "*")
                                            WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, stream)
                                        } catch (_: Exception) { null }
                                    }
                                }
                                return null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.onResume()
                                isEngineReady = true
                                loadActiveModelInWebView()
                                reloadBackground()
                            }
                        }
                        val initialEngine = if (isCurrentModelSpine()) WaifuPreferences.ENGINE_SPINE else WaifuPreferences.ENGINE_LIVE2D
                        currentLoadedEngine = initialEngine
                        val initialUrl = if (initialEngine == WaifuPreferences.ENGINE_SPINE) {
                            "https://appassets.androidview.local/assets/spine/index.html"
                        } else {
                            "https://appassets.androidview.local/assets/live2d/index.html"
                        }
                        loadUrl(initialUrl)
                    }

                    rootLayout?.addView(bgVideoView)
                    rootLayout?.addView(bgImageView)
                    rootLayout?.addView(webView)

                    rootLayout?.let { setContentView(it) }
                    reloadBackground()
                    applyClockZOrderAndVisibility()
                }
            }

            try {
                presentation?.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun updateClockText() {
            val clockView = clockTextView ?: return
            if (!prefs.isClockEnabled) return
            val now = Calendar.getInstance().time
            val timePattern = when {
                prefs.clockFormat24h && prefs.clockShowSeconds -> "HH:mm:ss"
                prefs.clockFormat24h -> "HH:mm"
                prefs.clockShowSeconds -> "hh:mm:ss a"
                else -> "hh:mm a"
            }
            val timeStr = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)

            if (prefs.isDateEnabled) {
                val isEn = prefs.appLanguage == WaifuPreferences.LANGUAGE_EN
                val clockLocale = if (isEn) Locale.ENGLISH else Locale.FRENCH
                val datePattern = if (isEn) {
                    if (prefs.dateFormat == WaifuPreferences.DATE_FORMAT_FULL) "MMMM d" else "MMM d"
                } else {
                    if (prefs.dateFormat == WaifuPreferences.DATE_FORMAT_FULL) "d MMMM" else "d MMM"
                }
                val dateStr = SimpleDateFormat(datePattern, clockLocale).format(now)
                val fullText = "$timeStr\n$dateStr"
                val spannable = SpannableString(fullText)
                val dateStart = timeStr.length + 1
                val dateEnd = fullText.length
                spannable.setSpan(
                    RelativeSizeSpan(0.52f),
                    dateStart,
                    dateEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                clockView.text = spannable
                clockView.gravity = Gravity.CENTER
            } else {
                clockView.text = timeStr
                clockView.gravity = Gravity.CENTER
            }
        }

        private fun updateClockStyle() {
            val clockView = clockTextView ?: return
            if (!prefs.isClockEnabled) {
                clockView.visibility = View.GONE
                return
            }

            clockView.visibility = View.VISIBLE
            clockView.textSize = prefs.clockSize
            clockView.setTextColor(prefs.clockColor)
            clockView.setShadowLayer(10f, 2f, 2f, 0xAA000000.toInt())

            val tf = when (prefs.clockFont) {
                WaifuPreferences.FONT_MONOSPACE -> Typeface.MONOSPACE
                WaifuPreferences.FONT_SANS_SERIF -> Typeface.SANS_SERIF
                WaifuPreferences.FONT_SERIF -> Typeface.SERIF
                WaifuPreferences.FONT_THIN -> Typeface.create("sans-serif-thin", Typeface.NORMAL)
                WaifuPreferences.FONT_CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
                WaifuPreferences.FONT_CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
                WaifuPreferences.FONT_CASUAL -> Typeface.create("casual", Typeface.BOLD)
                else -> Typeface.MONOSPACE
            }
            clockView.typeface = tf

            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            clockView.layoutParams = lp
            clockView.translationX = prefs.clockOffsetX
            clockView.translationY = prefs.clockOffsetY

            updateClockText()
        }

        private fun isKeyguardLocked(): Boolean {
            val km = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            return km?.isKeyguardLocked ?: false
        }

        private fun applyClockZOrderAndVisibility() {
            val clockView = clockTextView ?: return
            rootLayout?.removeView(clockView)

            if (!prefs.isClockEnabled) {
                clockView.visibility = View.GONE
                clockHandler.removeCallbacks(clockRunnable)
                return
            }

            updateClockStyle()

            val shouldShowClock = !isKeyguardLocked() && isVisible && isScreenInteractive()
            clockView.visibility = if (shouldShowClock) View.VISIBLE else View.GONE

            if (prefs.clockPosition == WaifuPreferences.CLOCK_POS_FRONT) {
                // DEVANT LE LIVE2D (Par-dessus WebView)
                rootLayout?.addView(clockView)
            } else {
                // DERRIÈRE LE LIVE2D (Sous WebView)
                val webViewIndex = rootLayout?.indexOfChild(webView) ?: -1
                if (webViewIndex >= 0) {
                    rootLayout?.addView(clockView, webViewIndex)
                } else {
                    rootLayout?.addView(clockView, 0)
                }
            }

            clockHandler.removeCallbacks(clockRunnable)
            if (shouldShowClock) {
                clockHandler.post(clockRunnable)
            }
        }

        private fun isScreenInteractive(): Boolean {
            val pm = getSystemService(POWER_SERVICE) as? PowerManager
            return pm?.isInteractive ?: true
        }

        private fun syncAudioAndVideoState() {
            val schedulePaused = isSchedulePauseActive()
            val isRagebait = prefs.isRagebaitEnabled
            val screenInteractive = isScreenInteractive()
            val keyguardLocked = isKeyguardLocked()

            // 1. GESTION DE LA VIDÉO DE FOND
            if (schedulePaused || !isVisible || !screenInteractive || keyguardLocked) {
                if (bgVideoView?.isPlaying == true) {
                    bgVideoView?.pause()
                }
            } else if (prefs.bgType == WaifuPreferences.TYPE_VIDEO && isVisible && screenInteractive && !keyguardLocked) {
                if (bgVideoView?.isPlaying == false) {
                    bgVideoView?.start()
                }
            }

            // 2. GESTION DE LA MUSIQUE DE FOND
            val shouldPlayMusic = if (isRagebait) {
                !schedulePaused
            } else {
                prefs.isMusicEnabled && isVisible && screenInteractive && !keyguardLocked && !schedulePaused
            }

            if (shouldPlayMusic) {
                if (musicPlayer?.isPlaying == false) {
                    try {
                        musicPlayer?.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                if (musicPlayer?.isPlaying == true) {
                    try {
                        musicPlayer?.pause()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        private fun checkAndApplySchedule(forceReload: Boolean = false) {
            val pauseActive = isSchedulePauseActive()
            if (pauseActive != isCurrentlyPaused || forceReload) {
                isCurrentlyPaused = pauseActive
                reloadBackground()
            }
            syncAudioAndVideoState()
        }

        private fun reloadBackground() {
            val pauseActive = isSchedulePauseActive()
            isCurrentlyPaused = pauseActive

            webView?.evaluateJavascript("setModelVisible(${!pauseActive})", null)

            if (pauseActive) {
                bgVideoView?.visibility = View.GONE
                bgVideoView?.stopPlayback()

                if (prefs.scheduleFallbackType == WaifuPreferences.TYPE_IMAGE && prefs.scheduleFallbackImagePath.isNotEmpty() && File(prefs.scheduleFallbackImagePath).exists()) {
                    val fallbackFile = File(prefs.scheduleFallbackImagePath)
                    bgImageView?.setImageURI(Uri.fromFile(fallbackFile))
                    bgImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
                    bgImageView?.scaleX = 1.0f
                    bgImageView?.scaleY = 1.0f
                    bgImageView?.translationX = 0f
                    bgImageView?.translationY = 0f
                    bgImageView?.visibility = View.VISIBLE
                    rootLayout?.setBackgroundColor(AndroidColor.BLACK)
                    webView?.evaluateJavascript("setCustomBackground('COLOR', 'transparent', 1.0, 0, 0)", null)
                } else {
                    bgImageView?.visibility = View.GONE
                    rootLayout?.setBackgroundColor(AndroidColor.BLACK)
                    webView?.evaluateJavascript("setCustomBackground('COLOR', '#000000', 1.0, 0, 0)", null)
                }
                rootLayout?.requestLayout()
                rootLayout?.invalidate()
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
                        webView?.evaluateJavascript("setCustomBackground('COLOR', 'transparent', 1.0, 0, 0)", null)
                        bgVideoView?.setVideoPath(prefs.bgVideoPath)
                        bgVideoView?.visibility = View.VISIBLE
                        if (isVisible) {
                            bgVideoView?.start()
                        }
                    } else {
                        val colorHex = String.format("#%06X", 0xFFFFFF and prefs.bgColor)
                        webView?.evaluateJavascript("setCustomBackground('COLOR', '$colorHex', 1.0, 0, 0)", null)
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

            if (!isSchedulePauseActive() && prefs.bgType == WaifuPreferences.TYPE_IMAGE && prefs.bgImagePath.isNotEmpty()) {
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
                            syncAudioAndVideoState()
                        }
                        prepareAsync()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun isSchedulePauseActive(): Boolean {
            if (!prefs.isScheduleEnabled) return false

            val now = Calendar.getInstance()
            val currentDayInt = now.get(Calendar.DAY_OF_WEEK) // 1 = Dimanche, 2 = Lundi, ..., 7 = Samedi
            val currentMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            // 1. Plage pour la journée d'aujourd'hui
            val todaySched = prefs.getDaySchedule(currentDayInt.toString(), "")
            if (todaySched.isEnabled) {
                val startMin = todaySched.startHour * 60 + todaySched.startMinute
                val endMin = todaySched.endHour * 60 + todaySched.endMinute

                if (startMin <= endMin) {
                    if (currentMin in startMin..endMin) return true
                } else {
                    // Plage de nuit débutant aujourd'hui (ex: 23h00 -> 07h00 demain)
                    if (currentMin >= startMin) return true
                }
            }

            // 2. Plage pour la journée d'hier (pour le bout de nuit finissant aujourd'hui)
            val yesterdayDayInt = if (currentDayInt == Calendar.SUNDAY) Calendar.SATURDAY else currentDayInt - 1
            val yesterdaySched = prefs.getDaySchedule(yesterdayDayInt.toString(), "")
            if (yesterdaySched.isEnabled) {
                val startMin = yesterdaySched.startHour * 60 + yesterdaySched.startMinute
                val endMin = yesterdaySched.endHour * 60 + yesterdaySched.endMinute

                if (startMin > endMin) {
                    // Plage de nuit débutée hier, finissant aujourd'hui (ex: jusqu'à 07h00)
                    if (currentMin <= endMin) return true
                }
            }

            return false
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible && isScreenInteractive() && !isKeyguardLocked()) {
                webView?.onResume()
                webView?.evaluateJavascript("resumeRendering()", null)

                val customWakeupAnim = prefs.wakeupAnimation
                if (customWakeupAnim.isNotEmpty()) {
                    val cleanAnim = customWakeupAnim.replace("'", "\\'")
                    webView?.evaluateJavascript("playNamedAnimation('$cleanAnim')", null)
                }

                checkAndApplySchedule(forceReload = true)
                applyClockZOrderAndVisibility()
            } else {
                // ENERGY: Arrêter complètement le rendu WebGL quand le wallpaper est masqué ou écran éteint
                webView?.evaluateJavascript("pauseRendering()", null)
                webView?.onPause()
                bgVideoView?.pause()
                clockHandler.removeCallbacks(clockRunnable)
            }
            syncAudioAndVideoState()
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnChangeListener(this)
            clockHandler.removeCallbacks(clockRunnable)
            if (isReceiverRegistered) {
                try {
                    unregisterReceiver(timeReceiver)
                } catch (_: Exception) {}
                isReceiverRegistered = false
            }
            try {
                bgVideoView?.stopPlayback()
                bgVideoView = null
                webView?.apply {
                    stopLoading()
                    onPause()
                    destroy()
                }
                webView = null
                musicPlayer?.release()
                musicPlayer = null
                presentation?.dismiss()
                presentation = null
                virtualDisplay?.release()
                virtualDisplay = null
            } catch (_: Exception) {}
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == null) return
            when {
                key in listOf(WaifuPreferences.KEY_SKEL, WaifuPreferences.KEY_ENGINE_TYPE, WaifuPreferences.KEY_SPINE_ATLAS) -> {
                    ensureCorrectEngineLoaded(forceReloadModel = true)
                }
                key in listOf(WaifuPreferences.KEY_SCALE, WaifuPreferences.KEY_OFFSET_X, WaifuPreferences.KEY_OFFSET_Y) -> {
                    webView?.evaluateJavascript("setCustomScale(${prefs.scale})", null)
                    webView?.evaluateJavascript("setCustomOffset(${prefs.offsetX}, ${prefs.offsetY})", null)
                }
                key == WaifuPreferences.KEY_POINTER_TRACKING_ENABLED -> {
                    webView?.evaluateJavascript("setPointerTrackingEnabled(${prefs.isPointerTrackingEnabled})", null)
                }
                key in listOf(
                    WaifuPreferences.KEY_BG_TYPE,
                    WaifuPreferences.KEY_BG_IMAGE,
                    WaifuPreferences.KEY_BG_VIDEO,
                    WaifuPreferences.KEY_BG_COLOR,
                    WaifuPreferences.KEY_SCHEDULE_ENABLED,
                    WaifuPreferences.KEY_SCHEDULE_FALLBACK_TYPE,
                    WaifuPreferences.KEY_SCHEDULE_FALLBACK_IMAGE
                ) || key.startsWith("day_sched_") -> {
                    checkAndApplySchedule(forceReload = true)
                }
                key in listOf(WaifuPreferences.KEY_BG_SCALE, WaifuPreferences.KEY_BG_OFFSET_X, WaifuPreferences.KEY_BG_OFFSET_Y) -> {
                    applyBackgroundScaleAndOffset()
                }
                key in listOf(
                    WaifuPreferences.KEY_CLOCK_ENABLED,
                    WaifuPreferences.KEY_CLOCK_FONT,
                    WaifuPreferences.KEY_CLOCK_COLOR,
                    WaifuPreferences.KEY_CLOCK_SIZE,
                    WaifuPreferences.KEY_CLOCK_FORMAT_24H,
                    WaifuPreferences.KEY_CLOCK_SHOW_SECONDS,
                    WaifuPreferences.KEY_CLOCK_POSITION,
                    WaifuPreferences.KEY_CLOCK_OFFSET_X,
                    WaifuPreferences.KEY_CLOCK_OFFSET_Y,
                    WaifuPreferences.KEY_DATE_ENABLED,
                    WaifuPreferences.KEY_DATE_FORMAT
                ) -> {
                    applyClockZOrderAndVisibility()
                }
                key in listOf(WaifuPreferences.KEY_MUSIC_PATH, WaifuPreferences.KEY_MUSIC_ENABLED, WaifuPreferences.KEY_RAGEBAIT_ENABLED) -> {
                    loadMusic()
                }
                key == WaifuPreferences.KEY_MUSIC_VOLUME -> {
                    musicPlayer?.setVolume(prefs.musicVolume, prefs.musicVolume)
                }
            }
        }
    }
}
