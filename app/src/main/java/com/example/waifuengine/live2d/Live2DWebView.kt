package com.example.waifuengine.live2d

import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler

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

@Composable
fun Live2DWebView(
    modelJsonPath: String,
    scale: Float = 1.0f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    doubleTapTrigger: Int = 0,
    onModelLoaded: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoaded by remember { mutableStateOf(false) }

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

    // Masquage forcé de la barre d'état lors de tout chargement de modèle Live2D
    val enforceStatusBarHidden = {
        (context as? FragmentActivity)?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    // 1. Chargement instantané du modèle Live2D dès que la page est prête ou que le modèle change
    LaunchedEffect(targetUrl, isPageLoaded, webViewRef) {
        if (isPageLoaded && targetUrl.isNotEmpty() && webViewRef != null) {
            enforceStatusBarHidden()
            val cleanPath = targetUrl.replace("'", "\\'")
            webViewRef?.evaluateJavascript("loadModelFromAndroid('$cleanPath')") {
                enforceStatusBarHidden()
                onModelLoaded?.invoke()
            }
            webViewRef?.evaluateJavascript("setCustomScale($scale)", null)
            webViewRef?.evaluateJavascript("setCustomOffset($offsetX, $offsetY)", null)
        }
    }

    // 2. Mise à jour réactive EN TEMPS RÉEL des transformations (Zoom & Déplacement)
    LaunchedEffect(scale, offsetX, offsetY, isPageLoaded, webViewRef) {
        if (isPageLoaded && webViewRef != null) {
            webViewRef?.evaluateJavascript("setCustomScale($scale)", null)
            webViewRef?.evaluateJavascript("setCustomOffset($offsetX, $offsetY)", null)
        }
    }

    // 3. Déclenchement des animations d'interaction au double-tap
    LaunchedEffect(doubleTapTrigger) {
        if (isPageLoaded && doubleTapTrigger > 0) {
            webViewRef?.evaluateJavascript("playActionAnimation()", null)
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true
                settings.domStorageEnabled = true
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                @Suppress("DEPRECATION")
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN
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
                        isPageLoaded = true
                        enforceStatusBarHidden()
                        if (targetUrl.isNotEmpty()) {
                            val cleanPath = targetUrl.replace("'", "\\'")
                            view?.evaluateJavascript("loadModelFromAndroid('$cleanPath')") {
                                enforceStatusBarHidden()
                                onModelLoaded?.invoke()
                            }
                            view?.evaluateJavascript("setCustomScale($scale)", null)
                            view?.evaluateJavascript("setCustomOffset($offsetX, $offsetY)", null)
                        }
                    }
                }

                loadUrl("https://appassets.androidview.local/assets/live2d/index.html")
            }
        },
        update = {},
        modifier = modifier
    )
}
