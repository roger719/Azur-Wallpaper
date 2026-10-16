package com.example.waifuengine.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
    val isPointerTrackingEnabled: Boolean = true,
    val isTouchZoneCustom: Boolean = false,
    val touchZoneLeft: Float = 0.1f,
    val touchZoneTop: Float = 0.2f,
    val touchZoneWidth: Float = 0.8f,
    val touchZoneHeight: Float = 0.6f,
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
    val isClockEnabled: Boolean = true,
    val clockFont: String = "digital",
    val clockColor: Int = 0xFFFFFFFF.toInt(),
    val clockSize: Float = 48f,
    val clockFormat24h: Boolean = true,
    val clockShowSeconds: Boolean = false,
    val isDateEnabled: Boolean = true,
    val dateFormat: String = "full",
    val clockPosition: String = "front",
    val clockOffsetX: Float = 0f,
    val clockOffsetY: Float = -350f,
    val engineType: String = WaifuPreferences.ENGINE_LIVE2D,
)

class WaifuPreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("waifu_prefs", Context.MODE_PRIVATE)

    var skelPath: String
        get() = prefs.getString(KEY_SKEL, DEFAULT_SKEL) ?: DEFAULT_SKEL
        set(value) = prefs.edit { putString(KEY_SKEL, value) }

    var engineType: String
        get() = prefs.getString(KEY_ENGINE_TYPE, ENGINE_LIVE2D) ?: ENGINE_LIVE2D
        set(value) = prefs.edit { putString(KEY_ENGINE_TYPE, value) }

    var spineAtlasPath: String
        get() = prefs.getString(KEY_SPINE_ATLAS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SPINE_ATLAS, value) }

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

    var isPointerTrackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_POINTER_TRACKING_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_POINTER_TRACKING_ENABLED, value) }

    var isTouchZoneCustom: Boolean
        get() = prefs.getBoolean(KEY_TOUCH_ZONE_CUSTOM, false)
        set(value) = prefs.edit { putBoolean(KEY_TOUCH_ZONE_CUSTOM, value) }

    var touchZoneLeft: Float
        get() = prefs.getFloat(KEY_TOUCH_ZONE_LEFT, 0.1f)
        set(value) = prefs.edit { putFloat(KEY_TOUCH_ZONE_LEFT, value) }

    var touchZoneTop: Float
        get() = prefs.getFloat(KEY_TOUCH_ZONE_TOP, 0.2f)
        set(value) = prefs.edit { putFloat(KEY_TOUCH_ZONE_TOP, value) }

    var touchZoneWidth: Float
        get() = prefs.getFloat(KEY_TOUCH_ZONE_WIDTH, 0.8f)
        set(value) = prefs.edit { putFloat(KEY_TOUCH_ZONE_WIDTH, value) }

    var touchZoneHeight: Float
        get() = prefs.getFloat(KEY_TOUCH_ZONE_HEIGHT, 0.6f)
        set(value) = prefs.edit { putFloat(KEY_TOUCH_ZONE_HEIGHT, value) }

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

    // Horloge Numérique
    var isClockEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOCK_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_CLOCK_ENABLED, value) }

    var clockFont: String
        get() = prefs.getString(KEY_CLOCK_FONT, FONT_MONOSPACE) ?: FONT_MONOSPACE
        set(value) = prefs.edit { putString(KEY_CLOCK_FONT, value) }

    var clockColor: Int
        get() = prefs.getInt(KEY_CLOCK_COLOR, 0xFFFFFFFF.toInt())
        set(value) = prefs.edit { putInt(KEY_CLOCK_COLOR, value) }

    var clockSize: Float
        get() = prefs.getFloat(KEY_CLOCK_SIZE, 42f)
        set(value) = prefs.edit { putFloat(KEY_CLOCK_SIZE, value) }

    var clockFormat24h: Boolean
        get() = prefs.getBoolean(KEY_CLOCK_FORMAT_24H, true)
        set(value) = prefs.edit { putBoolean(KEY_CLOCK_FORMAT_24H, value) }

    var clockShowSeconds: Boolean
        get() = prefs.getBoolean(KEY_CLOCK_SHOW_SECONDS, false)
        set(value) = prefs.edit { putBoolean(KEY_CLOCK_SHOW_SECONDS, value) }

    var clockPosition: String
        get() = prefs.getString(KEY_CLOCK_POSITION, CLOCK_POS_FRONT) ?: CLOCK_POS_FRONT
        set(value) = prefs.edit { putString(KEY_CLOCK_POSITION, value) }

    var clockOffsetX: Float
        get() = prefs.getFloat(KEY_CLOCK_OFFSET_X, 0f)
        set(value) = prefs.edit { putFloat(KEY_CLOCK_OFFSET_X, value) }

    var clockOffsetY: Float
        get() = prefs.getFloat(KEY_CLOCK_OFFSET_Y, -350f)
        set(value) = prefs.edit { putFloat(KEY_CLOCK_OFFSET_Y, value) }

    var isDateEnabled: Boolean
        get() = prefs.getBoolean(KEY_DATE_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_DATE_ENABLED, value) }

    var dateFormat: String
        get() = prefs.getString(KEY_DATE_FORMAT, DATE_FORMAT_SHORT) ?: DATE_FORMAT_SHORT
        set(value) = prefs.edit { putString(KEY_DATE_FORMAT, value) }

    // Mode Ragebait
    var isRagebaitEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAGEBAIT_ENABLED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_RAGEBAIT_ENABLED, value) }
            applyRagebaitAlias(value)
        }

    fun applyRagebaitAlias(enabled: Boolean) {
        try {
            val pm = context.packageManager
            val mainComp = ComponentName(context, "com.example.waifuengine.MainActivity")
            val simComp = ComponentName(context, "com.example.waifuengine.SimToolkitAlias")

            if (enabled) {
                pm.setComponentEnabledSetting(
                    simComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                pm.setComponentEnabledSetting(
                    mainComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    simComp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
                        isPointerTrackingEnabled = obj.optBoolean("isPointerTrackingEnabled", true),
                        isTouchZoneCustom = obj.optBoolean("isTouchZoneCustom", false),
                        touchZoneLeft = obj.optDouble("touchZoneLeft", 0.1).toFloat(),
                        touchZoneTop = obj.optDouble("touchZoneTop", 0.2).toFloat(),
                        touchZoneWidth = obj.optDouble("touchZoneWidth", 0.8).toFloat(),
                        touchZoneHeight = obj.optDouble("touchZoneHeight", 0.6).toFloat(),
                        bgType = obj.optString("bgType", TYPE_NONE),
                        bgColor = obj.optInt("bgColor", 0xFF141418.toInt()),
                        bgImagePath = obj.optString("bgImagePath", ""),
                        bgVideoPath = obj.optString("bgVideoPath", ""),
                        bgScale = obj.optDouble("bgScale", 1.0).toFloat(),
                        bgOffsetX = obj.optDouble("bgOffsetX", 0.0).toFloat(),
                        bgOffsetY = obj.optDouble("bgOffsetY", 0.0).toFloat(),
                        musicPath = obj.optString("musicPath", ""),
                        isMusicEnabled = obj.optBoolean("isMusicEnabled", false),
                        musicVolume = obj.optDouble("musicVolume", 0.8).toFloat(),
                        isClockEnabled = obj.optBoolean("isClockEnabled", true),
                        clockFont = obj.optString("clockFont", FONT_MONOSPACE),
                        clockColor = obj.optInt("clockColor", 0xFFFFFFFF.toInt()),
                        clockSize = obj.optDouble("clockSize", 48.0).toFloat(),
                        clockFormat24h = obj.optBoolean("clockFormat24h", true),
                        clockShowSeconds = obj.optBoolean("clockShowSeconds", false),
                        isDateEnabled = obj.optBoolean("isDateEnabled", true),
                        dateFormat = obj.optString("dateFormat", DATE_FORMAT_FULL),
                        clockPosition = obj.optString("clockPosition", CLOCK_POS_FRONT),
                        clockOffsetX = obj.optDouble("clockOffsetX", 0.0).toFloat(),
                        clockOffsetY = obj.optDouble("clockOffsetY", -350.0).toFloat(),
                        engineType = obj.optString("engineType", ENGINE_LIVE2D)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun serializePresetList(currentList: List<PresetItem>): String {
        val array = JSONArray()
        for (item in currentList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("skelPath", item.skelPath)
                put("engineType", item.engineType)
                put("scale", item.scale)
                put("offsetX", item.offsetX)
                put("offsetY", item.offsetY)
                put("selectedCharacterName", item.selectedCharacterName)
                put("selectedVariantLabel", item.selectedVariantLabel)
                put("wakeupAnimation", item.wakeupAnimation)
                put("clickAnimation", item.clickAnimation)
                put("isPointerTrackingEnabled", item.isPointerTrackingEnabled)
                put("isTouchZoneCustom", item.isTouchZoneCustom)
                put("touchZoneLeft", item.touchZoneLeft)
                put("touchZoneTop", item.touchZoneTop)
                put("touchZoneWidth", item.touchZoneWidth)
                put("touchZoneHeight", item.touchZoneHeight)
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
                put("isClockEnabled", item.isClockEnabled)
                put("clockFont", item.clockFont)
                put("clockColor", item.clockColor)
                put("clockSize", item.clockSize)
                put("clockFormat24h", item.clockFormat24h)
                put("clockShowSeconds", item.clockShowSeconds)
                put("isDateEnabled", item.isDateEnabled)
                put("dateFormat", item.dateFormat)
                put("clockPosition", item.clockPosition)
                put("clockOffsetX", item.clockOffsetX)
                put("clockOffsetY", item.clockOffsetY)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun savePreset(preset: PresetItem) {
        val currentList = getPresets().toMutableList()
        currentList.removeAll { it.id == preset.id || it.name.equals(preset.name, ignoreCase = true) }
        currentList.add(0, preset)
        prefs.edit { putString("user_presets_json", serializePresetList(currentList)) }
    }

    fun deletePreset(id: String) {
        val currentList = getPresets().toMutableList()
        currentList.removeAll { it.id == id }
        prefs.edit { putString("user_presets_json", serializePresetList(currentList)) }
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
        const val KEY_ENGINE_TYPE = "key_engine_type"
        const val KEY_SPINE_ATLAS = "key_spine_atlas"
        const val ENGINE_LIVE2D = "LIVE2D"
        const val ENGINE_SPINE = "SPINE"
        const val KEY_SCALE = "key_scale"
        const val KEY_OFFSET_X = "key_offset_x"
        const val KEY_OFFSET_Y = "key_offset_y"
        const val KEY_CHAR_NAME = "key_char_name"
        const val KEY_VARIANT_LABEL = "key_variant_label"
        const val KEY_WAKEUP_ANIM = "key_wakeup_anim"
        const val KEY_CLICK_ANIM = "key_click_anim"
        const val KEY_POINTER_TRACKING_ENABLED = "key_pointer_tracking_enabled"
        const val KEY_TOUCH_ZONE_CUSTOM = "key_touch_zone_custom"
        const val KEY_TOUCH_ZONE_LEFT = "key_touch_zone_left"
        const val KEY_TOUCH_ZONE_TOP = "key_touch_zone_top"
        const val KEY_TOUCH_ZONE_WIDTH = "key_touch_zone_width"
        const val KEY_TOUCH_ZONE_HEIGHT = "key_touch_zone_height"
        
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

        const val KEY_CLOCK_ENABLED = "key_clock_enabled"
        const val KEY_CLOCK_FONT = "key_clock_font"
        const val KEY_CLOCK_COLOR = "key_clock_color"
        const val KEY_CLOCK_SIZE = "key_clock_size"
        const val KEY_CLOCK_FORMAT_24H = "key_clock_format_24h"
        const val KEY_CLOCK_SHOW_SECONDS = "key_clock_show_seconds"
        const val KEY_CLOCK_POSITION = "key_clock_position"
        const val KEY_CLOCK_OFFSET_X = "key_clock_offset_x"
        const val KEY_CLOCK_OFFSET_Y = "key_clock_offset_y"
        const val KEY_DATE_ENABLED = "key_date_enabled"
        const val KEY_DATE_FORMAT = "key_date_format"

        const val CLOCK_POS_FRONT = "FRONT"
        const val CLOCK_POS_BACK = "BACK"

        const val DATE_FORMAT_SHORT = "SHORT"
        const val DATE_FORMAT_FULL = "FULL"

        const val FONT_MONOSPACE = "MONOSPACE"
        const val FONT_SANS_SERIF = "SANS_SERIF"
        const val FONT_SERIF = "SERIF"
        const val FONT_THIN = "THIN"
        const val FONT_CONDENSED = "CONDENSED"
        const val FONT_CURSIVE = "CURSIVE"
        const val FONT_CASUAL = "CASUAL"

        const val TYPE_NONE = "NONE"
        const val TYPE_COLOR = "COLOR"
        const val TYPE_IMAGE = "IMAGE"
        const val TYPE_VIDEO = "VIDEO"

        const val DEFAULT_SKEL = ""
    }
}
