package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubsonicResponseWrapper(
    @Json(name = "subsonic-response") val response: SubsonicResponse
)

@JsonClass(generateAdapter = true)
data class SubsonicResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val artists: ArtistList? = null,
    val artist: ArtistDetail? = null,
    val album: AlbumDetail? = null,
    val starred: StarredContent? = null,
    val starred2: StarredContent? = null,
    val searchResult3: SearchResult3? = null,
    val playlists: PlaylistList? = null,
    val playlist: PlaylistDetail? = null,
    val lyricsList: List<LyricsInfo>? = null,
    val randomSongs: SongList? = null,
    val albumList: AlbumList? = null, // Used for random/newest lists
    val albumList2: AlbumList? = null
)

@JsonClass(generateAdapter = true)
data class SubsonicError(
    val code: Int,
    val message: String
)

@JsonClass(generateAdapter = true)
data class ArtistList(
    val index: List<ArtistIndex>? = null,
    val ignoredArticles: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtistIndex(
    val name: String,
    val artist: List<SubsonicArtist>? = null
)

@JsonClass(generateAdapter = true)
data class SubsonicArtist(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val albumCount: Int? = 0,
    val artistImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtistDetail(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val album: List<SubsonicAlbum>? = null
)

@JsonClass(generateAdapter = true)
data class AlbumList(
    val album: List<SubsonicAlbum>? = null
)

@JsonClass(generateAdapter = true)
data class SubsonicAlbum(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val created: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val playCount: Long? = 0,
    val starred: String? = null // Presence of date indicates starred
)

@JsonClass(generateAdapter = true)
data class AlbumDetail(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val playCount: Long? = 0,
    val song: List<SubsonicSong>? = null
)

@JsonClass(generateAdapter = true)
data class SongList(
    val song: List<SubsonicSong>? = null
)

@JsonClass(generateAdapter = true)
data class SubsonicSong(
    val id: String,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val isDir: Boolean? = false,
    val bitRate: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val duration: Int? = 0, // In seconds
    val size: Long? = 0, // In bytes
    val path: String? = null,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val playCount: Long? = 0,
    val starred: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class StarredContent(
    val artist: List<SubsonicArtist>? = null,
    val album: List<SubsonicAlbum>? = null,
    val song: List<SubsonicSong>? = null
)

@JsonClass(generateAdapter = true)
data class SearchResult3(
    val artist: List<SubsonicArtist>? = null,
    val album: List<SubsonicAlbum>? = null,
    val song: List<SubsonicSong>? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistList(
    val playlist: List<SubsonicPlaylist>? = null
)

@JsonClass(generateAdapter = true)
data class SubsonicPlaylist(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val public: Boolean? = false,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistDetail(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val songCount: Int? = 0,
    val duration: Int? = 0,
    val public: Boolean? = false,
    val created: String? = null,
    val entry: List<SubsonicSong>? = null
)

@JsonClass(generateAdapter = true)
data class LyricsList(
    val lyrics: List<LyricsInfo>? = null
)

@JsonClass(generateAdapter = true)
data class LyricsInfo(
    val artist: String? = null,
    val title: String? = null,
    val value: String? = null // Contains lyrics text
)
