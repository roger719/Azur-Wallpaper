package com.example.waifuengine.live2d

import android.annotation.SuppressLint
import androidx.annotation.Keep
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Outline
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import org.json.JSONObject
import com.example.waifuengine.data.WaifuPreferences
import com.example.waifuengine.spine.SpineModelHelper
import java.io.File
import java.io.FileInputStream

object Live2DUrlHelper {
    fun convertToAssetUrl(context: Context, absolutePath: String): String {
        if (absolutePath.isEmpty()) return ""
        val cleanPath = if (absolutePath.startsWith("file://")) {
            absolutePath.removePrefix("file://")
        } else {
            absolutePath
        }
        val filesDirStr = context.filesDir.absolutePath
        return if (cleanPath.startsWith(filesDirStr)) {
            val relativePath = cleanPath.substring(filesDirStr.length).replace("\\", "/").trimStart('/')
            val encodedPath = relativePath.split("/").filter { it.isNotEmpty() }.joinToString("/") { Uri.encode(it) }
            "https://appassets.androidview.local/files/$encodedPath"
        } else {
            cleanPath
        }
    }
}

@Keep
private class Live2DJsBridge(
    private val handler: Handler,
    private val onEngineReady: () -> Unit,
    private val onModelLoaded: (String) -> Unit
) {
    @Keep
    @JavascriptInterface
    fun onEngineReady() {
        handler.post(onEngineReady)
    }

    @Keep
    @JavascriptInterface
    fun onModelLoaded(path: String?) {
        handler.post { onModelLoaded(path.orEmpty()) }
    }

    @Keep
    @JavascriptInterface
    fun onModelError(path: String?) {
        handler.post { onModelLoaded(path.orEmpty()) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Live2DWebView(
    modelJsonPath: String,
    scale: Float = 1.0f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    idleAnimation: String = "DEFAULT",
    isPointerTrackingEnabled: Boolean = true,
    doubleTapTrigger: Int = 0,
    cornerRadiusPx: Float = 0f,
    isAnimating: Boolean = false,
    isActive: Boolean = true,
    onModelLoaded: ((loadedUrl: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { WaifuPreferences(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isEngineReady by remember { mutableStateOf(false) }

    val onModelLoadedState = rememberUpdatedState(onModelLoaded)
    val cornerRadiusState = rememberUpdatedState(cornerRadiusPx)

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                try {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.stopLoading()
                    wv.removeAllViews()
                    wv.destroy()
                } catch (_: Exception) {}
            }
            webViewRef = null
        }
    }

    val assetLoader = remember {
        try {
            WebViewAssetLoader.Builder()
                .setDomain("appassets.androidview.local")
                .addPathHandler("/assets/", AssetsPathHandler(context))
                .build()
        } catch (_: Exception) {
            null
        }
    }

    val targetUrl = remember(modelJsonPath) { Live2DUrlHelper.convertToAssetUrl(context, modelJsonPath) }

    val enforceStatusBarHidden = {
        (context as? FragmentActivity)?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    fun applyTransform(view: WebView?) {
        view ?: return
        view.evaluateJavascript("setCustomScale($scale)", null)
        view.evaluateJavascript("setCustomOffset($offsetX, $offsetY)", null)
        view.evaluateJavascript("setPointerTrackingEnabled($isPointerTrackingEnabled)", null)
    }

    // Gestion de l'état actif / inactif (pause et reprise du rendu PIXI)
    LaunchedEffect(isActive, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (!isEngineReady) return@LaunchedEffect
        if (isActive) {
            view.evaluateJavascript("setModelVisible(true)", null)
            view.evaluateJavascript("resumeRendering()", null)
            view.evaluateJavascript("resizeToContainer()", null)
            applyTransform(view)
        } else {
            view.evaluateJavascript("setModelVisible(false)", null)
            view.evaluateJavascript("pauseRendering()", null)
        }
    }

    // Chargement du modèle
    LaunchedEffect(targetUrl, isActive, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (!isEngineReady || !isActive || targetUrl.isEmpty()) return@LaunchedEffect
        enforceStatusBarHidden()
        val quotedPath = JSONObject.quote(targetUrl)
        val shouldPma = prefs.isSpinePma || SpineModelHelper.shouldEnablePma(modelJsonPath, "")
        view.evaluateJavascript("loadModelFromAndroid($quotedPath, $shouldPma)", null)
        val quotedIdle = JSONObject.quote(idleAnimation)
        view.evaluateJavascript("setIdleAnimation($quotedIdle)", null)
        applyTransform(view)
        view.evaluateJavascript("resizeToContainer()", null)
    }

    LaunchedEffect(prefs.isSpinePma, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (isEngineReady && isActive) {
            val shouldPma = prefs.isSpinePma || SpineModelHelper.shouldEnablePma(modelJsonPath, "")
            view.evaluateJavascript("setPmaMode($shouldPma)", null)
        }
    }

    // Changement de l'animation au repos (idle)
    LaunchedEffect(idleAnimation, isActive, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (isEngineReady && isActive) {
            val quotedIdle = JSONObject.quote(idleAnimation)
            view.evaluateJavascript("setIdleAnimation($quotedIdle)", null)
        }
    }

    // Application des transformations
    LaunchedEffect(scale, offsetX, offsetY, isPointerTrackingEnabled, isEngineReady, webViewRef, isActive) {
        if (isEngineReady && isActive) {
            applyTransform(webViewRef)
        }
    }

    // Déclenchement de l'animation lors du double-tap
    LaunchedEffect(doubleTapTrigger) {
        if (isEngineReady && isActive && doubleTapTrigger > 0) {
            webViewRef?.evaluateJavascript("playActionAnimation()", null)
        }
    }

    // Ajustement de la taille lors des animations
    LaunchedEffect(isAnimating) {
        if (!isAnimating && isEngineReady && webViewRef != null && isActive) {
            webViewRef?.evaluateJavascript("resizeToContainer()", null)
        }
    }

    LaunchedEffect(prefs.fpsLimit, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (isEngineReady && isActive) {
            view.evaluateJavascript("setTargetFps(${prefs.fpsLimit})", null)
        }
    }

    AndroidView(
        factory = { ctx ->
            isEngineReady = false
            WebView(ctx).apply {
                webViewRef = this
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.loadsImagesAutomatically = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = true
                overScrollMode = View.OVER_SCROLL_NEVER
                isNestedScrollingEnabled = false
                setBackgroundColor(AndroidColor.TRANSPARENT)

                addJavascriptInterface(
                    Live2DJsBridge(
                        handler = mainHandler,
                        onEngineReady = {
                            isEngineReady = true
                            enforceStatusBarHidden()
                            evaluateJavascript("resizeToContainer()", null)
                        },
                        onModelLoaded = { path ->
                            enforceStatusBarHidden()
                            onModelLoadedState.value?.invoke(path)
                        }
                    ),
                    "AzurLive2D"
                )

                @Suppress("DEPRECATION")
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                @Suppress("DEPRECATION")
                setOnSystemUiVisibilityChangeListener {
                    enforceStatusBarHidden()
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url ?: return null

                        // 1. Priorité absolue aux fichiers locaux (/files/) avec headers CORS et types MIME explicites
                        if (url.host == "appassets.androidview.local") {
                            val path = url.path.orEmpty()
                            if (path.startsWith("/files/")) {
                                val relPath = Uri.decode(path.removePrefix("/files/").trimStart('/'))
                                val file = File(context.filesDir, relPath)
                                if (file.exists() && file.isFile) {
                                    val ext = file.extension.lowercase()
                                    val isBinary = ext == "moc3" || ext == "moc" || ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "webp"
                                    val mimeType = when (ext) {
                                        "json", "model3.json" -> "application/json"
                                        "moc3", "moc" -> "application/octet-stream"
                                        "png" -> "image/png"
                                        "jpg", "jpeg" -> "image/jpeg"
                                        "webp" -> "image/webp"
                                        "js" -> "application/javascript"
                                        "css" -> "text/css"
                                        "html" -> "text/html"
                                        else -> "application/octet-stream"
                                    }
                                    val encoding = if (isBinary) null else "UTF-8"
                                    return try {
                                        val stream = FileInputStream(file)
                                        val headers = mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Access-Control-Allow-Methods" to "GET, OPTIONS, HEAD",
                                            "Access-Control-Allow-Headers" to "*"
                                        )
                                        WebResourceResponse(mimeType, encoding, 200, "OK", headers, stream)
                                    } catch (_: Exception) { null }
                                }
                            }
                        }

                        // 2. Traitement des assets et autres ressources via assetLoader
                        val defaultResponse = try {
                            assetLoader?.shouldInterceptRequest(url)
                        } catch (_: Exception) {
                            null
                        }
                        if (defaultResponse != null) return defaultResponse

                        // 3. Fallback direct pour /assets/ si assetLoader ne résout pas
                        if (url.host == "appassets.androidview.local") {
                            val path = url.path.orEmpty()
                            if (path.startsWith("/assets/")) {
                                val relPath = Uri.decode(path.removePrefix("/assets/").trimStart('/'))
                                val ext = relPath.substringAfterLast('.', "").lowercase()
                                val isBinary = ext == "moc3" || ext == "moc" || ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "webp"
                                val mimeType = when (ext) {
                                    "json", "model3.json" -> "application/json"
                                    "moc3", "moc" -> "application/octet-stream"
                                    "png" -> "image/png"
                                    "jpg", "jpeg" -> "image/jpeg"
                                    "webp" -> "image/webp"
                                    "js" -> "application/javascript"
                                    "css" -> "text/css"
                                    "html" -> "text/html"
                                    else -> "application/octet-stream"
                                }
                                val encoding = if (isBinary) null else "UTF-8"
                                return try {
                                    val stream = context.assets.open(relPath)
                                    val headers = mapOf("Access-Control-Allow-Origin" to "*")
                                    WebResourceResponse(mimeType, encoding, 200, "OK", headers, stream)
                                } catch (_: Exception) { null }
                            }
                        }
                        return null
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("init()", null)
                        enforceStatusBarHidden()
                    }
                }

                loadUrl("https://appassets.androidview.local/assets/live2d/index.html?fps=${prefs.fpsLimit}&antialias=${prefs.isAntialiasEnabled}&scale=${prefs.renderScale}")
            }
        },
        update = { view ->
            view.isEnabled = isActive
            view.isClickable = isActive
            view.isFocusable = isActive
            val radius = cornerRadiusState.value
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(target: View, outline: Outline) {
                    if (target.width <= 0 || target.height <= 0) return
                    outline.setRoundRect(0, 0, target.width, target.height, radius.coerceAtLeast(0f))
                }
            }
            view.clipToOutline = radius > 0.5f
            view.invalidateOutline()
        },
        modifier = modifier
            .alpha(if (isActive) 1f else 0f)
            .onSizeChanged { size ->
                if (!isAnimating && size.width > 0 && size.height > 0 && isEngineReady && isActive) {
                    webViewRef?.evaluateJavascript("resizeToContainer()", null)
                }
            }
    )
}
