package com.glassify.music.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Playlist(val name: String, val ids: List<Long>)

/** Everything the user saves: favourites, history, playlists and preferences. */
class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("neumo", Context.MODE_PRIVATE)

    var favourites by mutableStateOf(readIds("fav").toSet())
        private set
    var recents by mutableStateOf(readIds("recent"))
        private set
    var recentSearches by mutableStateOf(readLines("searches"))
        private set
    var playlists by mutableStateOf(readPlaylists())
        private set
    var gridView by mutableStateOf(prefs.getBoolean("grid", false))
        private set
    /** 0 = title, 1 = artist, 2 = recently added */
    var sort by mutableIntStateOf(prefs.getInt("sort", 0))
        private set
    /** 0 = violet, 1 = blue */
    var accent by mutableIntStateOf(prefs.getInt("accent", 0))
        private set
    var showVisualizer by mutableStateOf(prefs.getBoolean("viz", true))
        private set

    private fun readIds(key: String): List<Long> =
        (prefs.getString(key, "") ?: "").split(",").mapNotNull { it.toLongOrNull() }

    private fun readLines(key: String): List<String> =
        (prefs.getString(key, "") ?: "").split("\n").filter { it.isNotBlank() }

    private fun readPlaylists(): List<Playlist> =
        readLines("playlists").map { line ->
            val parts = line.split("\t")
            Playlist(parts[0], (parts.getOrElse(1) { "" }).split(",").mapNotNull { it.toLongOrNull() })
        }

    private fun savePlaylists() {
        prefs.edit()
            .putString("playlists", playlists.joinToString("\n") { it.name + "\t" + it.ids.joinToString(",") })
            .apply()
    }

    fun toggleFavourite(id: Long) {
        favourites = if (id in favourites) favourites - id else favourites + id
        prefs.edit().putString("fav", favourites.joinToString(",")).apply()
    }

    fun pushRecent(id: Long) {
        recents = (listOf(id) + recents.filter { it != id }).take(30)
        prefs.edit().putString("recent", recents.joinToString(",")).apply()
    }

    fun addSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        recentSearches = (listOf(q) + recentSearches.filter { !it.equals(q, ignoreCase = true) }).take(8)
        prefs.edit().putString("searches", recentSearches.joinToString("\n")).apply()
    }

    fun clearSearches() {
        recentSearches = emptyList()
        prefs.edit().putString("searches", "").apply()
    }

    fun createPlaylist(rawName: String) {
        val name = rawName.replace("\t", " ").replace("\n", " ").trim()
        if (name.isEmpty() || playlists.any { it.name.equals(name, ignoreCase = true) }) return
        playlists = playlists + Playlist(name, emptyList())
        savePlaylists()
    }

    fun deletePlaylist(name: String) {
        playlists = playlists.filter { it.name != name }
        savePlaylists()
    }

    fun addToPlaylist(name: String, id: Long) {
        playlists = playlists.map { if (it.name == name && id !in it.ids) it.copy(ids = it.ids + id) else it }
        savePlaylists()
    }

    fun removeFromPlaylist(name: String, id: Long) {
        playlists = playlists.map { if (it.name == name) it.copy(ids = it.ids.filter { x -> x != id }) else it }
        savePlaylists()
    }

    fun updateGrid(value: Boolean) {
        gridView = value
        prefs.edit().putBoolean("grid", value).apply()
    }

    fun updateSort(value: Int) {
        sort = value
        prefs.edit().putInt("sort", value).apply()
    }

    fun updateAccent(value: Int) {
        accent = value
        prefs.edit().putInt("accent", value).apply()
    }

    fun updateVisualizer(value: Boolean) {
        showVisualizer = value
        prefs.edit().putBoolean("viz", value).apply()
    }
}
