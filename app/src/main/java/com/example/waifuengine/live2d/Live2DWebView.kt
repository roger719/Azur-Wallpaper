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
import android.view.ViewOutlineProvider
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

object Live2DUrlHelper {
    fun convertToAssetUrl(context: Context, absolutePath: String): String {
        if (absolutePath.isEmpty()) return ""
        val filesDirStr = context.filesDir.absolutePath
        return if (absolutePath.startsWith(filesDirStr)) {
            val rel = absolutePath.substring(filesDirStr.length).replace("\\", "/")
            val encodedRel = rel.split("/").joinToString("/") { Uri.encode(it) }
            "https://appassets.androidview.local/files$encodedRel"
        } else {
            absolutePath
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
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Live2DWebView(
    modelJsonPath: String,
    scale: Float = 1.0f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    doubleTapTrigger: Int = 0,
    cornerRadiusPx: Float = 0f,
    isAnimating: Boolean = false,
    onModelLoaded: ((loadedUrl: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isEngineReady by remember { mutableStateOf(false) }

    val onModelLoadedState = rememberUpdatedState(onModelLoaded)
    val cornerRadiusState = rememberUpdatedState(cornerRadiusPx)

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
    }

    LaunchedEffect(isAnimating) {
        if (!isAnimating && isEngineReady && webViewRef != null) {
            webViewRef?.evaluateJavascript("resizeToContainer()", null)
        }
    }

    LaunchedEffect(targetUrl, isEngineReady, webViewRef) {
        val view = webViewRef ?: return@LaunchedEffect
        if (!isEngineReady || targetUrl.isEmpty()) return@LaunchedEffect
        enforceStatusBarHidden()
        val quotedPath = JSONObject.quote(targetUrl)
        view.evaluateJavascript("loadModelFromAndroid($quotedPath)", null)
        applyTransform(view)
    }

    LaunchedEffect(scale, offsetX, offsetY, isEngineReady, webViewRef) {
        if (isEngineReady) applyTransform(webViewRef)
    }

    LaunchedEffect(doubleTapTrigger) {
        if (isEngineReady && doubleTapTrigger > 0) {
            webViewRef?.evaluateJavascript("playActionAnimation()", null)
        }
    }

    AndroidView(
        factory = { ctx ->
            isEngineReady = false
            WebView(ctx).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.loadsImagesAutomatically = true
                overScrollMode = View.OVER_SCROLL_NEVER
                isNestedScrollingEnabled = false
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

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
                        return try {
                            assetLoader?.shouldInterceptRequest(url)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.onResume()
                        view?.resumeTimers()
                        view?.evaluateJavascript("init()", null)
                        enforceStatusBarHidden()
                    }
                }

                loadUrl("https://appassets.androidview.local/assets/live2d/index.html")
            }
        },
        update = { view ->
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
        modifier = modifier.onSizeChanged { size ->
            if (!isAnimating && size.width > 0 && size.height > 0 && isEngineReady) {
                webViewRef?.evaluateJavascript("resizeToContainer()", null)
            }
        }
    )
}
