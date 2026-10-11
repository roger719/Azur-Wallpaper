package com.example.waifuengine

import android.app.TimePickerDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.VideoView
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.example.waifuengine.data.CharacterItem
import com.example.waifuengine.data.CharacterRepository
import com.example.waifuengine.data.FileNode
import com.example.waifuengine.data.PresetItem
import com.example.waifuengine.data.WaifuPreferences
import com.example.waifuengine.live2d.Live2DWebView
import com.example.waifuengine.service.WaifuLive2DWallpaperService
import com.example.waifuengine.ui.theme.WaifuEngineTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        color = Color(0xFF08132B).copy(alpha = 0.92f),
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
            Text(
                text = "CHARGEMENT...",
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
    modifier: Modifier = Modifier
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
                Text("MODE RAGEBAIT", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF0D1B3E))
                Text("Joue la musique en continu même en veille", fontSize = 10.sp, color = Color(0xFF2D4B75))
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRagebaitEnabled) Color(0xFFD9383A) else Color(0xFFD2E3F5)
            ) {
                Text(
                    text = if (isRagebaitEnabled) "ACTIF (PIN)" else "INACTIF",
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

@OptIn(ExperimentalFoundationApi::class)
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

    var isModelLoading by remember { mutableStateOf(false) }

    // Transitoire propre lors du changement de modèle
    LaunchedEffect(currentSkelPath) {
        if (currentSkelPath.isNotEmpty()) {
            isModelLoading = true
            delay(400L)
            isModelLoading = false
        }
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

    // GESTION DES PRÉSETS
    var userPresets by remember { mutableStateOf(prefs.getPresets()) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showDeletePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<PresetItem?>(null) }

    val daysOfWeek = remember {
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

    var selectedDayKey by remember { mutableStateOf("2") }
    var selectedDayLabel by remember { mutableStateOf("Lun") }
    var currentDaySchedule by remember(selectedDayKey) {
        mutableStateOf(prefs.getDaySchedule(selectedDayKey, selectedDayLabel))
    }

    var pickedColor by remember { mutableStateOf(Color(prefs.bgColor)) }

    // Onglet actif (0 = personnage, 1 = fond, 2 = planification)
    var activeTab by remember { mutableIntStateOf(prefs.activeTab.coerceIn(0, 2)) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isFullscreenLoading by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab) {
        prefs.activeTab = activeTab
    }

    // Gestion de l'indicateur de chargement lors de l'ouverture du plein écran
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            isFullscreenLoading = true
            delay(400L)
            isFullscreenLoading = false
        } else {
            isFullscreenLoading = false
        }
    }

    // POPUP & CURSEURS DE DÉPLACEMENT ET DE ZOOM RÉACTIFS (STYLE AZUR WALLPAPER)
    var showTransformDialog by remember { mutableStateOf(false) }
    var currentScale by remember { mutableFloatStateOf(prefs.scale) }
    var currentOffsetX by remember { mutableFloatStateOf(prefs.offsetX) }
    var currentOffsetY by remember { mutableFloatStateOf(prefs.offsetY) }

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
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { doubleTapTrigger++ }
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

    // VERIFICATION DU TIMER DE DÉVERROUILLAGE RAGEBAIT (15 MINUTES)
    LaunchedEffect(isRagebaitEnabled, ragebaitLockoutTime) {
        while (isRagebaitEnabled && ragebaitLockoutTime > 0L) {
            val now = System.currentTimeMillis()
            if (now >= ragebaitLockoutTime) {
                prefs.isRagebaitEnabled = false
                prefs.ragebaitLockoutTime = 0L
                isRagebaitEnabled = false
                ragebaitLockoutTime = 0L
                Toast.makeText(context, "Mode Ragebait désactivé automatiquement !", Toast.LENGTH_LONG).show()
                break
            }
            delay(1000L)
        }
    }

    // Dialog States
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
                Toast.makeText(context, "Image de fond appliquée !", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Image de pause enregistrée !", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur lors du chargement de l'image de pause", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Vidéo de fond appliquée !", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur lors du chargement de la vidéo", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Musique chargée !", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur lors du chargement de la musique", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val applyWallpaper = {
        val variant = selectedVariant
        val charItem = selectedCharacter
        if ((variant != null) && (charItem != null)) {
            // Sauvegarder la configuration choisie dans les préférences uniquement au clic sur APPLIQUER
            prefs.skelPath = variant.modelJsonPath.ifEmpty { variant.mocPath }
            prefs.selectedCharacterName = charItem.name
            prefs.selectedVariantLabel = variant.label
            prefs.scale = currentScale
            prefs.offsetX = currentOffsetX
            prefs.offsetY = currentOffsetY
            prefs.bgType = bgType
            if (bgType == WaifuPreferences.TYPE_COLOR) {
                prefs.bgColor = pickedColor.toArgb()
            }

            Toast.makeText(context, "Configuration sauvegardée : ${charItem.name}", Toast.LENGTH_SHORT).show()

            val targetServiceClass = WaifuLive2DWallpaperService::class.java

            try {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, targetServiceClass),
                    )
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur lors de l'ouverture du fond d'écran", Toast.LENGTH_LONG).show()
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
                    text = "SAUVEGARDER UN PRÉSET",
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
                            val newPreset = PresetItem(
                                id = "${System.currentTimeMillis()}",
                                name = cleanName,
                                skelPath = currentSkelPath,
                                scale = currentScale,
                                offsetX = currentOffsetX,
                                offsetY = currentOffsetY,
                                selectedCharacterName = prefs.selectedCharacterName,
                                selectedVariantLabel = prefs.selectedVariantLabel,
                                wakeupAnimation = prefs.wakeupAnimation,
                                clickAnimation = prefs.clickAnimation,
                                bgType = bgType,
                                bgColor = pickedColor.toArgb(),
                                bgImagePath = prefs.bgImagePath,
                                bgVideoPath = prefs.bgVideoPath,
                                bgScale = prefs.bgScale,
                                bgOffsetX = prefs.bgOffsetX,
                                bgOffsetY = prefs.bgOffsetY,
                                musicPath = prefs.musicPath,
                                isMusicEnabled = prefs.isMusicEnabled,
                                musicVolume = prefs.musicVolume
                            )
                            prefs.savePreset(newPreset)
                            userPresets = prefs.getPresets()
                            Toast.makeText(context, "Préset '$cleanName' sauvegardé !", Toast.LENGTH_SHORT).show()
                            showSavePresetDialog = false
                            presetNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ENREGISTRER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
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
            title = { Text("SUPPRIMER LE PRÉSET", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
            text = { Text("Voulez-vous supprimer le préset '${targetPreset.name}' ?", fontSize = 11.sp, color = nothingTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.deletePreset(targetPreset.id)
                        userPresets = prefs.getPresets()
                        Toast.makeText(context, "Préset supprimé !", Toast.LENGTH_SHORT).show()
                        showDeletePresetDialog = false
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9383A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("SUPPRIMER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePresetDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
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
                    text = "AJUSTER LE PERSONNAGE",
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
                            Text("ZOOM", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)
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
                            Text("POSITION HORIZONTALE (X)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)
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
                            Text("POSITION VERTICALE (Y)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextSecondary)
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
                        Text("OK", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            currentScale = 1.0f
                            currentOffsetX = 0f
                            currentOffsetY = 0f
                        }
                    ) {
                        Text("RÉINITIALISER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFD9383A))
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
            title = { Text("MODE RAGEBAIT", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
            text = {
                Column {
                    Text("Définissez un code PIN secret pour protéger ce mode :", fontSize = 11.sp, color = nothingTextSecondary)
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
                            Toast.makeText(context, "Mode Ragebait activé !", Toast.LENGTH_LONG).show()
                            showRagebaitEnableDialog = false
                            pinInputText = ""
                        } else {
                            Toast.makeText(context, "Veuillez entrer un code PIN valide", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ACTIVIER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRagebaitEnableDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
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
            title = { Text("DÉSACTIVER RAGEBAIT", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
            text = {
                Column {
                    if (isLocked) {
                        val remainingMin = (remainingMs / 1000) / 60
                        val remainingSec = (remainingMs / 1000) % 60
                        Text(
                            text = String.format("Accès bloqué. Désactivation automatique dans %02d:%02d min.", remainingMin, remainingSec),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD9383A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("Saisissez votre code PIN secret :", fontSize = 11.sp, color = nothingTextSecondary)
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
                            Toast.makeText(context, "Mode Ragebait désactivé !", Toast.LENGTH_SHORT).show()
                            showRagebaitDisableDialog = false
                            pinInputText = ""
                        } else {
                            val lockoutEnd = System.currentTimeMillis() + 15 * 60 * 1000L
                            prefs.ragebaitLockoutTime = lockoutEnd
                            ragebaitLockoutTime = lockoutEnd
                            Toast.makeText(context, "Code PIN incorrect ! Désactivation automatique bloquée pendant 15 minutes.", Toast.LENGTH_LONG).show()
                            showRagebaitDisableDialog = false
                            pinInputText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("VALIDER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRagebaitDisableDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
                }
            }
        )
    }

    // --- DIALOGUE CRÉATION DOSSIER ---
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = nothingSurface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("NOUVEAU DOSSIER", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
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
                                Toast.makeText(context, "Dossier créé !", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showCreateFolderDialog = false
                        folderNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CRÉER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
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
            title = { Text("NOMMER LE PERSONNAGE", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
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
                                        Toast.makeText(context, "Personnage importé et affiché !", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "ZIP invalide : Modèle Live2D (.moc3) introuvable", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, "Erreur d'importation", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("IMPORTER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
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
            title = { Text("RENOMMER", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
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
                            Toast.makeText(context, "Nom mis à jour !", Toast.LENGTH_SHORT).show()
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ENREGISTRER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
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
            title = { Text("SUPPRIMER", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
            text = { Text("Voulez-vous supprimer définitivement ${targetNode.name} ?", fontSize = 11.sp, color = nothingTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val file = File(targetNode.absolutePath)
                        CharacterRepository.deleteNode(file)
                        refreshFileManager()
                        Toast.makeText(context, "Supprimé !", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9383A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("SUPPRIMER", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
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
            title = { Text("DÉPLACER ${targetNode.name}", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Choisir le dossier de destination :", fontSize = 11.sp, color = nothingTextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allAvailableFolders) { folder ->
                            val isRoot = folder.absolutePath == rootDir.absolutePath
                            val folderLabel = if (isRoot) "[RACINE]" else "Dossier: ${folder.relativeTo(rootDir).path}"

                            Button(
                                onClick = {
                                    CharacterRepository.moveNode(File(targetNode.absolutePath), folder)
                                    refreshFileManager()
                                    showMoveDialog = false
                                    Toast.makeText(context, "Déplacé vers $folderLabel !", Toast.LENGTH_SHORT).show()
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
                    Text("ANNULER", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
                }
            }
        )
    }

    // CONTENEUR RACINE
    Box(modifier = modifier.fillMaxSize()) {
        // COUCHE 1 : ÉCRAN PRINCIPAL D'ÉDITION & APERÇU DANS LA CARTE
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(nothingBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Azur Wallpaper",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = nothingTextSecondary
                )
                Text(
                    text = selectedCharacter?.name ?: "Nom Personnage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = nothingTextPrimary
                )
            }

            // Zone d'Aperçu Carte (Taille Fixe 380dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .then(customBgModifier)
                    .border(1.dp, nothingBorder, RoundedCornerShape(28.dp))
                    .then(live2DGestureModifier)
            ) {
                // RENDU DU FOND
                when (bgType) {
                    WaifuPreferences.TYPE_COLOR -> {
                        Box(modifier = Modifier.fillMaxSize().background(pickedColor))
                    }
                    WaifuPreferences.TYPE_IMAGE -> {
                        if (prefs.bgImagePath.isNotEmpty() && File(prefs.bgImagePath).exists()) {
                            val bitmap = remember(prefs.bgImagePath) {
                                BitmapFactory.decodeFile(prefs.bgImagePath)?.asImageBitmap()
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Fond d'écran",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = prefs.bgScale
                                            scaleY = prefs.bgScale
                                            translationX = prefs.bgOffsetX
                                            translationY = prefs.bgOffsetY
                                        }
                                )
                            }
                        }
                    }
                    WaifuPreferences.TYPE_VIDEO -> {
                        if (prefs.bgVideoPath.isNotEmpty() && File(prefs.bgVideoPath).exists()) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(prefs.bgVideoPath)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            val vol = if (isVideoMuted) 0f else 0.8f
                                            mp.setVolume(vol, vol)
                                            mp.start()
                                        }
                                    }
                                },
                                update = { view ->
                                    if (!view.isPlaying) view.start()
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = prefs.bgScale
                                        scaleY = prefs.bgScale
                                        translationX = prefs.bgOffsetX
                                        translationY = prefs.bgOffsetY
                                    }
                            )
                        }
                    }
                }

                // RENDU DU MODÈLE EN APERÇU CARTE
                key(isFullscreen) {
                    if (!isFullscreen) {
                        Live2DWebView(
                            modelJsonPath = currentSkelPath,
                            scale = currentScale,
                            offsetX = currentOffsetX,
                            offsetY = currentOffsetY,
                            doubleTapTrigger = doubleTapTrigger,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // VOILE OBSCURCISSANT & INDICATEUR DE CHARGEMENT
                TopLevelAnimatedVisibility(
                    visible = isModelLoading,
                    enter = fadeIn(tween(180, easing = LinearOutSlowInEasing)),
                    exit = fadeOut(tween(250, easing = FastOutLinearInEasing)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF08132B).copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NothingLoadingIndicator()
                    }
                }

                // Boutons d'Aperçu (Plein Écran & Sauvegarder)
                Box(modifier = Modifier.fillMaxSize()) {
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
                                text = "sauvegarder",
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

            // CARTE "présets" DÉFILANTE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = nothingSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, nothingBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "présets",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = nothingTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (userPresets.isEmpty()) {
                        Text(
                            text = "Aucun préset enregistré. Cliquez sur sauvegarder pour créer votre premier préset !",
                            fontSize = 10.sp,
                            color = nothingTextSecondary
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(userPresets) { preset ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFD2E3F5),
                                    border = BorderStroke(1.dp, nothingBorder),
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            currentSkelPath = preset.skelPath
                                            currentScale = preset.scale
                                            currentOffsetX = preset.offsetX
                                            currentOffsetY = preset.offsetY

                                            bgType = preset.bgType
                                            pickedColor = Color(preset.bgColor)

                                            musicPath = preset.musicPath
                                            isMusicEnabled = preset.isMusicEnabled
                                            musicVolume = preset.musicVolume

                                            val charMatch = allCharacters.firstOrNull { it.name == preset.selectedCharacterName }
                                            if (charMatch != null) {
                                                updateSelectedCharacterAndVariant(charMatch)
                                                val varMatch = charMatch.variants.firstOrNull { it.label == preset.selectedVariantLabel } ?: charMatch.variants.firstOrNull()
                                                selectedVariant = varMatch
                                            }

                                            Toast.makeText(context, "Préset '${preset.name}' appliqué !", Toast.LENGTH_SHORT).show()
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
                                        color = nothingTextPrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CARTE PRINCIPALE ENGLOBANT LES ONGLETS ET LEUR CONTENU
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = nothingSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, nothingBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    val tab0Text by animateColorAsState(if (activeTab == 0) Color.White else nothingTextSecondary, animationSpec = tween(160), label = "tab0Text")
                    val tab1Text by animateColorAsState(if (activeTab == 1) Color.White else nothingTextSecondary, animationSpec = tween(160), label = "tab1Text")
                    val tab2Text by animateColorAsState(if (activeTab == 2) Color.White else nothingTextSecondary, animationSpec = tween(160), label = "tab2Text")

                    var barWidthDp by remember { mutableStateOf(0.dp) }
                    val density = LocalDensity.current
                    val snappyCurve = remember { CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f) }

                    val animatedIndicatorIndex by animateFloatAsState(
                        targetValue = activeTab.toFloat(),
                        animationSpec = tween(160, easing = snappyCurve),
                        label = "tabIndicatorPosition"
                    )

                    // Tab Bar Segmentée
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .onSizeChanged { size ->
                                barWidthDp = with(density) { size.width.toDp() }
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFD2E3F5))
                            .padding(4.dp)
                    ) {
                        if (barWidthDp > 0.dp) {
                            val availableWidth = barWidthDp - 8.dp
                            val w0 = availableWidth * (1.2f / 3.4f)
                            val w1 = availableWidth * (0.9f / 3.4f)
                            val w2 = availableWidth * (1.3f / 3.4f)

                            val x0 = 0.dp
                            val x1 = w0
                            val x2 = w0 + w1

                            val currentOffsetX = when {
                                animatedIndicatorIndex <= 1f -> {
                                    val t = animatedIndicatorIndex.coerceIn(0f, 1f)
                                    x0 + (x1 - x0) * t
                                }
                                else -> {
                                    val t = (animatedIndicatorIndex - 1f).coerceIn(0f, 1f)
                                    x1 + (x2 - x1) * t
                                }
                            }

                            val currentIndicatorWidth = when {
                                animatedIndicatorIndex <= 1f -> {
                                    val t = animatedIndicatorIndex.coerceIn(0f, 1f)
                                    w0 + (w1 - w0) * t
                                }
                                else -> {
                                    val t = (animatedIndicatorIndex - 1f).coerceIn(0f, 1f)
                                    w1 + (w2 - w1) * t
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .offset(x = currentOffsetX)
                                    .width(currentIndicatorWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(nothingBlack)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        activeTab = 0
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "personnage",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = tab0Text
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        activeTab = 1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "fond",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = tab1Text
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        activeTab = 2
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "planification",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = tab2Text
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                    if (allCharacters.isEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "AUCUN PERSONNAGE IMPORTÉ",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = nothingTextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    targetImportFolder = rootDir
                                                    zipPickerLauncher.launch("application/zip")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                                                shape = RoundedCornerShape(18.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth(0.9f)
                                                    .height(54.dp)
                                            ) {
                                                Text(
                                                    text = "IMPORTER UN PERSONNAGE (.ZIP)",
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Arborescence",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = nothingTextSecondary
                                                )

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            targetFolderForNewSub = rootDir
                                                            folderNameInput = ""
                                                            showCreateFolderDialog = true
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(1.dp, nothingBorder)
                                                    ) {
                                                        Text("+ Dossier", fontSize = 10.sp, color = nothingTextPrimary)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            targetImportFolder = rootDir
                                                            zipPickerLauncher.launch("application/zip")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("+ Importer", fontSize = 10.sp, color = Color.White)
                                                    }
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = nothingBorder)

                                            rootNode?.let { node ->
                                                IdeTreeViewNode(
                                                    node = node,
                                                    level = 0,
                                                    selectedCharacterPath = selectedCharacter?.folderPath ?: "",
                                                    onSelectCharacter = { charItem ->
                                                        updateSelectedCharacterAndVariant(charItem)
                                                    },
                                                    onNewSubFolder = { folderFile ->
                                                        targetFolderForNewSub = folderFile
                                                        folderNameInput = ""
                                                        showCreateFolderDialog = true
                                                    },
                                                    onImportZip = { folderFile ->
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
                                        }
                                    }
                                }
                                1 -> {
                                    // ONGLET 1: FOND
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Couleur de Fond",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = nothingTextPrimary
                                        )

                                        ColorSpectrumPicker(
                                            initialColor = pickedColor,
                                            onColorSelected = { color ->
                                                pickedColor = color
                                            }
                                        )

                                        Button(
                                            onClick = {
                                                clearOldBgFiles()
                                                bgType = WaifuPreferences.TYPE_COLOR
                                                Toast.makeText(context, "Couleur appliquée à l'aperçu !", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Appliquer cette couleur", fontSize = 11.sp, color = Color.White)
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = nothingBorder)

                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { imagePickerLauncher.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = nothingBlack,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text("Image", fontSize = 12.sp, color = Color.White)
                                            }

                                            Button(
                                                onClick = { videoPickerLauncher.launch("video/*") },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = nothingBlack,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text("Vidéo", fontSize = 12.sp, color = Color.White)
                                            }

                                            if (bgType != WaifuPreferences.TYPE_NONE) {
                                                OutlinedButton(
                                                    onClick = {
                                                        clearOldBgFiles()
                                                        bgType = WaifuPreferences.TYPE_NONE
                                                        Toast.makeText(context, "Fond réinitialisé", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = BorderStroke(1.dp, nothingBorder)
                                                ) {
                                                    Text("Effacer", fontSize = 12.sp, color = nothingTextPrimary)
                                                }
                                            }
                                        }

                                        if (bgType == WaifuPreferences.TYPE_IMAGE || bgType == WaifuPreferences.TYPE_VIDEO) {
                                            TextButton(
                                                onClick = {
                                                    Toast.makeText(context, "Position du fond réinitialisée", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("Réinitialiser position du fond", fontSize = 11.sp, color = nothingTextSecondary)
                                            }
                                        }

                                        if (bgType == WaifuPreferences.TYPE_VIDEO) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Couper le son vidéo", fontSize = 11.sp, color = nothingTextSecondary)
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

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Musique de fond", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Button(
                                                        onClick = { musicPickerLauncher.launch("audio/*") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Choisir audio", fontSize = 11.sp, color = Color.White)
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
                                                    Text("Volume : ${(musicVolume * 100).toInt()}%", fontSize = 11.sp, color = nothingTextSecondary)
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
                                    // ONGLET 2: PLANIFICATION
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
                                            Text("Activer la planification globale", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)
                                            Switch(
                                                checked = isScheduleEnabled,
                                                onCheckedChange = { enabled ->
                                                    isScheduleEnabled = enabled
                                                    prefs.isScheduleEnabled = enabled
                                                }
                                            )
                                        }

                                        HorizontalDivider(color = nothingBorder)

                                        Text("Sélectionner un jour à configurer :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

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

                                        HorizontalDivider(color = nothingBorder)

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
                                                    Text("Pause le $selectedDayLabel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)
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

                                                        Text("Plage : $startStr à $endStr", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = nothingTextPrimary)

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
                                                                Text("Début", fontSize = 10.sp, color = Color.White)
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
                                                                Text("Fin", fontSize = 10.sp, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = nothingBorder)

                                        Text("Fond de remplacement pendant la pause :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = nothingTextPrimary)
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
                                                label = { Text("Noir Pur", fontSize = 11.sp) },
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
                                                label = { Text("Image de secours", fontSize = 11.sp) },
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
                                                    Text("Choisir image", fontSize = 10.sp, color = Color.White)
                                                }
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = nothingBorder)

                                        RagebaitButton(
                                            isRagebaitEnabled = isRagebaitEnabled,
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
                when (bgType) {
                    WaifuPreferences.TYPE_COLOR -> {
                        Box(modifier = Modifier.fillMaxSize().background(pickedColor))
                    }
                    WaifuPreferences.TYPE_IMAGE -> {
                        if (prefs.bgImagePath.isNotEmpty() && File(prefs.bgImagePath).exists()) {
                            val bitmap = remember(prefs.bgImagePath) {
                                BitmapFactory.decodeFile(prefs.bgImagePath)?.asImageBitmap()
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Fond d'écran",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = prefs.bgScale
                                            scaleY = prefs.bgScale
                                            translationX = prefs.bgOffsetX
                                            translationY = prefs.bgOffsetY
                                        }
                                )
                            }
                        }
                    }
                    WaifuPreferences.TYPE_VIDEO -> {
                        if (prefs.bgVideoPath.isNotEmpty() && File(prefs.bgVideoPath).exists()) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(prefs.bgVideoPath)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            val vol = if (isVideoMuted) 0f else 0.8f
                                            mp.setVolume(vol, vol)
                                            mp.start()
                                        }
                                    }
                                },
                                update = { view ->
                                    if (!view.isPlaying) view.start()
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = prefs.bgScale
                                        scaleY = prefs.bgScale
                                        translationX = prefs.bgOffsetX
                                        translationY = prefs.bgOffsetY
                                    }
                            )
                        }
                    }
                }

                // RENDU EN PLEIN ÉCRAN
                key(isFullscreen) {
                    if (isFullscreen) {
                        Live2DWebView(
                            modelJsonPath = currentSkelPath,
                            scale = currentScale,
                            offsetX = currentOffsetX,
                            offsetY = currentOffsetY,
                            doubleTapTrigger = doubleTapTrigger,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // VOILE OBSCURCISSANT & INDICATEUR DE CHARGEMENT
                TopLevelAnimatedVisibility(
                    visible = isFullscreenLoading || isModelLoading,
                    enter = fadeIn(tween(180, easing = LinearOutSlowInEasing)),
                    exit = fadeOut(tween(250, easing = FastOutLinearInEasing)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF08132B).copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NothingLoadingIndicator()
                    }
                }

                // BARRE ET COMMANDES EN SURIMPOSITION DU PLEIN ÉCRAN
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
                        Text("APPLIQUER", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = { showTransformDialog = true },
                    shape = CircleShape,
                    color = Color(0xFFEDF4FC).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, nothingBorder),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp, start = 20.dp)
                        .size(52.dp)
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
            }
        }
    }
}

// PIPETTE DE COULEUR AZUR
@Composable
fun ColorSpectrumPicker(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFB4CDEB), RoundedCornerShape(16.dp))
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
                    radius = 12.dp.toPx(),
                    center = Offset(indicatorX, indicatorY),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = currentColor,
                    radius = 9.dp.toPx(),
                    center = Offset(indicatorX, indicatorY)
                )
            }
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
                            text = "[DOSSIER]",
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
                            text = { Text("+ Nouveau sous-dossier", fontSize = 12.sp, color = Color(0xFF0D1B3E)) },
                            onClick = {
                                showContextMenu = false
                                onNewSubFolder(File(node.absolutePath))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("+ Importer (.zip) ici", fontSize = 12.sp, color = Color(0xFF0D1B3E)) },
                            onClick = {
                                showContextMenu = false
                                onImportZip(File(node.absolutePath))
                            }
                        )
                        if (level > 0) {
                            DropdownMenuItem(
                                text = { Text("Renommer le dossier", fontSize = 12.sp, color = Color(0xFF0D1B3E)) },
                                onClick = {
                                    showContextMenu = false
                                    onRename(node)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Déplacer le dossier", fontSize = 12.sp, color = Color(0xFF0D1B3E)) },
                                onClick = {
                                    showContextMenu = false
                                    onMove(node)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer le dossier", fontSize = 12.sp, color = Color(0xFFD9383A)) },
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
                            onDelete = onDelete
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
                    Text(
                        text = "[PERSONNAGE]",
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
                        text = { Text("Renommer", fontSize = 12.sp, color = Color(0xFF0D1B3E)) },
                        onClick = {
                            showContextMenu = false
                            onRename(node)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Déplacer vers un dossier", fontSize = 12.sp, color = Color(0xFF0D1B3E)) },
                        onClick = {
                            showContextMenu = false
                            onMove(node)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", fontSize = 12.sp, color = Color(0xFFD9383A)) },
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
