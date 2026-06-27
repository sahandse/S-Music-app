package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_servers")
data class SavedServer(
    @PrimaryKey val url: String,
    val username: String,
    val token: String,
    val salt: String,
    val isActive: Boolean = false
)

@Entity(tableName = "favorite_tracks")
data class FavoriteTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val coverArtUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_albums")
data class FavoriteAlbum(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val songCount: Int,
    val coverArtUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_artists")
data class FavoriteArtist(
    @PrimaryKey val id: String,
    val name: String,
    val albumCount: Int,
    val coverArtUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val localFilePath: String,
    val sizeBytes: Long,
    val coverArtPath: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_playlists")
data class LocalPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_playlist_items")
data class LocalPlaylistItem(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    val playlistId: Long,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val coverArtUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_plays")
data class RecentPlay(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val coverArtUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)
