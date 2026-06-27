package com.example.playback

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
                        connectionScope.launch {
                            repository.insertRecentPlay(songInQueue)
                        }
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

    private fun startTrackingProgress() {
        progressJob?.cancel()
        progressJob = connectionScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    _playbackProgress.value = controller.currentPosition
                    _trackDuration.value = controller.duration.coerceAtLeast(0)
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
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }

            controller.setMediaItems(mediaItems)
            val indexToPlay = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            controller.seekTo(indexToPlay, 0)
            controller.prepare()
            controller.play()

            _currentTrack.value = song
            repository.insertRecentPlay(song)
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.hasPreviousMediaItem()) {
            controller.seekToPrevious()
        } else {
            controller.seekTo(0)
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _playbackProgress.value = positionMs
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
