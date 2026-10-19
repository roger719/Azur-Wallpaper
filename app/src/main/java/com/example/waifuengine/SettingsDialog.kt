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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val nothingSurface = Color(0xFFF9F9FA)
    val nothingBorder = Color(0xFFE2E2E6)
    val nothingTextPrimary = Color(0xFF141418)
    val nothingTextSecondary = Color(0xFF757580)
    val nothingBlack = Color(0xFF141418)
    val accentGreen = Color(0xFF2E7D32)

    val strings = AppStrings(currentLanguage)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = nothingSurface,
            border = BorderStroke(1.dp, nothingBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // EN-TÊTE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.settings,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = nothingTextPrimary
                        )
                        Text(
                            text = strings.settingsSubtitle,
                            fontSize = 11.sp,
                            color = nothingTextSecondary
                        )
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, nothingBorder),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✕",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = nothingTextSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 0.8.dp)

                // SECTION LANGUE
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.languageSection,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = nothingTextPrimary
                    )
                    Text(
                        text = strings.languageDescription,
                        fontSize = 10.sp,
                        color = nothingTextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option Français
                    val isFr = currentLanguage == WaifuPreferences.LANGUAGE_FR
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFr) nothingBlack else Color.White,
                        border = BorderStroke(1.dp, if (isFr) nothingBlack else nothingBorder),
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
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🇫🇷",
                                    fontSize = 18.sp
                                )
                                Column {
                                    Text(
                                        text = strings.french,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFr) Color.White else nothingTextPrimary
                                    )
                                    Text(
                                        text = "Français (Par défaut)",
                                        fontSize = 9.sp,
                                        color = if (isFr) Color(0xFFD0D0D5) else nothingTextSecondary
                                    )
                                }
                            }

                            if (isFr) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF2A2A30)
                                ) {
                                    Text(
                                        text = "ACTIF",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Option English
                    val isEn = currentLanguage == WaifuPreferences.LANGUAGE_EN
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEn) nothingBlack else Color.White,
                        border = BorderStroke(1.dp, if (isEn) nothingBlack else nothingBorder),
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
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🇬🇧",
                                    fontSize = 18.sp
                                )
                                Column {
                                    Text(
                                        text = strings.english,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEn) Color.White else nothingTextPrimary
                                    )
                                    Text(
                                        text = "English (International)",
                                        fontSize = 9.sp,
                                        color = if (isEn) Color(0xFFD0D0D5) else nothingTextSecondary
                                    )
                                }
                            }

                            if (isEn) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF2A2A30)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 0.8.dp)

                // VERSION & FERMETURE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.appVersion,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = nothingTextSecondary
                    )

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = nothingBlack)
                    ) {
                        Text(
                            text = strings.close,
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
