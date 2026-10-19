package com.example.waifuengine.workshop

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.waifuengine.data.CharacterItem
import com.example.waifuengine.data.CharacterRepository
import com.example.waifuengine.data.EngineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WorkshopFile(
    val path: String,
    val size: Long
)

data class WorkshopModel(
    val id: String,
    val name: String,
    val game: String,
    val engine: EngineType,
    val totalSize: Long,
    val files: List<WorkshopFile>,
    val isZip: Boolean,
    val branch: String
)

object WorkshopManager {

    fun sanitizeToken(token: String): String {
        var t = token.trim()
        while ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            t = t.substring(1, t.length - 1).trim()
        }
        if (t.startsWith("Bearer ", ignoreCase = true)) {
            t = t.substring(7).trim()
        } else if (t.startsWith("token ", ignoreCase = true)) {
            t = t.substring(6).trim()
        }
        return t
    }

    private fun applyAuth(conn: HttpURLConnection, token: String) {
        val clean = sanitizeToken(token)
        if (clean.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $clean")
        }
    }

    private fun isCategoryFolder(folderName: String): Boolean {
        val lower = folderName.lowercase(Locale.ROOT)
        return lower.contains("live2d") ||
               lower.contains("spine") ||
               lower.contains("model") ||
               lower.contains("character") ||
               lower.contains("database") ||
               lower.contains("asset") ||
               lower.contains("skin") ||
               lower.contains("data")
    }

    fun extractBaseCharacterName(name: String): String {
        val clean = name.trim().replace(Regex("(?i)\\.zip$"), "").replace("'", "")

        // 1. Extraire la partie avant parenthèses ou crochets
        val parenIdx = clean.indexOfAny(charArrayOf('(', '[', '{'))
        val cand = if (parenIdx > 0) clean.substring(0, parenIdx).trim() else clean

        // 2. Extraire la partie avant underscore si suffixe de variante (ex: aijier_3, dafeng_2, tiancheng_cv_2)
        val uIdx = cand.indexOf('_')
        val cand2 = if (uIdx > 0) cand.substring(0, uIdx) else cand

        // 3. Si plusieurs mots séparés par des espaces (ex: "Ch'en the Holungday" -> "Chen", "Eyjafjalla the Hwyx" -> "Eyjafjalla")
        val words = cand2.split(" ").filter { it.isNotBlank() }
        val cand3 = if (words.size > 1 && words[0].length >= 3) words[0] else cand2

        return cand3.replaceFirstChar { it.uppercase(Locale.ROOT) }
    }

    fun parseGitHubRepo(url: String): Pair<String, String>? {
        val clean = url.trim()
            .split("?")[0]
            .split("#")[0]
            .removeSuffix(".git")
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .trim('/')
        val parts = clean.split("/").filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            return Pair(parts[0], parts[1])
        }
        return null
    }

    suspend fun fetchWorkshopModels(
        repoUrl: String,
        token: String,
        context: Context? = null
    ): Result<List<WorkshopModel>> = withContext(Dispatchers.IO) {
        val repoInfo = parseGitHubRepo(repoUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("Format d'URL GitHub invalide. Exemple : https://github.com/utilisateur/mon-depot"))

        val owner = repoInfo.first
        val repo = repoInfo.second
        val cleanToken = sanitizeToken(token)

        Log.d("WorkshopManager", "fetchWorkshopModels: owner=$owner, repo=$repo, hasToken=${cleanToken.isNotBlank()}")

        var defaultBranch: String? = null
        try {
            val metaUrl = "https://api.github.com/repos/$owner/$repo"
            val metaConn = URL(metaUrl).openConnection() as HttpURLConnection
            metaConn.connectTimeout = 10000
            metaConn.readTimeout = 15000
            metaConn.setRequestProperty("User-Agent", "AzurWallpaper-App")
            metaConn.setRequestProperty("Accept", "application/vnd.github+json")
            metaConn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            applyAuth(metaConn, cleanToken)

            val metaCode = metaConn.responseCode
            Log.d("WorkshopManager", "metaUrl responseCode: $metaCode")
            if (metaCode in 200..299) {
                val metaJson = metaConn.inputStream.bufferedReader().use { it.readText() }
                defaultBranch = JSONObject(metaJson).optString("default_branch", "main")
            }
            metaConn.disconnect()
        } catch (e: Exception) {
            Log.w("WorkshopManager", "Error fetching repo metadata: ${e.message}")
        }

        var branch = defaultBranch ?: "main"
        var jsonText: String? = null
        val branchesToTry = if (defaultBranch != null) listOf(defaultBranch, "main", "master").distinct() else listOf("main", "master")
        var lastException: Exception? = null

        for (b in branchesToTry) {
            try {
                val apiUrl = "https://api.github.com/repos/$owner/$repo/git/trees/$b?recursive=1"
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "AzurWallpaper-App")
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                applyAuth(conn, cleanToken)

                var responseCode = conn.responseCode
                Log.d("WorkshopManager", "Branch '$b' trees responseCode: $responseCode")

                // Si Bearer a renvoyé 401, retenter immédiatement avec le schéma 'token '
                if (responseCode == 401 && cleanToken.isNotBlank()) {
                    conn.disconnect()
                    val fallbackConn = URL(apiUrl).openConnection() as HttpURLConnection
                    fallbackConn.connectTimeout = 15000
                    fallbackConn.readTimeout = 30000
                    fallbackConn.setRequestProperty("User-Agent", "AzurWallpaper-App")
                    fallbackConn.setRequestProperty("Accept", "application/vnd.github+json")
                    fallbackConn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    fallbackConn.setRequestProperty("Authorization", "token $cleanToken")
                    responseCode = fallbackConn.responseCode
                    Log.d("WorkshopManager", "Branch '$b' retry with 'token': $responseCode")
                    if (responseCode in 200..299) {
                        branch = b
                        jsonText = fallbackConn.inputStream.bufferedReader().use { it.readText() }
                        fallbackConn.disconnect()
                        break
                    }
                    fallbackConn.disconnect()
                }

                if (responseCode in 200..299) {
                    branch = b
                    jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    break
                } else {
                    conn.disconnect()
                    if (responseCode == 401 || responseCode == 403) {
                        return@withContext Result.failure(Exception("Accès refusé ($responseCode). Votre clé d'accès (token) a été rejetée par GitHub. Vérifiez sa validité et ses permissions."))
                    }
                    if (responseCode == 404 && b == branchesToTry.last()) {
                        val msg = if (cleanToken.isBlank()) {
                            "Dépôt ou branche introuvable (404). S'il s'agit d'un dépôt privé, veuillez renseigner votre clé d'accès (PAT)."
                        } else {
                            "Dépôt privé introuvable (404). Vérifiez l'URL et assurez-vous que votre token possède bien l'accès au dépôt."
                        }
                        return@withContext Result.failure(Exception(msg))
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.e("WorkshopManager", "Exception during tree fetch for '$b'", e)
            }
        }

        if (jsonText == null) {
            return@withContext Result.failure(lastException ?: Exception("Impossible de contacter GitHub. Vérifiez votre connexion Internet."))
        }

        try {
            val root = JSONObject(jsonText)
            val treeArray = root.optJSONArray("tree") ?: return@withContext Result.success(emptyList())

            val rawFiles = mutableListOf<WorkshopFile>()
            for (i in 0 until treeArray.length()) {
                val item = treeArray.getJSONObject(i)
                val type = item.optString("type")
                if (type == "blob") {
                    val path = item.optString("path")
                    val size = item.optLong("size", 0L)
                    rawFiles.add(WorkshopFile(path, size))
                }
            }

            data class ModelGroupKey(val gameRaw: String, val charName: String, val categoryHint: String)
            val modelsMap = mutableMapOf<ModelGroupKey, MutableList<WorkshopFile>>()

            for (file in rawFiles) {
                val p = file.path
                val fileName = p.substringAfterLast('/')
                // Ignorer fichiers cachés ou métadonnées
                if (fileName.startsWith(".") || fileName.equals("README.md", ignoreCase = true) || fileName.equals(".gitattributes", ignoreCase = true)) {
                    continue
                }

                val parts = p.split("/").filter { it.isNotEmpty() }
                if (parts.size < 2) continue

                val isFirstCategory = isCategoryFolder(parts[0])
                val (categoryHint, gameRaw, rest) = if (isFirstCategory && parts.size >= 3) {
                    Triple(parts[0], parts[1], parts.drop(2))
                } else {
                    Triple("", parts[0], parts.drop(1))
                }

                if (rest.isEmpty()) continue

                val charName = if (rest.size == 1) {
                    rest[0].replace(Regex("(?i)\\.zip$"), "")
                } else {
                    rest[0]
                }

                val groupKey = ModelGroupKey(gameRaw, charName, categoryHint)
                modelsMap.getOrPut(groupKey) { mutableListOf() }.add(file)
            }

            val resultList = mutableListOf<WorkshopModel>()
            for ((groupKey, files) in modelsMap) {
                val gameRaw = groupKey.gameRaw
                val charName = groupKey.charName
                val categoryHint = groupKey.categoryHint

                val isZip = files.size == 1 && files[0].path.endsWith(".zip", ignoreCase = true)

                val hasLive2D = files.any {
                    val l = it.path.lowercase(Locale.ROOT)
                    l.endsWith(".model3.json") || l.endsWith(".model.json") || l.endsWith(".moc3") || l.endsWith(".moc")
                }
                val hasSpine = files.any {
                    val l = it.path.lowercase(Locale.ROOT)
                    l.endsWith(".skel") || l.endsWith(".skel.bytes") || l.endsWith(".atlas") || l.endsWith(".atlas.txt")
                }

                val engine = when {
                    hasLive2D -> EngineType.LIVE2D
                    hasSpine -> EngineType.SPINE
                    categoryHint.contains("live2d", ignoreCase = true) -> EngineType.LIVE2D
                    categoryHint.contains("spine", ignoreCase = true) -> EngineType.SPINE
                    files.any { it.path.contains("live2d", ignoreCase = true) } -> EngineType.LIVE2D
                    files.any { it.path.contains("spine", ignoreCase = true) } -> EngineType.SPINE
                    else -> EngineType.LIVE2D
                }

                val gameDisplay = when (gameRaw.lowercase(Locale.ROOT).replace(" ", "").replace("_", "")) {
                    "arknights", "arknight" -> "Arknights"
                    "nikke" -> "NIKKE"
                    "browndust2", "browndust" -> "Brown Dust 2"
                    "azurelane", "azurlane" -> "Azure Lane"
                    "bluearchive" -> "Blue Archive"
                    "genshin", "genshinimpact" -> "Genshin Impact"
                    "honkai", "honkaistarrail" -> "Honkai: Star Rail"
                    "zenless", "zenlesszonezero", "zzz" -> "Zenless Zone Zero"
                    else -> gameRaw.replace("_", " ").capitalizeWords()
                }

                val totalSize = files.sumOf { it.size }
                val modelId = "$gameRaw/$charName"

                resultList.add(
                    WorkshopModel(
                        id = modelId,
                        name = charName,
                        game = gameDisplay,
                        engine = engine,
                        totalSize = totalSize,
                        files = files,
                        isZip = isZip,
                        branch = branch
                    )
                )
            }

            resultList.sortWith(compareBy({ it.game }, { it.name.lowercase(Locale.ROOT) }))
            Log.d("WorkshopManager", "fetchWorkshopModels success: ${resultList.size} models loaded")
            if (context != null) {
                saveModelsToCache(context, repoUrl, resultList)
            }
            Result.success(resultList)
        } catch (e: Exception) {
            Log.e("WorkshopManager", "Error parsing workshop tree", e)
            Result.failure(e)
        }
    }

    fun saveModelsToCache(context: Context, repoUrl: String, models: List<WorkshopModel>) {
        try {
            val cacheFile = File(context.filesDir, "workshop_cache.json")
            val rootObj = JSONObject()
            rootObj.put("repoUrl", repoUrl.trim())
            rootObj.put("timestamp", System.currentTimeMillis())
            val array = org.json.JSONArray()
            models.forEach { m ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("game", m.game)
                    put("engine", m.engine.name)
                    put("totalSize", m.totalSize)
                    put("isZip", m.isZip)
                    put("branch", m.branch)
                    val filesArr = org.json.JSONArray()
                    m.files.forEach { f ->
                        filesArr.put(JSONObject().apply {
                            put("path", f.path)
                            put("size", f.size)
                        })
                    }
                    put("files", filesArr)
                }
                array.put(obj)
            }
            rootObj.put("models", array)
            cacheFile.writeText(rootObj.toString())
            Log.d("WorkshopManager", "Saved ${models.size} models to cache")
        } catch (e: Exception) {
            Log.w("WorkshopManager", "Error saving cache: ${e.message}")
        }
    }

    fun loadCachedModels(context: Context, repoUrl: String): List<WorkshopModel>? {
        return try {
            val cacheFile = File(context.filesDir, "workshop_cache.json")
            if (!cacheFile.exists()) return null
            val text = cacheFile.readText()
            if (text.isBlank()) return null
            val rootObj = JSONObject(text)
            val cachedUrl = rootObj.optString("repoUrl", "")
            if (!cachedUrl.equals(repoUrl.trim(), ignoreCase = true)) {
                return null
            }
            val array = rootObj.optJSONArray("models") ?: return null
            val list = mutableListOf<WorkshopModel>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val filesList = mutableListOf<WorkshopFile>()
                val filesArr = obj.optJSONArray("files")
                if (filesArr != null) {
                    for (j in 0 until filesArr.length()) {
                        val fObj = filesArr.getJSONObject(j)
                        filesList.add(WorkshopFile(
                            path = fObj.getString("path"),
                            size = fObj.optLong("size", 0L)
                        ))
                    }
                }
                val engineStr = obj.optString("engine", "LIVE2D")
                val engine = if (engineStr == "SPINE") EngineType.SPINE else EngineType.LIVE2D
                list.add(WorkshopModel(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    game = obj.getString("game"),
                    engine = engine,
                    totalSize = obj.optLong("totalSize", 0L),
                    files = filesList,
                    isZip = obj.optBoolean("isZip", false),
                    branch = obj.optString("branch", "main")
                ))
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            Log.w("WorkshopManager", "Error loading cache: ${e.message}")
            null
        }
    }

    fun clearCache(context: Context) {
        try {
            File(context.filesDir, "workshop_cache.json").delete()
        } catch (_: Exception) {}
    }

    fun getTempPreviewDir(context: Context): File {
        val dir = File(context.filesDir, "workshop_temp_preview")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun clearTempPreview(context: Context) {
        try {
            val dir = File(context.filesDir, "workshop_temp_preview")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            val oldDir = File(context.filesDir, ".workshop_temp_preview")
            if (oldDir.exists()) {
                oldDir.deleteRecursively()
            }
            Log.d("WorkshopManager", "Cleared temp preview folder")
        } catch (e: Exception) {
            Log.w("WorkshopManager", "Error clearing temp preview: ${e.message}")
        }
    }

    suspend fun downloadModelToDirectory(
        context: Context,
        model: WorkshopModel,
        repoUrl: String,
        token: String,
        targetBaseDir: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val repoInfo = parseGitHubRepo(repoUrl) ?: return@withContext false
        val owner = repoInfo.first
        val repo = repoInfo.second
        val cleanToken = sanitizeToken(token)

        targetBaseDir.mkdirs()

        try {
            if (model.isZip) {
                val file = model.files.first()
                val encodedPath = file.path.split("/").joinToString("/") { Uri.encode(it) }
                val downloadUrl = "https://raw.githubusercontent.com/$owner/$repo/${Uri.encode(model.branch)}/$encodedPath"
                val conn = URL(downloadUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                conn.setRequestProperty("User-Agent", "AzurWallpaper-App")
                applyAuth(conn, cleanToken)

                if (conn.responseCode !in 200..299) {
                    Log.e("WorkshopManager", "Download ZIP failed: HTTP ${conn.responseCode}")
                    conn.disconnect()
                    return@withContext false
                }

                val success = conn.inputStream.use { input ->
                    CharacterRepository.importZipToDirectory(
                        targetParentDir = targetBaseDir,
                        inputStream = input,
                        zipName = file.path.substringAfterLast('/'),
                        customDisplayName = model.name
                    )
                }
                conn.disconnect()
                success
            } else {
                val cleanFolderName = model.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
                val targetCharDir = File(targetBaseDir, cleanFolderName)
                targetCharDir.mkdirs()
                File(targetCharDir, "display_name.txt").writeText(model.name)

                var totalBytesRead = 0L
                val totalBytes = maxOf(1L, model.totalSize)

                for (file in model.files) {
                    val encodedPath = file.path.split("/").joinToString("/") { Uri.encode(it) }
                    val downloadUrl = "https://raw.githubusercontent.com/$owner/$repo/${Uri.encode(model.branch)}/$encodedPath"
                    val conn = URL(downloadUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 60000
                    conn.setRequestProperty("User-Agent", "AzurWallpaper-App")
                    applyAuth(conn, cleanToken)

                    if (conn.responseCode !in 200..299) {
                        Log.e("WorkshopManager", "Download file failed (${file.path}): HTTP ${conn.responseCode}")
                        conn.disconnect()
                        return@withContext false
                    }

                    val parts = file.path.split("/")
                    val charIdx = parts.indexOfFirst { it.equals(model.name, ignoreCase = true) }
                    val subPath = if (charIdx != -1 && charIdx < parts.size - 1) {
                        parts.drop(charIdx + 1).joinToString("/")
                    } else {
                        file.path.substringAfterLast('/')
                    }

                    val destFile = File(targetCharDir, subPath)
                    destFile.parentFile?.mkdirs()

                    conn.inputStream.use { input ->
                        destFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                totalBytesRead += read
                                onProgress((totalBytesRead.toFloat() / totalBytes).coerceIn(0f, 1f))
                            }
                        }
                    }
                    conn.disconnect()

                    if (model.files.size == 1 && destFile.length() > 4) {
                        try {
                            val magic = ByteArray(4)
                            destFile.inputStream().use { it.read(magic) }
                            if (magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() && magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()) {
                                destFile.inputStream().use { stream ->
                                    CharacterRepository.importZipToDirectory(
                                        targetParentDir = targetBaseDir,
                                        inputStream = stream,
                                        zipName = destFile.name,
                                        customDisplayName = model.name
                                    )
                                }
                                destFile.delete()
                            }
                        } catch (e: Exception) {
                            Log.w("WorkshopManager", "Auto-extract ZIP check exception: ${e.message}")
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            Log.e("WorkshopManager", "downloadModelToDirectory exception", e)
            false
        }
    }

    suspend fun downloadModel(
        context: Context,
        model: WorkshopModel,
        repoUrl: String,
        token: String,
        onProgress: (Float) -> Unit
    ): Boolean {
        val rootDir = CharacterRepository.getRootDirectory(context)
        val gameDir = File(rootDir, model.game)
        return downloadModelToDirectory(context, model, repoUrl, token, gameDir, onProgress)
    }

    suspend fun downloadModelForPreview(
        context: Context,
        model: WorkshopModel,
        repoUrl: String,
        token: String,
        onProgress: (Float) -> Unit
    ): CharacterItem? {
        val tempBase = getTempPreviewDir(context)
        val success = downloadModelToDirectory(context, model, repoUrl, token, tempBase, onProgress)
        if (!success) return null

        val cleanName = model.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
        val charDir = File(tempBase, cleanName)
        if (charDir.exists() && charDir.isDirectory) {
            val allFiles = charDir.walkTopDown().toList()
            val hasValidModel = allFiles.any { f ->
                val l = f.name.lowercase(Locale.ROOT)
                l.endsWith(".moc3") || l.endsWith(".model3.json") || l.endsWith(".model.json") || l.endsWith(".skel") || l.endsWith(".atlas")
            }
            if (!hasValidModel) {
                val hasUnityFS = allFiles.any { f ->
                    if (f.isFile && f.length() >= 7) {
                        try {
                            val magic = ByteArray(7)
                            f.inputStream().use { it.read(magic) }
                            String(magic) == "UnityFS"
                        } catch (_: Exception) { false }
                    } else false
                }
                if (hasUnityFS) {
                    Log.w("WorkshopManager", "Model ${model.name} contains raw UnityFS asset bundle, not supported for direct preview")
                    return null
                }
            }
        }

        val allChars = CharacterRepository.loadAllCustomCharacters(context, tempBase)
        return allChars.firstOrNull { it.name.equals(model.name, ignoreCase = true) }
            ?: allChars.firstOrNull { it.folderPath.contains(model.name, ignoreCase = true) }
            ?: allChars.firstOrNull { it.folderPath.contains(cleanName, ignoreCase = true) }
            ?: allChars.firstOrNull { File(it.folderPath).name.equals(model.name, ignoreCase = true) }
            ?: allChars.firstOrNull { File(it.folderPath).name.equals(cleanName, ignoreCase = true) }
            ?: allChars.firstOrNull()
    }

    fun installFromPreview(context: Context, model: WorkshopModel): Boolean {
        val tempBase = getTempPreviewDir(context)
        val cleanName = model.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
        val sourceDir = tempBase.listFiles()?.firstOrNull { dir ->
            val dispFile = File(dir, "display_name.txt")
            (dispFile.exists() && dispFile.readText().trim().equals(model.name, ignoreCase = true)) ||
            dir.name.equals(cleanName, ignoreCase = true) || dir.name.equals(model.name, ignoreCase = true)
        } ?: return false

        val gameDir = File(CharacterRepository.getRootDirectory(context), model.game)
        gameDir.mkdirs()
        val targetDir = File(gameDir, sourceDir.name)
        if (targetDir.exists()) targetDir.deleteRecursively()

        val moved = sourceDir.renameTo(targetDir)
        if (!moved) {
            sourceDir.copyRecursively(targetDir, overwrite = true)
            sourceDir.deleteRecursively()
        }
        return true
    }

    fun getInstalledModelIdentifiers(context: Context): Set<String> {
        val root = CharacterRepository.getRootDirectory(context)
        val set = mutableSetOf<String>()
        if (!root.exists()) return set
        try {
            root.walkTopDown().filter { it.isDirectory }.forEach { dir ->
                set.add(dir.name.lowercase(Locale.ROOT))
                val cleanDirName = dir.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_").lowercase(Locale.ROOT)
                set.add(cleanDirName)
                val dispFile = File(dir, "display_name.txt")
                if (dispFile.exists()) {
                    try {
                        val disp = dispFile.readText().trim().lowercase(Locale.ROOT)
                        if (disp.isNotEmpty()) {
                            set.add(disp)
                            set.add(disp.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_"))
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w("WorkshopManager", "getInstalledModelIdentifiers error: ${e.message}")
        }
        return set
    }

    fun isModelInstalledFast(model: WorkshopModel, installedSet: Set<String>): Boolean {
        val lower = model.name.lowercase(Locale.ROOT)
        val clean = model.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_").lowercase(Locale.ROOT)
        return installedSet.contains(lower) || installedSet.contains(clean)
    }

    fun isModelInstalled(context: Context, model: WorkshopModel): Boolean {
        val root = CharacterRepository.getRootDirectory(context)
        val cleanName = model.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
        return root.walkTopDown().filter { it.isDirectory }.any { dir ->
            val dispFile = File(dir, "display_name.txt")
            if (dispFile.exists() && dispFile.readText().trim().equals(model.name, ignoreCase = true)) {
                true
            } else {
                dir.name.equals(cleanName, ignoreCase = true) || dir.name.equals(model.name, ignoreCase = true)
            }
        }
    }

    fun deleteInstalledModel(context: Context, model: WorkshopModel): Boolean {
        val root = CharacterRepository.getRootDirectory(context)
        val cleanName = model.name.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "_")
        val found = root.walkTopDown().filter { it.isDirectory }.firstOrNull { dir ->
            val dispFile = File(dir, "display_name.txt")
            (dispFile.exists() && dispFile.readText().trim().equals(model.name, ignoreCase = true)) ||
            dir.name.equals(cleanName, ignoreCase = true) || dir.name.equals(model.name, ignoreCase = true)
        }
        return found?.deleteRecursively() ?: false
    }

    fun formatSize(bytes: Long): String {
        return if (bytes < 1024 * 1024) {
            String.format(Locale.ROOT, "%.1f Ko", bytes / 1024.0)
        } else if (bytes < 1024 * 1024 * 1024) {
            String.format(Locale.ROOT, "%.2f Mo", bytes / (1024.0 * 1024.0))
        } else {
            String.format(Locale.ROOT, "%.2f Go", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase(Locale.ROOT) } }
    }
}
