package com.glassify.music.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MusicRepository {

    /** Reads every music file known to Android's MediaStore (local storage only). */
    suspend fun loadSongs(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = ArrayList<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            order,
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            while (c.moveToNext()) {
                songs += Song(
                    id = c.getLong(iId),
                    title = c.getString(iTitle)?.takeIf { it.isNotBlank() } ?: "Unknown title",
                    artist = cleanUnknown(c.getString(iArtist), "Unknown artist"),
                    album = cleanUnknown(c.getString(iAlbum), "Unknown album"),
                    albumId = c.getLong(iAlbumId),
                    durationMs = c.getLong(iDuration),
                    dateAdded = c.getLong(iAdded),
                    track = c.getInt(iTrack),
                )
            }
        }
        songs
    }

    private fun cleanUnknown(value: String?, fallback: String): String =
        if (value.isNullOrBlank() || value == "<unknown>") fallback else value
}
