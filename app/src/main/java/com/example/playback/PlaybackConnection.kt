package com.example.playback

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.network.SubsonicSong
import com.example.data.repository.MusicRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("UnsafeOptInUsageError")
class PlaybackConnection(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val _currentTrack = MutableStateFlow<SubsonicSong?>(null)
    val currentTrack: StateFlow<SubsonicSong?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    private val _trackDuration = MutableStateFlow(0L)
    val trackDuration: StateFlow<Long> = _trackDuration.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<SubsonicSong>>(emptyList())
    val playbackQueue: StateFlow<List<SubsonicSong>> = _playbackQueue.asStateFlow()

    private val _sleepTimerMinutesLeft = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesLeft: StateFlow<Int?> = _sleepTimerMinutesLeft.asStateFlow()

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val connectionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupController()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        val controller = mediaController ?: return
        
        // Sync states initially
        _isPlaying.value = controller.isPlaying
        _isShuffleEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        _playbackSpeed.value = controller.playbackParameters.speed
        _trackDuration.value = controller.duration.coerceAtLeast(0)

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startTrackingProgress()
                } else {
                    stopTrackingProgress()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val songId = mediaItem?.mediaId
                if (songId != null) {
                    val songInQueue = _playbackQueue.value.find { it.id == songId }
                    if (songInQueue != null) {
                        _currentTrack.value = songInQueue
                    }
                }
                _trackDuration.value = controller.duration.coerceAtLeast(0)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _trackDuration.value = controller.duration.coerceAtLeast(0)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })

        if (controller.isPlaying) {
            startTrackingProgress()
        }
    }

    private var lastSavedPodcastPos = 0L

    private fun startTrackingProgress() {
        progressJob?.cancel()
        progressJob = connectionScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    _playbackProgress.value = controller.currentPosition
                    _trackDuration.value = controller.duration.coerceAtLeast(0)

                    // Remember podcast episode position (throttled to every ~5s)
                    val track = _currentTrack.value
                    if (track?.id?.startsWith("podcast_") == true) {
                        val pos = controller.currentPosition
                        if (kotlin.math.abs(pos - lastSavedPodcastPos) >= 5000L) {
                            lastSavedPodcastPos = pos
                            val duration = controller.duration
                            val prefs = context.getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
                            if (duration > 0 && pos > duration - 15_000L) {
                                // Episode essentially finished — restart from zero next time
                                prefs.edit().remove("podcast_pos_${track.id}").apply()
                            } else {
                                prefs.edit().putLong("podcast_pos_${track.id}", pos).apply()
                            }
                        }
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopTrackingProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(song: SubsonicSong, queue: List<SubsonicSong> = listOf(song)) {
        connectionScope.launch {
            val controller = mediaController ?: return@launch
            _playbackQueue.value = queue

            // Prepare MediaItems
            val mediaItems = queue.map { item ->
                val streamUrl = repository.getStreamUrl(item)
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.artist)
                    .setAlbumTitle(item.album)
                    .setArtworkUri(android.net.Uri.parse(repository.getCoverArtUrl(item.coverArt)))
                    .build()

                MediaItem.Builder()
                    .setMediaId(item.id)
                    .setUri(streamUrl)
                    .setMimeType(MimeTypes.AUDIO_MPEG)
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }

            controller.setMediaItems(mediaItems)
            val indexToPlay = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            // Podcast episodes resume from where the listener left off
            val resumePosition = if (song.id.startsWith("podcast_")) {
                context.getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
                    .getLong("podcast_pos_${song.id}", 0L)
            } else 0L
            controller.seekTo(indexToPlay, resumePosition)
            controller.prepare()
            controller.play()

            _currentTrack.value = song
            repository.insertRecentPlay(song)
        }
    }

    fun addToQueue(songs: List<SubsonicSong>) {
        if (songs.isEmpty()) return
        connectionScope.launch {
            val controller = mediaController ?: return@launch

            if (controller.mediaItemCount == 0) {
                // Nothing is queued yet, behave like a fresh play of the whole list
                playTrack(songs.first(), songs)
                return@launch
            }

            val mediaItems = songs.map { item ->
                val streamUrl = repository.getStreamUrl(item)
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setArtist(item.artist)
                    .setAlbumTitle(item.album)
                    .setArtworkUri(android.net.Uri.parse(repository.getCoverArtUrl(item.coverArt)))
                    .build()

                MediaItem.Builder()
                    .setMediaId(item.id)
                    .setUri(streamUrl)
                    .setMimeType(MimeTypes.AUDIO_MPEG)
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }

            controller.addMediaItems(mediaItems)
            _playbackQueue.value = _playbackQueue.value + songs
        }
    }

    fun stopPlayback() {
        val controller = mediaController ?: return
        controller.stop()
        controller.clearMediaItems()
        _currentTrack.value = null
        _playbackQueue.value = emptyList()
        _playbackProgress.value = 0L
    }

    private var fadeJob: Job? = null

    fun togglePlayPause() {
        val controller = mediaController ?: return
        val fadeEnabled = context
            .getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
            .getBoolean("fade_on_pause", false)
        if (!fadeEnabled) {
            if (controller.isPlaying) controller.pause() else controller.play()
            return
        }

        // Smooth ~300ms volume ramp instead of a hard cut. Ramps between fixed
        // 0..1 so rapid re-taps that cancel a ramp midway can never leave the
        // volume stuck at a partial level.
        fadeJob?.cancel()
        fadeJob = connectionScope.launch {
            if (controller.isPlaying) {
                for (step in 9 downTo 0) {
                    controller.volume = step / 10f
                    delay(30)
                }
                controller.pause()
                controller.volume = 1f
            } else {
                controller.volume = 0f
                controller.play()
                for (step in 1..10) {
                    controller.volume = step / 10f
                    delay(30)
                }
            }
        }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        // Seek by playlist index directly rather than relying on hasNextMediaItem()/seekToNext(),
        // whose backing COMMAND_SEEK_TO_NEXT_MEDIA_ITEM availability can lag behind the actual
        // playlist state over the MediaController IPC connection, making the button appear dead.
        val nextIndex = controller.currentMediaItemIndex + 1
        if (nextIndex < controller.mediaItemCount) {
            controller.seekTo(nextIndex, 0)
            controller.playWhenReady = true
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0)
            return
        }
        val previousIndex = controller.currentMediaItemIndex - 1
        if (previousIndex >= 0) {
            controller.seekTo(previousIndex, 0)
            controller.playWhenReady = true
        } else {
            controller.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _playbackProgress.value = positionMs
    }

    // Relative seek used by the podcast ±30s buttons
    fun seekBy(deltaMs: Long) {
        val controller = mediaController ?: return
        val duration = controller.duration
        val target = (controller.currentPosition + deltaMs)
            .coerceAtLeast(0L)
            .let { if (duration > 0) it.coerceAtMost(duration - 1000L) else it }
        controller.seekTo(target)
        _playbackProgress.value = target
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val nextMode = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = nextMode
        _isShuffleEnabled.value = nextMode
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val currentMode = controller.repeatMode
        val nextMode = when (currentMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun setPlaybackSpeed(speed: Float) {
        val controller = mediaController ?: return
        controller.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesLeft.value = minutes
        sleepTimerJob = connectionScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60000)
                remaining--
                _sleepTimerMinutesLeft.value = remaining
            }
            togglePlayPause() // Pause playback when timer completes
            _sleepTimerMinutesLeft.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerMinutesLeft.value = null
    }

    fun cleanUp() {
        connectionScope.cancel()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
