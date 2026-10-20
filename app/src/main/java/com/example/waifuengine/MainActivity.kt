package com.example.waifuengine



import android.app.TimePickerDialog

import android.app.WallpaperManager

import android.content.ComponentName

import android.content.Intent

import android.graphics.Color as AndroidColor

import android.net.Uri

import android.os.Bundle

import android.view.View

import android.widget.Toast

import androidx.activity.compose.BackHandler

import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.compose.setContent

import androidx.activity.result.contract.ActivityResultContracts

import androidx.activity.enableEdgeToEdge

import androidx.compose.animation.*

import androidx.compose.animation.core.*

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.gestures.detectTransformGestures

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.clipToBounds

import androidx.compose.ui.window.Dialog

import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.Shadow

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.StrokeJoin

import androidx.compose.ui.graphics.TransformOrigin

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.layout.positionInRoot

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.unit.IntSize

import androidx.compose.ui.util.lerp

import androidx.compose.ui.zIndex

import com.example.waifuengine.ui.WallpaperPreviewBackground

import kotlin.math.roundToInt

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.core.view.WindowCompat

import androidx.core.view.WindowInsetsCompat

import androidx.core.view.WindowInsetsControllerCompat

import androidx.fragment.app.FragmentActivity

import android.webkit.WebView

import com.example.waifuengine.data.AppStrings

import com.example.waifuengine.data.CharacterItem

import com.example.waifuengine.data.CharacterRepository

import com.example.waifuengine.data.EngineType

import com.example.waifuengine.data.FileNode

import com.example.waifuengine.data.PresetItem

import com.example.waifuengine.data.VariantInfo

import com.example.waifuengine.data.WaifuPreferences

import com.example.waifuengine.live2d.Live2DUrlHelper

import com.example.waifuengine.live2d.Live2DWebView

import com.example.waifuengine.spine.SpineModelHelper

import com.example.waifuengine.spine.SpineUrlHelper

import com.example.waifuengine.spine.SpineWebView

import com.example.waifuengine.service.WaifuLive2DWallpaperService

import com.example.waifuengine.service.WaifuSpineWallpaperService

import com.example.waifuengine.ui.theme.WaifuEngineTheme

import com.example.waifuengine.workshop.WorkshopDialog

import java.io.File

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat

import java.util.Calendar

import java.util.Locale



@Composable

fun TopLevelAnimatedVisibility(

    visible: Boolean,

    modifier: Modifier = Modifier,

    enter: EnterTransition = fadeIn() + expandIn(),

    exit: ExitTransition = shrinkOut() + fadeOut(),

    label: String = "AnimatedVisibility",

    content: @Composable AnimatedVisibilityScope.() -> Unit

) {

    AnimatedVisibility(

        visible = visible,

        modifier = modifier,

        enter = enter,

        exit = exit,

        label = label,

        content = content

    )

}



class MainActivity : FragmentActivity() {



    private fun hideSystemBars() {

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)

        controller.hide(WindowInsetsCompat.Type.statusBars())

        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE



        @Suppress("DEPRECATION")

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->

            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {

                controller.hide(WindowInsetsCompat.Type.statusBars())

            }

        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        hideSystemBars()

        try {

            WebView(this).resumeTimers()

        } catch (_: Exception) {}



        setContent {

            WaifuEngineTheme {

                Scaffold(

                    modifier = Modifier.fillMaxSize(),

                    containerColor = Color(0xFFDCE8F6),

                    contentWindowInsets = WindowInsets(0, 0, 0, 0)

                ) { innerPadding ->

                    CustomizationScreen(

                        modifier = Modifier.padding(innerPadding),

                    )

                }

            }

        }

    }



    override fun onResume() {

        super.onResume()

        hideSystemBars()

        try {

            WebView(this).resumeTimers()

        } catch (_: Exception) {}

    }

}



@Composable

fun NothingLoadingIndicator(modifier: Modifier = Modifier) {

    val infiniteTransition = rememberInfiniteTransition(label = "LoadingDotPulse")

    val alphaAnim by infiniteTransition.animateFloat(

        initialValue = 0.3f,

        targetValue = 1.0f,

        animationSpec = infiniteRepeatable(

            animation = tween(600, easing = LinearOutSlowInEasing),

            repeatMode = RepeatMode.Reverse

        ),

        label = "AlphaPulse"

    )



    Surface(

        shape = RoundedCornerShape(20.dp),

        color = Color(0xFF08132B),

        border = BorderStroke(1.dp, Color(0xFFB4CDEB).copy(alpha = 0.6f)),

        modifier = modifier

    ) {

        Row(

            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(12.dp)

        ) {

            CircularProgressIndicator(

                modifier = Modifier.size(18.dp),

                color = Color.White,

                strokeWidth = 2.5.dp

            )

            val context = LocalContext.current

            val isEn = WaifuPreferences(context).appLanguage == WaifuPreferences.LANGUAGE_EN

            Text(

                text = if (isEn) "LOADING..." else "CHARGEMENT...",

                color = Color.White.copy(alpha = alphaAnim),

                fontSize = 11.sp,

                fontFamily = FontFamily.Monospace,

                fontWeight = FontWeight.Bold

            )

        }

    }

}



@Composable

fun RagebaitButton(

    isRagebaitEnabled: Boolean,

    onClick: () -> Unit,

    modifier: Modifier = Modifier,

    strings: AppStrings = AppStrings(WaifuPreferences(LocalContext.current).appLanguage)

) {

    val infiniteTransition = rememberInfiniteTransition(label = "RainbowTransition")

    val hueAnim by infiniteTransition.animateFloat(

        initialValue = 0f,

        targetValue = 360f,

        animationSpec = infiniteRepeatable(

            animation = tween(3000, easing = LinearEasing),

            repeatMode = RepeatMode.Restart

        ),

        label = "HueAnim"

    )



    val rainbowBrush = remember(hueAnim) {

        Brush.horizontalGradient(

            colors = listOf(

                Color.hsv((hueAnim) % 360f, 0.9f, 1f),

                Color.hsv((hueAnim + 60f) % 360f, 0.9f, 1f),

                Color.hsv((hueAnim + 120f) % 360f, 0.9f, 1f),

                Color.hsv((hueAnim + 180f) % 360f, 0.9f, 1f),

                Color.hsv((hueAnim + 240f) % 360f, 0.9f, 1f),

                Color.hsv((hueAnim + 300f) % 360f, 0.9f, 1f),

                Color.hsv((hueAnim + 360f) % 360f, 0.9f, 1f)

            )

        )

    }



    Card(

        shape = RoundedCornerShape(16.dp),

        border = BorderStroke(2.5.dp, rainbowBrush),

        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF4FC)),

        modifier = modifier.clickable(onClick = onClick)

    ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 14.dp, vertical = 10.dp),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.CenterVertically

        ) {

            Column {

                Text(strings.ragebaitMode, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))

                Text(strings.playMusicContinuous, fontSize = 10.sp, color = Color(0xFF2D4B75))

            }



            Surface(

                shape = RoundedCornerShape(12.dp),

                color = if (isRagebaitEnabled) Color(0xFFD9383A) else Color(0xFFD2E3F5)

            ) {

                Text(

                    text = if (isRagebaitEnabled) strings.ragebaitActive else strings.ragebaitInactive,

                    fontSize = 10.sp,

                    fontFamily = FontFamily.Monospace,

                    fontWeight = FontWeight.Bold,

                    color = if (isRagebaitEnabled) Color.White else Color(0xFF0D1B3E),

                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)

                )

            }

        }

    }

}



@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

@Composable

fun CustomizationScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    val prefs = remember { WaifuPreferences(context) }

    val coroutineScope = rememberCoroutineScope()



    val rootDir = remember { CharacterRepository.getRootDirectory(context) }

    var targetImportFolder by remember { mutableStateOf(rootDir) }



    var rootNode by remember { mutableStateOf<FileNode?>(null) }

    var allCharacters by remember { mutableStateOf<List<CharacterItem>>(emptyList()) }



    // Chargement asynchrone des personnages

    LaunchedEffect(rootDir) {

        withContext(Dispatchers.IO) {

            val scannedNode = CharacterRepository.scanDirectoryNode(rootDir)

            val scannedChars = CharacterRepository.loadAllCustomCharacters(context)

            withContext(Dispatchers.Main) {

                rootNode = scannedNode

                allCharacters = scannedChars

            }

        }

    }



    var selectedCharacter by remember { mutableStateOf<CharacterItem?>(null) }

    var selectedVariant by remember { mutableStateOf(selectedCharacter?.variants?.firstOrNull()) }



    // Synchronisation réactive au premier chargement SANS modifier les prefs automatiquement

    LaunchedEffect(allCharacters) {

        if (selectedCharacter == null && allCharacters.isNotEmpty()) {

            val appliedPath = prefs.skelPath

            val match = allCharacters.find { char -> char.variants.any { v -> v.modelJsonPath == appliedPath || v.mocPath == appliedPath } }

            selectedCharacter = match ?: allCharacters.firstOrNull()

            selectedVariant = selectedCharacter?.variants?.find { v -> v.modelJsonPath == appliedPath || v.mocPath == appliedPath } ?: selectedCharacter?.variants?.firstOrNull()

        }

    }



    var currentSkelPath by remember { mutableStateOf(prefs.skelPath) }
    var currentScale by remember { mutableFloatStateOf(prefs.scale) }
    var currentOffsetX by remember { mutableFloatStateOf(prefs.offsetX) }
    var currentOffsetY by remember { mutableFloatStateOf(prefs.offsetY) }
    var idleAnimation by remember { mutableStateOf(prefs.idleAnimation) }
    var wakeupAnimation by remember { mutableStateOf(prefs.wakeupAnimation) }
    var clickAnimation by remember { mutableStateOf(prefs.clickAnimation) }
    var isSpinePma by remember { mutableStateOf(prefs.isSpinePma) }
    var userPresets by remember { mutableStateOf(prefs.getPresets()) }



    // DÉSACTIVER CONTINUELLEMENT LA BARRE D'ÉTAT ANDROID

    DisposableEffect(currentSkelPath, prefs.activeTab) {

        val act = context as? FragmentActivity

        act?.let {

            WindowCompat.setDecorFitsSystemWindows(it.window, false)

            val controller = WindowCompat.getInsetsController(it.window, it.window.decorView)

            controller.hide(WindowInsetsCompat.Type.statusBars())

            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        }

        onDispose {}

    }





    val updateSelectedCharacterAndVariant: (CharacterItem) -> Unit = { charItem ->

        selectedCharacter = charItem

        val firstVariant = charItem.variants.firstOrNull()

        selectedVariant = firstVariant

        if (firstVariant != null) {

            val targetPath = firstVariant.modelJsonPath.ifEmpty { firstVariant.mocPath }

            currentSkelPath = targetPath

        }

    }



    LaunchedEffect(selectedCharacter) {

        if (selectedCharacter != null && selectedVariant?.let { v -> v in selectedCharacter!!.variants } != true) {

            selectedVariant = selectedCharacter?.variants?.firstOrNull()

        }

    }



    LaunchedEffect(selectedVariant) {
        selectedVariant?.let { variant ->
            val targetPath = variant.modelJsonPath.ifEmpty { variant.mocPath }
            currentSkelPath = targetPath
            val isCurrentApplied = targetPath.isNotEmpty() && targetPath == prefs.skelPath

            if (isCurrentApplied) {
                currentScale = prefs.scale
                currentOffsetX = prefs.offsetX
                currentOffsetY = prefs.offsetY
                idleAnimation = prefs.idleAnimation
                wakeupAnimation = prefs.wakeupAnimation
                clickAnimation = prefs.clickAnimation
                val isProjectQt = targetPath.contains("Project QT", ignoreCase = true) || targetPath.contains("projectqt", ignoreCase = true)
                isSpinePma = if (isProjectQt) true else prefs.isSpinePma
            } else {
                val matchingPreset = userPresets.find { it.skelPath == targetPath }
                if (matchingPreset != null) {
                    currentScale = matchingPreset.scale
                    currentOffsetX = matchingPreset.offsetX
                    currentOffsetY = matchingPreset.offsetY
                    idleAnimation = matchingPreset.idleAnimation
                    clickAnimation = matchingPreset.clickAnimation
                    isSpinePma = matchingPreset.isSpinePma
                } else {
                    currentScale = 1.0f
                    currentOffsetX = 0f
                    currentOffsetY = 0f
                    idleAnimation = ""
                    wakeupAnimation = ""
                    clickAnimation = ""
                    val isProjectQt = targetPath.contains("Project QT", ignoreCase = true) || targetPath.contains("projectqt", ignoreCase = true)
                    isSpinePma = isProjectQt
                }
            }
        }
    }



    var bgType by remember { mutableStateOf(prefs.bgType) }

    var isVideoMuted by remember { mutableStateOf(prefs.isVideoMuted) }



    var musicPath by remember { mutableStateOf(prefs.musicPath) }

    var isMusicEnabled by remember { mutableStateOf(prefs.isMusicEnabled) }

    var musicVolume by remember { mutableFloatStateOf(prefs.musicVolume) }



    var isScheduleEnabled by remember { mutableStateOf(prefs.isScheduleEnabled) }

    var scheduleVersion by remember { mutableIntStateOf(0) }



    var fallbackType by remember { mutableStateOf(prefs.scheduleFallbackType) }

    var fallbackImagePath by remember { mutableStateOf(prefs.scheduleFallbackImagePath) }



    var isRagebaitEnabled by remember { mutableStateOf(prefs.isRagebaitEnabled) }

    var ragebaitLockoutTime by remember { mutableLongStateOf(prefs.ragebaitLockoutTime) }



    var showRagebaitEnableDialog by remember { mutableStateOf(false) }

    var showRagebaitDisableDialog by remember { mutableStateOf(false) }

    var pinInputText by remember { mutableStateOf("") }



    var isPointerTrackingEnabled by remember { mutableStateOf(prefs.isPointerTrackingEnabled) }

    var isTouchZoneCustom by remember { mutableStateOf(prefs.isTouchZoneCustom) }
    var touchZoneLeft by remember { mutableFloatStateOf(prefs.touchZoneLeft) }
    var touchZoneTop by remember { mutableFloatStateOf(prefs.touchZoneTop) }
    var touchZoneWidth by remember { mutableFloatStateOf(prefs.touchZoneWidth) }
    var touchZoneHeight by remember { mutableFloatStateOf(prefs.touchZoneHeight) }
    var showTouchZoneEditor by remember { mutableStateOf(false) }

    val availableMotions = remember(selectedCharacter) {
        val char = selectedCharacter
        val folderPath = char?.folderPath.orEmpty()
        val folderFile = File(folderPath)
        if (folderPath.isNotEmpty() && folderFile.exists()) {
            CharacterRepository.getAvailableMotionsForCharacter(folderFile, char?.engineType ?: EngineType.LIVE2D)
        } else {
            listOf("Idle", "Action", "Touch", "Wakeup", "Special")
        }
    }

    LaunchedEffect(selectedCharacter) {
        isPointerTrackingEnabled = prefs.isPointerTrackingEnabled
        isTouchZoneCustom = prefs.isTouchZoneCustom
        touchZoneLeft = prefs.touchZoneLeft
        touchZoneTop = prefs.touchZoneTop
        touchZoneWidth = prefs.touchZoneWidth
        touchZoneHeight = prefs.touchZoneHeight
    }

    // GESTION DES PRÉSETS
    var showSavePresetDialog by remember { mutableStateOf(false) }

    var showDeletePresetDialog by remember { mutableStateOf(false) }

    var presetNameInput by remember { mutableStateOf("") }

    var presetToDelete by remember { mutableStateOf<PresetItem?>(null) }



    var currentAppLanguage by remember { mutableStateOf(prefs.appLanguage) }

    val strings = remember(currentAppLanguage) { AppStrings(currentAppLanguage) }



    val daysOfWeek = remember(strings.isEn) {

        if (strings.isEn) {

            listOf(

                "2" to "Mon",

                "3" to "Tue",

                "4" to "Wed",

                "5" to "Thu",

                "6" to "Fri",

                "7" to "Sat",

                "1" to "Sun"

            )

        } else {

            listOf(

                "2" to "Lun",

                "3" to "Mar",

                "4" to "Mer",

                "5" to "Jeu",

                "6" to "Ven",

                "7" to "Sam",

                "1" to "Dim"

            )

        }

    }



    var selectedDayKey by remember { mutableStateOf("2") }

    var selectedDayLabel by remember(selectedDayKey, strings.isEn) {

        mutableStateOf(daysOfWeek.firstOrNull { it.first == selectedDayKey }?.second ?: if (strings.isEn) "Mon" else "Lun")

    }

    var currentDaySchedule by remember(selectedDayKey) {

        mutableStateOf(prefs.getDaySchedule(selectedDayKey, selectedDayLabel))

    }



    var pickedColor by remember { mutableStateOf(Color(prefs.bgColor)) }



    // State Horloge Numérique & Date

    var isClockEnabled by remember { mutableStateOf(prefs.isClockEnabled) }

    var clockFont by remember { mutableStateOf(prefs.clockFont) }

    var clockColor by remember { mutableIntStateOf(prefs.clockColor) }

    var clockSize by remember { mutableFloatStateOf(prefs.clockSize) }

    var clockFormat24h by remember { mutableStateOf(prefs.clockFormat24h) }

    var clockShowSeconds by remember { mutableStateOf(prefs.clockShowSeconds) }

    var isDateEnabled by remember { mutableStateOf(prefs.isDateEnabled) }

    var dateFormat by remember { mutableStateOf(prefs.dateFormat) }

    var clockPosition by remember { mutableStateOf(prefs.clockPosition) }

    var clockOffsetX by remember { mutableFloatStateOf(prefs.clockOffsetX) }

    var clockOffsetY by remember { mutableFloatStateOf(prefs.clockOffsetY) }



    var showFontDialog by remember { mutableStateOf(false) }

    var showColorDialog by remember { mutableStateOf(false) }

    var showBgMediaPickerDialog by remember { mutableStateOf(false) }



    val currentClockX by rememberUpdatedState(clockOffsetX)

    val currentClockY by rememberUpdatedState(clockOffsetY)



    val onClockDrag: (Float, Float) -> Unit = remember {

        { dx, dy ->

            val clampedX = (currentClockX + dx).coerceIn(-600f, 600f)

            val clampedY = (currentClockY + dy).coerceIn(-1200f, 1200f)

            clockOffsetX = clampedX

            clockOffsetY = clampedY

            prefs.clockOffsetX = clampedX

            prefs.clockOffsetY = clampedY

        }

    }



    // Onglet actif (0 = personnage, 1 = personnalisation, 2 = planification)

    var activeTab by remember { mutableIntStateOf(prefs.activeTab.coerceIn(0, 2)) }

    var isFullscreen by remember { mutableStateOf(false) }

    var isFullscreenUiVisible by remember { mutableStateOf(true) }

    var cardOffset by remember { mutableStateOf(IntOffset.Zero) }

    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    var rootSize by remember { mutableStateOf(IntSize.Zero) }



    LaunchedEffect(activeTab) {

        prefs.activeTab = activeTab

    }



    LaunchedEffect(isFullscreen) {

        if (isFullscreen) {

            isFullscreenUiVisible = true

        }

    }



    // POPUP & CURSEURS DE DÉPLACEMENT ET DE ZOOM RÉACTIFS (STYLE AZUR WALLPAPER)
    var showTransformDialog by remember { mutableStateOf(false) }



    LaunchedEffect(prefs.scale, prefs.offsetX, prefs.offsetY) {

        currentScale = prefs.scale

        currentOffsetX = prefs.offsetX

        currentOffsetY = prefs.offsetY

    }



    // GESTION DES GESTES TACTILES LIVE2D

    var doubleTapTrigger by remember { mutableIntStateOf(0) }

    var viewWidth by remember { mutableFloatStateOf(1080f) }

    var viewHeight by remember { mutableFloatStateOf(1920f) }



    val live2DGestureModifier = Modifier

        .onSizeChanged {

            if (it.width > 0 && it.height > 0) {

                viewWidth = it.width.toFloat()

                viewHeight = it.height.toFloat()

            }

        }

        .pointerInput(activeTab, isFullscreen) {

            detectTransformGestures { _, pan, zoom, _ ->

                val worldFactorX = 800f / viewWidth

                val worldFactorY = 1280f / viewHeight



                if (activeTab == 0) {

                    val dampedRatio = 1.0f + (zoom - 1.0f) * 0.5f

                    val newScale = (currentScale * dampedRatio).coerceIn(0.2f, 6.0f)

                    currentScale = newScale

                    currentOffsetX += pan.x * worldFactorX

                    currentOffsetY -= pan.y * worldFactorY

                } else {

                    val dampedRatio = 1.0f + (zoom - 1.0f) * 0.35f

                    val newScale = (prefs.bgScale * dampedRatio).coerceIn(0.5f, 5.0f)

                    prefs.bgScale = newScale

                    prefs.bgOffsetX += pan.x * worldFactorX

                    prefs.bgOffsetY -= pan.y * worldFactorY

                }

            }

        }

        .pointerInput(isFullscreen) {

            detectTapGestures(

                onDoubleTap = {

                    if (isFullscreen) {

                        isFullscreenUiVisible = !isFullscreenUiVisible

                    } else {

                        if (clickAnimation.isNotBlank() && !clickAnimation.equals("NONE", ignoreCase = true)) {
                            doubleTapTrigger++
                        }

                    }

                },

                onTap = {

                    if (isFullscreen && !isFullscreenUiVisible) {

                        isFullscreenUiVisible = true

                    }

                }

            )

        }



    // GESTION DU GESTE DE RETOUR

    BackHandler(enabled = true) {

        if (isFullscreen) {

            isFullscreen = false

        } else {

            (context as? FragmentActivity)?.moveTaskToBack(true)

        }

    }



    // APPLICATION DU MASQUAGE DE L'APPLICATION (BOÎTE À OUTILS SIM) QUAND RAGEBAIT EST ACTIF

    LaunchedEffect(isRagebaitEnabled) {

        prefs.applyRagebaitAlias(isRagebaitEnabled)

    }



    // VERIFICATION DU TIMER DE DÉVERROUILLAGE RAGEBAIT (15 MINUTES)

    LaunchedEffect(isRagebaitEnabled, ragebaitLockoutTime) {

        while (isRagebaitEnabled && ragebaitLockoutTime > 0L) {

            val now = System.currentTimeMillis()

            if (now >= ragebaitLockoutTime) {

                prefs.isRagebaitEnabled = false

                prefs.ragebaitLockoutTime = 0L

                isRagebaitEnabled = false

                ragebaitLockoutTime = 0L

                Toast.makeText(context, strings.ragebaitAutoDisabledToast, Toast.LENGTH_LONG).show()

                break

            }

            delay(1000L)

        }

    }



    // Dialog States

    var showSettingsDialog by remember { mutableStateOf(false) }



    var showFilesDialog by remember { mutableStateOf(false) }

    var showWorkshopDialog by remember { mutableStateOf(false) }

    var pendingZipUri by remember { mutableStateOf<Uri?>(null) }

    var pendingZipName by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }



    var showCreateFolderDialog by remember { mutableStateOf(false) }

    var targetFolderForNewSub by remember { mutableStateOf(rootDir) }

    var folderNameInput by remember { mutableStateOf("") }



    var selectedNodeForAction by remember { mutableStateOf<FileNode?>(null) }

    var showRenameDialog by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    var showMoveDialog by remember { mutableStateOf(false) }

    var renameInputText by remember { mutableStateOf("") }



    val refreshFileManager = {

        coroutineScope.launch(Dispatchers.IO) {

            val scannedNode = CharacterRepository.scanDirectoryNode(rootDir)

            val scannedChars = CharacterRepository.loadAllCustomCharacters(context)

            withContext(Dispatchers.Main) {

                rootNode = scannedNode

                allCharacters = scannedChars

                if (selectedCharacter == null || !allCharacters.any { it.folderPath == selectedCharacter?.folderPath }) {

                    val match = allCharacters.find { it.variants.any { v -> v.modelJsonPath == prefs.skelPath || v.mocPath == prefs.skelPath } }

                    val firstChar = match ?: allCharacters.firstOrNull()

                    if (firstChar != null) {

                        updateSelectedCharacterAndVariant(firstChar)

                    }

                }

            }

        }

    }



    val clearOldBgFiles = {

        try {

            File(context.filesDir, "custom_bg.jpg").delete()

            File(context.filesDir, "custom_bg.mp4").delete()

        } catch (_: Exception) {}

    }



    val zipPickerLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        if (uri != null) {

            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".zip")?.removeSuffix(".ZIP") ?: "Nouveau_Personnage"

            pendingZipUri = uri

            pendingZipName = fileName

            renameInputText = fileName

            showImportDialog = true

        }

    }



    val imagePickerLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        if (uri != null) {

            try {

                clearOldBgFiles()

                val inputStream = context.contentResolver.openInputStream(uri)

                val destFile = File(context.filesDir, "custom_bg.jpg")

                inputStream?.use { input ->

                    destFile.outputStream().use { output ->

                        input.copyTo(output)

                    }

                }

                prefs.bgImagePath = destFile.absolutePath

                prefs.bgVideoPath = ""

                prefs.bgType = WaifuPreferences.TYPE_IMAGE

                prefs.bgScale = 1.0f

                prefs.bgOffsetX = 0f

                prefs.bgOffsetY = 0f

                bgType = WaifuPreferences.TYPE_IMAGE

                Toast.makeText(context, strings.bgImageAppliedToast, Toast.LENGTH_SHORT).show()

            } catch (_: Exception) {

                Toast.makeText(context, strings.bgImageErrorToast, Toast.LENGTH_SHORT).show()

            }

        }

    }



    val fallbackImagePickerLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        if (uri != null) {

            try {

                val inputStream = context.contentResolver.openInputStream(uri)

                val destFile = File(context.filesDir, "fallback_bg.jpg")

                inputStream?.use { input ->

                    destFile.outputStream().use { output ->

                        input.copyTo(output)

                    }

                }

                prefs.scheduleFallbackImagePath = destFile.absolutePath

                prefs.scheduleFallbackType = WaifuPreferences.TYPE_IMAGE

                fallbackImagePath = destFile.absolutePath

                fallbackType = WaifuPreferences.TYPE_IMAGE

                Toast.makeText(context, strings.pauseImageSavedToast, Toast.LENGTH_SHORT).show()

            } catch (_: Exception) {

                Toast.makeText(context, strings.pauseImageErrorToast, Toast.LENGTH_SHORT).show()

            }

        }

    }



    val videoPickerLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        if (uri != null) {

            try {

                clearOldBgFiles()

                val inputStream = context.contentResolver.openInputStream(uri)

                val destFile = File(context.filesDir, "custom_bg.mp4")

                inputStream?.use { input ->

                    destFile.outputStream().use { output ->

                        input.copyTo(output)

                    }

                }

                prefs.bgVideoPath = destFile.absolutePath

                prefs.bgImagePath = ""

                prefs.bgType = WaifuPreferences.TYPE_VIDEO

                prefs.bgScale = 1.0f

                prefs.bgOffsetX = 0f

                prefs.bgOffsetY = 0f

                bgType = WaifuPreferences.TYPE_VIDEO

                Toast.makeText(context, if (strings.isEn) "Background video applied!" else "Vidéo de fond appliquée !", Toast.LENGTH_SHORT).show()

            } catch (_: Exception) {

                Toast.makeText(context, if (strings.isEn) "Error loading video" else "Erreur lors du chargement de la vidéo", Toast.LENGTH_SHORT).show()

            }

        }

    }



    val mediaPickerLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.OpenDocument()

    ) { uri: Uri? ->

        if (uri != null) {

            try {

                clearOldBgFiles()

                val mimeType = context.contentResolver.getType(uri) ?: ""

                val uriStr = uri.toString().lowercase()

                val isVideo = mimeType.startsWith("video/") || uriStr.endsWith(".mp4") || uriStr.endsWith(".mkv") || uriStr.endsWith(".webm")



                val inputStream = context.contentResolver.openInputStream(uri)

                val fileName = if (isVideo) "custom_bg.mp4" else "custom_bg.jpg"

                val destFile = File(context.filesDir, fileName)



                inputStream?.use { input ->

                    destFile.outputStream().use { output ->

                        input.copyTo(output)

                    }

                }



                if (isVideo) {

                    prefs.bgVideoPath = destFile.absolutePath

                    prefs.bgImagePath = ""

                    prefs.bgType = WaifuPreferences.TYPE_VIDEO

                    bgType = WaifuPreferences.TYPE_VIDEO

                    Toast.makeText(context, if (strings.isEn) "Background video applied!" else "Vidéo de fond appliquée !", Toast.LENGTH_SHORT).show()

                } else {

                    prefs.bgImagePath = destFile.absolutePath

                    prefs.bgVideoPath = ""

                    prefs.bgType = WaifuPreferences.TYPE_IMAGE

                    bgType = WaifuPreferences.TYPE_IMAGE

                    Toast.makeText(context, strings.bgImageAppliedToast, Toast.LENGTH_SHORT).show()

                }

                prefs.bgScale = 1.0f

                prefs.bgOffsetX = 0f

                prefs.bgOffsetY = 0f

            } catch (_: Exception) {

                Toast.makeText(context, if (strings.isEn) "Error loading file" else "Erreur lors du chargement du fichier", Toast.LENGTH_SHORT).show()

            }

        }

    }



    val musicPickerLauncher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.GetContent()

    ) { uri: Uri? ->

        if (uri != null) {

            try {

                val inputStream = context.contentResolver.openInputStream(uri)

                val destFile = File(context.filesDir, "custom_music.mp3")

                inputStream?.use { input ->

                    destFile.outputStream().use { output ->

                        input.copyTo(output)

                    }

                }

                prefs.musicPath = destFile.absolutePath

                prefs.isMusicEnabled = true

                musicPath = destFile.absolutePath

                isMusicEnabled = true

                Toast.makeText(context, if (strings.isEn) "Music loaded!" else "Musique chargée !", Toast.LENGTH_SHORT).show()

            } catch (_: Exception) {

                Toast.makeText(context, if (strings.isEn) "Error loading music" else "Erreur lors du chargement de la musique", Toast.LENGTH_SHORT).show()

            }

        }

    }



    val applyWallpaper = {

        val variant = selectedVariant

        val charItem = selectedCharacter

        if ((variant != null) && (charItem != null)) {

            // Sauvegarder la configuration choisie dans les préférences uniquement au clic sur APPLIQUER

            prefs.skelPath = variant.modelJsonPath.ifEmpty { variant.mocPath }

            prefs.spineAtlasPath = variant.spineAtlasPath

            prefs.engineType = charItem.engineType.name

            prefs.selectedCharacterName = charItem.name

            prefs.selectedVariantLabel = variant.label

            prefs.scale = currentScale
            prefs.offsetX = currentOffsetX
            prefs.offsetY = currentOffsetY
            prefs.idleAnimation = idleAnimation
            prefs.wakeupAnimation = wakeupAnimation
            prefs.clickAnimation = clickAnimation
            prefs.isSpinePma = isSpinePma
            prefs.isPointerTrackingEnabled = isPointerTrackingEnabled
            prefs.bgType = bgType
            if (bgType == WaifuPreferences.TYPE_COLOR) {
                prefs.bgColor = pickedColor.toArgb()
            }

            val targetServiceClass = if (charItem.engineType == EngineType.SPINE) {
                WaifuSpineWallpaperService::class.java
            } else {
                WaifuLive2DWallpaperService::class.java
            }
            val wallpaperManager = WallpaperManager.getInstance(context)
            val currentService = wallpaperManager.wallpaperInfo?.serviceName
            val isWallpaperActive = currentService == WaifuSpineWallpaperService::class.java.name ||
                                    currentService == WaifuLive2DWallpaperService::class.java.name



            if (!isWallpaperActive) {



                try {

                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {

                        putExtra(

                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,

                            ComponentName(context, targetServiceClass),

                        )

                    }

                    context.startActivity(intent)

                } catch (_: Exception) {

                    Toast.makeText(context, strings.wallpaperOpenErrorToast, Toast.LENGTH_LONG).show()

                }

            } else {

                Toast.makeText(context, strings.wallpaperUpdatedToast(charItem.name), Toast.LENGTH_SHORT).show()

            }

        }

    }



    // Palette de couleurs Azur Vif & Élégant

    val nothingBg = Color(0xFFDCE8F6)            // Bleu azur vibrant très clair (Fond principal)

    val nothingSurface = Color(0xFFEDF4FC)       // Bleu céruléen très lumineux (Cartes & Surfaces)

    val nothingBorder = Color(0xFFB4CDEB)        // Bordure bleu cobalt doux

    val nothingTextPrimary = Color(0xFF0D1B3E)    // Bleu marine profond & intense (Texte principal)

    val nothingTextSecondary = Color(0xFF2D4B75)  // Bleu azur moyen vibrant (Texte secondaire)

    val nothingBlack = Color(0xFF0B1B3D)          // Bleu nuit royal intense (Boutons principaux & accents)



    val textFieldColors = OutlinedTextFieldDefaults.colors(

        focusedTextColor = Color(0xFF0D1B3E),

        unfocusedTextColor = Color(0xFF0D1B3E),

        focusedBorderColor = Color(0xFF1D4ED8),

        unfocusedBorderColor = Color(0xFFB4CDEB),

        focusedContainerColor = Color(0xFFEDF4FC),

        unfocusedContainerColor = Color(0xFFEDF4FC)

    )



    // BACKGROUND DYNAMIQUE EN APERÇU COMPOSÉ

    val customBgModifier = remember(bgType, pickedColor) {

        when (bgType) {

            WaifuPreferences.TYPE_COLOR -> Modifier.background(Color(pickedColor.toArgb()))

            else -> Modifier.background(Color(0xFF0A1633))

        }

    }



    // --- DIALOGUE SAUVEGARDER PRÉSET ---

    if (showSavePresetDialog) {

        AlertDialog(

            onDismissRequest = { showSavePresetDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = {

                Text(

                    text = strings.savePresetTitle,

                    fontSize = 13.sp,

                    fontFamily = FontFamily.Monospace,

                    fontWeight = FontWeight.Bold,

                    color = nothingTextPrimary

                )

            },

            text = {

                OutlinedTextField(

                    value = presetNameInput,

                    onValueChange = { presetNameInput = it },

                    singleLine = true,

                    colors = textFieldColors,

                    modifier = Modifier.fillMaxWidth()

                )

            },

            confirmButton = {

                Button(

                    onClick = {

                        val cleanName = presetNameInput.trim()

                        if (cleanName.isNotEmpty()) {

                            val isSpine = (selectedCharacter?.engineType == EngineType.SPINE &&

                                !currentSkelPath.endsWith(".model3.json", ignoreCase = true) &&

                                !currentSkelPath.endsWith(".model.json", ignoreCase = true) &&

                                !currentSkelPath.endsWith(".moc3", ignoreCase = true)) ||

                                currentSkelPath.endsWith(".skel", ignoreCase = true) ||

                                currentSkelPath.endsWith(".skel.bytes", ignoreCase = true) ||

                                SpineModelHelper.isSpineFile(currentSkelPath)



                            val currentAtlas = if (isSpine) {

                                selectedVariant?.spineAtlasPath?.ifEmpty { null }

                                    ?: SpineModelHelper.findAtlasFile(File(currentSkelPath).parentFile ?: File(""))?.absolutePath ?: ""

                            } else ""



                            val newPreset = PresetItem(

                                id = "${System.currentTimeMillis()}",

                                name = cleanName,

                                skelPath = currentSkelPath,

                                scale = currentScale,

                                offsetX = currentOffsetX,

                                offsetY = currentOffsetY,

                                selectedCharacterName = selectedCharacter?.name ?: prefs.selectedCharacterName,

                                selectedVariantLabel = selectedVariant?.label ?: prefs.selectedVariantLabel,

                                idleAnimation = idleAnimation,

                                wakeupAnimation = wakeupAnimation,

                                clickAnimation = clickAnimation,

                                isPointerTrackingEnabled = isPointerTrackingEnabled,

                                isTouchZoneCustom = isTouchZoneCustom,

                                touchZoneLeft = touchZoneLeft,

                                touchZoneTop = touchZoneTop,

                                touchZoneWidth = touchZoneWidth,

                                touchZoneHeight = touchZoneHeight,

                                bgType = bgType,

                                bgColor = pickedColor.toArgb(),

                                bgImagePath = prefs.bgImagePath,

                                bgVideoPath = prefs.bgVideoPath,

                                bgScale = prefs.bgScale,

                                bgOffsetX = prefs.bgOffsetX,

                                bgOffsetY = prefs.bgOffsetY,

                                musicPath = prefs.musicPath,

                                isMusicEnabled = prefs.isMusicEnabled,

                                musicVolume = prefs.musicVolume,

                                isClockEnabled = isClockEnabled,

                                clockFont = clockFont,

                                clockColor = clockColor,

                                clockSize = clockSize,

                                clockFormat24h = clockFormat24h,

                                clockShowSeconds = clockShowSeconds,

                                isDateEnabled = isDateEnabled,

                                dateFormat = dateFormat,

                                clockPosition = clockPosition,

                                clockOffsetX = clockOffsetX,

                                clockOffsetY = clockOffsetY,

                                engineType = if (isSpine) WaifuPreferences.ENGINE_SPINE else WaifuPreferences.ENGINE_LIVE2D,

                                spineAtlasPath = currentAtlas,

                                isSpinePma = isSpinePma

                            )

                            prefs.savePreset(newPreset)

                            userPresets = prefs.getPresets()

                            Toast.makeText(context, strings.presetSavedToast(cleanName), Toast.LENGTH_SHORT).show()

                            showSavePresetDialog = false

                            presetNameInput = ""

                        }

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.saveUpper, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showSavePresetDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE SUPPRIMER PRÉSET ---

    if (showDeletePresetDialog && presetToDelete != null) {

        val targetPreset = presetToDelete!!

        AlertDialog(

            onDismissRequest = { showDeletePresetDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.deletePresetTitle, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = { Text(strings.deletePresetConfirm(targetPreset.name), fontSize = 11.sp, color = nothingTextSecondary) },

            confirmButton = {

                Button(

                    onClick = {

                        prefs.deletePreset(targetPreset.id)

                        userPresets = prefs.getPresets()

                        Toast.makeText(context, strings.presetDeletedToast, Toast.LENGTH_SHORT).show()

                        showDeletePresetDialog = false

                        presetToDelete = null

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9383A)),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.deleteUpper, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showDeletePresetDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- POPUP / DIALOGUE DE DÉPLACEMENT ET RE-DIMENSIONNEMENT PAR CURSEURS ---

    if (showTransformDialog) {

        AlertDialog(

            onDismissRequest = { showTransformDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = {

                Text(

                    text = strings.adjustCharacterTitle,

                    fontSize = 13.sp,

                    fontFamily = FontFamily.Monospace,

                    fontWeight = FontWeight.Bold,

                    color = nothingTextPrimary

                )

            },

            text = {

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    verticalArrangement = Arrangement.spacedBy(14.dp)

                ) {

                    // CURSEUR ZOOM / ÉCHELLE (JUSQU'À 600%)

                    Column {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(strings.zoom, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)

                            Text("${(currentScale * 100).toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                        }

                        Slider(

                            value = currentScale,

                            onValueChange = { currentScale = it },

                            valueRange = 0.2f..6.0f,

                            colors = SliderDefaults.colors(

                                thumbColor = nothingBlack,

                                activeTrackColor = Color(0xFF1D4ED8),

                                inactiveTrackColor = Color(0xFFB4CDEB)

                            )

                        )

                    }



                    // CURSEUR POSITION HORIZONTALE X

                    Column {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(strings.horizontalPosition, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)

                            Text("${currentOffsetX.toInt()} px", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                        }

                        Slider(

                            value = currentOffsetX,

                            onValueChange = { currentOffsetX = it },

                            valueRange = -600f..600f,

                            colors = SliderDefaults.colors(

                                thumbColor = nothingBlack,

                                activeTrackColor = Color(0xFF1D4ED8),

                                inactiveTrackColor = Color(0xFFB4CDEB)

                            )

                        )

                    }



                    // CURSEUR POSITION VERTICALE Y

                    Column {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(strings.verticalPosition, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)

                            Text("${currentOffsetY.toInt()} px", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                        }

                        Slider(

                            value = currentOffsetY,

                            onValueChange = { currentOffsetY = it },

                            valueRange = -800f..800f,

                            colors = SliderDefaults.colors(

                                thumbColor = nothingBlack,

                                activeTrackColor = Color(0xFF1D4ED8),

                                inactiveTrackColor = Color(0xFFB4CDEB)

                            )

                        )

                    }



                    HorizontalDivider(color = nothingBorder)



                    // COMMANDE ZONE TACTILE PERSONNALISÉE

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text(strings.customTouchZoneTitle, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                        Switch(

                            checked = isTouchZoneCustom,

                            onCheckedChange = { enabled ->

                                isTouchZoneCustom = enabled

                                prefs.isTouchZoneCustom = enabled

                                showTouchZoneEditor = enabled

                            }

                        )

                    }



                    if (isTouchZoneCustom) {

                        Column {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(strings.horizontalPosition, fontSize = 10.sp, color = nothingTextSecondary)

                                Text("${(touchZoneLeft * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            }

                            Slider(

                                value = touchZoneLeft,

                                onValueChange = {

                                    touchZoneLeft = it

                                    prefs.touchZoneLeft = it

                                },

                                valueRange = 0.0f..0.9f,

                                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)

                            )

                        }



                        Column {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(strings.verticalPosition, fontSize = 10.sp, color = nothingTextSecondary)

                                Text("${(touchZoneTop * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            }

                            Slider(

                                value = touchZoneTop,

                                onValueChange = {

                                    touchZoneTop = it

                                    prefs.touchZoneTop = it

                                },

                                valueRange = 0.0f..0.9f,

                                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)

                            )

                        }



                        Column {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(strings.touchZoneWidthLabel, fontSize = 10.sp, color = nothingTextSecondary)

                                Text("${(touchZoneWidth * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            }

                            Slider(

                                value = touchZoneWidth,

                                onValueChange = {

                                    touchZoneWidth = it

                                    prefs.touchZoneWidth = it

                                },

                                valueRange = 0.1f..1.0f,

                                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)

                            )

                        }



                        Column {

                            Row(

                                modifier = Modifier.fillMaxWidth(),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Text(strings.touchZoneHeightLabel, fontSize = 10.sp, color = nothingTextSecondary)

                                Text("${(touchZoneHeight * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            }

                            Slider(

                                value = touchZoneHeight,

                                onValueChange = {

                                    touchZoneHeight = it

                                    prefs.touchZoneHeight = it

                                },

                                valueRange = 0.1f..1.0f,

                                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)

                            )

                        }

                    }

                }

            },

            confirmButton = {

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Button(

                        onClick = { showTransformDialog = false },

                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                        shape = RoundedCornerShape(16.dp)

                    ) {

                        Text(strings.ok, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                    }



                    TextButton(

                        onClick = {

                            currentScale = 1.0f

                            currentOffsetX = 0f

                            currentOffsetY = 0f

                        }

                    ) {

                        Text(strings.reset, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFD9383A))

                    }

                }

            },

            dismissButton = null

        )

    }



    // --- DIALOGUE ACTIVATION RAGEBAIT ---

    if (showRagebaitEnableDialog) {

        AlertDialog(

            onDismissRequest = { showRagebaitEnableDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.ragebaitMode, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = {

                Column {

                    Text(strings.ragebaitPinPrompt, fontSize = 11.sp, color = nothingTextSecondary)

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(

                        value = pinInputText,

                        onValueChange = { pinInputText = it },

                        singleLine = true,

                        colors = textFieldColors,

                        modifier = Modifier.fillMaxWidth()

                    )

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (pinInputText.isNotBlank()) {

                            prefs.ragebaitPin = pinInputText.trim()

                            prefs.isRagebaitEnabled = true

                            isRagebaitEnabled = true

                            Toast.makeText(context, strings.ragebaitEnabledToast, Toast.LENGTH_LONG).show()

                            showRagebaitEnableDialog = false

                            pinInputText = ""

                        } else {

                            Toast.makeText(context, strings.ragebaitInvalidPinToast, Toast.LENGTH_SHORT).show()

                        }

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.activateUpper, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showRagebaitEnableDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE DÉSACTIVATION RAGEBAIT ---

    if (showRagebaitDisableDialog) {

        val remainingMs = ragebaitLockoutTime - System.currentTimeMillis()

        val isLocked = remainingMs > 0L



        AlertDialog(

            onDismissRequest = { showRagebaitDisableDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.disableRagebait, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = {

                Column {

                    if (isLocked) {

                        val remainingMin = (remainingMs / 1000) / 60

                        val remainingSec = (remainingMs / 1000) % 60

                        Text(

                            text = strings.accessLockedText(remainingMin, remainingSec),

                            fontSize = 11.sp,

                            fontWeight = FontWeight.Bold,

                            color = Color(0xFFD9383A)

                        )

                        Spacer(modifier = Modifier.height(8.dp))

                    }

                    Text(strings.ragebaitEnterPin, fontSize = 11.sp, color = nothingTextSecondary)

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(

                        value = pinInputText,

                        onValueChange = { pinInputText = it },

                        singleLine = true,

                        colors = textFieldColors,

                        modifier = Modifier.fillMaxWidth()

                    )

                }

            },

            confirmButton = {

                Button(

                    onClick = {

                        val correctPin = prefs.ragebaitPin

                        if (pinInputText.trim() == correctPin && correctPin.isNotEmpty()) {

                            prefs.isRagebaitEnabled = false

                            prefs.ragebaitLockoutTime = 0L

                            isRagebaitEnabled = false

                            ragebaitLockoutTime = 0L

                            Toast.makeText(context, strings.ragebaitDisabledToast, Toast.LENGTH_SHORT).show()

                            showRagebaitDisableDialog = false

                            pinInputText = ""

                        } else {

                            val lockoutEnd = System.currentTimeMillis() + 15 * 60 * 1000L

                            prefs.ragebaitLockoutTime = lockoutEnd

                            ragebaitLockoutTime = lockoutEnd

                            Toast.makeText(context, strings.ragebaitWrongPinToast, Toast.LENGTH_LONG).show()

                            showRagebaitDisableDialog = false

                            pinInputText = ""

                        }

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.validateUpper, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showRagebaitDisableDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE WORKSHOP DISTANT ---

    if (showWorkshopDialog) {

        WorkshopDialog(

            prefs = prefs,

            onDismiss = { showWorkshopDialog = false },

            onModelApplied = { charItem ->

                updateSelectedCharacterAndVariant(charItem)

                showWorkshopDialog = false

            },

            onTreeUpdated = {

                refreshFileManager()

            }

        )

    }



    // --- DIALOGUE PARAMÈTRES (LANGUE & CONFIGURATION) ---

    if (showSettingsDialog) {

        SettingsDialog(

            prefs = prefs,

            currentLanguage = currentAppLanguage,

            onLanguageChanged = { newLang ->

                currentAppLanguage = newLang

            },

            onDismiss = { showSettingsDialog = false }

        )

    }



    // --- DIALOGUE ARBORESCENCE FICHIERS LOCAUX ---

    if (showFilesDialog) {

        FilesDialog(

            rootNode = rootNode,

            allCharacters = allCharacters,

            selectedCharacterPath = selectedCharacter?.folderPath ?: "",

            appLanguage = currentAppLanguage,

            onDismiss = { showFilesDialog = false },

            onSelectCharacter = { charItem ->

                updateSelectedCharacterAndVariant(charItem)

                showFilesDialog = false

            },

            onNewFolder = {

                targetFolderForNewSub = rootDir

                folderNameInput = ""

                showCreateFolderDialog = true

            },

            onImportZip = {

                targetImportFolder = rootDir

                zipPickerLauncher.launch("application/zip")

            },

            onNewSubFolder = { folderFile ->

                targetFolderForNewSub = folderFile

                folderNameInput = ""

                showCreateFolderDialog = true

            },

            onImportZipToFolder = { folderFile ->

                targetImportFolder = folderFile

                zipPickerLauncher.launch("application/zip")

            },

            onRename = { nodeToRename ->

                selectedNodeForAction = nodeToRename

                renameInputText = nodeToRename.name

                showRenameDialog = true

            },

            onMove = { nodeToMove ->

                selectedNodeForAction = nodeToMove

                showMoveDialog = true

            },

            onDelete = { nodeToDelete ->

                selectedNodeForAction = nodeToDelete

                showDeleteDialog = true

            }

        )

    }



    // --- DIALOGUE CRÉATION DOSSIER ---

    if (showCreateFolderDialog) {

        AlertDialog(

            onDismissRequest = { showCreateFolderDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.newFolderTitle, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = {

                OutlinedTextField(

                    value = folderNameInput,

                    onValueChange = { folderNameInput = it },

                    singleLine = true,

                    colors = textFieldColors,

                    modifier = Modifier.fillMaxWidth()

                )

            },

            confirmButton = {

                Button(

                    onClick = {

                        if (folderNameInput.isNotBlank()) {

                            val success = CharacterRepository.createFolder(targetFolderForNewSub, folderNameInput)

                            if (success) {

                                refreshFileManager()

                                Toast.makeText(context, strings.folderCreatedToast, Toast.LENGTH_SHORT).show()

                            }

                        }

                        showCreateFolderDialog = false

                        folderNameInput = ""

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.create, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showCreateFolderDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE IMPORTATION ZIP ---

    if (showImportDialog) {

        AlertDialog(

            onDismissRequest = { showImportDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.nameCharacterTitle, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = {

                OutlinedTextField(

                    value = renameInputText,

                    onValueChange = { renameInputText = it },

                    singleLine = true,

                    colors = textFieldColors,

                    modifier = Modifier.fillMaxWidth()

                )

            },

            confirmButton = {

                Button(

                    onClick = {

                        val uri = pendingZipUri

                        val nameToImport = renameInputText.trim()

                        if (uri != null && nameToImport.isNotEmpty()) {

                            try {

                                val inputStream = context.contentResolver.openInputStream(uri)

                                if (inputStream != null) {

                                    val success = CharacterRepository.importZipToDirectory(

                                        targetParentDir = targetImportFolder,

                                        inputStream = inputStream,

                                        zipName = pendingZipName,

                                        customDisplayName = nameToImport

                                    )

                                    if (success) {

                                        refreshFileManager()

                                        Toast.makeText(context, strings.charImportedToast, Toast.LENGTH_SHORT).show()

                                    } else {

                                        Toast.makeText(context, strings.invalidZipToast, Toast.LENGTH_LONG).show()

                                    }

                                }

                            } catch (_: Exception) {

                                Toast.makeText(context, strings.importErrorToast, Toast.LENGTH_SHORT).show()

                            }

                        }

                        showImportDialog = false

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.importUpper, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showImportDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE RENOMMER ---

    if (showRenameDialog && selectedNodeForAction != null) {

        val targetNode = selectedNodeForAction!!

        AlertDialog(

            onDismissRequest = { showRenameDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.renameTitle, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = {

                OutlinedTextField(

                    value = renameInputText,

                    onValueChange = { renameInputText = it },

                    singleLine = true,

                    colors = textFieldColors,

                    modifier = Modifier.fillMaxWidth()

                )

            },

            confirmButton = {

                Button(

                    onClick = {

                        val file = File(targetNode.absolutePath)

                        if (renameInputText.isNotBlank()) {

                            CharacterRepository.renameNode(file, renameInputText)

                            refreshFileManager()

                            Toast.makeText(context, strings.nameUpdatedToast, Toast.LENGTH_SHORT).show()

                        }

                        showRenameDialog = false

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.saveUpper, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showRenameDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE SUPPRIMER ---

    if (showDeleteDialog && selectedNodeForAction != null) {

        val targetNode = selectedNodeForAction!!

        AlertDialog(

            onDismissRequest = { showDeleteDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.deleteUpper, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = { Text(strings.deleteConfirm(targetNode.name), fontSize = 11.sp, color = nothingTextSecondary) },

            confirmButton = {

                Button(

                    onClick = {

                        val file = File(targetNode.absolutePath)

                        CharacterRepository.deleteNode(file)

                        refreshFileManager()

                        Toast.makeText(context, strings.deletedToast, Toast.LENGTH_SHORT).show()

                        showDeleteDialog = false

                    },

                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9383A)),

                    shape = RoundedCornerShape(16.dp)

                ) {

                    Text(strings.deleteUpper, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showDeleteDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE DÉPLACER VERS N'IMPORTE QUEL SOUS-DOSSIER ---

    if (showMoveDialog && selectedNodeForAction != null) {

        val targetNode = selectedNodeForAction!!

        val allAvailableFolders = remember { CharacterRepository.getAllFolders(context, rootDir) }



        AlertDialog(

            onDismissRequest = { showMoveDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.moveTitle(targetNode.name), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = {

                Column(modifier = Modifier.fillMaxWidth()) {

                    Text(strings.chooseDestFolder, fontSize = 11.sp, color = nothingTextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))



                    LazyColumn(

                        modifier = Modifier

                            .fillMaxWidth()

                            .heightIn(max = 240.dp),

                        verticalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        items(allAvailableFolders) { folder ->

                            val isRoot = folder.absolutePath == rootDir.absolutePath

                            val folderLabel = if (isRoot) strings.rootBadge else strings.folderPrefix(folder.relativeTo(rootDir).path)



                            Button(

                                onClick = {

                                    CharacterRepository.moveNode(File(targetNode.absolutePath), folder)

                                    refreshFileManager()

                                    showMoveDialog = false

                                    Toast.makeText(context, strings.movedToFolderToast(folderLabel), Toast.LENGTH_SHORT).show()

                                },

                                colors = ButtonDefaults.buttonColors(

                                    containerColor = if (isRoot) nothingBlack else Color(0xFFD2E3F5),

                                    contentColor = if (isRoot) Color.White else nothingBlack

                                ),

                                shape = RoundedCornerShape(12.dp),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Text(folderLabel, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                            }

                        }

                    }

                }

            },

            confirmButton = {},

            dismissButton = {

                TextButton(onClick = { showMoveDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE SÉLECTION FOND IMAGE OU VIDÉO ---

    if (showBgMediaPickerDialog) {

        AlertDialog(

            onDismissRequest = { showBgMediaPickerDialog = false },

            containerColor = nothingSurface,

            shape = RoundedCornerShape(24.dp),

            title = { Text(strings.bgMediaTitle, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },

            text = {

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    verticalArrangement = Arrangement.spacedBy(10.dp)

                ) {

                    Button(

                        onClick = {

                            showBgMediaPickerDialog = false

                            imagePickerLauncher.launch("image/*")

                        },

                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                        shape = RoundedCornerShape(14.dp),

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        Text(strings.chooseImage, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                    }



                    Button(

                        onClick = {

                            showBgMediaPickerDialog = false

                            videoPickerLauncher.launch("video/*")

                        },

                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                        shape = RoundedCornerShape(14.dp),

                        modifier = Modifier.fillMaxWidth()

                    ) {

                        Text(strings.chooseVideo, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                    }



                    if (bgType != WaifuPreferences.TYPE_NONE) {

                        OutlinedButton(

                            onClick = {

                                clearOldBgFiles()

                                bgType = WaifuPreferences.TYPE_NONE

                                prefs.bgType = WaifuPreferences.TYPE_NONE

                                showBgMediaPickerDialog = false

                                Toast.makeText(context, strings.bgResetToast, Toast.LENGTH_SHORT).show()

                            },

                            shape = RoundedCornerShape(14.dp),

                            border = BorderStroke(1.dp, nothingBorder),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Text(strings.clearBgMedia, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = nothingTextPrimary, fontWeight = FontWeight.Bold)

                        }

                    }

                }

            },

            confirmButton = {},

            dismissButton = {

                TextButton(onClick = { showBgMediaPickerDialog = false }) {

                    Text(strings.cancelUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)

                }

            }

        )

    }



    // --- DIALOGUE CHOIX DE POLICE DE CARACTÈRES (THÈME CLAIR) ---

    if (showFontDialog) {

        Dialog(onDismissRequest = { showFontDialog = false }) {

            Surface(

                shape = RoundedCornerShape(24.dp),

                color = nothingSurface,

                border = BorderStroke(1.dp, nothingBorder),

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(8.dp)

            ) {

                Column(

                    modifier = Modifier.padding(18.dp),

                    verticalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text(

                            text = strings.changeFontTitle,

                            fontSize = 13.sp,

                            fontFamily = FontFamily.Monospace,

                            fontWeight = FontWeight.Bold,

                            color = nothingTextPrimary

                        )

                        IconButton(onClick = { showFontDialog = false }, modifier = Modifier.size(28.dp)) {

                            Text("✕", fontSize = 16.sp, color = nothingTextPrimary, fontWeight = FontWeight.Bold)

                        }

                    }



                    HorizontalDivider(color = nothingBorder)



                    val fontsList = remember {

                        listOf(

                            WaifuPreferences.FONT_MONOSPACE to "Digital (Monospace)",

                            WaifuPreferences.FONT_SANS_SERIF to "Sans-Serif",

                            WaifuPreferences.FONT_SERIF to "Serif",

                            WaifuPreferences.FONT_THIN to "Thin (Fine)",

                            WaifuPreferences.FONT_CONDENSED to "Condensed",

                            WaifuPreferences.FONT_CURSIVE to "Cursive",

                            WaifuPreferences.FONT_CASUAL to "Casual"

                        )

                    }



                    Column(

                        verticalArrangement = Arrangement.spacedBy(8.dp)

                    ) {

                        fontsList.forEach { (fontKey, fontLabel) ->

                            val isSelected = clockFont == fontKey

                            Surface(

                                onClick = {

                                    clockFont = fontKey

                                    prefs.clockFont = fontKey

                                    showFontDialog = false

                                },

                                shape = RoundedCornerShape(14.dp),

                                color = if (isSelected) nothingBlack else Color(0xFFEDF4FC),

                                border = BorderStroke(1.dp, nothingBorder),

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                Row(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .padding(horizontal = 14.dp, vertical = 10.dp),

                                    horizontalArrangement = Arrangement.SpaceBetween,

                                    verticalAlignment = Alignment.CenterVertically

                                ) {

                                    Text(

                                        text = fontLabel,

                                        fontSize = 12.sp,

                                        fontFamily = FontFamily.Monospace,

                                        fontWeight = FontWeight.Bold,

                                        color = if (isSelected) Color.White else nothingTextPrimary

                                    )

                                    Text(

                                        text = "11:01",

                                        fontSize = 18.sp,

                                        fontFamily = when (fontKey) {

                                            WaifuPreferences.FONT_MONOSPACE -> FontFamily.Monospace

                                            WaifuPreferences.FONT_SERIF -> FontFamily.Serif

                                            WaifuPreferences.FONT_SANS_SERIF -> FontFamily.SansSerif

                                            WaifuPreferences.FONT_THIN -> FontFamily.SansSerif

                                            WaifuPreferences.FONT_CONDENSED -> FontFamily.SansSerif

                                            WaifuPreferences.FONT_CURSIVE -> FontFamily.Cursive

                                            WaifuPreferences.FONT_CASUAL -> FontFamily.Default

                                            else -> FontFamily.Monospace

                                        },

                                        fontWeight = when (fontKey) {

                                            WaifuPreferences.FONT_THIN -> FontWeight.Thin

                                            WaifuPreferences.FONT_CONDENSED -> FontWeight.Medium

                                            WaifuPreferences.FONT_CURSIVE -> FontWeight.Normal

                                            else -> FontWeight.Bold

                                        },

                                        color = if (isSelected) Color.White else nothingTextPrimary

                                    )

                                }

                            }

                        }

                    }

                }

            }

        }

    }



    // --- DIALOGUE PERSONNALISER LES COULEURS (THÈME CLAIR CONFORME À LA DA) ---

    if (showColorDialog) {

        var hsvHue by remember {

            val hsv = FloatArray(3)

            AndroidColor.colorToHSV(clockColor, hsv)

            mutableFloatStateOf(hsv[0])

        }

        var hsvSat by remember {

            val hsv = FloatArray(3)

            AndroidColor.colorToHSV(clockColor, hsv)

            mutableFloatStateOf(hsv[1])

        }

        var hsvVal by remember {

            val hsv = FloatArray(3)

            AndroidColor.colorToHSV(clockColor, hsv)

            mutableFloatStateOf(hsv[2])

        }



        val currentHsvColor = remember(hsvHue, hsvSat, hsvVal) {

            AndroidColor.HSVToColor(floatArrayOf(hsvHue, hsvSat, hsvVal))

        }



        val hueActiveColor = remember(hsvHue) {

            Color(AndroidColor.HSVToColor(floatArrayOf(hsvHue, 1f, 1f)))

        }



        val currentSelectedColor = remember(currentHsvColor) {

            Color(currentHsvColor)

        }



        @OptIn(ExperimentalMaterial3Api::class)

        val createColorThumb: @Composable (Color) -> @Composable (SliderState) -> Unit = { activeDotColor ->

            {

                Box(

                    modifier = Modifier

                        .size(24.dp)

                        .clip(CircleShape)

                        .background(Color.White)

                        .border(1.5.dp, nothingBorder, CircleShape),

                    contentAlignment = Alignment.Center

                ) {

                    Box(

                        modifier = Modifier

                            .size(12.dp)

                            .clip(CircleShape)

                            .background(activeDotColor)

                    )

                }

            }

        }



        Dialog(onDismissRequest = { showColorDialog = false }) {

            Surface(

                shape = RoundedCornerShape(24.dp),

                color = nothingSurface,

                border = BorderStroke(1.dp, nothingBorder),

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(8.dp)

            ) {

                Column(

                    modifier = Modifier.padding(18.dp),

                    verticalArrangement = Arrangement.spacedBy(16.dp)

                ) {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(10.dp)

                    ) {

                        IconButton(

                            onClick = { showColorDialog = false },

                            modifier = Modifier.size(28.dp)

                        ) {

                            Text("✕", fontSize = 18.sp, color = nothingTextPrimary, fontWeight = FontWeight.Bold)

                        }

                        Text(

                            text = strings.customizeColorsTitle,

                            fontSize = 13.sp,

                            fontFamily = FontFamily.Monospace,

                            fontWeight = FontWeight.Bold,

                            color = nothingTextPrimary

                        )

                    }



                    HorizontalDivider(color = nothingBorder)



                    // 1. TEINTE (Hue Rainbow)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        Text(strings.hue, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)



                        val rainbowBrush = remember {

                            Brush.horizontalGradient(

                                colors = listOf(

                                    Color.Red, Color.Yellow, Color.Green,

                                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red

                                )

                            )

                        }



                        Box(

                            modifier = Modifier

                                .fillMaxWidth()

                                .height(26.dp)

                                .clip(RoundedCornerShape(13.dp))

                                .background(rainbowBrush)

                        ) {

                            Slider(

                                value = hsvHue,

                                onValueChange = { hsvHue = it },

                                valueRange = 0f..360f,

                                thumb = createColorThumb(hueActiveColor),

                                colors = SliderDefaults.colors(

                                    activeTrackColor = Color.Transparent,

                                    inactiveTrackColor = Color.Transparent

                                ),

                                modifier = Modifier.fillMaxSize()

                            )

                        }

                    }



                    // 2. SATURATION

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        Text(strings.saturation, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)



                        val satBrush = remember(hsvHue, hsvVal) {

                            val cMin = Color(AndroidColor.HSVToColor(floatArrayOf(hsvHue, 0f, hsvVal)))

                            val cMax = Color(AndroidColor.HSVToColor(floatArrayOf(hsvHue, 1f, hsvVal)))

                            Brush.horizontalGradient(listOf(cMin, cMax))

                        }



                        Box(

                            modifier = Modifier

                                .fillMaxWidth()

                                .height(26.dp)

                                .clip(RoundedCornerShape(13.dp))

                                .background(satBrush)

                        ) {

                            Slider(

                                value = hsvSat,

                                onValueChange = { hsvSat = it },

                                valueRange = 0f..1f,

                                thumb = createColorThumb(currentSelectedColor),

                                colors = SliderDefaults.colors(

                                    activeTrackColor = Color.Transparent,

                                    inactiveTrackColor = Color.Transparent

                                ),

                                modifier = Modifier.fillMaxSize()

                            )

                        }

                    }



                    // 3. CLARTÉ

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        Text(strings.brightness, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)



                        val valBrush = remember(hsvHue, hsvSat) {

                            val cMin = Color.Black

                            val cMax = Color(AndroidColor.HSVToColor(floatArrayOf(hsvHue, hsvSat, 1f)))

                            Brush.horizontalGradient(listOf(cMin, cMax))

                        }



                        Box(

                            modifier = Modifier

                                .fillMaxWidth()

                                .height(26.dp)

                                .clip(RoundedCornerShape(13.dp))

                                .background(valBrush)

                        ) {

                            Slider(

                                value = hsvVal,

                                onValueChange = { hsvVal = it },

                                valueRange = 0f..1f,

                                thumb = createColorThumb(currentSelectedColor),

                                colors = SliderDefaults.colors(

                                    activeTrackColor = Color.Transparent,

                                    inactiveTrackColor = Color.Transparent

                                ),

                                modifier = Modifier.fillMaxSize()

                            )

                        }

                    }



                    Spacer(modifier = Modifier.height(4.dp))



                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            Box(

                                modifier = Modifier

                                    .size(32.dp)

                                    .clip(CircleShape)

                                    .background(Color(currentHsvColor))

                                    .border(1.5.dp, nothingBorder, CircleShape)

                            )

                            Text(

                                text = String.format("#%06X", 0xFFFFFF and currentHsvColor),

                                fontSize = 12.sp,

                                fontFamily = FontFamily.Monospace,

                                fontWeight = FontWeight.Bold,

                                color = nothingTextPrimary

                            )

                        }



                        Button(

                            onClick = {

                                clockColor = currentHsvColor

                                prefs.clockColor = currentHsvColor

                                showColorDialog = false

                            },

                            colors = ButtonDefaults.buttonColors(containerColor = nothingBlack, contentColor = Color.White),

                            shape = RoundedCornerShape(16.dp)

                        ) {

                            Text(strings.applyUpper, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                        }

                    }

                }

            }

        }

    }



    val density = LocalDensity.current



    // CONTENEUR RACINE

    Box(

        modifier = modifier

            .fillMaxSize()

            .onSizeChanged { rootSize = it }

    ) {

        // COUCHE 1 : ÉCRAN PRINCIPAL D'ÉDITION & APERÇU DANS LA CARTE

        Column(

            modifier = Modifier

                .fillMaxSize()

                .verticalScroll(rememberScrollState())

                .background(nothingBg)

        ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .statusBarsPadding()

                    .padding(horizontal = 20.dp, vertical = 10.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(

                        text = "Azur Wallpaper",

                        fontSize = 12.sp,

                        fontFamily = FontFamily.Monospace,

                        fontWeight = FontWeight.Bold,

                        color = nothingTextSecondary

                    )

                    val displayCharName = when (val name = selectedCharacter?.name) {

                        null, "", "Compagnon", "Companion" -> strings.defaultCharacterName

                        else -> name

                    }

                    Text(

                        text = displayCharName,

                        style = MaterialTheme.typography.titleLarge,

                        fontWeight = FontWeight.Bold,

                        color = nothingTextPrimary,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis

                    )

                }



                Spacer(modifier = Modifier.width(12.dp))



                // Bouton Paramètres (carré avec angles arrondis et engrenage au centre)

                Surface(

                    onClick = { showSettingsDialog = true },

                    shape = RoundedCornerShape(12.dp),

                    color = nothingSurface,

                    border = BorderStroke(1.2.dp, nothingBorder),

                    modifier = Modifier.size(42.dp)

                ) {

                    Box(

                        contentAlignment = Alignment.Center,

                        modifier = Modifier.fillMaxSize()

                    ) {

                        Text(

                            text = "⚙",

                            fontSize = 20.sp,

                            color = nothingTextPrimary

                        )

                    }

                }

            }



            // Zone d'Aperçu Carte (Taille Fixe 380dp - Défilement synchrone natif à 100%)

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .height(380.dp)

                    .padding(horizontal = 16.dp)

                    .clip(RoundedCornerShape(28.dp))

                    .clipToBounds()

                    .then(customBgModifier)

                    .border(1.dp, nothingBorder, RoundedCornerShape(28.dp))

                    .then(live2DGestureModifier)

            ) {

                WallpaperPreviewBackground(

                    bgType = bgType,

                    pickedColor = pickedColor,

                    bgImagePath = prefs.bgImagePath,

                    bgVideoPath = prefs.bgVideoPath,

                    bgScale = prefs.bgScale,

                    bgOffsetX = prefs.bgOffsetX,

                    bgOffsetY = prefs.bgOffsetY,

                    isVideoMuted = isVideoMuted

                )



                if (clockPosition == WaifuPreferences.CLOCK_POS_BACK) {

                    DigitalClockOverlay(

                        isClockEnabled = isClockEnabled,

                        clockFont = clockFont,

                        clockColor = clockColor,

                        clockSizeSp = clockSize,

                        clockFormat24h = clockFormat24h,

                        clockShowSeconds = clockShowSeconds,

                        isDateEnabled = isDateEnabled,

                        dateFormat = dateFormat,

                        clockOffsetXPx = clockOffsetX,

                        clockOffsetYPx = clockOffsetY,

                        isEn = strings.isEn,

                        onPositionDrag = onClockDrag,

                        modifier = Modifier.zIndex(0f)

                    )

                }



                val isSpineModel = (selectedCharacter?.engineType == EngineType.SPINE &&

                    !currentSkelPath.endsWith(".model3.json", ignoreCase = true) &&

                    !currentSkelPath.endsWith(".model.json", ignoreCase = true) &&

                    !currentSkelPath.endsWith(".moc3", ignoreCase = true) &&

                    !currentSkelPath.endsWith(".moc", ignoreCase = true)) ||

                    currentSkelPath.endsWith(".skel", ignoreCase = true) ||

                    currentSkelPath.endsWith(".skel.bytes", ignoreCase = true) ||

                    SpineModelHelper.isSpineFile(currentSkelPath)



                val currentAtlas = if (isSpineModel) {

                    selectedVariant?.spineAtlasPath?.ifEmpty { null }

                        ?: prefs.spineAtlasPath.ifEmpty { null }

                        ?: SpineModelHelper.findAtlasFile(File(currentSkelPath).parentFile ?: File(""))?.absolutePath ?: ""

                } else ""

                val isCardActive = !isFullscreen



                Live2DWebView(

                    modelJsonPath = if (!isSpineModel) currentSkelPath else "",

                    scale = currentScale,

                    offsetX = currentOffsetX,

                    offsetY = currentOffsetY,

                    idleAnimation = idleAnimation,

                    isPointerTrackingEnabled = isPointerTrackingEnabled,

                    doubleTapTrigger = doubleTapTrigger,

                    cornerRadiusPx = with(density) { 28.dp.toPx() },

                    isAnimating = false,

                    isActive = isCardActive && !isSpineModel,

                    modifier = Modifier

                        .fillMaxSize()

                        .zIndex(if (!isSpineModel) 2f else 1f)

                )



                SpineWebView(

                    modelPath = if (isSpineModel) currentSkelPath else "",

                    atlasPath = if (isSpineModel) currentAtlas else "",

                    scale = currentScale,

                    offsetX = currentOffsetX,

                    offsetY = currentOffsetY,

                    idleAnimation = idleAnimation,

                    clickAnimation = clickAnimation,

                    isPma = isSpinePma,

                    isPointerTrackingEnabled = isPointerTrackingEnabled,

                    doubleTapTrigger = doubleTapTrigger,

                    cornerRadiusPx = with(density) { 28.dp.toPx() },

                    isAnimating = false,

                    isActive = isCardActive && isSpineModel,

                    modifier = Modifier

                        .fillMaxSize()

                        .zIndex(if (isSpineModel) 2f else 1f)

                )



                if (clockPosition == WaifuPreferences.CLOCK_POS_FRONT) {

                    DigitalClockOverlay(

                        isClockEnabled = isClockEnabled,

                        clockFont = clockFont,

                        clockColor = clockColor,

                        clockSizeSp = clockSize,

                        clockFormat24h = clockFormat24h,

                        clockShowSeconds = clockShowSeconds,

                        isDateEnabled = isDateEnabled,

                        dateFormat = dateFormat,

                        clockOffsetXPx = clockOffsetX,

                        clockOffsetYPx = clockOffsetY,

                        isEn = strings.isEn,

                        onPositionDrag = onClockDrag,

                        modifier = Modifier.zIndex(3f)

                    )

                }



                // Boutons d'Aperçu Carte (Plein Écran & Sauvegarder)

                Box(modifier = Modifier.fillMaxSize().zIndex(5f)) {

                    Row(

                        modifier = Modifier

                            .align(Alignment.BottomEnd)

                            .padding(bottom = 12.dp, end = 12.dp),

                        horizontalArrangement = Arrangement.spacedBy(8.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Surface(

                            onClick = { isFullscreen = true },

                            shape = CircleShape,

                            color = Color(0xFFEDF4FC).copy(alpha = 0.95f),

                            border = BorderStroke(1.dp, nothingBorder),

                            modifier = Modifier.size(36.dp)

                        ) {

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier.fillMaxSize()

                            ) {

                                Canvas(modifier = Modifier.size(16.dp)) {

                                    val w = size.width

                                    val h = size.height

                                    val strokeW = 2.dp.toPx()



                                    drawLine(Color(0xFF0D1B3E), Offset(w * 0.45f, h * 0.45f), Offset(0f, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)

                                    drawLine(Color(0xFF0D1B3E), Offset(0f, 0f), Offset(w * 0.32f, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)

                                    drawLine(Color(0xFF0D1B3E), Offset(0f, 0f), Offset(0f, h * 0.32f), strokeWidth = strokeW, cap = StrokeCap.Round)



                                    drawLine(Color(0xFF0D1B3E), Offset(w * 0.55f, h * 0.55f), Offset(w, h), strokeWidth = strokeW, cap = StrokeCap.Round)

                                    drawLine(Color(0xFF0D1B3E), Offset(w, h), Offset(w * 0.68f, h), strokeWidth = strokeW, cap = StrokeCap.Round)

                                    drawLine(Color(0xFF0D1B3E), Offset(w, h), Offset(w, h * 0.68f), strokeWidth = strokeW, cap = StrokeCap.Round)

                                }

                            }

                        }



                        Surface(

                            onClick = { showSavePresetDialog = true },

                            shape = CircleShape,

                            color = Color(0xFFEDF4FC).copy(alpha = 0.95f),

                            border = BorderStroke(1.dp, nothingBorder)

                        ) {

                            Text(

                                text = strings.saveSmall,

                                color = Color(0xFF0D1B3E),

                                fontSize = 11.sp,

                                fontFamily = FontFamily.Monospace,

                                fontWeight = FontWeight.Bold,

                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

                            )

                        }

                    }

                }

            }



            Spacer(modifier = Modifier.height(10.dp))



            Spacer(modifier = Modifier.height(10.dp))



            // CARTE PRINCIPALE UNIFIÉE (STYLE SCHÉMATISÉ EXACTEMENT COMME LE DESSIN)

            Card(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp),

                colors = CardDefaults.cardColors(containerColor = nothingSurface),

                shape = RoundedCornerShape(28.dp),

                border = BorderStroke(1.2.dp, nothingBorder)

            ) {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(14.dp)

                ) {

                    val tab0Color by animateColorAsState(if (activeTab == 0) Color.White else Color(0xFF222222), animationSpec = tween(160), label = "tab0Color")

                    val tab1Color by animateColorAsState(if (activeTab == 1) Color.White else Color(0xFF222222), animationSpec = tween(160), label = "tab1Color")

                    val tab2Color by animateColorAsState(if (activeTab == 2) Color.White else Color(0xFF222222), animationSpec = tween(160), label = "tab2Color")

                    val tab3Color by animateColorAsState(if (activeTab == 3) Color.White else Color(0xFF222222), animationSpec = tween(160), label = "tab3Color")



                    var barWidthDp by remember { mutableStateOf(0.dp) }

                    val density = LocalDensity.current

                    val snappyCurve = remember { CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f) }



                    val animatedIndicatorIndex by animateFloatAsState(

                        targetValue = activeTab.toFloat(),

                        animationSpec = tween(160, easing = snappyCurve),

                        label = "tabIndicatorPosition"

                    )



                    // PISTE INTÉRIEURE DES ONGLETS (CADRE ARRONDI CONFORME AU SCHÉMA)

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(52.dp)

                            .onSizeChanged { size ->

                                barWidthDp = with(density) { size.width.toDp() }

                            }

                            .clip(RoundedCornerShape(20.dp))

                            .background(Color.White)

                            .border(1.2.dp, nothingBorder, RoundedCornerShape(20.dp))

                            .padding(4.dp)

                    ) {

                        if (barWidthDp > 0.dp) {

                            val availableWidth = barWidthDp - 8.dp

                            val tabWidth = availableWidth / 4f

                            val currentOffsetX = tabWidth * animatedIndicatorIndex



                            Box(

                                modifier = Modifier

                                    .offset(x = currentOffsetX)

                                    .width(tabWidth)

                                    .fillMaxHeight()

                                    .clip(RoundedCornerShape(16.dp))

                                    .background(nothingBlack)

                            )

                        }



                        Row(

                            modifier = Modifier.fillMaxSize(),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            // ONGLET 0: PERSONNAGE

                            Box(

                                modifier = Modifier

                                    .weight(1f)

                                    .fillMaxHeight()

                                    .clip(RoundedCornerShape(16.dp))

                                    .clickable(

                                        interactionSource = remember { MutableInteractionSource() },

                                        indication = null

                                    ) {

                                        activeTab = 0

                                    },

                                contentAlignment = Alignment.Center

                            ) {

                                Canvas(modifier = Modifier.size(20.dp)) {

                                    val color = tab0Color

                                    val strokeW = 2.dp.toPx()

                                    val w = size.width

                                    val h = size.height



                                    drawCircle(

                                        color = color,

                                        radius = w * 0.24f,

                                        center = Offset(w * 0.5f, h * 0.35f),

                                        style = Stroke(width = strokeW)

                                    )



                                    drawArc(

                                        color = color,

                                        startAngle = 190f,

                                        sweepAngle = 160f,

                                        useCenter = false,

                                        topLeft = Offset(w * 0.12f, h * 0.58f),

                                        size = Size(w * 0.76f, h * 0.65f),

                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)

                                    )

                                }

                            }



                            // ONGLET 1: FOND

                            Box(

                                modifier = Modifier

                                    .weight(1f)

                                    .fillMaxHeight()

                                    .clip(RoundedCornerShape(16.dp))

                                    .clickable(

                                        interactionSource = remember { MutableInteractionSource() },

                                        indication = null

                                    ) {

                                        activeTab = 1

                                    },

                                contentAlignment = Alignment.Center

                            ) {

                                Canvas(modifier = Modifier.size(20.dp)) {

                                    val color = tab1Color

                                    val strokeW = 2.dp.toPx()

                                    val w = size.width

                                    val h = size.height



                                    drawRoundRect(

                                        color = color,

                                        topLeft = Offset(strokeW * 0.5f, strokeW * 0.5f),

                                        size = Size(w - strokeW, h - strokeW),

                                        cornerRadius = CornerRadius(4.dp.toPx()),

                                        style = Stroke(width = strokeW)

                                    )



                                    drawCircle(

                                        color = color,

                                        radius = w * 0.12f,

                                        center = Offset(w * 0.35f, h * 0.38f)

                                    )



                                    val path = Path().apply {

                                        moveTo(w * 0.12f, h * 0.85f)

                                        lineTo(w * 0.45f, h * 0.52f)

                                        lineTo(w * 0.62f, h * 0.68f)

                                        lineTo(w * 0.78f, h * 0.55f)

                                        lineTo(w * 0.88f, h * 0.85f)

                                    }

                                    drawPath(

                                        path = path,

                                        color = color,

                                        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)

                                    )

                                }

                            }



                            // ONGLET 2: HORLOGE

                            Box(

                                modifier = Modifier

                                    .weight(1f)

                                    .fillMaxHeight()

                                    .clip(RoundedCornerShape(16.dp))

                                    .clickable(

                                        interactionSource = remember { MutableInteractionSource() },

                                        indication = null

                                    ) {

                                        activeTab = 2

                                    },

                                contentAlignment = Alignment.Center

                            ) {

                                Canvas(modifier = Modifier.size(20.dp)) {

                                    val color = tab2Color

                                    val strokeW = 2.dp.toPx()

                                    val w = size.width

                                    val h = size.height



                                    drawCircle(

                                        color = color,

                                        radius = (w - strokeW) * 0.5f,

                                        center = Offset(w * 0.5f, h * 0.5f),

                                        style = Stroke(width = strokeW)

                                    )



                                    drawLine(

                                        color = color,

                                        start = Offset(w * 0.5f, h * 0.5f),

                                        end = Offset(w * 0.5f, h * 0.28f),

                                        strokeWidth = strokeW,

                                        cap = StrokeCap.Round

                                    )



                                    drawLine(

                                        color = color,

                                        start = Offset(w * 0.5f, h * 0.5f),

                                        end = Offset(w * 0.72f, h * 0.5f),

                                        strokeWidth = strokeW,

                                        cap = StrokeCap.Round

                                    )

                                }

                            }



                            // ONGLET 3: PLANIFICATION

                            Box(

                                modifier = Modifier

                                    .weight(1f)

                                    .fillMaxHeight()

                                    .clip(RoundedCornerShape(16.dp))

                                    .clickable(

                                        interactionSource = remember { MutableInteractionSource() },

                                        indication = null

                                    ) {

                                        activeTab = 3

                                    },

                                contentAlignment = Alignment.Center

                            ) {

                                Canvas(modifier = Modifier.size(20.dp)) {

                                    val color = tab3Color

                                    val strokeW = 2.dp.toPx()

                                    val w = size.width

                                    val h = size.height



                                    drawRoundRect(

                                        color = color,

                                        topLeft = Offset(strokeW * 0.5f, h * 0.18f),

                                        size = Size(w - strokeW, h * 0.8f - strokeW),

                                        cornerRadius = CornerRadius(4.dp.toPx()),

                                        style = Stroke(width = strokeW)

                                    )



                                    drawLine(

                                        color = color,

                                        start = Offset(strokeW * 0.5f, h * 0.42f),

                                        end = Offset(w - strokeW * 0.5f, h * 0.42f),

                                        strokeWidth = strokeW

                                    )



                                    drawLine(

                                        color = color,

                                        start = Offset(w * 0.3f, h * 0.08f),

                                        end = Offset(w * 0.3f, h * 0.25f),

                                        strokeWidth = strokeW,

                                        cap = StrokeCap.Round

                                    )



                                    drawLine(

                                        color = color,

                                        start = Offset(w * 0.7f, h * 0.08f),

                                        end = Offset(w * 0.7f, h * 0.25f),

                                        strokeWidth = strokeW,

                                        cap = StrokeCap.Round

                                    )



                                    drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(w * 0.35f, h * 0.62f))

                                    drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(w * 0.65f, h * 0.62f))

                                    drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(w * 0.35f, h * 0.80f))

                                    drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(w * 0.65f, h * 0.80f))

                                }

                            }

                        }

                    }



                    Spacer(modifier = Modifier.height(10.dp))



                    Crossfade(

                        targetState = activeTab,

                        animationSpec = tween(120, easing = LinearOutSlowInEasing),

                        label = "FastCrossfadeTabTransition",

                        modifier = Modifier.fillMaxWidth()

                    ) { page ->

                        Box(

                            modifier = Modifier

                                .fillMaxWidth()

                                .background(nothingSurface)

                        ) {

                            when (page) {

                                0 -> {

                                    // ONGLET 0: PERSONNAGE

                                    Column(

                                        modifier = Modifier.fillMaxWidth(),

                                        verticalArrangement = Arrangement.spacedBy(12.dp)

                                    ) {

                                        // SECTION PRÉSETS (DIRECTEMENT SUR LE FOND)

                                        Column(

                                            modifier = Modifier.fillMaxWidth(),

                                            verticalArrangement = Arrangement.spacedBy(8.dp)

                                        ) {

                                            Text(

                                                text = strings.presetsLabel,

                                                fontSize = 12.sp,

                                                fontFamily = FontFamily.Monospace,

                                                fontWeight = FontWeight.Bold,

                                                color = nothingTextPrimary

                                            )



                                            if (userPresets.isEmpty()) {

                                                Text(

                                                    text = strings.noPresetHint,

                                                    fontSize = 10.sp,

                                                    color = nothingTextSecondary

                                                )

                                            } else {

                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                                    items(userPresets) { preset ->

                                                        Surface(

                                                            shape = RoundedCornerShape(16.dp),

                                                            color = nothingBlack,

                                                            modifier = Modifier.combinedClickable(

                                                                onClick = {

                                                                    currentSkelPath = preset.skelPath

                                                                    currentScale = preset.scale

                                                                    currentOffsetX = preset.offsetX

                                                                    currentOffsetY = preset.offsetY



                                                                    prefs.skelPath = preset.skelPath

                                                                    prefs.scale = preset.scale

                                                                    prefs.offsetX = preset.offsetX

                                                                    prefs.offsetY = preset.offsetY



                                                                    idleAnimation = preset.idleAnimation

                                                                    wakeupAnimation = preset.wakeupAnimation

                                                                    clickAnimation = preset.clickAnimation

                                                                    isSpinePma = preset.isSpinePma

                                                                    isPointerTrackingEnabled = preset.isPointerTrackingEnabled



                                                                    prefs.idleAnimation = preset.idleAnimation

                                                                    prefs.wakeupAnimation = preset.wakeupAnimation

                                                                    prefs.clickAnimation = preset.clickAnimation

                                                                    prefs.isSpinePma = preset.isSpinePma

                                                                    prefs.isPointerTrackingEnabled = preset.isPointerTrackingEnabled



                                                                    isTouchZoneCustom = preset.isTouchZoneCustom

                                                                    touchZoneLeft = preset.touchZoneLeft

                                                                    touchZoneTop = preset.touchZoneTop

                                                                    touchZoneWidth = preset.touchZoneWidth

                                                                    touchZoneHeight = preset.touchZoneHeight



                                                                    prefs.isTouchZoneCustom = preset.isTouchZoneCustom

                                                                    prefs.touchZoneLeft = preset.touchZoneLeft

                                                                    prefs.touchZoneTop = preset.touchZoneTop

                                                                    prefs.touchZoneWidth = preset.touchZoneWidth

                                                                    prefs.touchZoneHeight = preset.touchZoneHeight



                                                                    bgType = preset.bgType

                                                                    pickedColor = Color(preset.bgColor)



                                                                    prefs.bgType = preset.bgType

                                                                    prefs.bgColor = preset.bgColor

                                                                    prefs.bgImagePath = preset.bgImagePath

                                                                    prefs.bgVideoPath = preset.bgVideoPath

                                                                    prefs.bgScale = preset.bgScale

                                                                    prefs.bgOffsetX = preset.bgOffsetX

                                                                    prefs.bgOffsetY = preset.bgOffsetY



                                                                    musicPath = preset.musicPath

                                                                    isMusicEnabled = preset.isMusicEnabled

                                                                    musicVolume = preset.musicVolume



                                                                    prefs.musicPath = preset.musicPath

                                                                    prefs.isMusicEnabled = preset.isMusicEnabled

                                                                    prefs.musicVolume = preset.musicVolume



                                                                    // APPLICATION DES PARAMÈTRES D'HORLOGE DU PRÉSET

                                                                    isClockEnabled = preset.isClockEnabled

                                                                    clockFont = preset.clockFont

                                                                    clockColor = preset.clockColor

                                                                    clockSize = preset.clockSize

                                                                    clockFormat24h = preset.clockFormat24h

                                                                    clockShowSeconds = preset.clockShowSeconds

                                                                    isDateEnabled = preset.isDateEnabled

                                                                    dateFormat = preset.dateFormat

                                                                    clockPosition = preset.clockPosition

                                                                    clockOffsetX = preset.clockOffsetX

                                                                    clockOffsetY = preset.clockOffsetY



                                                                    prefs.isClockEnabled = preset.isClockEnabled

                                                                    prefs.clockFont = preset.clockFont

                                                                    prefs.clockColor = preset.clockColor

                                                                    prefs.clockSize = preset.clockSize

                                                                    prefs.clockFormat24h = preset.clockFormat24h

                                                                    prefs.clockShowSeconds = preset.clockShowSeconds

                                                                    prefs.isDateEnabled = preset.isDateEnabled

                                                                    prefs.dateFormat = preset.dateFormat

                                                                    prefs.clockPosition = preset.clockPosition

                                                                    prefs.clockOffsetX = preset.clockOffsetX

                                                                    prefs.clockOffsetY = preset.clockOffsetY



                                                                    // Résolution robuste du personnage et du variant pour éviter tout freeze ou écran noir

                                                                    val isSpinePreset = preset.engineType == WaifuPreferences.ENGINE_SPINE ||

                                                                        preset.skelPath.endsWith(".skel", ignoreCase = true) ||

                                                                        preset.skelPath.endsWith(".skel.bytes", ignoreCase = true) ||

                                                                        SpineModelHelper.isSpineFile(preset.skelPath)

                                                                    val finalEngine = if (isSpinePreset) EngineType.SPINE else EngineType.LIVE2D

                                                                    val resolvedAtlas = if (isSpinePreset) {

                                                                        preset.spineAtlasPath.ifEmpty { null }

                                                                            ?: SpineModelHelper.findAtlasFile(File(preset.skelPath).parentFile ?: File(""))?.absolutePath ?: ""

                                                                    } else ""



                                                                    // 1. Recherche exacte par chemin de modèle / moc / json

                                                                    var resolvedChar = allCharacters.firstOrNull { char ->

                                                                        char.variants.any { v ->

                                                                            (v.modelJsonPath.isNotEmpty() && (v.modelJsonPath == preset.skelPath || preset.skelPath.endsWith(v.modelJsonPath))) ||

                                                                            (v.mocPath.isNotEmpty() && (v.mocPath == preset.skelPath || preset.skelPath.endsWith(v.mocPath)))

                                                                        }

                                                                    }



                                                                    // 2. Recherche par dossier parent

                                                                    if (resolvedChar == null && preset.skelPath.isNotEmpty()) {

                                                                        val parentFolder = File(preset.skelPath).parentFile?.absolutePath ?: ""

                                                                        resolvedChar = allCharacters.firstOrNull { char ->

                                                                            char.folderPath.isNotEmpty() && (parentFolder.startsWith(char.folderPath) || char.folderPath.startsWith(parentFolder))

                                                                        }

                                                                    }



                                                                    // 3. Recherche par nom de personnage

                                                                    if (resolvedChar == null && preset.selectedCharacterName.isNotEmpty() && preset.selectedCharacterName != "Compagnon" && preset.selectedCharacterName != "Companion") {

                                                                        resolvedChar = allCharacters.firstOrNull { char ->

                                                                            char.name.equals(preset.selectedCharacterName, ignoreCase = true)

                                                                        }

                                                                    }



                                                                    val charName = preset.selectedCharacterName.ifEmpty {

                                                                        File(preset.skelPath).parentFile?.name ?: strings.defaultCharacterName

                                                                    }

                                                                    val varLabel = preset.selectedVariantLabel.ifEmpty { strings.defaultWord }



                                                                    var resolvedVariant: VariantInfo? = null

                                                                    if (resolvedChar != null) {

                                                                        resolvedVariant = resolvedChar.variants.firstOrNull { v ->

                                                                            v.modelJsonPath == preset.skelPath || v.mocPath == preset.skelPath

                                                                        } ?: resolvedChar.variants.firstOrNull { it.label == preset.selectedVariantLabel }

                                                                          ?: resolvedChar.variants.firstOrNull()

                                                                    }



                                                                    if (resolvedChar == null) {

                                                                        val synthesizedVariant = VariantInfo(

                                                                            label = varLabel,

                                                                            modelJsonPath = if (finalEngine == EngineType.SPINE) "" else preset.skelPath,

                                                                            mocPath = if (finalEngine == EngineType.SPINE) preset.skelPath else "",

                                                                            spineAtlasPath = resolvedAtlas,

                                                                            engineType = finalEngine

                                                                        )

                                                                        resolvedChar = CharacterItem(

                                                                            id = preset.id,

                                                                            name = charName,

                                                                            folderPath = File(preset.skelPath).parentFile?.absolutePath ?: "",

                                                                            isCustom = true,

                                                                            engineType = finalEngine,

                                                                            variants = listOf(synthesizedVariant)

                                                                        )

                                                                        resolvedVariant = synthesizedVariant

                                                                    } else if (resolvedVariant != null && finalEngine == EngineType.SPINE && resolvedVariant.spineAtlasPath.isEmpty() && resolvedAtlas.isNotEmpty()) {

                                                                        resolvedVariant = resolvedVariant.copy(spineAtlasPath = resolvedAtlas)

                                                                    }



                                                                    selectedCharacter = resolvedChar

                                                                    selectedVariant = resolvedVariant



                                                                    prefs.selectedCharacterName = resolvedChar.name

                                                                    prefs.selectedVariantLabel = resolvedVariant?.label ?: varLabel

                                                                    if (finalEngine == EngineType.SPINE) {

                                                                        prefs.engineType = WaifuPreferences.ENGINE_SPINE

                                                                        prefs.spineAtlasPath = resolvedAtlas

                                                                    } else {

                                                                        prefs.engineType = WaifuPreferences.ENGINE_LIVE2D

                                                                        prefs.spineAtlasPath = ""

                                                                    }



                                                                    Toast.makeText(context, strings.presetAppliedToast(preset.name), Toast.LENGTH_SHORT).show()

                                                                },

                                                                onLongClick = {

                                                                    presetToDelete = preset

                                                                    showDeletePresetDialog = true

                                                                }

                                                            )

                                                        ) {

                                                            Text(

                                                                text = preset.name,

                                                                fontSize = 11.sp,

                                                                fontFamily = FontFamily.Monospace,

                                                                fontWeight = FontWeight.Bold,

                                                                color = Color.White,

                                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)

                                                            )

                                                        }

                                                    }

                                                }

                                            }

                                        }



                                        // DEUX ENCADRÉS : FICHIERS & WORKSHOP (HIÉRARCHIE VISUELLE ÉPURÉE)

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.spacedBy(12.dp)

                                        ) {

                                            // ENCADRÉ FICHIERS (OUVRE LA FENÊTRE DE L'ARBORESCENCE)

                                            Surface(

                                                modifier = Modifier

                                                    .weight(1f)

                                                    .height(85.dp)

                                                    .clip(RoundedCornerShape(18.dp))

                                                    .clickable { showFilesDialog = true },

                                                color = nothingSurface,

                                                shape = RoundedCornerShape(18.dp),

                                                border = BorderStroke(1.5.dp, nothingBorder)

                                            ) {

                                                Box(

                                                    modifier = Modifier.fillMaxSize(),

                                                    contentAlignment = Alignment.Center

                                                ) {

                                                    Column(

                                                        horizontalAlignment = Alignment.CenterHorizontally,

                                                        verticalArrangement = Arrangement.spacedBy(4.dp)

                                                    ) {

                                                        Text(

                                                            text = strings.files,

                                                            fontSize = 15.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            fontWeight = FontWeight.Bold,

                                                            color = nothingTextPrimary

                                                        )

                                                        Text(

                                                            text = if (strings.isEn) "Tree view" else "Arborescence",

                                                            fontSize = 10.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            color = nothingTextSecondary

                                                        )

                                                    }

                                                }

                                            }



                                            // ENCADRÉ WORKSHOP (OUVRE LA FENÊTRE DU WORKSHOP GITHUB)

                                            Surface(

                                                modifier = Modifier

                                                    .weight(1f)

                                                    .height(85.dp)

                                                    .clip(RoundedCornerShape(18.dp))

                                                    .clickable { showWorkshopDialog = true },

                                                color = nothingSurface,

                                                shape = RoundedCornerShape(18.dp),

                                                border = BorderStroke(1.5.dp, nothingBorder)

                                            ) {

                                                Box(

                                                    modifier = Modifier.fillMaxSize(),

                                                    contentAlignment = Alignment.Center

                                                ) {

                                                    Column(

                                                        horizontalAlignment = Alignment.CenterHorizontally,

                                                        verticalArrangement = Arrangement.spacedBy(4.dp)

                                                    ) {

                                                        Text(

                                                            text = strings.workshop,

                                                            fontSize = 15.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            fontWeight = FontWeight.Bold,

                                                            color = nothingTextPrimary

                                                        )

                                                        Text(

                                                            text = if (strings.isEn) "Remote repo" else "Dépôt distant",

                                                            fontSize = 10.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            color = nothingTextSecondary

                                                        )

                                                    }

                                                }

                                            }

                                        }



                                        // SECTION ANIMATIONS & INTERACTION (EN DESSOUS DE L'ARBORESCENCE DANS L'ONGLET PERSONNAGE)

                                        Card(

                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD2E3F5)),

                                            shape = RoundedCornerShape(16.dp),

                                            modifier = Modifier.fillMaxWidth()

                                        ) {

                                            Column(

                                                modifier = Modifier

                                                    .fillMaxWidth()

                                                    .padding(12.dp),

                                                verticalArrangement = Arrangement.spacedBy(10.dp)

                                            ) {

                                                Text(

                                                    text = strings.animationsAndInteraction,

                                                    fontSize = 12.sp,

                                                    fontFamily = FontFamily.Monospace,

                                                    fontWeight = FontWeight.Bold,

                                                    color = nothingTextPrimary

                                                )



                                                // 1. BOUTON / INTERRUPTEUR DE SUIVI DU DOIGT (TÊTE / REGARD)

                                                Row(

                                                    modifier = Modifier.fillMaxWidth(),

                                                    horizontalArrangement = Arrangement.SpaceBetween,

                                                    verticalAlignment = Alignment.CenterVertically

                                                ) {

                                                    Column(modifier = Modifier.weight(1f)) {

                                                        Text(strings.fingerTracking, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                        Text(strings.fingerTrackingSubtitle, fontSize = 10.sp, color = nothingTextSecondary)

                                                    }

                                                    Switch(

                                                        checked = isPointerTrackingEnabled,

                                                        onCheckedChange = { enabled ->

                                                            isPointerTrackingEnabled = enabled

                                                            prefs.isPointerTrackingEnabled = enabled

                                                        }

                                                    )

                                                }



                                                HorizontalDivider(color = nothingBorder)



                                                // 2. SÉLECTION DES ANIMATIONS DE SORTIE DE VEILLE (DÉVERROUILLAGE)

                                                // SÉLECTION DE L'ANIMATION AU REPOS (IDLE EN BOUCLE)

                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                                                    Text(strings.idleAnimationLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                    Text(strings.idleAnimationSubtitle, fontSize = 10.sp, color = nothingTextSecondary)



                                                    val isDefaultSelected = idleAnimation.isEmpty() || idleAnimation.equals("DEFAULT", ignoreCase = true)



                                                     LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                                         item {

                                                             FilterChip(

                                                                 selected = isDefaultSelected,

                                                                 onClick = {

                                                                     idleAnimation = "DEFAULT"

                                                                     prefs.idleAnimation = "DEFAULT"

                                                                 },

                                                                 label = {

                                                                     Text(

                                                                         strings.animationDefault,

                                                                         fontSize = 11.sp,

                                                                         fontWeight = if (isDefaultSelected) FontWeight.Bold else FontWeight.Normal

                                                                     )

                                                                 },

                                                                 shape = RoundedCornerShape(12.dp),

                                                                 colors = FilterChipDefaults.filterChipColors(

                                                                     selectedContainerColor = nothingBlack,

                                                                     selectedLabelColor = Color.White

                                                                 )

                                                             )

                                                         }

                                                         items(availableMotions) { motionName ->

                                                             val isSelected = motionName.equals(idleAnimation, ignoreCase = true)

                                                             FilterChip(

                                                                 selected = isSelected,

                                                                 onClick = {

                                                                     idleAnimation = motionName

                                                                     prefs.idleAnimation = motionName

                                                                 },

                                                                 label = {

                                                                     Text(

                                                                         text = motionName,

                                                                         fontSize = 11.sp,

                                                                         fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                                                                     )

                                                                 },

                                                                 shape = RoundedCornerShape(12.dp),

                                                                 colors = FilterChipDefaults.filterChipColors(

                                                                     selectedContainerColor = nothingBlack,

                                                                     selectedLabelColor = Color.White

                                                                 )

                                                             )

                                                         }

                                                     }

                                                 }



                                                HorizontalDivider(color = nothingBorder)



                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                                                    Text(strings.wakeupAnimationLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                    Text(strings.wakeupAnimationSubtitle, fontSize = 10.sp, color = nothingTextSecondary)



                                                    val selectedWakeupList = remember(wakeupAnimation) {

                                                        wakeupAnimation.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.equals("NONE", ignoreCase = true) }.toSet()

                                                    }

                                                    val isWakeupNoneSelected = selectedWakeupList.isEmpty() || wakeupAnimation.equals("NONE", ignoreCase = true)



                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                                        item {

                                                            FilterChip(

                                                                selected = isWakeupNoneSelected,

                                                                onClick = {

                                                                    wakeupAnimation = "NONE"

                                                                    prefs.wakeupAnimation = "NONE"

                                                                },

                                                                label = { Text(strings.animationNone, fontSize = 11.sp, fontWeight = if (isWakeupNoneSelected) FontWeight.Bold else FontWeight.Normal) },

                                                                shape = RoundedCornerShape(12.dp),

                                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = nothingBlack, selectedLabelColor = Color.White)

                                                            )

                                                        }

                                                        items(availableMotions) { motionName ->

                                                            val isSelected = motionName in selectedWakeupList

                                                            FilterChip(

                                                                selected = isSelected,

                                                                onClick = {

                                                                    val newList = selectedWakeupList.toMutableSet()

                                                                    if (isSelected) {

                                                                        newList.remove(motionName)

                                                                    } else {

                                                                        newList.add(motionName)

                                                                    }

                                                                    val newStr = if (newList.isEmpty()) "NONE" else newList.joinToString(",")

                                                                    wakeupAnimation = newStr

                                                                    prefs.wakeupAnimation = newStr

                                                                },

                                                                label = {

                                                                    Text(

                                                                        text = motionName,

                                                                        fontSize = 11.sp,

                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                                                                    )

                                                                },

                                                                shape = RoundedCornerShape(12.dp),

                                                                colors = FilterChipDefaults.filterChipColors(

                                                                    selectedContainerColor = nothingBlack,

                                                                    selectedLabelColor = Color.White

                                                                )

                                                            )

                                                        }

                                                    }

                                                }



                                                HorizontalDivider(color = nothingBorder)



                                                // 3. SÉLECTION DES ANIMATIONS AU TOUCHER (TAP / CLIC)

                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                                                    Text(strings.clickAnimationLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                    Text(strings.clickAnimationSubtitle, fontSize = 10.sp, color = nothingTextSecondary)



                                                    val selectedClickList = remember(clickAnimation) {

                                                        clickAnimation.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.equals("NONE", ignoreCase = true) }.toSet()

                                                    }

                                                    val isClickNoneSelected = selectedClickList.isEmpty() || clickAnimation.equals("NONE", ignoreCase = true)



                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                                        item {

                                                            FilterChip(

                                                                selected = isClickNoneSelected,

                                                                onClick = {

                                                                    clickAnimation = "NONE"

                                                                    prefs.clickAnimation = "NONE"

                                                                },

                                                                label = { Text(strings.animationNone, fontSize = 11.sp, fontWeight = if (isClickNoneSelected) FontWeight.Bold else FontWeight.Normal) },

                                                                shape = RoundedCornerShape(12.dp),

                                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = nothingBlack, selectedLabelColor = Color.White)

                                                            )

                                                        }

                                                        items(availableMotions) { motionName ->

                                                            val isSelected = motionName in selectedClickList

                                                            FilterChip(

                                                                selected = isSelected,

                                                                onClick = {

                                                                    val newList = selectedClickList.toMutableSet()

                                                                    if (isSelected) {

                                                                        newList.remove(motionName)

                                                                    } else {

                                                                        newList.add(motionName)

                                                                    }

                                                                    val newStr = if (newList.isEmpty()) "NONE" else newList.joinToString(",")

                                                                    clickAnimation = newStr

                                                                    prefs.clickAnimation = newStr

                                                                },

                                                                label = {

                                                                    Text(

                                                                        text = motionName,

                                                                        fontSize = 11.sp,

                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                                                                    )

                                                                },

                                                                shape = RoundedCornerShape(12.dp),

                                                                colors = FilterChipDefaults.filterChipColors(

                                                                    selectedContainerColor = nothingBlack,

                                                                    selectedLabelColor = Color.White

                                                                )

                                                            )

                                                        }

                                                    }

                                                    val isSpineModelActive = selectedCharacter?.engineType == EngineType.SPINE || SpineModelHelper.isSpineFile(currentSkelPath)
                                                    if (isSpineModelActive) {
                                                        HorizontalDivider(color = nothingBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = strings.spinePmaLabel,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = nothingTextPrimary
                                                                )
                                                                Text(
                                                                    text = strings.spinePmaSubtitle,
                                                                    fontSize = 10.sp,
                                                                    color = nothingTextSecondary
                                                                )
                                                            }
                                                            Switch(
                                                                checked = isSpinePma,
                                                                onCheckedChange = { enabled ->
                                                                    isSpinePma = enabled
                                                                    prefs.isSpinePma = enabled
                                                                }
                                                            )
                                                        }
                                                    }

                                                }

                                            }

                                        }

                                    }

                                }

                                1 -> {

                                    // ONGLET 1: FOND

                                    val isColorActive = bgType == WaifuPreferences.TYPE_COLOR

                                    val isMediaActive = bgType == WaifuPreferences.TYPE_IMAGE || bgType == WaifuPreferences.TYPE_VIDEO



                                    Column(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .padding(vertical = 4.dp),

                                        verticalArrangement = Arrangement.spacedBy(10.dp)

                                    ) {

                                        // LAYOUT SCHÉMATISÉ : SÉLECTEUR TOTALEMENT REMPLI (GAUCHE) ET BOUTONS EMPILÉS (DROITE)

                                        Row(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .height(180.dp),

                                            horizontalArrangement = Arrangement.spacedBy(10.dp)

                                        ) {

                                            // CÔTÉ GAUCHE : BLOC "Couleur" AVEC CONTOUR PARFAITEMENT PARALLÈLE ET LINÉAIRE

                                            Box(

                                                modifier = Modifier

                                                    .weight(1.2f)

                                                    .fillMaxHeight()

                                                    .clip(RoundedCornerShape(20.dp))

                                                    .background(if (isColorActive) nothingBlack else nothingBorder)

                                                    .padding(if (isColorActive) 2.5.dp else 1.dp)

                                                    .clip(RoundedCornerShape(17.5.dp))

                                                    .background(if (isColorActive) Color.White else Color.Transparent)

                                                    .padding(if (isColorActive) 1.5.dp else 0.dp)

                                                    .clip(RoundedCornerShape(16.dp))

                                            ) {

                                                ColorSpectrumPicker(

                                                    initialColor = pickedColor,

                                                    onColorSelected = { color ->

                                                        pickedColor = color

                                                    },

                                                    modifier = Modifier.fillMaxSize()

                                                )

                                            }



                                            // CÔTÉ DROIT : 2 BOUTONS EMPILÉS ("image/vidéo" ET "appliquer couleur")

                                            Column(

                                                modifier = Modifier

                                                    .weight(0.9f)

                                                    .fillMaxHeight(),

                                                verticalArrangement = Arrangement.spacedBy(10.dp)

                                            ) {

                                                // BOUTON TOP RIGHT : "Image / Vidéo"

                                                Surface(

                                                    onClick = { mediaPickerLauncher.launch(arrayOf("image/*", "video/*")) },

                                                    shape = RoundedCornerShape(20.dp),

                                                    color = if (isMediaActive) nothingBlack else Color(0xFFEDF4FC),

                                                    border = if (isMediaActive) null else BorderStroke(1.dp, nothingBorder),

                                                    modifier = Modifier

                                                        .fillMaxWidth()

                                                        .weight(1f)

                                                ) {

                                                    Box(

                                                        modifier = Modifier

                                                            .fillMaxSize()

                                                            .padding(8.dp),

                                                        contentAlignment = Alignment.Center

                                                    ) {

                                                        Text(

                                                            text = strings.imageOrVideo,

                                                            fontSize = 11.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            fontWeight = FontWeight.Bold,

                                                            color = if (isMediaActive) Color.White else nothingTextPrimary,

                                                            textAlign = TextAlign.Center,

                                                            modifier = Modifier.fillMaxWidth()

                                                        )

                                                    }

                                                }



                                                // BOUTON BOTTOM RIGHT : "Appliquer Couleur"

                                                Surface(

                                                    onClick = {

                                                        clearOldBgFiles()

                                                        bgType = WaifuPreferences.TYPE_COLOR

                                                        prefs.bgType = WaifuPreferences.TYPE_COLOR

                                                        prefs.bgColor = pickedColor.toArgb()

                                                        Toast.makeText(context, strings.bgAppliedToast, Toast.LENGTH_SHORT).show()

                                                    },

                                                    shape = RoundedCornerShape(20.dp),

                                                    color = if (isColorActive) nothingBlack else Color(0xFFEDF4FC),

                                                    border = if (isColorActive) null else BorderStroke(1.dp, nothingBorder),

                                                    modifier = Modifier

                                                        .fillMaxWidth()

                                                        .weight(1f)

                                                ) {

                                                    Box(

                                                        modifier = Modifier

                                                            .fillMaxSize()

                                                            .padding(8.dp),

                                                        contentAlignment = Alignment.Center

                                                    ) {

                                                        Text(

                                                            text = strings.applyColor,

                                                            fontSize = 11.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            fontWeight = FontWeight.Bold,

                                                            color = if (isColorActive) Color.White else nothingTextPrimary,

                                                            textAlign = TextAlign.Center,

                                                            modifier = Modifier.fillMaxWidth()

                                                        )

                                                    }

                                                }

                                            }

                                        }



                                        if (bgType == WaifuPreferences.TYPE_VIDEO) {

                                            Row(

                                                verticalAlignment = Alignment.CenterVertically,

                                                horizontalArrangement = Arrangement.spacedBy(8.dp)

                                            ) {

                                                Text(strings.muteVideoSound, fontSize = 11.sp, color = nothingTextSecondary)

                                                Switch(

                                                    checked = isVideoMuted,

                                                    onCheckedChange = {

                                                        isVideoMuted = it

                                                        prefs.isVideoMuted = it

                                                    }

                                                )

                                            }

                                        }



                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = nothingBorder)



                                        // SECTION MUSIQUE DE FOND

                                        Column(

                                            modifier = Modifier.fillMaxWidth(),

                                            verticalArrangement = Arrangement.spacedBy(6.dp)

                                        ) {

                                            Row(

                                                modifier = Modifier.fillMaxWidth(),

                                                horizontalArrangement = Arrangement.SpaceBetween,

                                                verticalAlignment = Alignment.CenterVertically

                                            ) {

                                                Text(strings.bgMusicLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                Row(verticalAlignment = Alignment.CenterVertically) {

                                                    Button(

                                                        onClick = { musicPickerLauncher.launch("audio/*") },

                                                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                                                        shape = RoundedCornerShape(12.dp),

                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                                                    ) {

                                                        Text(strings.chooseAudio, fontSize = 11.sp, color = Color.White)

                                                    }

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    Switch(

                                                        checked = isMusicEnabled,

                                                        onCheckedChange = {

                                                            isMusicEnabled = it

                                                            prefs.isMusicEnabled = it

                                                        }

                                                    )

                                                }

                                            }



                                            if (isMusicEnabled) {

                                                Row(

                                                    modifier = Modifier.fillMaxWidth(),

                                                    verticalAlignment = Alignment.CenterVertically,

                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                                                ) {

                                                    Text("${strings.volume}${if (strings.isEn) ":" else " :"} ${(musicVolume * 100).toInt()}%", fontSize = 11.sp, color = nothingTextSecondary)

                                                    Slider(

                                                        value = musicVolume,

                                                        onValueChange = {

                                                            musicVolume = it

                                                            prefs.musicVolume = it

                                                        },

                                                        colors = SliderDefaults.colors(

                                                            thumbColor = nothingBlack,

                                                            activeTrackColor = Color(0xFF1D4ED8),

                                                            inactiveTrackColor = Color(0xFFB4CDEB)

                                                        ),

                                                        modifier = Modifier.weight(1f)

                                                    )

                                                }

                                            }

                                        }

                                    }

                                }

                                2 -> {

                                    // ONGLET 2: HORLOGE NUMÉRIQUE

                                    Column(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .padding(vertical = 8.dp),

                                        verticalArrangement = Arrangement.spacedBy(12.dp)

                                    ) {

                                        // EN-TÊTE HORLOGE NUMÉRIQUE

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween,

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Text(strings.digitalClockLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                            Switch(

                                                checked = isClockEnabled,

                                                onCheckedChange = { enabled ->

                                                    isClockEnabled = enabled

                                                    prefs.isClockEnabled = enabled

                                                }

                                            )

                                        }



                                        if (isClockEnabled) {

                                            // TROIS GROS BOUTONS : POLICE, COULEUR, POSITION

                                            Row(

                                                modifier = Modifier.fillMaxWidth(),

                                                horizontalArrangement = Arrangement.spacedBy(8.dp)

                                            ) {

                                                // 1. BOUTON POLICE

                                                Surface(

                                                    onClick = { showFontDialog = true },

                                                    shape = RoundedCornerShape(18.dp),

                                                    color = nothingBlack,

                                                    modifier = Modifier

                                                        .weight(1f)

                                                        .height(84.dp)

                                                ) {

                                                    Column(

                                                        modifier = Modifier.fillMaxSize().padding(6.dp),

                                                        horizontalAlignment = Alignment.CenterHorizontally,

                                                        verticalArrangement = Arrangement.SpaceEvenly

                                                    ) {

                                                        Text(

                                                            text = strings.font,

                                                            fontSize = 12.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            fontWeight = FontWeight.Bold,

                                                            color = Color.White

                                                        )

                                                        Text(

                                                            text = "Aa",

                                                            fontSize = 18.sp,

                                                            fontFamily = when (clockFont) {

                                                                WaifuPreferences.FONT_MONOSPACE -> FontFamily.Monospace

                                                                WaifuPreferences.FONT_SERIF -> FontFamily.Serif

                                                                WaifuPreferences.FONT_SANS_SERIF -> FontFamily.SansSerif

                                                                WaifuPreferences.FONT_THIN -> FontFamily.SansSerif

                                                                WaifuPreferences.FONT_CONDENSED -> FontFamily.SansSerif

                                                                WaifuPreferences.FONT_CURSIVE -> FontFamily.Cursive

                                                                WaifuPreferences.FONT_CASUAL -> FontFamily.Default

                                                                else -> FontFamily.Monospace

                                                            },

                                                            fontWeight = when (clockFont) {

                                                                WaifuPreferences.FONT_THIN -> FontWeight.Thin

                                                                WaifuPreferences.FONT_CONDENSED -> FontWeight.Medium

                                                                WaifuPreferences.FONT_CURSIVE -> FontWeight.Normal

                                                                else -> FontWeight.Bold

                                                            },

                                                            color = Color.White

                                                        )

                                                    }

                                                }



                                                // 2. BOUTON COULEUR

                                                Surface(

                                                    onClick = { showColorDialog = true },

                                                    shape = RoundedCornerShape(18.dp),

                                                    color = nothingBlack,

                                                    modifier = Modifier

                                                        .weight(1f)

                                                        .height(84.dp)

                                                ) {

                                                    Column(

                                                        modifier = Modifier.fillMaxSize().padding(6.dp),

                                                        horizontalAlignment = Alignment.CenterHorizontally,

                                                        verticalArrangement = Arrangement.SpaceEvenly

                                                    ) {

                                                        Text(

                                                            text = strings.color,

                                                            fontSize = 12.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            fontWeight = FontWeight.Bold,

                                                            color = Color.White

                                                        )

                                                        Canvas(modifier = Modifier.size(20.dp)) {

                                                            val color = Color.White

                                                            val strokeW = 1.8.dp.toPx()

                                                            val w = size.width

                                                            val h = size.height



                                                            drawCircle(

                                                                color = color,

                                                                radius = w * 0.45f,

                                                                center = Offset(w * 0.5f, h * 0.5f),

                                                                style = Stroke(width = strokeW)

                                                            )

                                                            drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(w * 0.35f, h * 0.35f))

                                                            drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(w * 0.65f, h * 0.35f))

                                                            drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(w * 0.35f, h * 0.65f))

                                                            drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(w * 0.68f, h * 0.65f))

                                                        }

                                                    }

                                                }



                                                // 3. BOUTON POSITION

                                                val isFront = clockPosition == WaifuPreferences.CLOCK_POS_FRONT

                                                Surface(

                                                    onClick = {

                                                        val newPos = if (isFront) WaifuPreferences.CLOCK_POS_BACK else WaifuPreferences.CLOCK_POS_FRONT

                                                        clockPosition = newPos

                                                        prefs.clockPosition = newPos

                                                    },

                                                    shape = RoundedCornerShape(18.dp),

                                                    color = if (isFront) nothingBlack else Color(0xFFD2E3F5),

                                                    border = if (isFront) null else BorderStroke(1.dp, nothingBorder),

                                                    modifier = Modifier

                                                        .weight(1f)

                                                        .height(84.dp)

                                                ) {

                                                    Column(

                                                        modifier = Modifier.fillMaxSize().padding(6.dp),

                                                        horizontalAlignment = Alignment.CenterHorizontally,

                                                        verticalArrangement = Arrangement.SpaceEvenly

                                                    ) {

                                                        Text(

                                                            text = "Position",

                                                            fontSize = 12.sp,

                                                            fontFamily = FontFamily.Monospace,

                                                            fontWeight = FontWeight.Bold,

                                                            color = if (isFront) Color.White else nothingTextPrimary

                                                        )

                                                        Canvas(modifier = Modifier.size(20.dp)) {

                                                            val iconColor = if (isFront) Color.White else nothingTextPrimary

                                                            val strokeW = 1.8.dp.toPx()

                                                            val w = size.width

                                                            val h = size.height



                                                            if (isFront) {

                                                                drawRoundRect(

                                                                    color = iconColor.copy(alpha = 0.5f),

                                                                    topLeft = Offset(w * 0.10f, h * 0.10f),

                                                                    size = Size(w * 0.55f, h * 0.55f),

                                                                    cornerRadius = CornerRadius(4.dp.toPx()),

                                                                    style = Stroke(width = strokeW)

                                                                )

                                                                drawRoundRect(

                                                                    color = iconColor,

                                                                    topLeft = Offset(w * 0.35f, h * 0.35f),

                                                                    size = Size(w * 0.55f, h * 0.55f),

                                                                    cornerRadius = CornerRadius(4.dp.toPx())

                                                                )

                                                            } else {

                                                                drawRoundRect(

                                                                    color = iconColor,

                                                                    topLeft = Offset(w * 0.10f, h * 0.10f),

                                                                    size = Size(w * 0.55f, h * 0.55f),

                                                                    cornerRadius = CornerRadius(4.dp.toPx())

                                                                )

                                                                drawRoundRect(

                                                                    color = iconColor.copy(alpha = 0.5f),

                                                                    topLeft = Offset(w * 0.35f, h * 0.35f),

                                                                    size = Size(w * 0.55f, h * 0.55f),

                                                                    cornerRadius = CornerRadius(4.dp.toPx()),

                                                                    style = Stroke(width = strokeW)

                                                                )

                                                            }

                                                        }

                                                    }

                                                }

                                            }



                                            HorizontalDivider(color = nothingBorder)



                                            Text(strings.dateDisplay, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                                    FilterChip(

                                                        selected = !isDateEnabled,

                                                        onClick = {

                                                            isDateEnabled = false

                                                            prefs.isDateEnabled = false

                                                        },

                                                        label = { Text(strings.hidden, fontSize = 11.sp) },

                                                        shape = RoundedCornerShape(12.dp),

                                                        colors = FilterChipDefaults.filterChipColors(

                                                            selectedContainerColor = nothingBlack,

                                                            selectedLabelColor = Color.White

                                                        )

                                                    )



                                                    FilterChip(

                                                        selected = isDateEnabled && dateFormat == WaifuPreferences.DATE_FORMAT_SHORT,

                                                        onClick = {

                                                            isDateEnabled = true

                                                            dateFormat = WaifuPreferences.DATE_FORMAT_SHORT

                                                            prefs.isDateEnabled = true

                                                            prefs.dateFormat = WaifuPreferences.DATE_FORMAT_SHORT

                                                        },

                                                        label = { Text(strings.dateShort, fontSize = 11.sp) },

                                                        shape = RoundedCornerShape(12.dp),

                                                        colors = FilterChipDefaults.filterChipColors(

                                                            selectedContainerColor = nothingBlack,

                                                            selectedLabelColor = Color.White

                                                        )

                                                    )



                                                    FilterChip(

                                                        selected = isDateEnabled && dateFormat == WaifuPreferences.DATE_FORMAT_FULL,

                                                        onClick = {

                                                            isDateEnabled = true

                                                            dateFormat = WaifuPreferences.DATE_FORMAT_FULL

                                                            prefs.isDateEnabled = true

                                                            prefs.dateFormat = WaifuPreferences.DATE_FORMAT_FULL

                                                        },

                                                        label = { Text(strings.dateFull, fontSize = 11.sp) },

                                                        shape = RoundedCornerShape(12.dp),

                                                        colors = FilterChipDefaults.filterChipColors(

                                                            selectedContainerColor = nothingBlack,

                                                            selectedLabelColor = Color.White

                                                        )

                                                    )

                                                }



                                                HorizontalDivider(color = nothingBorder)



                                                Row(

                                                    modifier = Modifier.fillMaxWidth(),

                                                    horizontalArrangement = Arrangement.SpaceBetween,

                                                    verticalAlignment = Alignment.CenterVertically

                                                ) {

                                                    Text(strings.format24h, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                    Switch(

                                                        checked = clockFormat24h,

                                                        onCheckedChange = {

                                                            clockFormat24h = it

                                                            prefs.clockFormat24h = it

                                                        }

                                                    )

                                                }



                                                Row(

                                                    modifier = Modifier.fillMaxWidth(),

                                                    horizontalArrangement = Arrangement.SpaceBetween,

                                                    verticalAlignment = Alignment.CenterVertically

                                                ) {

                                                    Text(strings.showSeconds, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                    Switch(

                                                        checked = clockShowSeconds,

                                                        onCheckedChange = {

                                                            clockShowSeconds = it

                                                            prefs.clockShowSeconds = it

                                                        }

                                                    )

                                                }



                                                HorizontalDivider(color = nothingBorder)



                                                Text("${strings.clockSize}${if (strings.isEn) ":" else " :"} ${clockSize.toInt()} sp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                Slider(

                                                    value = clockSize,

                                                    onValueChange = {

                                                        clockSize = it

                                                        prefs.clockSize = it

                                                    },

                                                    valueRange = 20f..96f,

                                                    colors = SliderDefaults.colors(

                                                        thumbColor = nothingBlack,

                                                        activeTrackColor = Color(0xFF1D4ED8),

                                                        inactiveTrackColor = Color(0xFFB4CDEB)

                                                    )

                                                )

                                            }

                                        }

                                }

                                3 -> {

                                    // ONGLET 3: PLANIFICATION

                                    Column(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .padding(vertical = 8.dp),

                                        verticalArrangement = Arrangement.spacedBy(10.dp)

                                    ) {

                                        Row(

                                            modifier = Modifier.fillMaxWidth(),

                                            horizontalArrangement = Arrangement.SpaceBetween,

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Text(strings.enableSchedule, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                            Switch(

                                                checked = isScheduleEnabled,

                                                onCheckedChange = { enabled ->

                                                    isScheduleEnabled = enabled

                                                    prefs.isScheduleEnabled = enabled

                                                }

                                            )

                                        }



                                        if (isScheduleEnabled) {

                                            HorizontalDivider(color = nothingBorder)



                                            Text(strings.selectDayToConfigure, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)



                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                                items(daysOfWeek) { (dayKey, dayLabel) ->

                                                    val isSelected = dayKey == selectedDayKey

                                                    val daySched = remember(selectedDayKey, dayKey, scheduleVersion) {

                                                        prefs.getDaySchedule(dayKey, dayLabel)

                                                    }

                                                    FilterChip(

                                                        selected = isSelected,

                                                        onClick = {

                                                            selectedDayKey = dayKey

                                                            selectedDayLabel = dayLabel

                                                            currentDaySchedule = prefs.getDaySchedule(dayKey, dayLabel)

                                                        },

                                                        label = {

                                                            Text(

                                                                text = if (daySched.isEnabled) dayLabel else "$dayLabel (Off)",

                                                                fontSize = 11.sp,

                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                                                            )

                                                        },

                                                        shape = RoundedCornerShape(12.dp),

                                                        colors = FilterChipDefaults.filterChipColors(

                                                            selectedContainerColor = nothingBlack,

                                                            selectedLabelColor = Color.White

                                                        )

                                                    )

                                                }

                                            }



                                            Card(

                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD2E3F5)),

                                                shape = RoundedCornerShape(16.dp),

                                                modifier = Modifier.fillMaxWidth()

                                            ) {

                                                Column(

                                                    modifier = Modifier.padding(12.dp),

                                                    verticalArrangement = Arrangement.spacedBy(8.dp)

                                                ) {

                                                    Row(

                                                        modifier = Modifier.fillMaxWidth(),

                                                        horizontalArrangement = Arrangement.SpaceBetween,

                                                        verticalAlignment = Alignment.CenterVertically

                                                    ) {

                                                        Text(strings.pauseOnDay(selectedDayLabel), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                                        Switch(

                                                            checked = currentDaySchedule.isEnabled,

                                                            onCheckedChange = { enabled ->

                                                                val updated = currentDaySchedule.copy(isEnabled = enabled)

                                                                currentDaySchedule = updated

                                                                prefs.setDaySchedule(updated)

                                                                scheduleVersion++

                                                            }

                                                        )

                                                    }



                                                    if (currentDaySchedule.isEnabled) {

                                                        Row(

                                                            modifier = Modifier.fillMaxWidth(),

                                                            horizontalArrangement = Arrangement.SpaceBetween,

                                                            verticalAlignment = Alignment.CenterVertically

                                                        ) {

                                                            val startStr = String.format("%02d:%02d", currentDaySchedule.startHour, currentDaySchedule.startMinute)

                                                            val endStr = String.format("%02d:%02d", currentDaySchedule.endHour, currentDaySchedule.endMinute)



                                                            Text(strings.timeRange(startStr, endStr), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary)



                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                                                                Button(

                                                                    onClick = {

                                                                        TimePickerDialog(

                                                                            context,

                                                                            { _, h, m ->

                                                                                val updated = currentDaySchedule.copy(startHour = h, startMinute = m)

                                                                                currentDaySchedule = updated

                                                                                prefs.setDaySchedule(updated)

                                                                                scheduleVersion++

                                                                            },

                                                                            currentDaySchedule.startHour,

                                                                            currentDaySchedule.startMinute,

                                                                            true

                                                                        ).show()

                                                                    },

                                                                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                                                                    shape = RoundedCornerShape(12.dp),

                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                                                                ) {

                                                                    Text(strings.startTime, fontSize = 10.sp, color = Color.White)

                                                                }



                                                                Button(

                                                                    onClick = {

                                                                        TimePickerDialog(

                                                                            context,

                                                                            { _, h, m ->

                                                                                val updated = currentDaySchedule.copy(endHour = h, endMinute = m)

                                                                                currentDaySchedule = updated

                                                                                prefs.setDaySchedule(updated)

                                                                                scheduleVersion++

                                                                            },

                                                                            currentDaySchedule.endHour,

                                                                            currentDaySchedule.endMinute,

                                                                            true

                                                                        ).show()

                                                                    },

                                                                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                                                                    shape = RoundedCornerShape(12.dp),

                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                                                                ) {

                                                                    Text(strings.endTime, fontSize = 10.sp, color = Color.White)

                                                                }

                                                            }

                                                        }

                                                    }

                                                }

                                            }



                                            Text(strings.fallbackBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

                                            Row(

                                                horizontalArrangement = Arrangement.spacedBy(8.dp),

                                                verticalAlignment = Alignment.CenterVertically

                                            ) {

                                                FilterChip(

                                                    selected = fallbackType == WaifuPreferences.TYPE_COLOR,

                                                    onClick = {

                                                        fallbackType = WaifuPreferences.TYPE_COLOR

                                                        prefs.scheduleFallbackType = WaifuPreferences.TYPE_COLOR

                                                    },

                                                    label = { Text(strings.pureBlack, fontSize = 11.sp) },

                                                    shape = RoundedCornerShape(12.dp),

                                                    colors = FilterChipDefaults.filterChipColors(

                                                        selectedContainerColor = nothingBlack,

                                                        selectedLabelColor = Color.White

                                                    )

                                                )



                                                FilterChip(

                                                    selected = fallbackType == WaifuPreferences.TYPE_IMAGE,

                                                    onClick = {

                                                        fallbackType = WaifuPreferences.TYPE_IMAGE

                                                        prefs.scheduleFallbackType = WaifuPreferences.TYPE_IMAGE

                                                    },

                                                    label = { Text(strings.backupImage, fontSize = 11.sp) },

                                                    shape = RoundedCornerShape(12.dp),

                                                    colors = FilterChipDefaults.filterChipColors(

                                                        selectedContainerColor = nothingBlack,

                                                        selectedLabelColor = Color.White

                                                    )

                                                )



                                                if (fallbackType == WaifuPreferences.TYPE_IMAGE) {

                                                    Button(

                                                        onClick = { fallbackImagePickerLauncher.launch("image/*") },

                                                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),

                                                        shape = RoundedCornerShape(12.dp),

                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

                                                    ) {

                                                        Text(strings.chooseImage, fontSize = 10.sp, color = Color.White)

                                                    }

                                                }

                                            }



                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = nothingBorder)



                                            RagebaitButton(

                                                isRagebaitEnabled = isRagebaitEnabled,

                                                strings = strings,

                                                onClick = {

                                                    if (isRagebaitEnabled) {

                                                        pinInputText = ""

                                                        showRagebaitDisableDialog = true

                                                    } else {

                                                        pinInputText = ""

                                                        showRagebaitEnableDialog = true

                                                    }

                                                },

                                                modifier = Modifier.fillMaxWidth()

                                            )

                                        }

                                    }

                                }

                            }

                        }

                    }

                }

            }



            Spacer(modifier = Modifier.height(20.dp))

        }



        // COUCHE 2 : OVERLAY PLEIN ÉCRAN FLUIDE ET ISOLÉ

        TopLevelAnimatedVisibility(

            visible = isFullscreen,

            enter = fadeIn(tween(200, easing = LinearOutSlowInEasing)) + scaleIn(

                initialScale = 0.96f,

                animationSpec = tween(200, easing = LinearOutSlowInEasing)

            ),

            exit = fadeOut(tween(160, easing = FastOutLinearInEasing)) + scaleOut(

                targetScale = 0.96f,

                animationSpec = tween(160, easing = FastOutLinearInEasing)

            )

        ) {

            Box(

                modifier = Modifier

                    .fillMaxSize()

                    .background(Color(0xFF08132B))

                    .then(customBgModifier)

                    .then(live2DGestureModifier)

            ) {

                WallpaperPreviewBackground(

                    bgType = bgType,

                    pickedColor = pickedColor,

                    bgImagePath = prefs.bgImagePath,

                    bgVideoPath = prefs.bgVideoPath,

                    bgScale = prefs.bgScale,

                    bgOffsetX = prefs.bgOffsetX,

                    bgOffsetY = prefs.bgOffsetY,

                    isVideoMuted = isVideoMuted

                )



                if (clockPosition == WaifuPreferences.CLOCK_POS_BACK) {

                    DigitalClockOverlay(

                        isClockEnabled = isClockEnabled,

                        clockFont = clockFont,

                        clockColor = clockColor,

                        clockSizeSp = clockSize,

                        clockFormat24h = clockFormat24h,

                        clockShowSeconds = clockShowSeconds,

                        isDateEnabled = isDateEnabled,

                        dateFormat = dateFormat,

                        clockOffsetXPx = clockOffsetX,

                        clockOffsetYPx = clockOffsetY,

                        isEn = strings.isEn,

                        onPositionDrag = onClockDrag,

                        modifier = Modifier.zIndex(0f)

                    )

                }



                val isSpineModel = (selectedCharacter?.engineType == EngineType.SPINE &&

                    !currentSkelPath.endsWith(".model3.json", ignoreCase = true) &&

                    !currentSkelPath.endsWith(".model.json", ignoreCase = true) &&

                    !currentSkelPath.endsWith(".moc3", ignoreCase = true) &&

                    !currentSkelPath.endsWith(".moc", ignoreCase = true)) ||

                    currentSkelPath.endsWith(".skel", ignoreCase = true) ||

                    currentSkelPath.endsWith(".skel.bytes", ignoreCase = true) ||

                    SpineModelHelper.isSpineFile(currentSkelPath)



                val currentAtlas = if (isSpineModel) {

                    selectedVariant?.spineAtlasPath?.ifEmpty { null }

                        ?: prefs.spineAtlasPath.ifEmpty { null }

                        ?: SpineModelHelper.findAtlasFile(File(currentSkelPath).parentFile ?: File(""))?.absolutePath ?: ""

                } else ""



                Live2DWebView(

                    modelJsonPath = if (!isSpineModel) currentSkelPath else "",

                    scale = currentScale,

                    offsetX = currentOffsetX,

                    offsetY = currentOffsetY,

                    idleAnimation = idleAnimation,

                    isPointerTrackingEnabled = isPointerTrackingEnabled,

                    doubleTapTrigger = doubleTapTrigger,

                    cornerRadiusPx = 0f,

                    isAnimating = false,

                    isActive = isFullscreen && !isSpineModel,

                    modifier = Modifier

                        .fillMaxSize()

                        .zIndex(if (!isSpineModel) 2f else 1f)

                )



                SpineWebView(

                    modelPath = if (isSpineModel) currentSkelPath else "",

                    atlasPath = if (isSpineModel) currentAtlas else "",

                    scale = currentScale,

                    offsetX = currentOffsetX,

                    offsetY = currentOffsetY,

                    idleAnimation = idleAnimation,

                    clickAnimation = clickAnimation,

                    isPma = isSpinePma,

                    isPointerTrackingEnabled = isPointerTrackingEnabled,

                    doubleTapTrigger = doubleTapTrigger,

                    cornerRadiusPx = 0f,

                    isAnimating = false,

                    isActive = isFullscreen && isSpineModel,

                    modifier = Modifier

                        .fillMaxSize()

                        .zIndex(if (isSpineModel) 2f else 1f)

                )



                if (clockPosition == WaifuPreferences.CLOCK_POS_FRONT) {

                    DigitalClockOverlay(

                        isClockEnabled = isClockEnabled,

                        clockFont = clockFont,

                        clockColor = clockColor,

                        clockSizeSp = clockSize,

                        clockFormat24h = clockFormat24h,

                        clockShowSeconds = clockShowSeconds,

                        isDateEnabled = isDateEnabled,

                        dateFormat = dateFormat,

                        clockOffsetXPx = clockOffsetX,

                        clockOffsetYPx = clockOffsetY,

                        isEn = strings.isEn,

                        onPositionDrag = onClockDrag,

                        modifier = Modifier.zIndex(2f)

                    )

                }



                // SURIMPOSITION DU CARRÉ ROUGE TRANSLUCIDE (ZONE TACTILE) EN PLEIN ÉCRAN

                if (isFullscreen && (isTouchZoneCustom || showTouchZoneEditor)) {

                    val density = LocalDensity.current

                    val leftPx = touchZoneLeft * viewWidth

                    val topPx = touchZoneTop * viewHeight

                    val widthPx = touchZoneWidth * viewWidth

                    val heightPx = touchZoneHeight * viewHeight



                    val leftDp = with(density) { leftPx.toDp() }

                    val topDp = with(density) { topPx.toDp() }

                    val widthDp = with(density) { widthPx.toDp() }

                    val heightDp = with(density) { heightPx.toDp() }



                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .zIndex(4f)

                    ) {

                        Box(

                            modifier = Modifier

                                .offset(x = leftDp, y = topDp)

                                .size(width = widthDp, height = heightDp)

                                .background(Color.Red.copy(alpha = 0.35f))

                                .border(2.dp, Color.Red, RoundedCornerShape(8.dp))

                                .pointerInput(Unit) {

                                    detectDragGestures { change, dragAmount ->

                                        change.consume()

                                        isTouchZoneCustom = true

                                        prefs.isTouchZoneCustom = true

                                        val deltaNormX = dragAmount.x / viewWidth

                                        val deltaNormY = dragAmount.y / viewHeight

                                        val newLeft = (touchZoneLeft + deltaNormX).coerceIn(0f, 1f - touchZoneWidth)

                                        val newTop = (touchZoneTop + deltaNormY).coerceIn(0f, 1f - touchZoneHeight)

                                        touchZoneLeft = newLeft

                                        touchZoneTop = newTop

                                        prefs.touchZoneLeft = newLeft

                                        prefs.touchZoneTop = newTop

                                    }

                                }

                        ) {

                            Text(

                                text = strings.touchZone,

                                fontSize = 10.sp,

                                fontFamily = FontFamily.Monospace,

                                fontWeight = FontWeight.Bold,

                                color = Color.White,

                                modifier = Modifier

                                    .align(Alignment.TopCenter)

                                    .background(Color.Red)

                                    .padding(horizontal = 6.dp, vertical = 2.dp)

                            )



                            // POIGNÉE DE REDIMENSIONNEMENT EN BAS À DROITE DU CARRÉ ROUGE

                            Box(

                                modifier = Modifier

                                    .align(Alignment.BottomEnd)

                                    .size(32.dp)

                                    .background(Color.Red, CircleShape)

                                    .pointerInput(Unit) {

                                        detectDragGestures { change, dragAmount ->

                                            change.consume()

                                            isTouchZoneCustom = true

                                            prefs.isTouchZoneCustom = true

                                            val deltaNormX = dragAmount.x / viewWidth

                                            val deltaNormY = dragAmount.y / viewHeight

                                            val newWidth = (touchZoneWidth + deltaNormX).coerceIn(0.1f, 1f - touchZoneLeft)

                                            val newHeight = (touchZoneHeight + deltaNormY).coerceIn(0.1f, 1f - touchZoneTop)

                                            touchZoneWidth = newWidth

                                            touchZoneHeight = newHeight

                                            prefs.touchZoneWidth = newWidth

                                            prefs.touchZoneHeight = newHeight

                                        }

                                    },

                                contentAlignment = Alignment.Center

                            ) {

                                Text("↘", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                            }

                        }

                    }

                }



                // BARRE ET COMMANDES EN SURIMPOSITION DU PLEIN ÉCRAN

                TopLevelAnimatedVisibility(

                    visible = isFullscreenUiVisible,

                    enter = fadeIn(tween(180, easing = LinearOutSlowInEasing)),

                    exit = fadeOut(tween(200, easing = FastOutLinearInEasing)),

                    modifier = Modifier

                        .fillMaxWidth()

                        .zIndex(6f)

                ) {

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .statusBarsPadding()

                            .padding(horizontal = 20.dp, vertical = 12.dp),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Surface(

                            onClick = { isFullscreen = false },

                            shape = CircleShape,

                            color = Color(0xFFEDF4FC).copy(alpha = 0.95f),

                            border = BorderStroke(1.dp, nothingBorder),

                            modifier = Modifier.size(44.dp)

                        ) {

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier.fillMaxSize()

                            ) {

                                Text(

                                    text = "←",

                                    fontSize = 22.sp,

                                    fontWeight = FontWeight.Bold,

                                    color = Color(0xFF0D1B3E)

                                )

                            }

                        }



                        Button(

                            onClick = {

                                isFullscreen = false

                                applyWallpaper()

                            },

                            colors = ButtonDefaults.buttonColors(

                                containerColor = nothingBlack,

                                contentColor = Color.White

                            ),

                            shape = RoundedCornerShape(20.dp)

                        ) {

                            Text(strings.applyUpper, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                        }

                    }

                }



                // BOUTONS REGLAGES (⚙) ET ZONE TACTILE (🎯) EN BAS À GAUCHE DU PLEIN ÉCRAN

                TopLevelAnimatedVisibility(

                    visible = isFullscreenUiVisible,

                    enter = fadeIn(tween(180, easing = LinearOutSlowInEasing)),

                    exit = fadeOut(tween(200, easing = FastOutLinearInEasing)),

                    modifier = Modifier

                        .align(Alignment.BottomStart)

                        .navigationBarsPadding()

                        .padding(bottom = 24.dp, start = 20.dp)

                        .zIndex(6f)

                ) {

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                        Surface(

                            onClick = { showTransformDialog = true },

                            shape = CircleShape,

                            color = Color(0xFFEDF4FC).copy(alpha = 0.95f),

                            border = BorderStroke(1.dp, nothingBorder),

                            modifier = Modifier.size(52.dp)

                        ) {

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier.fillMaxSize()

                            ) {

                                Text(

                                    text = "⚙",

                                    fontSize = 22.sp,

                                    color = Color(0xFF0D1B3E),

                                    fontFamily = FontFamily.Monospace

                                )

                            }

                        }



                        Surface(

                            onClick = {

                                val nextState = !isTouchZoneCustom

                                isTouchZoneCustom = nextState

                                prefs.isTouchZoneCustom = nextState

                                showTouchZoneEditor = nextState

                                if (nextState) {

                                    Toast.makeText(context, strings.touchZoneActiveToast, Toast.LENGTH_SHORT).show()

                                } else {

                                    Toast.makeText(context, strings.touchZoneInactiveToast, Toast.LENGTH_SHORT).show()

                                }

                            },

                            shape = CircleShape,

                            color = if (isTouchZoneCustom) Color.Red else Color(0xFFEDF4FC).copy(alpha = 0.95f),

                            border = BorderStroke(1.dp, if (isTouchZoneCustom) Color.Red else nothingBorder),

                            modifier = Modifier.size(52.dp)

                        ) {

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier.fillMaxSize()

                            ) {

                                Text(

                                    text = "🎯",

                                    fontSize = 20.sp,

                                    color = if (isTouchZoneCustom) Color.White else Color(0xFF0D1B3E)

                                )

                            }

                        }

                    }

                }

            }

        }

    }

}



// PIPETTE DE COULEUR AZUR

@Composable

fun ColorSpectrumPicker(

    initialColor: Color,

    onColorSelected: (Color) -> Unit,

    modifier: Modifier = Modifier

) {

    var selectedOffset by remember { mutableStateOf(Offset(0.5f, 0.5f)) }

    var currentColor by remember { mutableStateOf(initialColor) }



    val hueGradient = remember {

        Brush.horizontalGradient(

            colors = listOf(

                Color.Red,

                Color.Yellow,

                Color.Green,

                Color.Cyan,

                Color.Blue,

                Color.Magenta,

                Color.Red

            )

        )

    }



    val saturationValueGradient = remember {

        Brush.verticalGradient(

            colors = listOf(

                Color.White.copy(alpha = 0.8f),

                Color.Transparent,

                Color.Black

            )

        )

    }



    Box(

        modifier = modifier

            .fillMaxSize()

            .clip(RoundedCornerShape(18.dp))

            .pointerInput(Unit) {

                detectTapGestures { offset ->

                    val x = (offset.x / size.width).coerceIn(0f, 1f)

                    val y = (offset.y / size.height).coerceIn(0f, 1f)

                    selectedOffset = Offset(x, y)



                    val hue = x * 360f

                    val saturation = 1f - (y * 0.5f)

                    val value = 1f - y



                    val hsvColor = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))

                    val newColor = Color(hsvColor)

                    currentColor = newColor

                    onColorSelected(newColor)

                }

            }

            .pointerInput(Unit) {

                detectDragGestures { change, _ ->

                    val x = (change.position.x / size.width).coerceIn(0f, 1f)

                    val y = (change.position.y / size.height).coerceIn(0f, 1f)

                    selectedOffset = Offset(x, y)



                    val hue = x * 360f

                    val saturation = 1f - (y * 0.5f)

                    val value = 1f - y



                    val hsvColor = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))

                    val newColor = Color(hsvColor)

                    currentColor = newColor

                    onColorSelected(newColor)

                }

            }

    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            drawRect(brush = hueGradient)

            drawRect(brush = saturationValueGradient)



            val indicatorX = selectedOffset.x * size.width

            val indicatorY = selectedOffset.y * size.height



            drawCircle(

                color = Color.White,

                radius = 11.dp.toPx(),

                center = Offset(indicatorX, indicatorY),

                style = Stroke(width = 2.5.dp.toPx())

            )

            drawCircle(

                color = currentColor,

                radius = 8.dp.toPx(),

                center = Offset(indicatorX, indicatorY)

            )

        }

    }

}



@OptIn(ExperimentalFoundationApi::class)

@Composable

fun IdeTreeViewNode(

    node: FileNode,

    level: Int = 0,

    selectedCharacterPath: String,

    onSelectCharacter: (CharacterItem) -> Unit,

    onNewSubFolder: (File) -> Unit,

    onImportZip: (File) -> Unit,

    onRename: (FileNode) -> Unit,

    onMove: (FileNode) -> Unit,

    onDelete: (FileNode) -> Unit,

    strings: AppStrings = AppStrings(WaifuPreferences(LocalContext.current).appLanguage)

) {

    var isExpanded by remember { mutableStateOf(true) }

    var showContextMenu by remember { mutableStateOf(false) }



    val nothingBlack = Color(0xFF0D1B3E)

    val nothingTextSecondary = Color(0xFF2D4B75)



    if (node.isFolder) {

        Column(modifier = Modifier.fillMaxWidth()) {

            Box(modifier = Modifier.fillMaxWidth()) {

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(8.dp))

                        .combinedClickable(

                            onClick = { isExpanded = !isExpanded },

                            onLongClick = { showContextMenu = true }

                        )

                        .padding(vertical = 6.dp, horizontal = 4.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Spacer(modifier = Modifier.width((level * 16).dp))



                    Text(

                        text = if (isExpanded) "v" else ">",

                        fontSize = 11.sp,

                        fontFamily = FontFamily.Monospace,

                        fontWeight = FontWeight.Bold,

                        color = nothingTextSecondary,

                        modifier = Modifier.width(16.dp)

                    )



                    Surface(

                        shape = RoundedCornerShape(4.dp),

                        color = Color(0xFFD2E3F5),

                        modifier = Modifier.padding(end = 6.dp)

                    ) {

                        Text(

                            text = strings.folderBadge,

                            fontSize = 8.sp,

                            fontFamily = FontFamily.Monospace,

                            fontWeight = FontWeight.Bold,

                            color = nothingBlack,

                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)

                        )

                    }



                    Text(

                        text = node.name,

                        fontSize = 12.sp,

                        fontFamily = FontFamily.Monospace,

                        fontWeight = FontWeight.Bold,

                        color = Color(0xFF0D1B3E)

                    )

                }



                MaterialTheme(

                    colorScheme = lightColorScheme(

                        surface = Color(0xFFEDF4FC),

                        onSurface = Color(0xFF0D1B3E)

                    )

                ) {

                    DropdownMenu(

                        expanded = showContextMenu,

                        onDismissRequest = { showContextMenu = false },

                        modifier = Modifier.background(Color(0xFFEDF4FC))

                    ) {

                        DropdownMenuItem(

                            text = { Text(strings.newSubFolder, fontSize = 12.sp, color = Color(0xFF0D1B3E)) },

                            onClick = {

                                showContextMenu = false

                                onNewSubFolder(File(node.absolutePath))

                            }

                        )

                        DropdownMenuItem(

                            text = { Text(strings.importZipHere, fontSize = 12.sp, color = Color(0xFF0D1B3E)) },

                            onClick = {

                                showContextMenu = false

                                onImportZip(File(node.absolutePath))

                            }

                        )

                        if (level > 0) {

                            DropdownMenuItem(

                                text = { Text(strings.renameFolder, fontSize = 12.sp, color = Color(0xFF0D1B3E)) },

                                onClick = {

                                    showContextMenu = false

                                    onRename(node)

                                }

                            )

                            DropdownMenuItem(

                                text = { Text(strings.moveFolder, fontSize = 12.sp, color = Color(0xFF0D1B3E)) },

                                onClick = {

                                    showContextMenu = false

                                    onMove(node)

                                }

                            )

                            DropdownMenuItem(

                                text = { Text(strings.deleteFolder, fontSize = 12.sp, color = Color(0xFFD9383A)) },

                                onClick = {

                                    showContextMenu = false

                                    onDelete(node)

                                }

                            )

                        }

                    }

                }

            }



            AnimatedVisibility(

                visible = isExpanded,

                enter = fadeIn(tween(140, easing = FastOutSlowInEasing)),

                exit = fadeOut(tween(90, easing = FastOutLinearInEasing))

            ) {

                Column(modifier = Modifier.fillMaxWidth()) {

                    node.children.forEach { child ->

                        IdeTreeViewNode(

                            node = child,

                            level = level + 1,

                            selectedCharacterPath = selectedCharacterPath,

                            onSelectCharacter = onSelectCharacter,

                            onNewSubFolder = onNewSubFolder,

                            onImportZip = onImportZip,

                            onRename = onRename,

                            onMove = onMove,

                            onDelete = onDelete,

                            strings = strings

                        )

                    }

                }

            }

        }

    } else {

        // NOEUD PERSONNAGE

        val isSelected = node.characterItem?.folderPath == selectedCharacterPath



        Box(modifier = Modifier.fillMaxWidth()) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .clip(RoundedCornerShape(8.dp))

                    .background(if (isSelected) Color(0xFF0D1B3E) else Color.Transparent)

                    .combinedClickable(

                        onClick = {

                            node.characterItem?.let { onSelectCharacter(it) }

                        },

                        onLongClick = { showContextMenu = true }

                    )

                    .padding(vertical = 6.dp, horizontal = 4.dp),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Spacer(modifier = Modifier.width((level * 16).dp + 16.dp))



                Surface(

                    shape = RoundedCornerShape(4.dp),

                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFD2E3F5),

                    modifier = Modifier.padding(end = 6.dp)

                ) {

                    val badgeLabel = if (node.characterItem?.engineType == EngineType.SPINE) "[SPINE]" else "[LIVE2D]"

                    Text(

                        text = badgeLabel,

                        fontSize = 8.sp,

                        fontFamily = FontFamily.Monospace,

                        fontWeight = FontWeight.Bold,

                        color = if (isSelected) Color.White else nothingBlack,

                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)

                    )

                }



                Text(

                    text = node.name,

                    fontSize = 12.sp,

                    fontFamily = FontFamily.Monospace,

                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,

                    color = if (isSelected) Color.White else Color(0xFF0D1B3E)

                )

            }



            MaterialTheme(

                colorScheme = lightColorScheme(

                    surface = Color(0xFFEDF4FC),

                    onSurface = Color(0xFF0D1B3E)

                )

            ) {

                DropdownMenu(

                    expanded = showContextMenu,

                    onDismissRequest = { showContextMenu = false },

                    modifier = Modifier.background(Color(0xFFEDF4FC))

                ) {

                    DropdownMenuItem(

                        text = { Text(strings.rename, fontSize = 12.sp, color = Color(0xFF0D1B3E)) },

                        onClick = {

                            showContextMenu = false

                            onRename(node)

                        }

                    )

                    DropdownMenuItem(

                        text = { Text(strings.moveToFolder, fontSize = 12.sp, color = Color(0xFF0D1B3E)) },

                        onClick = {

                            showContextMenu = false

                            onMove(node)

                        }

                    )

                    DropdownMenuItem(

                        text = { Text(strings.deleteNormal, fontSize = 12.sp, color = Color(0xFFD9383A)) },

                        onClick = {

                            showContextMenu = false

                            onDelete(node)

                        }

                    )

                }

            }

        }

    }

}



@Composable

fun DigitalClockOverlay(

    isClockEnabled: Boolean,

    clockFont: String,

    clockColor: Int,

    clockSizeSp: Float,

    clockFormat24h: Boolean,

    clockShowSeconds: Boolean,

    isDateEnabled: Boolean,

    dateFormat: String,

    clockOffsetXPx: Float,

    clockOffsetYPx: Float,

    isEn: Boolean = false,

    onPositionDrag: ((Float, Float) -> Unit)? = null,

    modifier: Modifier = Modifier

) {

    if (!isClockEnabled) return



    var timeText by remember { mutableStateOf("") }

    var dateText by remember { mutableStateOf("") }



    LaunchedEffect(clockFormat24h, clockShowSeconds, isDateEnabled, dateFormat, isEn) {

        while (true) {

            val now = Calendar.getInstance()

            val timePattern = when {

                clockFormat24h && clockShowSeconds -> "HH:mm:ss"

                clockFormat24h -> "HH:mm"

                clockShowSeconds -> "hh:mm:ss a"

                else -> "hh:mm a"

            }

            timeText = SimpleDateFormat(timePattern, Locale.getDefault()).format(now.time)



            if (isDateEnabled) {

                val datePattern = if (isEn) {

                    if (dateFormat == WaifuPreferences.DATE_FORMAT_FULL) "MMMM d" else "MMM d"

                } else {

                    if (dateFormat == WaifuPreferences.DATE_FORMAT_FULL) "d MMMM" else "d MMM"

                }

                dateText = SimpleDateFormat(datePattern, if (isEn) Locale.ENGLISH else Locale.FRENCH).format(now.time)

            } else {

                dateText = ""

            }

            delay(1000L)

        }

    }



    val fontFamily = remember(clockFont) {

        when (clockFont) {

            WaifuPreferences.FONT_MONOSPACE -> FontFamily.Monospace

            WaifuPreferences.FONT_SERIF -> FontFamily.Serif

            WaifuPreferences.FONT_SANS_SERIF -> FontFamily.SansSerif

            WaifuPreferences.FONT_THIN -> FontFamily.SansSerif

            WaifuPreferences.FONT_CONDENSED -> FontFamily.SansSerif

            WaifuPreferences.FONT_CURSIVE -> FontFamily.Cursive

            WaifuPreferences.FONT_CASUAL -> FontFamily.Default

            else -> FontFamily.Monospace

        }

    }



    val fontWeight = remember(clockFont) {

        when (clockFont) {

            WaifuPreferences.FONT_THIN -> FontWeight.Thin

            WaifuPreferences.FONT_CONDENSED -> FontWeight.Medium

            WaifuPreferences.FONT_CURSIVE -> FontWeight.Normal

            else -> FontWeight.Bold

        }

    }



    Box(

        modifier = modifier.fillMaxSize(),

        contentAlignment = Alignment.Center

    ) {

        Column(

            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier

                .graphicsLayer {

                    translationX = clockOffsetXPx

                    translationY = clockOffsetYPx

                }

                .pointerInput(onPositionDrag) {

                    if (onPositionDrag != null) {

                        detectDragGestures { change, dragAmount ->

                            change.consume()

                            onPositionDrag(dragAmount.x, dragAmount.y)

                        }

                    }

                }

        ) {

            Text(

                text = timeText,

                fontSize = clockSizeSp.sp,

                fontFamily = fontFamily,

                fontWeight = fontWeight,

                color = Color(clockColor),

                textAlign = TextAlign.Center,

                style = TextStyle(

                    shadow = Shadow(

                        color = Color.Black.copy(alpha = 0.65f),

                        blurRadius = 10f

                    )

                )

            )



            if (isDateEnabled && dateText.isNotEmpty()) {

                Text(

                    text = dateText,

                    fontSize = (clockSizeSp * 0.52f).sp,

                    fontFamily = fontFamily,

                    fontWeight = fontWeight,

                    color = Color(clockColor),

                    textAlign = TextAlign.Center,

                    style = TextStyle(

                        shadow = Shadow(

                            color = Color.Black.copy(alpha = 0.65f),

                            blurRadius = 8f

                        )

                    )

                )

            }

        }

    }

}

