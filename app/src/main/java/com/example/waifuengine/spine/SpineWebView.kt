package com.example.waifuengine.spine

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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

@Keep
private class SpineJsBridge(
    private val handler: Handler,
    private val onEngineReady: () -> Unit,
    private val onModelLoaded: (String) -> Unit,
    private val onAnimationsLoaded: (List<String>) -> Unit
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

    @Keep
    @JavascriptInterface
    fun onAnimationsLoaded(jsonListStr: String?) {
        if (jsonListStr.isNullOrEmpty()) return
        handler.post {
            try {
                val array = JSONArray(jsonListStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                onAnimationsLoaded(list)
            } catch (_: Exception) {}
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpineWebView(
    modelPath: String,
    atlasPath: String = "",
    scale: Float = 1.0f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    idleAnimation: String = "DEFAULT",
    clickAnimation: String = "",
    isPma: Boolean = false,
    isPointerTrackingEnabled: Boolean = true,
    doubleTapTrigger: Int = 0,
    cornerRadiusPx: Float = 0f,
    isAnimating: Boolean = false,
    isActive: Boolean = true,
    onModelLoaded: ((loadedUrl: String) -> Unit)? = null,
    onAnimationsDiscovered: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isEngineReady by remember { mutableStateOf(false) }

    val onModelLoadedState = rememberUpdatedState(onModelLoaded)
    val onAnimationsDiscoveredState = rememberUpdatedState(onAnimationsDiscovered)
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
                .addPathHandler("/files/", InternalStoragePathHandler(context, context.filesDir))
                .build()
        } catch (_: Exception) {
            null
        }
    }

    val targetModelUrl = remember(modelPath) { SpineUrlHelper.convertToAssetUrl(context, modelPath) }
    val targetAtlasUrl = remember(atlasPath) { SpineUrlHelper.convertToAssetUrl(context, atlasPath) }

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

    // Gestion de l'état actif / inactif (pause et reprise du rendu Spine)
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

    // Chargement du modèle Spine
    LaunchedEffect(targetModelUrl, targetAtlasUrl, isPma, isActive, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (!isEngineReady || !isActive || targetModelUrl.isEmpty()) return@LaunchedEffect
        enforceStatusBarHidden()
        val quotedModel = JSONObject.quote(targetModelUrl)
        val quotedAtlas = if (targetAtlasUrl.isNotEmpty()) JSONObject.quote(targetAtlasUrl) else "null"
        view.evaluateJavascript("loadModelFromAndroid($quotedModel, $quotedAtlas, $isPma)", null)
        val quotedIdle = JSONObject.quote(idleAnimation)
        view.evaluateJavascript("setIdleAnimation($quotedIdle)", null)
        applyTransform(view)
        view.evaluateJavascript("resizeToContainer()", null)
    }

    // Basculement dynamique du mode PMA
    LaunchedEffect(isPma, isActive, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (isEngineReady && isActive) {
            view.evaluateJavascript("setPmaMode($isPma)", null)
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

    // Déclenchement de l'animation lors du double-tap (uniquement si une animation est définie)
    LaunchedEffect(doubleTapTrigger) {
        if (isEngineReady && isActive && doubleTapTrigger > 0) {
            if (clickAnimation.isNotBlank() && !clickAnimation.equals("NONE", ignoreCase = true)) {
                val quotedClick = JSONObject.quote(clickAnimation)
                webViewRef?.evaluateJavascript("playNamedAnimation($quotedClick)", null)
            }
        }
    }

    // Ajustement de la taille lors des animations
    LaunchedEffect(isAnimating) {
        if (!isAnimating && isEngineReady && webViewRef != null && isActive) {
            webViewRef?.evaluateJavascript("resizeToContainer()", null)
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
                    SpineJsBridge(
                        handler = mainHandler,
                        onEngineReady = {
                            isEngineReady = true
                            enforceStatusBarHidden()
                            evaluateJavascript("resizeToContainer()", null)
                        },
                        onModelLoaded = { path ->
                            enforceStatusBarHidden()
                            onModelLoadedState.value?.invoke(path)
                        },
                        onAnimationsLoaded = { anims ->
                            onAnimationsDiscoveredState.value?.invoke(anims)
                        }
                    ),
                    "AzurSpine"
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
                                    val isBinary = ext == "skel"
                                    val mimeType = when (ext) {
                                        "json" -> "application/json"
                                        "skel" -> "application/octet-stream"
                                        "atlas" -> "text/plain"
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
                                val isBinary = ext == "skel"
                                val mimeType = when (ext) {
                                    "json" -> "application/json"
                                    "skel" -> "application/octet-stream"
                                    "atlas" -> "text/plain"
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

                loadUrl("https://appassets.androidview.local/assets/spine/index.html")
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
