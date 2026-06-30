package com.example.data.repository

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlaylistsBackup(
    val playlists: List<BackupPlaylist>
)

@JsonClass(generateAdapter = true)
data class BackupPlaylist(
    val name: String,
    val description: String?,
    val items: List<BackupItem>
)

@JsonClass(generateAdapter = true)
data class BackupItem(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val coverArtUrl: String?
)
