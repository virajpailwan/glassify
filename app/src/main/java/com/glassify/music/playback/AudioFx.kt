package com.glassify.music.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Equalizer, bass boost and virtualizer. Lives in the same process as the playback service, so the
 * service attaches it to the player's audio session and the UI edits it directly.
 */
object AudioFx {
    private var eqFx: Equalizer? = null
    private var bassFx: BassBoost? = null
    private var virtFx: Virtualizer? = null
    private var prefs: SharedPreferences? = null

    var attached by mutableStateOf(false)
        private set
    var enabled by mutableStateOf(true)
        private set
    /** Index into [presetNames]; 5 means Custom. */
    var preset by mutableIntStateOf(0)
        private set
    var minLevel by mutableIntStateOf(-1500)
        private set
    var maxLevel by mutableIntStateOf(1500)
        private set
    val bandLevels = mutableStateListOf(0, 0, 0, 0, 0)
    var bandFreqs by mutableStateOf(listOf(60, 230, 910, 3600, 14000))
        private set
    /** 0..1000 */
    var bassLevel by mutableIntStateOf(0)
        private set
    /** 0..1000 */
    var virtLevel by mutableIntStateOf(0)
        private set

    val presetNames = listOf("Flat", "Rock", "Pop", "Classical", "Jazz", "Custom")

    private val shapes = listOf(
        floatArrayOf(0f, 0f, 0f, 0f, 0f),
        floatArrayOf(0.5f, 0.3f, -0.2f, 0.3f, 0.5f),
        floatArrayOf(-0.1f, 0.3f, 0.4f, 0.2f, -0.2f),
        floatArrayOf(0.4f, 0.2f, -0.2f, 0.2f, 0.4f),
        floatArrayOf(0.3f, 0.2f, -0.2f, 0.2f, 0.4f),
    )

    fun load(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("audiofx", Context.MODE_PRIVATE)
        prefs = p
        enabled = p.getBoolean("enabled", true)
        preset = p.getInt("preset", 0)
        bassLevel = p.getInt("bass", 0)
        virtLevel = p.getInt("virt", 0)
        val saved = (p.getString("bands", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }
        if (saved.isNotEmpty()) {
            bandLevels.clear()
            bandLevels.addAll(saved)
        }
    }

    fun attach(context: Context, sessionId: Int) {
        load(context)
        release()
        if (sessionId == 0) return
        runCatching {
            val e = Equalizer(0, sessionId)
            val n = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            minLevel = range[0].toInt()
            maxLevel = range[1].toInt()
            bandFreqs = List(n) { i -> e.getCenterFreq(i.toShort()) / 1000 }
            if (bandLevels.size != n) {
                bandLevels.clear()
                repeat(n) { bandLevels.add(0) }
                if (preset in 0..4) applyPresetLevels(preset)
            }
            eqFx = e
        }
        runCatching { bassFx = BassBoost(0, sessionId) }
        runCatching { virtFx = Virtualizer(0, sessionId) }
        attached = true
        applyAll()
    }

    fun release() {
        runCatching { eqFx?.release() }
        runCatching { bassFx?.release() }
        runCatching { virtFx?.release() }
        eqFx = null
        bassFx = null
        virtFx = null
        attached = false
    }

    private fun applyPresetLevels(index: Int) {
        val shape = shapes[index]
        val n = bandLevels.size
        for (i in 0 until n) {
            val pos = if (n == 1) 0f else i * (shape.size - 1f) / (n - 1f)
            val lo = pos.toInt().coerceAtMost(shape.size - 2)
            val f = pos - lo
            val v = shape[lo] * (1f - f) + shape[lo + 1] * f
            bandLevels[i] = (v * maxLevel).toInt().coerceIn(minLevel, maxLevel)
        }
    }

    private fun applyAll() {
        runCatching {
            eqFx?.let { e ->
                e.setEnabled(enabled)
                val count = minOf(bandLevels.size, e.numberOfBands.toInt())
                for (i in 0 until count) {
                    e.setBandLevel(i.toShort(), bandLevels[i].coerceIn(minLevel, maxLevel).toShort())
                }
            }
        }
        runCatching {
            bassFx?.let { b ->
                b.setEnabled(enabled && bassLevel > 0)
                if (b.strengthSupported) b.setStrength(bassLevel.toShort())
            }
        }
        runCatching {
            virtFx?.let { v ->
                v.setEnabled(enabled && virtLevel > 0)
                if (v.strengthSupported) v.setStrength(virtLevel.toShort())
            }
        }
    }

    private fun save() {
        prefs?.edit()
            ?.putBoolean("enabled", enabled)
            ?.putInt("preset", preset)
            ?.putInt("bass", bassLevel)
            ?.putInt("virt", virtLevel)
            ?.putString("bands", bandLevels.joinToString(","))
            ?.apply()
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        save()
        applyAll()
    }

    fun choosePreset(index: Int) {
        preset = index
        if (index in 0..4) applyPresetLevels(index)
        save()
        applyAll()
    }

    fun updateBand(index: Int, level: Int) {
        if (index !in bandLevels.indices) return
        bandLevels[index] = level.coerceIn(minLevel, maxLevel)
        preset = 5
        save()
        applyAll()
    }

    fun updateBass(value: Int) {
        bassLevel = value.coerceIn(0, 1000)
        save()
        applyAll()
    }

    fun updateVirtualizer(value: Int) {
        virtLevel = value.coerceIn(0, 1000)
        save()
        applyAll()
    }
}
