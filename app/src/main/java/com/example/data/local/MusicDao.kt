package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Saved Server
    @Query("SELECT * FROM saved_servers WHERE isActive = 1 LIMIT 1")
    fun getActiveServerFlow(): Flow<SavedServer?>

    @Query("SELECT * FROM saved_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServer(): SavedServer?

    @Query("SELECT * FROM saved_servers ORDER BY url ASC")
    fun getAllServersFlow(): Flow<List<SavedServer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: SavedServer)

    @Query("UPDATE saved_servers SET isActive = 0")
    suspend fun deactivateAllServers()

    @Query("UPDATE saved_servers SET isActive = 1 WHERE url = :url")
    suspend fun activateServer(url: String)

    @Query("DELETE FROM saved_servers WHERE url = :url")
    suspend fun deleteServer(url: String)

    @Query("DELETE FROM saved_servers")
    suspend fun clearServers()

    // Favorite Tracks
    @Query("SELECT * FROM favorite_tracks ORDER BY timestamp DESC")
    fun getFavoriteTracksFlow(): Flow<List<FavoriteTrack>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :id)")
    suspend fun isTrackFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTrack(track: FavoriteTrack)

    @Query("DELETE FROM favorite_tracks WHERE id = :id")
    suspend fun deleteFavoriteTrack(id: String)

    // Favorite Albums
    @Query("SELECT * FROM favorite_albums ORDER BY timestamp DESC")
    fun getFavoriteAlbumsFlow(): Flow<List<FavoriteAlbum>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_albums WHERE id = :id)")
    suspend fun isAlbumFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteAlbum(album: FavoriteAlbum)

    @Query("DELETE FROM favorite_albums WHERE id = :id")
    suspend fun deleteFavoriteAlbum(id: String)

    // Favorite Artists
    @Query("SELECT * FROM favorite_artists ORDER BY timestamp DESC")
    fun getFavoriteArtistsFlow(): Flow<List<FavoriteArtist>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_artists WHERE id = :id)")
    suspend fun isArtistFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteArtist(artist: FavoriteArtist)

    @Query("DELETE FROM favorite_artists WHERE id = :id")
    suspend fun deleteFavoriteArtist(id: String)

    // Downloaded Tracks
    @Query("SELECT * FROM downloaded_tracks ORDER BY timestamp DESC")
    fun getDownloadedTracksFlow(): Flow<List<DownloadedTrack>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tracks WHERE id = :id)")
    suspend fun isTrackDownloaded(id: String): Boolean

    @Query("SELECT * FROM downloaded_tracks WHERE id = :id LIMIT 1")
    suspend fun getDownloadedTrack(id: String): DownloadedTrack?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedTrack(track: DownloadedTrack)

    @Query("DELETE FROM downloaded_tracks WHERE id = :id")
    suspend fun deleteDownloadedTrack(id: String)

    // Local Playlists
    @Query("SELECT * FROM local_playlists ORDER BY timestamp DESC")
    fun getLocalPlaylistsFlow(): Flow<List<LocalPlaylist>>

    @Query("SELECT * FROM local_playlists ORDER BY timestamp DESC")
    suspend fun getAllPlaylistsOnce(): List<LocalPlaylist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalPlaylist(playlist: LocalPlaylist): Long

    @Query("DELETE FROM local_playlists WHERE id = :id")
    suspend fun deleteLocalPlaylist(id: Long)

    @Query("DELETE FROM local_playlist_items WHERE playlistId = :playlistId")
    suspend fun deletePlaylistItems(playlistId: Long)

    // Local Playlist Items
    @Query("SELECT * FROM local_playlist_items WHERE playlistId = :playlistId ORDER BY timestamp ASC")
    fun getPlaylistItemsFlow(playlistId: Long): Flow<List<LocalPlaylistItem>>

    @Query("SELECT * FROM local_playlist_items WHERE playlistId = :playlistId ORDER BY timestamp ASC")
    suspend fun getPlaylistItemsOnce(playlistId: Long): List<LocalPlaylistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: LocalPlaylistItem)

    @Query("DELETE FROM local_playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    // Recent Plays
    @Query("SELECT * FROM recent_plays ORDER BY timestamp DESC LIMIT 30")
    fun getRecentPlaysFlow(): Flow<List<RecentPlay>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlay(play: RecentPlay)

    @Query("DELETE FROM recent_plays WHERE trackId = :trackId")
    suspend fun deleteRecentPlay(trackId: String)
}
