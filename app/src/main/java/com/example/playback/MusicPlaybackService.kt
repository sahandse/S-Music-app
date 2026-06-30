package com.example.playback

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.R
import com.example.data.local.MusicDatabase
import com.example.data.network.SubsonicSong
import com.example.data.repository.MusicRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
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
    }

    private fun favoriteButton(isFavorite: Boolean): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (isFavorite) "Unfavorite" else "Favorite")
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY))
            .setIconResId(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            .build()
    }

    private val sessionCallback = object : MediaSession.Callback {
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
            mediaSession = MediaSession.Builder(this, exoPlayer)
                .setCallback(sessionCallback)
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
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
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
        }
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
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
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
