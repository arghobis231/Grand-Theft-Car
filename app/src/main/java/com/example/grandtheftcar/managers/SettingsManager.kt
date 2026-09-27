package com.example.grandtheftcar.managers

import android.content.Context
import android.content.SharedPreferences

enum class ControlScheme {
    TOUCH_DRAG,
    SWIPE_LANES,
    ON_SCREEN_BUTTONS
}

enum class GraphicsQuality {
    LOW,
    MEDIUM,
    HIGH
}

enum class Difficulty {
    EASY,
    NORMAL,
    HARDCORE
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gtc_game_settings", Context.MODE_PRIVATE)

    var masterVolume: Float
        get() = prefs.getFloat(KEY_MASTER_VOL, 0.8f)
        set(value) = prefs.edit().putFloat(KEY_MASTER_VOL, value.coerceIn(0f, 1f)).apply()

    var musicVolume: Float
        get() = prefs.getFloat(KEY_MUSIC_VOL, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_MUSIC_VOL, value.coerceIn(0f, 1f)).apply()

    var sfxVolume: Float
        get() = prefs.getFloat(KEY_SFX_VOL, 0.9f)
        set(value) = prefs.edit().putFloat(KEY_SFX_VOL, value.coerceIn(0f, 1f)).apply()

    var isMuted: Boolean
        get() = prefs.getBoolean(KEY_IS_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_MUTED, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, value).apply()

    var brightness: Float
        get() = prefs.getFloat(KEY_BRIGHTNESS, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_BRIGHTNESS, value.coerceIn(0.5f, 1.5f)).apply()

    var graphicsQuality: GraphicsQuality
        get() {
            val name = prefs.getString(KEY_GRAPHICS, GraphicsQuality.HIGH.name)
            return try {
                GraphicsQuality.valueOf(name ?: GraphicsQuality.HIGH.name)
            } catch (_: Exception) {
                GraphicsQuality.HIGH
            }
        }
        set(value) = prefs.edit().putString(KEY_GRAPHICS, value.name).apply()

    var controlScheme: ControlScheme
        get() {
            val name = prefs.getString(KEY_CONTROLS, ControlScheme.TOUCH_DRAG.name)
            return try {
                ControlScheme.valueOf(name ?: ControlScheme.TOUCH_DRAG.name)
            } catch (_: Exception) {
                ControlScheme.TOUCH_DRAG
            }
        }
        set(value) = prefs.edit().putString(KEY_CONTROLS, value.name).apply()

    var difficulty: Difficulty
        get() {
            val name = prefs.getString(KEY_DIFFICULTY, Difficulty.NORMAL.name)
            return try {
                Difficulty.valueOf(name ?: Difficulty.NORMAL.name)
            } catch (_: Exception) {
                Difficulty.NORMAL
            }
        }
        set(value) = prefs.edit().putString(KEY_DIFFICULTY, value.name).apply()

    var highScore: Int
        get() = prefs.getInt(KEY_HIGH_SCORE, 0)
        set(value) {
            if (value > highScore) {
                prefs.edit().putInt(KEY_HIGH_SCORE, value).apply()
            }
        }

    var highestStage: Int
        get() = prefs.getInt(KEY_HIGHEST_STAGE, 1)
        set(value) {
            if (value > highestStage) {
                prefs.edit().putInt(KEY_HIGHEST_STAGE, value).apply()
            }
        }

    companion object {
        private const val KEY_MASTER_VOL = "master_volume"
        private const val KEY_MUSIC_VOL = "music_volume"
        private const val KEY_SFX_VOL = "sfx_volume"
        private const val KEY_IS_MUTED = "is_muted"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
        private const val KEY_BRIGHTNESS = "brightness"
        private const val KEY_GRAPHICS = "graphics_quality"
        private const val KEY_CONTROLS = "control_scheme"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_HIGHEST_STAGE = "highest_stage"
    }
}
