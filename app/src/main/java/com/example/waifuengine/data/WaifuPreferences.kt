package com.example.waifuengine.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class DaySchedule(
    val dayKey: String, // "1" = Dimanche, "2" = Lundi, ..., "7" = Samedi
    val dayLabel: String,
    var isEnabled: Boolean = true,
    var startHour: Int = 23,
    var startMinute: Int = 0,
    var endHour: Int = 7,
    var endMinute: Int = 0,
)

data class PresetItem(
    val id: String,
    val name: String,
    val skelPath: String,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val selectedCharacterName: String,
    val selectedVariantLabel: String,
    val wakeupAnimation: String,
    val clickAnimation: String,
    val bgType: String,
    val bgColor: Int,
    val bgImagePath: String,
    val bgVideoPath: String,
    val bgScale: Float,
    val bgOffsetX: Float,
    val bgOffsetY: Float,
    val musicPath: String,
    val isMusicEnabled: Boolean,
    val musicVolume: Float,
)

class WaifuPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("waifu_prefs", Context.MODE_PRIVATE)

    var skelPath: String
        get() = prefs.getString(KEY_SKEL, DEFAULT_SKEL) ?: DEFAULT_SKEL
        set(value) = prefs.edit { putString(KEY_SKEL, value) }

    var scale: Float
        get() = prefs.getFloat(KEY_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_SCALE, value) }

    var offsetX: Float
        get() = prefs.getFloat(KEY_OFFSET_X, 0f)
        set(value) = prefs.edit { putFloat(KEY_OFFSET_X, value) }

    var offsetY: Float
        get() = prefs.getFloat(KEY_OFFSET_Y, 0f)
        set(value) = prefs.edit { putFloat(KEY_OFFSET_Y, value) }

    var selectedCharacterName: String
        get() = prefs.getString(KEY_CHAR_NAME, "Compagnon") ?: "Compagnon"
        set(value) = prefs.edit { putString(KEY_CHAR_NAME, value) }

    var selectedVariantLabel: String
        get() = prefs.getString(KEY_VARIANT_LABEL, "Skin Principal") ?: "Skin Principal"
        set(value) = prefs.edit { putString(KEY_VARIANT_LABEL, value) }

    var wakeupAnimation: String
        get() = prefs.getString(KEY_WAKEUP_ANIM, "") ?: ""
        set(value) = prefs.edit { putString(KEY_WAKEUP_ANIM, value) }

    var clickAnimation: String
        get() = prefs.getString(KEY_CLICK_ANIM, "") ?: ""
        set(value) = prefs.edit { putString(KEY_CLICK_ANIM, value) }

    var bgType: String
        get() = prefs.getString(KEY_BG_TYPE, TYPE_NONE) ?: TYPE_NONE
        set(value) = prefs.edit { putString(KEY_BG_TYPE, value) }

    var bgColor: Int
        get() = prefs.getInt(KEY_BG_COLOR, 0xFF141418.toInt())
        set(value) = prefs.edit { putInt(KEY_BG_COLOR, value) }

    var bgImagePath: String
        get() = prefs.getString(KEY_BG_IMAGE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BG_IMAGE, value) }

    var bgVideoPath: String
        get() = prefs.getString(KEY_BG_VIDEO, "") ?: ""
        set(value) = prefs.edit { putString(KEY_BG_VIDEO, value) }

    var bgScale: Float
        get() = prefs.getFloat(KEY_BG_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_BG_SCALE, value) }

    var bgOffsetX: Float
        get() = prefs.getFloat(KEY_BG_OFFSET_X, 0f)
        set(value) = prefs.edit { putFloat(KEY_BG_OFFSET_X, value) }

    var bgOffsetY: Float
        get() = prefs.getFloat(KEY_BG_OFFSET_Y, 0f)
        set(value) = prefs.edit { putFloat(KEY_BG_OFFSET_Y, value) }

    var activeTab: Int
        get() = prefs.getInt(KEY_ACTIVE_TAB, 0)
        set(value) = prefs.edit { putInt(KEY_ACTIVE_TAB, value) }

    var isVideoMuted: Boolean
        get() = prefs.getBoolean(KEY_VIDEO_MUTED, true)
        set(value) = prefs.edit { putBoolean(KEY_VIDEO_MUTED, value) }

    var musicPath: String
        get() = prefs.getString(KEY_MUSIC_PATH, "") ?: ""
        set(value) = prefs.edit { putString(KEY_MUSIC_PATH, value) }

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_MUSIC_ENABLED, value) }

    var musicVolume: Float
        get() = prefs.getFloat(KEY_MUSIC_VOLUME, 0.8f)
        set(value) = prefs.edit { putFloat(KEY_MUSIC_VOLUME, value) }

    var isScheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_SCHEDULE_ENABLED, value) }

    var scheduleFallbackType: String
        get() = prefs.getString(KEY_SCHEDULE_FALLBACK_TYPE, TYPE_COLOR) ?: TYPE_COLOR
        set(value) = prefs.edit { putString(KEY_SCHEDULE_FALLBACK_TYPE, value) }

    var scheduleFallbackImagePath: String
        get() = prefs.getString(KEY_SCHEDULE_FALLBACK_IMAGE, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SCHEDULE_FALLBACK_IMAGE, value) }

    // Mode Ragebait
    var isRagebaitEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAGEBAIT_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_RAGEBAIT_ENABLED, value) }

    var ragebaitPin: String
        get() = prefs.getString(KEY_RAGEBAIT_PIN, "") ?: ""
        set(value) = prefs.edit { putString(KEY_RAGEBAIT_PIN, value) }

    var ragebaitLockoutTime: Long
        get() = prefs.getLong(KEY_RAGEBAIT_LOCKOUT_TIME, 0L)
        set(value) = prefs.edit { putLong(KEY_RAGEBAIT_LOCKOUT_TIME, value) }

    fun getPresets(): List<PresetItem> {
        val raw = prefs.getString("user_presets_json", "[]") ?: "[]"
        val list = mutableListOf<PresetItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PresetItem(
                        id = obj.optString("id", "${System.currentTimeMillis()}"),
                        name = obj.optString("name", "Préset"),
                        skelPath = obj.optString("skelPath", ""),
                        scale = obj.optDouble("scale", 1.0).toFloat(),
                        offsetX = obj.optDouble("offsetX", 0.0).toFloat(),
                        offsetY = obj.optDouble("offsetY", 0.0).toFloat(),
                        selectedCharacterName = obj.optString("selectedCharacterName", ""),
                        selectedVariantLabel = obj.optString("selectedVariantLabel", ""),
                        wakeupAnimation = obj.optString("wakeupAnimation", ""),
                        clickAnimation = obj.optString("clickAnimation", ""),
                        bgType = obj.optString("bgType", TYPE_NONE),
                        bgColor = obj.optInt("bgColor", 0xFF141418.toInt()),
                        bgImagePath = obj.optString("bgImagePath", ""),
                        bgVideoPath = obj.optString("bgVideoPath", ""),
                        bgScale = obj.optDouble("bgScale", 1.0).toFloat(),
                        bgOffsetX = obj.optDouble("bgOffsetX", 0.0).toFloat(),
                        bgOffsetY = obj.optDouble("bgOffsetY", 0.0).toFloat(),
                        musicPath = obj.optString("musicPath", ""),
                        isMusicEnabled = obj.optBoolean("isMusicEnabled", false),
                        musicVolume = obj.optDouble("musicVolume", 0.8).toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun savePreset(preset: PresetItem) {
        val currentList = getPresets().toMutableList()
        currentList.removeAll { it.id == preset.id || it.name.equals(preset.name, ignoreCase = true) }
        currentList.add(0, preset)

        val array = JSONArray()
        for (item in currentList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("skelPath", item.skelPath)
                put("scale", item.scale)
                put("offsetX", item.offsetX)
                put("offsetY", item.offsetY)
                put("selectedCharacterName", item.selectedCharacterName)
                put("selectedVariantLabel", item.selectedVariantLabel)
                put("wakeupAnimation", item.wakeupAnimation)
                put("clickAnimation", item.clickAnimation)
                put("bgType", item.bgType)
                put("bgColor", item.bgColor)
                put("bgImagePath", item.bgImagePath)
                put("bgVideoPath", item.bgVideoPath)
                put("bgScale", item.bgScale)
                put("bgOffsetX", item.bgOffsetX)
                put("bgOffsetY", item.bgOffsetY)
                put("musicPath", item.musicPath)
                put("isMusicEnabled", item.isMusicEnabled)
                put("musicVolume", item.musicVolume)
            }
            array.put(obj)
        }
        prefs.edit { putString("user_presets_json", array.toString()) }
    }

    fun deletePreset(id: String) {
        val currentList = getPresets().toMutableList()
        currentList.removeAll { it.id == id }
        val array = JSONArray()
        for (item in currentList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("skelPath", item.skelPath)
                put("scale", item.scale)
                put("offsetX", item.offsetX)
                put("offsetY", item.offsetY)
                put("selectedCharacterName", item.selectedCharacterName)
                put("selectedVariantLabel", item.selectedVariantLabel)
                put("wakeupAnimation", item.wakeupAnimation)
                put("clickAnimation", item.clickAnimation)
                put("bgType", item.bgType)
                put("bgColor", item.bgColor)
                put("bgImagePath", item.bgImagePath)
                put("bgVideoPath", item.bgVideoPath)
                put("bgScale", item.bgScale)
                put("bgOffsetX", item.bgOffsetX)
                put("bgOffsetY", item.bgOffsetY)
                put("musicPath", item.musicPath)
                put("isMusicEnabled", item.isMusicEnabled)
                put("musicVolume", item.musicVolume)
            }
            array.put(obj)
        }
        prefs.edit { putString("user_presets_json", array.toString()) }
    }

    fun getDaySchedule(dayKey: String, defaultLabel: String): DaySchedule {
        val raw = prefs.getString("day_sched_$dayKey", "true,23,0,7,0") ?: "true,23,0,7,0"
        val parts = raw.split(",")
        return try {
            DaySchedule(
                dayKey = dayKey,
                dayLabel = defaultLabel,
                isEnabled = parts[0].toBoolean(),
                startHour = parts[1].toInt(),
                startMinute = parts[2].toInt(),
                endHour = parts[3].toInt(),
                endMinute = parts[4].toInt(),
            )
        } catch (_: Exception) {
            DaySchedule(dayKey = dayKey, dayLabel = defaultLabel)
        }
    }

    fun setDaySchedule(daySchedule: DaySchedule) {
        val str = "${daySchedule.isEnabled},${daySchedule.startHour},${daySchedule.startMinute},${daySchedule.endHour},${daySchedule.endMinute}"
        prefs.edit { putString("day_sched_${daySchedule.dayKey}", str) }
    }

    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    fun registerOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        synchronized(listeners) {
            listeners.add(listener)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val KEY_SKEL = "key_skel"
        const val KEY_SCALE = "key_scale"
        const val KEY_OFFSET_X = "key_offset_x"
        const val KEY_OFFSET_Y = "key_offset_y"
        const val KEY_CHAR_NAME = "key_char_name"
        const val KEY_VARIANT_LABEL = "key_variant_label"
        const val KEY_WAKEUP_ANIM = "key_wakeup_anim"
        const val KEY_CLICK_ANIM = "key_click_anim"
        
        const val KEY_BG_TYPE = "key_bg_type"
        const val KEY_BG_COLOR = "key_bg_color"
        const val KEY_BG_IMAGE = "key_bg_image"
        const val KEY_BG_VIDEO = "key_bg_video"
        const val KEY_BG_SCALE = "key_bg_scale"
        const val KEY_BG_OFFSET_X = "key_bg_offset_x"
        const val KEY_BG_OFFSET_Y = "key_bg_offset_y"
        const val KEY_ACTIVE_TAB = "key_active_tab"

        const val KEY_VIDEO_MUTED = "key_video_muted"
        const val KEY_MUSIC_PATH = "key_music_path"
        const val KEY_MUSIC_ENABLED = "key_music_enabled"
        const val KEY_MUSIC_VOLUME = "key_music_volume"

        const val KEY_SCHEDULE_ENABLED = "key_schedule_enabled"
        const val KEY_SCHEDULE_FALLBACK_TYPE = "key_schedule_fallback_type"
        const val KEY_SCHEDULE_FALLBACK_IMAGE = "key_schedule_fallback_image"

        const val KEY_RAGEBAIT_ENABLED = "key_ragebait_enabled"
        const val KEY_RAGEBAIT_PIN = "key_ragebait_pin"
        const val KEY_RAGEBAIT_LOCKOUT_TIME = "key_ragebait_lockout_time"

        const val TYPE_NONE = "NONE"
        const val TYPE_COLOR = "COLOR"
        const val TYPE_IMAGE = "IMAGE"
        const val TYPE_VIDEO = "VIDEO"

        const val DEFAULT_SKEL = ""
    }
}
