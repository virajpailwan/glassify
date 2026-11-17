package com.glassify.music.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long,
    val track: Int,
) {
    val uri: Uri
        get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    val artUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

data class AlbumInfo(
    val id: Long,
    val title: String,
    val artist: String,
    val songs: List<Song>,
)

data class ArtistInfo(
    val name: String,
    val songs: List<Song>,
)

fun List<Song>.toAlbums(): List<AlbumInfo> =
    groupBy { it.albumId }
        .map { (id, list) ->
            val first = list.first()
            AlbumInfo(id, first.album, first.artist, list.sortedBy { it.track })
        }
        .sortedBy { it.title.lowercase() }

fun List<Song>.toArtists(): List<ArtistInfo> =
    groupBy { it.artist }
        .map { (name, list) -> ArtistInfo(name, list.sortedBy { it.title.lowercase() }) }
        .sortedBy { it.name.lowercase() }
