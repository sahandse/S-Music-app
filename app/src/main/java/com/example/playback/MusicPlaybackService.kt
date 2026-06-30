package com.example.playback

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
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

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when (key) {
            "bass_level", "treble_level" -> applyEqualizer(sp)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreate() {
        super.onCreate()

        // Configure audio attributes for high-quality music streaming
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // Automatically pause when headphone disconnected
            .build()

        player?.let { exoPlayer ->
            mediaSession = MediaSession.Builder(this, exoPlayer)
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
