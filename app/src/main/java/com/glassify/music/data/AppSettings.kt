package com.glassify.music.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * User settings. All glass values are 0..1.
 *  - amount:     master "how glassy" slider (0 = solid, 1 = crystal clear glass)
 *  - blur:       how frosted the glass is
 *  - refraction: how much the glass bends what is behind it
 *  - opacity:    how dark / solid the glass tint is
 *  - shine:      brightness of the glass edge highlight
 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("glassify_v2", Context.MODE_PRIVATE)

    var amount by mutableFloatStateOf(prefs.getFloat("amount", 0.7f))
        private set
    var blur by mutableFloatStateOf(prefs.getFloat("blur", 0.3f))
        private set
    var refraction by mutableFloatStateOf(prefs.getFloat("refraction", 0.68f))
        private set
    var opacity by mutableFloatStateOf(prefs.getFloat("opacity", 0.3f))
        private set
    var shine by mutableFloatStateOf(prefs.getFloat("shine", 0.68f))
        private set
    /** 0 = Smooth (fastest), 1 = Balanced, 2 = Max (sharpest, slowest) */
    var quality by mutableIntStateOf(prefs.getInt("quality", 0))
        private set
    var albumBackground by mutableStateOf(prefs.getBoolean("albumBg", true))
        private set
    /** 0 = title, 1 = artist, 2 = recently added */
    var sort by mutableIntStateOf(prefs.getInt("sort", 0))
        private set

    /** Master slider: moves every glass value together. */
    @JvmName("applyAmount")
    fun setAmount(a: Float) {
        amount = a
        blur = mix(0.8f, 0.1f, a)
        refraction = mix(0f, 0.97f, a)
        opacity = mix(1f, 0f, a)
        shine = mix(0.15f, 0.9f, a)
        saveGlass()
    }

    @JvmName("applyQuality")
    fun setQuality(v: Int) {
        quality = v
        prefs.edit().putInt("quality", v).apply()
    }

    @JvmName("applyBlur")
    fun setBlur(v: Float) { blur = v; saveGlass() }

    @JvmName("applyRefraction")
    fun setRefraction(v: Float) { refraction = v; saveGlass() }

    @JvmName("applyOpacity")
    fun setOpacity(v: Float) { opacity = v; saveGlass() }

    @JvmName("applyShine")
    fun setShine(v: Float) { shine = v; saveGlass() }

    @JvmName("applyAlbumBackground")
    fun setAlbumBackground(v: Boolean) {
        albumBackground = v
        prefs.edit().putBoolean("albumBg", v).apply()
    }

    @JvmName("applySort")
    fun setSort(v: Int) {
        sort = v
        prefs.edit().putInt("sort", v).apply()
    }

    private fun saveGlass() {
        prefs.edit()
            .putFloat("amount", amount)
            .putFloat("blur", blur)
            .putFloat("refraction", refraction)
            .putFloat("opacity", opacity)
            .putFloat("shine", shine)
            .apply()
    }
}
