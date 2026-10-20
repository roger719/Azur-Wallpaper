package com.example.waifuengine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.waifuengine.data.FileNode
import com.example.waifuengine.data.WaifuPreferences
import java.io.File

private val nothingBg = Color(0xFFDCE8F6)
private val nothingSurface = Color(0xFFEDF4FC)
private val nothingBorder = Color(0xFFB4CDEB)
private val nothingTextPrimary = Color(0xFF0D1B3E)
private val nothingTextSecondary = Color(0xFF2D4B75)
private val nothingBlack = Color(0xFF0B1B3D)

@Composable
fun FilesDialog(
    rootNode: FileNode?,
    allCharacters: List<CharacterItem>,
    selectedCharacterPath: String,
    appLanguage: String = WaifuPreferences.LANGUAGE_EN,
    onDismiss: () -> Unit,
    onSelectCharacter: (CharacterItem) -> Unit,
    onNewFolder: () -> Unit,
    onImportZip: () -> Unit,
    onNewSubFolder: (File) -> Unit,
    onImportZipToFolder: (File) -> Unit,
    onRename: (FileNode) -> Unit,
    onMove: (FileNode) -> Unit,
    onDelete: (FileNode) -> Unit,
) {
    val strings = AppStrings(appLanguage)
    Dialog(
        onDismissRequest = onDismiss,
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
                // EN-TÊTE IDENTIQUE AU WORKSHOP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = strings.files,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = nothingTextPrimary
                        )
                        Text(
                            text = strings.localTree,
                            fontSize = 10.sp,
                            color = nothingTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = nothingBg,
                            border = BorderStroke(1.dp, nothingBorder),
                            modifier = Modifier.clickable { onNewFolder() }
                        ) {
                            Text(
                                text = if (strings.isEn) "+ Folder" else "+ Dossier",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = nothingTextPrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = nothingBlack,
                            modifier = Modifier.clickable { onImportZip() }
                        ) {
                            Text(
                                text = strings.importModel,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = nothingBorder, thickness = 1.dp)

                // CORPS DE LA FENÊTRE
                if (allCharacters.isEmpty() && (rootNode == null || rootNode.children.isEmpty())) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = strings.noCharacterImported,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = nothingTextPrimary
                            )
                            Text(
                                text = strings.filesEmptyTip,
                                fontSize = 11.sp,
                                color = nothingTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        rootNode?.let { node ->
                            IdeTreeViewNode(
                                node = node,
                                level = 0,
                                selectedCharacterPath = selectedCharacterPath,
                                onSelectCharacter = onSelectCharacter,
                                onNewSubFolder = onNewSubFolder,
                                onImportZip = onImportZipToFolder,
                                onRename = onRename,
                                onMove = onMove,
                                onDelete = onDelete,
                                strings = strings
                            )
                        }
                    }
                }
            }
        }
    }
}
