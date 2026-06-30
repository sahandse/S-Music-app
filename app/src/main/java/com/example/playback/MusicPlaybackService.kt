package com.example.playback

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.cast.CastPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.local.MusicDatabase
import com.example.data.network.SubsonicSong
import com.example.data.repository.MusicRepository
import com.google.android.gms.cast.framework.CastContext
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null
    private var mediaSession: MediaLibrarySession? = null
    private var equalizer: Equalizer? = null
    private var prefs: SharedPreferences? = null
    private var fadeJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: MusicRepository

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when (key) {
            "bass_level", "treble_level" -> applyEqualizer(sp)
        }
    }

    companion object {
        private const val CUSTOM_COMMAND_TOGGLE_FAVORITE = "com.example.smusic.TOGGLE_FAVORITE"
        private const val ROOT_ID = "root"
        private const val SUGGESTED_ID = "suggested"
        private const val FAVORITES_ID = "favorites"
        private const val PLAYLISTS_ID = "playlists"
        private const val PLAYLIST_PREFIX = "playlist_"
    }

    private fun favoriteButton(isFavorite: Boolean): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (isFavorite) "Unfavorite" else "Favorite")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
            .setIconResId(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            .build()
    }

    private fun browsableItem(id: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun playableMediaItem(
        id: String,
        title: String,
        artist: String?,
        album: String?,
        coverArtUrl: String?
    ): MediaItem {
        val streamUrl = repository.getStreamUrl(SubsonicSong(id = id, title = title, artist = artist, album = album))
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(coverArtUrl?.let { Uri.parse(it) })
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun playableMediaItem(song: SubsonicSong): MediaItem {
        return playableMediaItem(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            coverArtUrl = repository.getCoverArtUrl(song.coverArt)
        )
    }

    private fun <T> futureResult(block: suspend () -> LibraryResult<T>): ListenableFuture<LibraryResult<T>> {
        val future = SettableFuture.create<LibraryResult<T>>()
        serviceScope.launch {
            val result = try {
                block()
            } catch (e: Exception) {
                LibraryResult.ofError<T>(LibraryResult.RESULT_ERROR_UNKNOWN)
            }
            future.set(result)
        }
        return future
    }

    private val librarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(connectionResult.availablePlayerCommands)
                .setCustomLayout(ImmutableList.of(favoriteButton(false)))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_COMMAND_TOGGLE_FAVORITE) {
                val mediaItem = player?.currentMediaItem
                if (mediaItem != null) {
                    serviceScope.launch {
                        val metadata = mediaItem.mediaMetadata
                        val song = SubsonicSong(
                            id = mediaItem.mediaId,
                            title = metadata.title?.toString() ?: "",
                            album = metadata.albumTitle?.toString(),
                            artist = metadata.artist?.toString(),
                            coverArt = metadata.artworkUri?.toString(),
                            duration = ((player?.duration ?: 0L) / 1000).toInt()
                        )
                        repository.toggleFavoriteTrack(song)
                        val isFavorite = repository.isTrackFavorite(song.id)
                        mediaSession?.setCustomLayout(ImmutableList.of(favoriteButton(isFavorite)))
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(browsableItem(ROOT_ID, "S Music"), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> = futureResult {
            val item = when {
                mediaId == ROOT_ID -> browsableItem(ROOT_ID, "S Music")
                mediaId == SUGGESTED_ID -> browsableItem(SUGGESTED_ID, "پیشنهادی برای شما")
                mediaId == FAVORITES_ID -> browsableItem(FAVORITES_ID, "علاقه‌مندی‌ها")
                mediaId == PLAYLISTS_ID -> browsableItem(PLAYLISTS_ID, "پلی‌لیست‌های من")
                mediaId.startsWith(PLAYLIST_PREFIX) -> {
                    val playlistId = mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                    val playlist = playlistId?.let { id -> repository.getLocalPlaylistsOnce().find { it.id == id } }
                    playlist?.let { browsableItem(mediaId, it.name) }
                }
                else -> null
            }
            if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = futureResult {
            val children: List<MediaItem> = when {
                parentId == ROOT_ID -> listOf(
                    browsableItem(SUGGESTED_ID, "پیشنهادی برای شما"),
                    browsableItem(FAVORITES_ID, "علاقه‌مندی‌ها"),
                    browsableItem(PLAYLISTS_ID, "پلی‌لیست‌های من")
                )
                parentId == SUGGESTED_ID -> repository.getRandomSongs().take(25).map { playableMediaItem(it) }
                parentId == FAVORITES_ID -> repository.getFavoriteTracksOnce().map { fav ->
                    playableMediaItem(fav.id, fav.title, fav.artist, fav.album, fav.coverArtUrl)
                }
                parentId == PLAYLISTS_ID -> repository.getLocalPlaylistsOnce().map { playlist ->
                    browsableItem("$PLAYLIST_PREFIX${playlist.id}", playlist.name)
                }
                parentId.startsWith(PLAYLIST_PREFIX) -> {
                    val playlistId = parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                    if (playlistId != null) {
                        repository.getPlaylistItemsOnce(playlistId).map { item ->
                            playableMediaItem(item.trackId, item.title, item.artist, item.album, item.coverArtUrl)
                        }
                    } else emptyList()
                }
                else -> emptyList()
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate() {
        super.onCreate()

        repository = MusicRepository(applicationContext, MusicDatabase.getDatabase(applicationContext).musicDao())

        // Configure audio attributes for high-quality music streaming
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // Automatically pause when headphone disconnected
            .setSeekForwardIncrementMs(10_000) // Adds a +10s lock screen control
            .setSeekBackIncrementMs(10_000) // Adds a -10s lock screen control
            .build()

        player?.let { exoPlayer ->
            val openAppIntent = Intent(this, MainActivity::class.java)
            val sessionActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            mediaSession = MediaLibrarySession.Builder(this, exoPlayer, librarySessionCallback)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()

            prefs = getSharedPreferences("smusic_prefs", MODE_PRIVATE)
            try {
                equalizer = Equalizer(0, exoPlayer.audioSessionId)
                equalizer?.enabled = true
                prefs?.let { applyEqualizer(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            prefs?.registerOnSharedPreferenceChangeListener(prefsListener)

            exoPlayer.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (prefs?.getBoolean("crossfade_enabled", false) == true) {
                        runFadeIn(exoPlayer)
                    }
                    val songId = mediaItem?.mediaId
                    if (songId != null) {
                        serviceScope.launch {
                            val isFavorite = repository.isTrackFavorite(songId)
                            mediaSession?.setCustomLayout(ImmutableList.of(favoriteButton(isFavorite)))
                        }
                    }
                }
            })

            setUpCastPlayer(exoPlayer)
        }
    }

    private fun setUpCastPlayer(localPlayer: ExoPlayer) {
        try {
            val newCastPlayer = CastPlayer(CastContext.getSharedInstance(this))
            newCastPlayer.setSessionAvailabilityListener(object : androidx.media3.cast.SessionAvailabilityListener {
                override fun onCastSessionAvailable() {
                    switchToPlayer(newCastPlayer)
                }

                override fun onCastSessionUnavailable() {
                    switchToPlayer(localPlayer)
                }
            })
            castPlayer = newCastPlayer
        } catch (e: Exception) {
            // Google Play services / Cast framework unavailable on this device.
            e.printStackTrace()
        }
    }

    private fun switchToPlayer(newPlayer: Player) {
        val session = mediaSession ?: return
        val oldPlayer = session.player
        if (oldPlayer === newPlayer) return

        val mediaItems = (0 until oldPlayer.mediaItemCount).map { oldPlayer.getMediaItemAt(it) }
        val currentIndex = oldPlayer.currentMediaItemIndex
        val position = oldPlayer.currentPosition
        val playWhenReady = oldPlayer.playWhenReady
        oldPlayer.stop()

        if (mediaItems.isNotEmpty()) {
            newPlayer.setMediaItems(mediaItems, currentIndex.coerceIn(0, mediaItems.size - 1), position)
            newPlayer.prepare()
            newPlayer.playWhenReady = playWhenReady
        }
        session.player = newPlayer
    }

    private fun applyEqualizer(sp: SharedPreferences) {
        val eq = equalizer ?: return
        try {
            val bass = sp.getInt("bass_level", 50) // 0..100, 50 = neutral
            val treble = sp.getInt("treble_level", 50)
            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]
            val bandCount = eq.numberOfBands
            if (bandCount > 0) {
                val bassLevel = (minLevel + (maxLevel - minLevel) * (bass / 100f)).toInt().toShort()
                val trebleLevel = (minLevel + (maxLevel - minLevel) * (treble / 100f)).toInt().toShort()
                eq.setBandLevel(0, bassLevel)
                eq.setBandLevel((bandCount - 1).toShort(), trebleLevel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun runFadeIn(exoPlayer: ExoPlayer) {
        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 10
            val durationMs = 600L
            exoPlayer.volume = 0f
            for (i in 1..steps) {
                exoPlayer.volume = i / steps.toFloat()
                delay(durationMs / steps)
            }
            exoPlayer.volume = 1f
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = player
        if (player != null) {
            if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE) {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        fadeJob?.cancel()
        serviceScope.cancel()
        prefs?.unregisterOnSharedPreferenceChangeListener(prefsListener)
        equalizer?.release()
        equalizer = null
        mediaSession?.release()
        mediaSession = null
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
        castPlayer = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
