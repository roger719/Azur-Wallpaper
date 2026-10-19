package com.example.waifuengine.workshop

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.waifuengine.data.AppStrings
import com.example.waifuengine.data.CharacterItem
import com.example.waifuengine.data.CharacterRepository
import com.example.waifuengine.data.EngineType
import com.example.waifuengine.data.WaifuPreferences
import com.example.waifuengine.live2d.Live2DWebView
import com.example.waifuengine.spine.SpineWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val nothingBg = Color(0xFFDCE8F6)
private val nothingSurface = Color(0xFFEDF4FC)
private val nothingBorder = Color(0xFFB4CDEB)
private val nothingTextPrimary = Color(0xFF0D1B3E)
private val nothingTextSecondary = Color(0xFF2D4B75)
private val nothingBlack = Color(0xFF0B1B3D)
private val accentRed = Color(0xFFD9383A)
private val accentGreen = Color(0xFF2E7D32)

data class WorkshopCharacterGroup(
    val characterName: String,
    val game: String,
    val engine: EngineType,
    val models: List<WorkshopModel>
) {
    val id: String get() = "$game:$characterName"
    val totalSize: Long get() = models.sumOf { it.totalSize }
    val isMultiSkin: Boolean get() = models.size > 1
}

@Composable
private fun ModelActionButtons(
    modifier: Modifier = Modifier.width(156.dp),
    isInstalled: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    isPreviewing: Boolean,
    previewProgress: Float,
    strings: AppStrings,
    onPreview: () -> Unit,
    onApply: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = modifier.height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // BOUTON APERÇU (ENCADRÉ)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = nothingSurface,
            border = BorderStroke(1.dp, nothingBorder),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(enabled = !isPreviewing && !isDownloading) { onPreview() }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
            ) {
                if (isPreviewing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            color = nothingTextPrimary,
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = "${(previewProgress * 100).toInt()}%",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = nothingTextPrimary
                        )
                    }
                } else {
                    Text(
                        text = strings.btnPreview,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = nothingTextPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        // BOUTON TÉLÉCHARGER / APPLIQUER (ENCADRÉ)
        if (isInstalled) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = nothingBlack,
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .clickable { onApply() }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = strings.btnApply,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFE5E5),
                border = BorderStroke(1.dp, Color(0xFFFFB3B3)),
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .clickable { onDelete() }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "🗑",
                        fontSize = 11.sp,
                        color = accentRed
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = nothingBlack,
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .clickable(enabled = !isDownloading && !isPreviewing) { onDownload() }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                ) {
                    if (isDownloading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = Color.White,
                                strokeWidth = 1.5.dp
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = strings.btnDownload,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkshopPreviewDialog(
    characterItem: CharacterItem,
    model: WorkshopModel,
    isInstalled: Boolean,
    strings: AppStrings,
    onApply: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = nothingSurface,
            border = BorderStroke(1.dp, nothingBorder),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // EN-TÊTE DE L'APERÇU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = model.name,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = nothingTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (model.engine == EngineType.SPINE) Color(0xFFB8D3F5) else Color(0xFFD6C8F5)
                            ) {
                                Text(
                                    text = if (model.engine == EngineType.SPINE) "SPINE" else "LIVE2D",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isInstalled) "Modèle installé • Aperçu interactif" else "Aperçu temporaire (non téléchargé)",
                            fontSize = 10.sp,
                            color = if (isInstalled) accentGreen else nothingTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = nothingBlack,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 1.dp)

                // ZONE DE RENDU INTERACTIF
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(nothingBg)
                        .border(1.dp, nothingBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val variant = characterItem.variants.firstOrNull()
                    if (variant != null) {
                        if (characterItem.engineType == EngineType.SPINE) {
                            SpineWebView(
                                modelPath = variant.modelJsonPath,
                                atlasPath = variant.spineAtlasPath,
                                scale = 1.0f,
                                isPointerTrackingEnabled = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Live2DWebView(
                                modelJsonPath = variant.modelJsonPath,
                                scale = 1.0f,
                                isPointerTrackingEnabled = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Text(
                            text = "Format de modèle non reconnu.",
                            fontSize = 12.sp,
                            color = nothingTextSecondary
                        )
                    }
                }

                // BARRE D'ACTION INFÉRIEURE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, nothingBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = strings.close,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = nothingTextPrimary
                        )
                    }

                    if (!isInstalled) {
                        Button(
                            onClick = onInstall,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = strings.btnKeep,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = onApply,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text(
                            text = strings.apply,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkshopDialog(
    prefs: WaifuPreferences,
    onDismiss: () -> Unit,
    onModelApplied: (CharacterItem) -> Unit,
    onTreeUpdated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val strings = remember(prefs.appLanguage) { AppStrings.get(prefs) }

    var repoUrlInput by remember {
        mutableStateOf(prefs.workshopRepoUrl)
    }
    var tokenInput by remember {
        mutableStateOf(prefs.workshopGithubToken)
    }
    var isConfigMode by remember {
        mutableStateOf(prefs.workshopRepoUrl.isBlank())
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var workshopModels by remember { mutableStateOf<List<WorkshopModel>>(emptyList()) }
    var selectedGameFilter by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var downloadingModelId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }

    // Version locale pour forcer le rafraîchissement
    var localInstalledVersion by remember { mutableStateOf(0) }
    var expandedGroupKeys by remember { mutableStateOf(setOf<String>()) }

    // États de prévisualisation (Aperçu)
    var previewCharacterItem by remember { mutableStateOf<CharacterItem?>(null) }
    var previewModel by remember { mutableStateOf<WorkshopModel?>(null) }
    var isPreviewInstalled by remember { mutableStateOf(false) }
    var previewingModelId by remember { mutableStateOf<String?>(null) }
    var previewDownloadProgress by remember { mutableStateOf(0f) }

    fun dismissWorkshop() {
        WorkshopManager.clearTempPreview(context)
        onDismiss()
    }

    // Chargement asynchrone des identifiants installés en mémoire pour éviter tout accès disque pendant le scroll
    val installedIdentifiers by produceState(initialValue = emptySet<String>(), localInstalledVersion) {
        value = withContext(Dispatchers.IO) {
            WorkshopManager.getInstalledModelIdentifiers(context)
        }
    }

    fun applyModel(model: WorkshopModel) {
        val allChars = CharacterRepository.loadAllCustomCharacters(context)
        val found = allChars.firstOrNull { it.name.equals(model.name, ignoreCase = true) }
            ?: allChars.firstOrNull { it.folderPath.contains(model.name, ignoreCase = true) }
        if (found != null) {
            onModelApplied(found)
            dismissWorkshop()
            Toast.makeText(context, "${model.name} appliqué en fond d'écran !", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Modèle introuvable dans les fichiers locaux", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadModel(model: WorkshopModel) {
        coroutineScope.launch {
            downloadingModelId = model.id
            downloadProgress = 0f
            val success = WorkshopManager.downloadModel(
                context = context,
                model = model,
                repoUrl = prefs.workshopRepoUrl,
                token = prefs.workshopGithubToken,
                onProgress = { downloadProgress = it }
            )
            downloadingModelId = null
            if (success) {
                localInstalledVersion++
                onTreeUpdated()
                Toast.makeText(context, "${model.name} téléchargé avec succès !", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Échec du téléchargement de ${model.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun previewModel(model: WorkshopModel) {
        val isInstalled = WorkshopManager.isModelInstalledFast(model, installedIdentifiers)
        if (isInstalled) {
            val allChars = CharacterRepository.loadAllCustomCharacters(context)
            val found = allChars.firstOrNull { it.name.equals(model.name, ignoreCase = true) }
                ?: allChars.firstOrNull { it.folderPath.contains(model.name, ignoreCase = true) }
            if (found != null) {
                previewCharacterItem = found
                previewModel = model
                isPreviewInstalled = true
            } else {
                Toast.makeText(context, "Modèle introuvable dans les fichiers locaux", Toast.LENGTH_SHORT).show()
            }
        } else {
            coroutineScope.launch {
                previewingModelId = model.id
                previewDownloadProgress = 0f
                val charItem = WorkshopManager.downloadModelForPreview(
                    context = context,
                    model = model,
                    repoUrl = prefs.workshopRepoUrl,
                    token = prefs.workshopGithubToken,
                    onProgress = { previewDownloadProgress = it }
                )
                previewingModelId = null
                if (charItem != null) {
                    previewCharacterItem = charItem
                    previewModel = model
                    isPreviewInstalled = false
                } else {
                    Toast.makeText(context, "Échec du téléchargement de l'aperçu", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteModel(model: WorkshopModel) {
        val deleted = WorkshopManager.deleteInstalledModel(context, model)
        if (deleted) {
            localInstalledVersion++
            onTreeUpdated()
            Toast.makeText(context, "${model.name} supprimé du téléphone.", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadModels(forceRefresh: Boolean = false) {
        val url = prefs.workshopRepoUrl
        if (url.isBlank()) {
            isConfigMode = true
            return
        }
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            if (!forceRefresh) {
                val cached = withContext(Dispatchers.IO) {
                    WorkshopManager.loadCachedModels(context, url)
                }
                if (cached != null && cached.isNotEmpty()) {
                    workshopModels = cached
                    isLoading = false
                    return@launch
                }
            }
            val result = WorkshopManager.fetchWorkshopModels(url, prefs.workshopGithubToken, context)
            isLoading = false
            result.onSuccess { models ->
                workshopModels = models
                errorMessage = null
            }.onFailure { err ->
                errorMessage = err.message ?: "Erreur inconnue lors du chargement."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (prefs.workshopRepoUrl.isNotBlank() && workshopModels.isEmpty()) {
            loadModels(forceRefresh = false)
        }
    }

    Dialog(
        onDismissRequest = { dismissWorkshop() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = nothingSurface,
            border = BorderStroke(1.dp, nothingBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // EN-TÊTE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WORKSHOP",
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = nothingTextPrimary
                        )
                        Text(
                            text = if (isConfigMode) "Configuration du dépôt" else "Bibliothèque Spine & Live2D distante",
                            fontSize = 10.sp,
                            color = nothingTextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isConfigMode) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = nothingBg,
                                border = BorderStroke(1.dp, nothingBorder),
                                modifier = Modifier.clickable { loadModels(forceRefresh = true) }
                            ) {
                                Text(
                                    text = "↻",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = nothingBg,
                                border = BorderStroke(1.dp, nothingBorder),
                                modifier = Modifier.clickable { isConfigMode = true }
                            ) {
                                Text(
                                    text = "Dépôt",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = nothingBlack,
                            modifier = Modifier.clickable { dismissWorkshop() }
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 1.dp)

                // MODE CONFIGURATION DU DÉPÔT
                if (isConfigMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = nothingBg),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Lier votre base de données GitHub",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = "Renseignez l'URL de votre dépôt GitHub. L'application découvrira et classera automatiquement chaque personnage selon vos sous-dossiers par jeu (Arknights, NIKKE, Brown Dust 2, Azure Lane...).",
                                    fontSize = 11.sp,
                                    color = nothingTextSecondary
                                )

                                OutlinedTextField(
                                    value = repoUrlInput,
                                    onValueChange = { repoUrlInput = it },
                                    label = { Text("URL DU DÉPÔT GITHUB", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                    placeholder = { Text("https://github.com/roger719/Azur-Database", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = nothingTextPrimary,
                                        unfocusedBorderColor = nothingBorder,
                                        focusedTextColor = nothingTextPrimary,
                                        unfocusedTextColor = nothingTextPrimary
                                    )
                                )

                                OutlinedTextField(
                                    value = tokenInput,
                                    onValueChange = { tokenInput = it },
                                    label = { Text("CLÉ D'ACCÈS / TOKEN PERSONNEL (PAT)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                    placeholder = { Text("ghp_... (Requis pour dépôt privé)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = nothingTextPrimary,
                                        unfocusedBorderColor = nothingBorder,
                                        focusedTextColor = nothingTextPrimary,
                                        unfocusedTextColor = nothingTextPrimary
                                    )
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isTokenPresent = tokenInput.trim().isNotBlank()
                                    Text(
                                        text = if (isTokenPresent) "● Clé d'accès renseignée" else "○ Dépôt public uniquement (aucune clé)",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTokenPresent) accentGreen else nothingTextSecondary
                                    )
                                }

                                Text(
                                    text = "Astuce : Pour un dépôt privé GitHub, utilisez un Token avec accès 'repo' ou 'contents:read'.",
                                    fontSize = 10.sp,
                                    color = nothingTextSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (prefs.workshopRepoUrl.isNotBlank()) {
                                TextButton(
                                    onClick = { isConfigMode = false },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Annuler", fontFamily = FontFamily.Monospace, color = nothingTextSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Button(
                                onClick = {
                                    val cleanUrl = repoUrlInput.trim()
                                    val cleanTok = WorkshopManager.sanitizeToken(tokenInput)
                                    if (cleanUrl.isNotEmpty()) {
                                        prefs.workshopRepoUrl = cleanUrl
                                        prefs.workshopGithubToken = cleanTok
                                        isConfigMode = false
                                        loadModels()
                                        Toast.makeText(context, "Dépôt et clé d'accès enregistrés !", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Veuillez renseigner une URL valide", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "ENREGISTRER & CHARGER",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // MODE PARCOURS DU WORKSHOP
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = nothingBlack)
                                Text(
                                    text = "Exploration du dépôt GitHub...",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = "Classement des modèles par jeu en cours",
                                    fontSize = 10.sp,
                                    color = nothingTextSecondary
                                )
                            }
                        }
                    } else if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Échec du chargement",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = accentRed
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = nothingTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { loadModels() },
                                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("RÉESSAYER", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    OutlinedButton(
                                        onClick = { isConfigMode = true },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, nothingBorder)
                                    ) {
                                        Text("DÉPÔT & CLÉ D'ACCÈS", color = nothingTextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    } else {
                        // BARRE DE RECHERCHE
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("recher", fontSize = 11.sp, color = nothingTextSecondary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    Text(
                                        text = "✕",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = nothingTextSecondary,
                                        modifier = Modifier
                                            .clickable { searchQuery = "" }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = nothingTextPrimary,
                                unfocusedBorderColor = nothingBorder,
                                focusedTextColor = nothingTextPrimary,
                                unfocusedTextColor = nothingTextPrimary,
                                focusedContainerColor = nothingBg,
                                unfocusedContainerColor = nothingBg
                            )
                        )

                        // REGROUPEMENT PAR PERSONNAGE
                        val characterGroups = remember(workshopModels) {
                            workshopModels.groupBy { model ->
                                Pair(model.game, WorkshopManager.extractBaseCharacterName(model.name))
                            }.map { (pair, list) ->
                                WorkshopCharacterGroup(
                                    characterName = pair.second,
                                    game = pair.first,
                                    engine = list.first().engine,
                                    models = list.sortedBy { it.name }
                                )
                            }.sortedBy { it.characterName.lowercase(Locale.ROOT) }
                        }

                        // FILTRES PAR JEU
                        val games = remember(characterGroups) {
                            listOf("Tous") + characterGroups.map { it.game }.distinct().sorted()
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(games) { game ->
                                val isSelected = selectedGameFilter == game
                                val count = if (game == "Tous") characterGroups.size else characterGroups.count { it.game == game }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) nothingBlack else nothingBg,
                                    border = if (!isSelected) BorderStroke(1.dp, nothingBorder) else null,
                                    modifier = Modifier.clickable { selectedGameFilter = game }
                                ) {
                                    Text(
                                        text = "$game ($count)",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else nothingTextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // FILTRAGE ET CHARGEMENT PROGRESSIF (VIRTUALISATION ET FLUIDITÉ OPTIMISÉE)
                        val filteredGroups = remember(characterGroups, selectedGameFilter, searchQuery) {
                            characterGroups.filter { group ->
                                val matchGame = selectedGameFilter == "Tous" || group.game.equals(selectedGameFilter, ignoreCase = true)
                                val matchQuery = searchQuery.isBlank() ||
                                        group.characterName.contains(searchQuery, ignoreCase = true) ||
                                        group.game.contains(searchQuery, ignoreCase = true) ||
                                        group.models.any { it.name.contains(searchQuery, ignoreCase = true) }
                                matchGame && matchQuery
                            }
                        }

                        var displayedLimit by remember { mutableStateOf(25) }
                        LaunchedEffect(searchQuery, selectedGameFilter) {
                            displayedLimit = 25
                        }

                        val listState = rememberLazyListState()
                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                lastIndex >= displayedLimit - 4
                            }
                        }
                        LaunchedEffect(shouldLoadMore) {
                            if (shouldLoadMore && displayedLimit < filteredGroups.size) {
                                displayedLimit = minOf(displayedLimit + 25, filteredGroups.size)
                            }
                        }

                        val displayedGroups = remember(filteredGroups, displayedLimit) {
                            filteredGroups.take(displayedLimit)
                        }

                        if (filteredGroups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucun personnage correspondant trouvé.",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = nothingTextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(displayedGroups, key = { it.id }) { group ->
                                    if (!group.isMultiSkin) {
                                        // DISPOSITION 2 : MODÈLE INDIVIDUEL (EXACTEMENT SELON LE SCHÉMA DU BAS)
                                        val singleModel = group.models.first()
                                        val isInstalled = WorkshopManager.isModelInstalledFast(singleModel, installedIdentifiers)
                                        val isDownloading = downloadingModelId == singleModel.id
                                        val isPreviewing = previewingModelId == singleModel.id

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = nothingBg),
                                            border = BorderStroke(1.dp, nothingBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // GAUCHE : ENCADRÉ (NOM + TYPE FICHIER) + (SÉRIE • POIDS)
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // ENCADRÉ DU HAUT : NOM + BADGE TYPE
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = nothingSurface,
                                                        border = BorderStroke(1.dp, nothingBorder),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = singleModel.name,
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = nothingTextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = if (group.engine == EngineType.SPINE) Color(0xFFB8D3F5) else Color(0xFFD6C8F5)
                                                            ) {
                                                                Text(
                                                                    text = if (group.engine == EngineType.SPINE) "SPINE" else "LIVE2D",
                                                                    fontSize = 8.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = nothingTextPrimary,
                                                                    maxLines = 1,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // DU BAS : SÉRIE • POIDS
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(start = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = group.game,
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Medium,
                                                            color = nothingTextSecondary
                                                        )
                                                        Text(
                                                            text = "•",
                                                            fontSize = 9.sp,
                                                            color = nothingBorder
                                                        )
                                                        Text(
                                                            text = WorkshopManager.formatSize(singleModel.totalSize),
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = nothingTextSecondary
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // DROITE : BOUTONS APERÇU ET TÉLÉCHARGER (OU APPLIQUER)
                                                ModelActionButtons(
                                                    modifier = Modifier.width(156.dp),
                                                    isInstalled = isInstalled,
                                                    isDownloading = isDownloading,
                                                    downloadProgress = downloadProgress,
                                                    isPreviewing = isPreviewing,
                                                    previewProgress = previewDownloadProgress,
                                                    strings = strings,
                                                    onPreview = { previewModel(singleModel) },
                                                    onApply = { applyModel(singleModel) },
                                                    onDownload = { downloadModel(singleModel) },
                                                    onDelete = { deleteModel(singleModel) }
                                                )
                                            }
                                        }
                                    } else {
                                        // DISPOSITION 1 : GROUPE MULTI-SKINS (EXACTEMENT SELON LE SCHÉMA DU HAUT)
                                        val installedCount = group.models.count { WorkshopManager.isModelInstalledFast(it, installedIdentifiers) }
                                        val isExpanded = expandedGroupKeys.contains(group.id) || (searchQuery.isNotBlank() && filteredGroups.size <= 5)

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = nothingBg),
                                            border = BorderStroke(1.dp, nothingBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // GAUCHE : ENCADRÉ (NOM + NBR DE SKIN + TYPE FICHIER) + (SÉRIE)
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        // ENCADRÉ DU HAUT : NOM + NBR DE SKINS + BADGE TYPE
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = nothingSurface,
                                                            border = BorderStroke(1.dp, nothingBorder),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = group.characterName,
                                                                    fontSize = 11.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = nothingTextPrimary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Row(
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = nothingBlack
                                                                    ) {
                                                                        Text(
                                                                            text = "${group.models.size} ${strings.skinsCount}",
                                                                            fontSize = 8.sp,
                                                                            fontFamily = FontFamily.Monospace,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color.White,
                                                                            maxLines = 1,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = if (group.engine == EngineType.SPINE) Color(0xFFB8D3F5) else Color(0xFFD6C8F5)
                                                                    ) {
                                                                        Text(
                                                                            text = if (group.engine == EngineType.SPINE) "SPINE" else "LIVE2D",
                                                                            fontSize = 8.sp,
                                                                            fontFamily = FontFamily.Monospace,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = nothingTextPrimary,
                                                                            maxLines = 1,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        // DU BAS : SÉRIE • TAILLE (ET NOMBRE INSTALLÉ)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.padding(start = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = group.game,
                                                                fontSize = 9.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Medium,
                                                                color = nothingTextSecondary
                                                            )
                                                            Text(
                                                                text = "•",
                                                                fontSize = 9.sp,
                                                                color = nothingBorder
                                                            )
                                                            Text(
                                                                text = WorkshopManager.formatSize(group.totalSize),
                                                                fontSize = 9.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = nothingTextSecondary
                                                            )
                                                            if (installedCount > 0) {
                                                                Text(
                                                                    text = "• $installedCount/${group.models.size} ${strings.installedStatus}",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = accentGreen
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // DROITE : BOUTON "VOIR ↓" OU "REPLIER ↑" (EXACTEMENT SELON LE SCHÉMA)
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = nothingSurface,
                                                        border = BorderStroke(1.dp, nothingBorder),
                                                        modifier = Modifier
                                                            .width(156.dp)
                                                            .height(38.dp)
                                                            .clickable {
                                                                expandedGroupKeys = if (expandedGroupKeys.contains(group.id)) {
                                                                    expandedGroupKeys - group.id
                                                                } else {
                                                                    expandedGroupKeys + group.id
                                                                }
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = if (isExpanded) strings.collapseUp else strings.viewDown,
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = nothingTextPrimary
                                                            )
                                                        }
                                                    }
                                                }

                                                // SOUS-ÉLÉMENTS DU GROUPE (DÉPLIÉ)
                                                if (isExpanded) {
                                                    HorizontalDivider(color = nothingBorder, thickness = 0.5.dp)
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        group.models.forEach { model ->
                                                            val isSkinInstalled = WorkshopManager.isModelInstalledFast(model, installedIdentifiers)
                                                            val isSkinDownloading = downloadingModelId == model.id
                                                            val isSkinPreviewing = previewingModelId == model.id

                                                            // CHAQUE SKIN DÉPLIÉ ADOPTE LE SCHÉMA DU BAS
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                                                ) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        color = nothingSurface,
                                                                        border = BorderStroke(1.dp, nothingBorder),
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                                        ) {
                                                                            Text(
                                                                                text = model.name,
                                                                                fontSize = 10.sp,
                                                                                fontFamily = FontFamily.Monospace,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = nothingTextPrimary,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis,
                                                                                modifier = Modifier.weight(1f, fill = false)
                                                                            )
                                                                            Spacer(modifier = Modifier.width(4.dp))
                                                                            Surface(
                                                                                shape = RoundedCornerShape(3.dp),
                                                                                color = if (model.engine == EngineType.SPINE) Color(0xFFB8D3F5) else Color(0xFFD6C8F5)
                                                                            ) {
                                                                                Text(
                                                                                    text = if (model.engine == EngineType.SPINE) "SPINE" else "LIVE2D",
                                                                                    fontSize = 7.sp,
                                                                                    fontFamily = FontFamily.Monospace,
                                                                                    fontWeight = FontWeight.Bold,
                                                                                    color = nothingTextPrimary,
                                                                                    maxLines = 1,
                                                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                        modifier = Modifier.padding(start = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = model.game,
                                                                            fontSize = 8.sp,
                                                                            fontFamily = FontFamily.Monospace,
                                                                            color = nothingTextSecondary
                                                                        )
                                                                        Text(
                                                                            text = "•",
                                                                            fontSize = 8.sp,
                                                                            color = nothingBorder
                                                                        )
                                                                        Text(
                                                                            text = WorkshopManager.formatSize(model.totalSize),
                                                                            fontSize = 8.sp,
                                                                            fontFamily = FontFamily.Monospace,
                                                                            color = nothingTextSecondary
                                                                        )
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.width(8.dp))

                                                                ModelActionButtons(
                                                                    modifier = Modifier.width(156.dp),
                                                                    isInstalled = isSkinInstalled,
                                                                    isDownloading = isSkinDownloading,
                                                                    downloadProgress = downloadProgress,
                                                                    isPreviewing = isSkinPreviewing,
                                                                    previewProgress = previewDownloadProgress,
                                                                    strings = strings,
                                                                    onPreview = { previewModel(model) },
                                                                    onApply = { applyModel(model) },
                                                                    onDownload = { downloadModel(model) },
                                                                    onDelete = { deleteModel(model) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (displayedLimit < filteredGroups.size) {
                                    item {
                                        val remaining = filteredGroups.size - displayedLimit
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = nothingBg,
                                            border = BorderStroke(1.dp, nothingBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { displayedLimit = minOf(displayedLimit + 25, filteredGroups.size) }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Charger plus ($remaining restants)...",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = nothingTextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (previewCharacterItem != null && previewModel != null) {
        val currentItem = previewCharacterItem!!
        val currentModel = previewModel!!
        WorkshopPreviewDialog(
            characterItem = currentItem,
            model = currentModel,
            isInstalled = isPreviewInstalled,
            strings = strings,
            onApply = {
                if (!isPreviewInstalled) {
                    WorkshopManager.installFromPreview(context, currentModel)
                    localInstalledVersion++
                    onTreeUpdated()
                }
                previewCharacterItem = null
                previewModel = null
                applyModel(currentModel)
            },
            onInstall = {
                val ok = WorkshopManager.installFromPreview(context, currentModel)
                if (ok) {
                    localInstalledVersion++
                    onTreeUpdated()
                    isPreviewInstalled = true
                    Toast.makeText(context, "${currentModel.name} installé avec succès !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Échec de l'installation", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                previewCharacterItem = null
                previewModel = null
            }
        )
    }
}
