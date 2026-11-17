package com.glassify.music.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("glassify", Context.MODE_PRIVATE)

    var amount by mutableFloatStateOf(prefs.getFloat("amount", 0.7f))
        private set
    var blur by mutableFloatStateOf(prefs.getFloat("blur", 0.35f))
        private set
    var refraction by mutableFloatStateOf(prefs.getFloat("refraction", 0.63f))
        private set
    var opacity by mutableFloatStateOf(prefs.getFloat("opacity", 0.33f))
        private set
    var shine by mutableFloatStateOf(prefs.getFloat("shine", 0.68f))
        private set
    var albumBackground by mutableStateOf(prefs.getBoolean("albumBg", true))
        private set
    var sort by mutableIntStateOf(prefs.getInt("sort", 0))
        private set

    @JvmName("applyAmount")
    fun setAmount(a: Float) {
        amount = a
        blur = mix(0.8f, 0.15f, a)
        refraction = mix(0f, 0.9f, a)
        opacity = mix(1f, 0.05f, a)
        shine = mix(0.15f, 0.9f, a)
        saveGlass()
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
