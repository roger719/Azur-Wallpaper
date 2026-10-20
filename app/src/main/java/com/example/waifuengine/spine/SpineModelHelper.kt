package com.example.waifuengine.spine

import org.json.JSONObject
import java.io.File

object SpineModelHelper {

    fun isSpineDirectory(dir: File): Boolean {
        if (!dir.isDirectory || dir.name.equals("CustomSkins", ignoreCase = true)) return false
        val files = dir.listFiles() ?: return false
        val hasDirectSkelOrJson = files.any {
            it.isFile && (
                it.name.endsWith(".skel", ignoreCase = true) ||
                it.name.endsWith(".skel.bytes", ignoreCase = true) ||
                (it.name.endsWith(".json", ignoreCase = true) &&
                 !it.name.endsWith(".model3.json", ignoreCase = true) &&
                 !it.name.endsWith(".motion3.json", ignoreCase = true) &&
                 !it.name.endsWith(".exp3.json", ignoreCase = true) &&
                 !it.name.endsWith(".pose3.json", ignoreCase = true) &&
                 !it.name.endsWith(".physics3.json", ignoreCase = true) &&
                 !it.name.endsWith(".cdi3.json", ignoreCase = true))
            )
        }
        val hasDirectAtlas = files.any {
            it.isFile && (it.name.endsWith(".atlas", ignoreCase = true) || it.name.endsWith(".atlas.txt", ignoreCase = true))
        }
        if (hasDirectSkelOrJson && hasDirectAtlas) return true

        val parent = dir.parentFile
        val isDirectGameFolder = parent != null && parent.name.equals("CustomSkins", ignoreCase = true)
        val hasDisplayName = File(dir, "display_name.txt").exists()

        if (isDirectGameFolder && !hasDisplayName) {
            return false
        }

        // If it has display_name.txt and exactly 1 subfolder that is a spine directory
        if (hasDisplayName) {
            val subDirs = files.filter { it.isDirectory && !it.name.startsWith(".") }
            if (subDirs.size == 1 && isSpineDirectory(subDirs.first())) {
                return true
            }
        }

        return false
    }

    fun isSpineFile(path: String): Boolean {
        if (path.isEmpty()) return false
        val lower = path.lowercase()
        if (lower.endsWith(".skel") || lower.endsWith(".skel.bytes")) return true
        val file = File(path)
        if (!file.exists()) return false
        val parent = file.parentFile ?: return false
        return findAtlasFile(parent) != null &&
            !lower.endsWith(".model3.json") &&
            !lower.endsWith(".model.json") &&
            !lower.endsWith(".moc3") &&
            !lower.endsWith(".moc")
    }

    fun shouldEnablePma(modelPath: String, atlasPath: String = ""): Boolean {
        if (modelPath.isEmpty()) return false
        val lower = modelPath.lowercase()
        if (lower.contains("project qt") || lower.contains("projectqt") ||
            lower.contains("bikini girls") || lower.contains("bikini%20girls") ||
            lower.contains("bikinigirls") || lower.contains("nutaku")) {
            return true
        }
        val atlasFile = if (atlasPath.isNotEmpty() && File(atlasPath).exists()) {
            File(atlasPath)
        } else {
            val parent = File(modelPath).parentFile
            if (parent != null) findAtlasFile(parent) else null
        }
        if (atlasFile != null && atlasFile.exists()) {
            try {
                atlasFile.useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim().lowercase()
                        if (trimmed == "pma: true" || trimmed == "pma:true") return true
                        if (trimmed == "pma: false" || trimmed == "pma:false") return false
                    }
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun findSkelOrJsonFile(dir: File): File? {
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        return files.firstOrNull { it.name.endsWith(".skel", ignoreCase = true) }
            ?: files.firstOrNull { it.name.endsWith(".skel.bytes", ignoreCase = true) }
            ?: files.firstOrNull {
                it.name.endsWith(".json", ignoreCase = true) &&
                !it.name.endsWith(".model3.json", ignoreCase = true) &&
                !it.name.endsWith(".motion3.json", ignoreCase = true) &&
                !it.name.endsWith(".exp3.json", ignoreCase = true) &&
                !it.name.endsWith(".pose3.json", ignoreCase = true) &&
                !it.name.endsWith(".physics3.json", ignoreCase = true) &&
                !it.name.endsWith(".cdi3.json", ignoreCase = true)
            }
    }

    fun findAtlasFile(dir: File): File? {
        return dir.walkTopDown().firstOrNull {
            it.isFile && (it.name.endsWith(".atlas", ignoreCase = true) || it.name.endsWith(".atlas.txt", ignoreCase = true))
        }
    }

    fun findTextureFiles(dir: File): List<String> {
        return dir.walkTopDown()
            .filter { it.isFile && (it.name.endsWith(".png", ignoreCase = true) || it.name.endsWith(".jpg", ignoreCase = true) || it.name.endsWith(".webp", ignoreCase = true)) }
            .map { it.absolutePath }
            .toList()
    }

    fun getAvailableAnimationsForSpine(characterFolderFile: File): List<String> {
        if (!characterFolderFile.exists()) return emptyList()
        val animSet = mutableSetOf<String>()

        val jsonFile = characterFolderFile.walkTopDown().firstOrNull {
            it.isFile && it.name.endsWith(".json", ignoreCase = true) &&
            !it.name.endsWith(".model3.json", ignoreCase = true) &&
            !it.name.endsWith(".motion3.json", ignoreCase = true) &&
            !it.name.endsWith(".exp3.json", ignoreCase = true)
        }

        if (jsonFile != null && jsonFile.exists()) {
            try {
                val root = JSONObject(jsonFile.readText())
                val animsObj = root.optJSONObject("animations")
                if (animsObj != null) {
                    val keys = animsObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k.isNotBlank()) animSet.add(k)
                    }
                }
            } catch (_: Exception) {}
        }

        val skelFile = characterFolderFile.walkTopDown().firstOrNull {
            it.isFile && (it.name.endsWith(".skel", ignoreCase = true) || it.name.endsWith(".skel.bytes", ignoreCase = true))
        }
        if (skelFile != null && skelFile.exists() && animSet.isEmpty()) {
            try {
                val bytes = skelFile.readBytes()
                val ascii = StringBuilder()
                val foundStrings = mutableSetOf<String>()
                for (b in bytes) {
                    val c = b.toInt().toChar()
                    if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '-') {
                        ascii.append(c)
                    } else {
                        if (ascii.length in 3..35) {
                            foundStrings.add(ascii.toString())
                        }
                        ascii.setLength(0)
                    }
                }
                val probableAnims = foundStrings.filter { s ->
                    val lower = s.lowercase()
                    lower.contains("idle") || lower.contains("walk") || lower.contains("run") ||
                    lower.contains("attack") || lower.contains("touch") || lower.contains("tap") ||
                    lower.contains("action") || lower.contains("special") || lower.contains("win") ||
                    lower.contains("stand") || lower.contains("dance") || lower.contains("normal")
                }
                animSet.addAll(probableAnims)
            } catch (_: Exception) {}
        }

        if (animSet.isEmpty()) {
            animSet.addAll(listOf("idle", "stand", "walk", "attack", "touch", "special"))
        }

        return animSet.toList().sorted()
    }
}
