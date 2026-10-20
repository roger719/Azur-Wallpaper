package com.example.waifuengine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.waifuengine.data.AppStrings
import com.example.waifuengine.data.WaifuPreferences

@Composable
fun SettingsDialog(
    prefs: WaifuPreferences,
    currentLanguage: String,
    onLanguageChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Palette Officielle DA Azur
    val nothingBg = Color(0xFFDCE8F6)
    val nothingSurface = Color(0xFFEDF4FC)
    val nothingBorder = Color(0xFFB4CDEB)
    val nothingTextPrimary = Color(0xFF0D1B3E)
    val nothingTextSecondary = Color(0xFF2D4B75)
    val nothingBlack = Color(0xFF0B1B3D)
    val accentBlue = Color(0xFF1D4ED8)

    val strings = AppStrings(currentLanguage)
    var showChangelog by remember { mutableStateOf(false) }
    var forcePma by remember { mutableStateOf(prefs.isSpinePma) }
    var fpsLimit by remember { mutableIntStateOf(prefs.fpsLimit) }
    var antialias by remember { mutableStateOf(prefs.isAntialiasEnabled) }
    var renderScale by remember { mutableFloatStateOf(prefs.renderScale) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = nothingSurface,
            border = BorderStroke(1.2.dp, nothingBorder),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // EN-TÊTE AZUR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = nothingBlack,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "⚙",
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Column {
                            Text(
                                text = strings.settings,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = nothingTextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = strings.settingsSubtitle,
                                fontSize = 11.sp,
                                color = nothingTextSecondary
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, nothingBorder),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = nothingTextPrimary
                            )
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 1.dp)

                // SECTION LANGUE
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.languageSection,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = nothingTextPrimary,
                        letterSpacing = 0.5.sp
                    )

                    // Option Français
                    val isFr = currentLanguage == WaifuPreferences.LANGUAGE_FR
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isFr) nothingBlack else Color.White,
                        border = BorderStroke(1.2.dp, if (isFr) nothingBlack else nothingBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.appLanguage = WaifuPreferences.LANGUAGE_FR
                                onLanguageChanged(WaifuPreferences.LANGUAGE_FR)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strings.french,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFr) Color.White else nothingTextPrimary
                                )
                                Text(
                                    text = if (strings.isEn) "French" else "Français",
                                    fontSize = 10.sp,
                                    color = if (isFr) Color(0xFFC7D7EC) else nothingTextSecondary
                                )
                            }

                            if (isFr) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accentBlue
                                ) {
                                    Text(
                                        text = if (strings.isEn) "ACTIVE" else "ACTIF",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Option English
                    val isEn = currentLanguage == WaifuPreferences.LANGUAGE_EN
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isEn) nothingBlack else Color.White,
                        border = BorderStroke(1.2.dp, if (isEn) nothingBlack else nothingBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                prefs.appLanguage = WaifuPreferences.LANGUAGE_EN
                                onLanguageChanged(WaifuPreferences.LANGUAGE_EN)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = strings.english,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEn) Color.White else nothingTextPrimary
                                )
                                Text(
                                    text = if (strings.isEn) "English (Default)" else "Anglais (Par défaut)",
                                    fontSize = 10.sp,
                                    color = if (isEn) Color(0xFFC7D7EC) else nothingTextSecondary
                                )
                            }

                            if (isEn) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accentBlue
                                ) {
                                    Text(
                                        text = if (strings.isEn) "ACTIVE" else "ACTIF",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 1.dp)

                // SECTION PERFORMANCES & RENDU
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.performanceSection,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = nothingTextPrimary,
                        letterSpacing = 0.5.sp
                    )

                    // 1. Forcer le mode PMA
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, nothingBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = strings.forcePmaLabel,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = strings.forcePmaSubtitle,
                                    fontSize = 10.sp,
                                    color = nothingTextSecondary
                                )
                            }
                            Switch(
                                checked = forcePma,
                                onCheckedChange = { enabled ->
                                    forcePma = enabled
                                    prefs.isSpinePma = enabled
                                }
                            )
                        }
                    }

                    // 2. Limite de framerate (FPS)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, nothingBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.framerateLimitLabel,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = if (fpsLimit == 0) "120Hz" else "${fpsLimit} FPS",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = accentBlue
                                )
                            }
                            Text(
                                text = strings.framerateLimitSubtitle,
                                fontSize = 10.sp,
                                color = nothingTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val fpsOptions = listOf(30 to strings.fps30, 45 to strings.fps45, 60 to strings.fps60, 0 to strings.fpsMax)
                                fpsOptions.forEach { (fpsVal, label) ->
                                    val isSelected = fpsLimit == fpsVal
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            fpsLimit = fpsVal
                                            prefs.fpsLimit = fpsVal
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = nothingBlack,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Anticrénelage WebGL
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, nothingBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = strings.antialiasLabel,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = strings.antialiasSubtitle,
                                    fontSize = 10.sp,
                                    color = nothingTextSecondary
                                )
                            }
                            Switch(
                                checked = antialias,
                                onCheckedChange = { enabled ->
                                    antialias = enabled
                                    prefs.isAntialiasEnabled = enabled
                                }
                            )
                        }
                    }

                    // 4. Résolution de rendu
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, nothingBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.renderResolutionLabel,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = "${(renderScale * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = accentBlue
                                )
                            }
                            Text(
                                text = strings.renderResolutionSubtitle,
                                fontSize = 10.sp,
                                color = nothingTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val resOptions = listOf(1.0f to strings.res100, 0.75f to strings.res75, 0.5f to strings.res50)
                                resOptions.forEach { (scaleVal, label) ->
                                    val isSelected = Math.abs(renderScale - scaleVal) < 0.05f
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            renderScale = scaleVal
                                            prefs.renderScale = scaleVal
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = nothingBlack,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 1.dp)

                // SECTION CHANGELOG (JOURNAL DES VERSIONS)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, nothingBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChangelog = !showChangelog }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentBlue
                                ) {
                                    Text(
                                        text = "v2.0",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = strings.changelogTitle,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = nothingTextPrimary
                                )
                            }

                            Text(
                                text = if (showChangelog) "▲" else "▼",
                                fontSize = 11.sp,
                                color = nothingTextSecondary
                            )
                        }

                        if (showChangelog) {
                            HorizontalDivider(color = nothingBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (strings.isEn) "• Spine Universal Alpha Mode (PMA vs Straight Alpha)" else "• Mode Alpha Universel Spine (PMA vs Straight Alpha)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = if (strings.isEn) "  Arknights & Brown Dust 2 models rendering fixed with clean Straight Alpha, while preserving PMA transparency for Project QT." else "  Rendu impeccable pour Arknights et Brown Dust 2 en Straight Alpha, avec maintien du PMA net pour Project QT.",
                                    fontSize = 9.sp,
                                    color = nothingTextSecondary
                                )

                                Text(
                                    text = if (strings.isEn) "• Precise Touch Behavior" else "• Comportement Tactile Précis",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = if (strings.isEn) "  No random animations are triggered when touching a model if touch animation is set to None." else "  Aucune animation aléatoire ne se déclenche au toucher lorsqu'aucune animation n'est définie.",
                                    fontSize = 9.sp,
                                    color = nothingTextSecondary
                                )

                                Text(
                                    text = if (strings.isEn) "• Custom Idle Loop Animation" else "• Animation au Repos Personnalisée",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = nothingTextPrimary
                                )
                                Text(
                                    text = if (strings.isEn) "  Choose any model motion as the continuous idle loop." else "  Sélectionnez n'importe quelle animation pour tourner en boucle permanente au repos.",
                                    fontSize = 9.sp,
                                    color = nothingTextSecondary
                                )

                                Text(
                                    text = if (strings.isEn) "• Full English & French Localization" else "• Traduction Complète Français & Anglais",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = nothingTextPrimary
                                )

                                Text(
                                    text = if (strings.isEn) "• Redesigned Azur Settings & UI" else "• Refonte Visuelle Azur des Paramètres",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = nothingTextPrimary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 1.dp)

                // PIED DE PAGE : FERMETURE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = nothingBlack,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = strings.close,
                            fontSize = 11.sp,
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
