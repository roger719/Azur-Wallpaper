package com.example.waifuengine.data

import android.content.Context
import com.example.waifuengine.spine.SpineModelHelper
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

enum class EngineType {
    LIVE2D,
    SPINE
}

data class VariantInfo(
    val label: String,
    val modelJsonPath: String = "",
    val mocPath: String = "",
    val texturePaths: List<String> = emptyList(),
    val spineAtlasPath: String = "",
    val engineType: EngineType = EngineType.LIVE2D,
)

data class CharacterItem(
    val id: String,
    val name: String,
    val folderPath: String = "",
    val isCustom: Boolean = true,
    val engineType: EngineType = EngineType.LIVE2D,
    val variants: List<VariantInfo>,
)

data class FileNode(
    val name: String,
    val absolutePath: String,
    val isFolder: Boolean,
    val characterItem: CharacterItem? = null,
    val children: List<FileNode> = emptyList(),
)

object CharacterRepository {

    fun getRootDirectory(context: Context): File {
        val root = File(context.filesDir, "CustomSkins")
        if (!root.exists()) {
            root.mkdirs()
        }
        return root
    }

    fun isLive2DDirectory(dir: File): Boolean {
        if (!dir.isDirectory || dir.name.equals("CustomSkins", ignoreCase = true)) return false
        val files = dir.listFiles() ?: return false
        val hasDirectLive2D = files.any {
            it.isFile && (
                it.name.endsWith(".moc3", ignoreCase = true) ||
                it.name.endsWith(".model3.json", ignoreCase = true) ||
                it.name.endsWith(".model.json", ignoreCase = true) ||
                it.name.endsWith(".moc", ignoreCase = true)
            )
        }
        if (hasDirectLive2D) return true

        // Cas d'un dossier avec display_name.txt contenant exactement un sous-dossier de modèle
        if (File(dir, "display_name.txt").exists()) {
            val subDirs = files.filter { it.isDirectory && !it.name.startsWith(".") }
            if (subDirs.size == 1 && isLive2DDirectory(subDirs.first())) {
                return true
            }
        }

        return false
    }

    fun isSpineDirectory(dir: File): Boolean {
        return SpineModelHelper.isSpineDirectory(dir)
    }

    fun isSkinDirectory(dir: File): Boolean {
        if (!dir.isDirectory || dir.name.equals("CustomSkins", ignoreCase = true)) return false
        return isLive2DDirectory(dir) || isSpineDirectory(dir)
    }

    fun getAvailableMotionsForCharacter(characterFolderFile: File, engineType: EngineType = EngineType.LIVE2D): List<String> {
        if (!characterFolderFile.exists()) return emptyList()

        if (engineType == EngineType.SPINE) {
            return SpineModelHelper.getAvailableAnimationsForSpine(characterFolderFile)
        }

        val motionSet = mutableSetOf<String>()

        // 1. Scanner tous les fichiers *.motion3.json ET *.motion3
        characterFolderFile.walkTopDown().forEach { file ->
            if (file.isFile) {
                val nameLower = file.name.lowercase()
                if (nameLower.endsWith(".motion3.json") || nameLower.endsWith(".motion3")) {
                    val animName = file.name
                        .replace(Regex("(?i)(\\.anim)?\\.motion3(\\.json)?$"), "")
                    if (animName.isNotBlank()) {
                        motionSet.add(animName)
                    }
                }
            }
        }

        // 2. Parser le fichier .model3.json si présent pour extraire les groupes et fichiers de motions
        val modelJsonFile = characterFolderFile.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".model3.json", ignoreCase = true) }
        if (modelJsonFile != null && modelJsonFile.exists()) {
            try {
                val text = modelJsonFile.readText()
                val root = JSONObject(text)
                val fileRefs = root.optJSONObject("FileReferences")
                val motionsObj = fileRefs?.optJSONObject("Motions")
                if (motionsObj != null) {
                    val keys = motionsObj.keys()
                    while (keys.hasNext()) {
                        val groupName = keys.next()
                        if (groupName.isNotBlank()) {
                            motionSet.add(groupName)
                        }
                        val arr = motionsObj.optJSONArray(groupName)
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i)
                                val f = item?.optString("File") ?: item?.optString("file") ?: ""
                                val filename = f.substringAfterLast('/')
                                    .replace(Regex("(?i)(\\.anim)?\\.motion3(\\.json)?$"), "")
                                if (filename.isNotBlank()) {
                                    motionSet.add(filename)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return motionSet.toList().sorted()
    }

    fun getAvailableExpressionsForCharacter(characterFolderFile: File): List<String> {
        val expressionSet = mutableSetOf<String>()
        if (!characterFolderFile.exists()) return emptyList()

        val modelJsonFile = characterFolderFile.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".model3.json", ignoreCase = true) }
        if (modelJsonFile != null && modelJsonFile.exists()) {
            try {
                val text = modelJsonFile.readText()
                val root = JSONObject(text)
                val fileRefs = root.optJSONObject("FileReferences")
                val expressionsArray = fileRefs?.optJSONArray("Expressions")
                if (expressionsArray != null) {
                    for (i in 0 until expressionsArray.length()) {
                        val obj = expressionsArray.optJSONObject(i)
                        val name = obj?.optString("Name") ?: ""
                        val file = obj?.optString("File") ?: ""
                        val cleanName = name.ifEmpty { file.substringAfterLast('/').substringBefore(".exp") }
                        if (cleanName.isNotEmpty()) {
                            expressionSet.add(cleanName)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        characterFolderFile.walkTopDown().filter {
            it.isFile && (it.name.endsWith(".exp3.json", ignoreCase = true) || it.name.endsWith(".exp.json", ignoreCase = true))
        }.forEach { expFile ->
            val name = expFile.name.substringBefore(".exp").substringBefore(".json")
            if (name.isNotEmpty()) {
                expressionSet.add(name)
            }
        }

        if (expressionSet.isEmpty()) {
            expressionSet.addAll(listOf("normal", "smile", "blush", "angry", "sad", "surprised"))
        }

        return expressionSet.toList().sorted()
    }

    private fun parseLive2DDirectory(skinFolder: File): CharacterItem? {
        val mocFile = skinFolder.walkTopDown().firstOrNull { it.isFile && (it.name.endsWith(".moc3", ignoreCase = true) || it.name.endsWith(".moc", ignoreCase = true)) }
        val modelJsonFile = skinFolder.walkTopDown().firstOrNull {
            it.isFile && (
                it.name.endsWith(".model3.json", ignoreCase = true) ||
                it.name.endsWith(".model.json", ignoreCase = true) ||
                (it.name.endsWith(".json", ignoreCase = true) &&
                 !it.name.endsWith(".motion3.json", ignoreCase = true) &&
                 !it.name.endsWith(".exp3.json", ignoreCase = true) &&
                 !it.name.endsWith(".pose3.json", ignoreCase = true) &&
                 !it.name.endsWith(".physics3.json", ignoreCase = true) &&
                 !it.name.endsWith(".cdi3.json", ignoreCase = true) &&
                 !it.name.endsWith(".atlas", ignoreCase = true))
            )
        }
        val textureFiles = skinFolder.walkTopDown().filter { it.isFile && (it.name.endsWith(".png", ignoreCase = true) || it.name.endsWith(".jpg", ignoreCase = true)) }.map { it.absolutePath }.toList()

        var finalModelJsonFile = modelJsonFile
        if (finalModelJsonFile == null && mocFile != null && textureFiles.isNotEmpty()) {
            try {
                val synthJson = File(mocFile.parentFile, "${mocFile.nameWithoutExtension}.model3.json")
                val relMoc = mocFile.name
                val relTextures = textureFiles.map { File(it).name }
                val jsonObj = JSONObject().apply {
                    put("Version", 3)
                    put("FileReferences", JSONObject().apply {
                        put("Moc", relMoc)
                        put("Textures", org.json.JSONArray(relTextures))
                    })
                }
                synthJson.writeText(jsonObj.toString(2))
                finalModelJsonFile = synthJson
            } catch (_: Exception) {}
        }

        val displayNameFile = File(skinFolder, "display_name.txt")
        val parentDisplayNameFile = if (skinFolder.parentFile != null) File(skinFolder.parentFile, "display_name.txt") else null
        val displayName = when {
            displayNameFile.exists() -> displayNameFile.readText().trim().ifEmpty { skinFolder.name }
            parentDisplayNameFile != null && parentDisplayNameFile.exists() -> parentDisplayNameFile.readText().trim().ifEmpty { skinFolder.name }
            else -> skinFolder.name
        }

        val variant = VariantInfo(
            label = "Live2D Model",
            mocPath = mocFile?.absolutePath ?: "",
            modelJsonPath = finalModelJsonFile?.absolutePath ?: (mocFile?.absolutePath ?: ""),
            texturePaths = textureFiles,
            engineType = EngineType.LIVE2D,
        )

        return CharacterItem(
            id = "live2d_${skinFolder.hashCode()}",
            name = displayName,
            folderPath = skinFolder.absolutePath,
            isCustom = true,
            engineType = EngineType.LIVE2D,
            variants = listOf(variant),
        )
    }

    private fun parseSpineDirectory(skinFolder: File): CharacterItem? {
        val skelOrJson = SpineModelHelper.findSkelOrJsonFile(skinFolder) ?: return null
        val atlasFile = SpineModelHelper.findAtlasFile(skinFolder)
        val textureFiles = SpineModelHelper.findTextureFiles(skinFolder)

        val displayNameFile = File(skinFolder, "display_name.txt")
        val parentDisplayNameFile = if (skinFolder.parentFile != null) File(skinFolder.parentFile, "display_name.txt") else null
        val displayName = when {
            displayNameFile.exists() -> displayNameFile.readText().trim().ifEmpty { skinFolder.name }
            parentDisplayNameFile != null && parentDisplayNameFile.exists() -> parentDisplayNameFile.readText().trim().ifEmpty { skinFolder.name }
            else -> skinFolder.name
        }

        val variant = VariantInfo(
            label = "Spine Model",
            modelJsonPath = skelOrJson.absolutePath,
            spineAtlasPath = atlasFile?.absolutePath ?: "",
            texturePaths = textureFiles,
            engineType = EngineType.SPINE,
        )

        return CharacterItem(
            id = "spine_${skinFolder.hashCode()}",
            name = displayName,
            folderPath = skinFolder.absolutePath,
            isCustom = true,
            engineType = EngineType.SPINE,
            variants = listOf(variant),
        )
    }

    fun parseSkinDirectory(skinFolder: File): CharacterItem? {
        return when {
            isLive2DDirectory(skinFolder) -> parseLive2DDirectory(skinFolder)
            isSpineDirectory(skinFolder) -> parseSpineDirectory(skinFolder)
            else -> null
        }
    }

    fun scanDirectoryNode(dir: File): FileNode {
        if (isSkinDirectory(dir)) {
            val charItem = parseSkinDirectory(dir)
            return FileNode(
                name = charItem?.name ?: dir.name,
                absolutePath = dir.absolutePath,
                isFolder = false,
                characterItem = charItem,
            )
        }

        val childrenList = mutableListOf<FileNode>()
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory && !file.name.startsWith(".")) {
                childrenList.add(scanDirectoryNode(file))
            }
        }

        childrenList.sortWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))

        return FileNode(
            name = dir.name,
            absolutePath = dir.absolutePath,
            isFolder = true,
            children = childrenList,
        )
    }

    fun getAllFolders(context: Context, dir: File = getRootDirectory(context)): List<File> {
        val result = mutableListOf<File>()
        if (dir.isDirectory && !isSkinDirectory(dir)) {
            result.add(dir)
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory && !isSkinDirectory(child) && !child.name.startsWith(".")) {
                    result.addAll(getAllFolders(context, child))
                }
            }
        }
        return result
    }

    fun loadAllCustomCharacters(context: Context, dir: File = getRootDirectory(context)): List<CharacterItem> {
        val result = mutableListOf<CharacterItem>()
        if (isSkinDirectory(dir)) {
            parseSkinDirectory(dir)?.let { result.add(it) }
        } else {
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory && !child.name.startsWith(".")) {
                    result.addAll(loadAllCustomCharacters(context, child))
                }
            }
        }
        return result
    }

    fun createFolder(parentDir: File, folderName: String): Boolean {
        val cleanName = folderName.trim().replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
        if (cleanName.isEmpty()) return false
        val newDir = File(parentDir, cleanName)
        return newDir.mkdirs()
    }

    fun importZipToDirectory(targetParentDir: File, inputStream: InputStream, zipName: String, customDisplayName: String? = null): Boolean {
        return try {
            val nameToUse = customDisplayName?.trim()?.ifEmpty { null }
                ?: zipName.removeSuffix(".zip").removeSuffix(".ZIP")

            val cleanFolderName = nameToUse.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
            val targetDir = File(targetParentDir, cleanFolderName)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            File(targetDir, "display_name.txt").writeText(nameToUse)

            val zipInput = ZipInputStream(inputStream)
            var entry = zipInput.nextEntry
            val buffer = ByteArray(4096)

            while (entry != null) {
                val entryPath = entry.name
                if (!entry.isDirectory && !entryPath.contains("__MACOSX") && !entryPath.startsWith(".")) {
                    val cleanPath = entryPath.replace("\\", "/")
                    val parts = cleanPath.split("/").filter { it.isNotEmpty() }

                    val relativePath = if (parts.size > 1) {
                        parts.drop(if (parts.first() == cleanFolderName || parts.first().contains(cleanFolderName)) 1 else 0).joinToString("/")
                    } else {
                        parts.last()
                    }

                    val destFile = File(targetDir, relativePath)
                    
                    // SÉCURITÉ ZIP SLIP : Vérification stricte du chemin canonique
                    if (!destFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        throw SecurityException("Entrée ZIP invalide hors du répertoire cible : $entryPath")
                    }

                    destFile.parentFile?.mkdirs()

                    destFile.outputStream().use { output ->
                        var len: Int
                        while (zipInput.read(buffer).also { len = it } > 0) {
                            output.write(buffer, 0, len)
                        }
                    }
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
            zipInput.close()

            isSkinDirectory(targetDir) || targetDir.walkTopDown().any { isSkinDirectory(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun renameNode(nodeFile: File, newName: String): Boolean {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return false

        if (isSkinDirectory(nodeFile)) {
            File(nodeFile, "display_name.txt").writeText(cleanName)
            return true
        }

        val destination = File(nodeFile.parentFile, cleanName)
        return nodeFile.renameTo(destination)
    }

    fun moveNode(sourceFile: File, targetDirectory: File): Boolean {
        if (!sourceFile.exists() || !targetDirectory.exists() || !targetDirectory.isDirectory) return false
        if (sourceFile.absolutePath == targetDirectory.absolutePath) return false
        val destination = File(targetDirectory, sourceFile.name)
        return sourceFile.renameTo(destination)
    }

    fun deleteNode(file: File): Boolean {
        return if (file.exists()) {
            file.deleteRecursively()
        } else false
    }
}
