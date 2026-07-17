package com.example.ui

import android.app.Application
import android.content.Context
import android.speech.RecognizerIntent
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.network.ArtistInfo
import com.example.data.network.EnrichedArtistInfo
import com.example.data.network.InternetRadioStation
import com.example.data.network.MusicBrainzArtistDetail
import com.example.data.network.SearchResult3
import com.example.data.network.SubsonicAlbum
import com.example.data.network.SubsonicArtist
import com.example.data.network.SubsonicSong
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackConnection
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull

data class MusicVideoItem(
    val id: Long,
    val title: String,
    val artistName: String,
    val thumbnailUrl: String,
    val previewUrl: String
)

data class ApplePreviewTrack(
    val id: Long,
    val title: String,
    val artistName: String,
    val artworkUrl: String,
    val previewUrl: String
)

data class LyricsSearchHit(
    val trackName: String,
    val artistName: String,
    val snippet: String
)

data class PodcastItem(
    val id: Long,
    val name: String,
    val artistName: String,
    val artworkUrl: String,
    val feedUrl: String
)

data class PodcastEpisode(
    val title: String,
    val audioUrl: String,
    val durationSec: Int,
    val pubDate: String,
    val imageUrl: String
)

data class TelegramSong(
    val fileId: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val durationSec: Int,
    val sizeBytes: Long
)

data class CatalogSong(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String,
    val durationSec: Int
)

class MusicViewModel(
    application: Application,
    private val repository: MusicRepository,
    val playbackConnection: PlaybackConnection
) : AndroidViewModel(application) {

    // --- Authentication State ---
    private val _isServerConfigured = MutableStateFlow(false)
    val isServerConfigured: StateFlow<Boolean> = _isServerConfigured.asStateFlow()

    private val _activeServer = MutableStateFlow<SavedServer?>(null)
    val activeServer: StateFlow<SavedServer?> = _activeServer.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // --- Home Content State ---
    private val _homeRandomSongs = MutableStateFlow<List<SubsonicSong>>(emptyList())
    val homeRandomSongs: StateFlow<List<SubsonicSong>> = _homeRandomSongs.asStateFlow()

    private val _homeNewestAlbums = MutableStateFlow<List<SubsonicAlbum>>(emptyList())
    val homeNewestAlbums: StateFlow<List<SubsonicAlbum>> = _homeNewestAlbums.asStateFlow()

    private val _artistsList = MutableStateFlow<List<SubsonicArtist>>(emptyList())
    val artistsList: StateFlow<List<SubsonicArtist>> = _artistsList.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    // --- Detail View States ---
    private val _selectedArtistDetails = MutableStateFlow<SubsonicArtist?>(null)
    val selectedArtistDetails: StateFlow<SubsonicArtist?> = _selectedArtistDetails.asStateFlow()

    private val _selectedArtistAlbums = MutableStateFlow<List<SubsonicAlbum>>(emptyList())
    val selectedArtistAlbums: StateFlow<List<SubsonicAlbum>> = _selectedArtistAlbums.asStateFlow()

    private val _selectedArtistBio = MutableStateFlow<ArtistInfo?>(null)
    val selectedArtistBio: StateFlow<ArtistInfo?> = _selectedArtistBio.asStateFlow()

    private val _selectedAlbumDetails = MutableStateFlow<SubsonicAlbum?>(null)
    val selectedAlbumDetails: StateFlow<SubsonicAlbum?> = _selectedAlbumDetails.asStateFlow()

    private val _selectedAlbumSongs = MutableStateFlow<List<SubsonicSong>>(emptyList())
    val selectedAlbumSongs: StateFlow<List<SubsonicSong>> = _selectedAlbumSongs.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    // --- Search State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResult3>(SearchResult3(null, null, null))
    val searchResults: StateFlow<SearchResult3> = _searchResults.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // --- Library States ---
    val favoriteTracks: StateFlow<List<FavoriteTrack>> = repository.getFavoriteTracksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteAlbums: StateFlow<List<FavoriteAlbum>> = repository.getFavoriteAlbumsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteArtists: StateFlow<List<FavoriteArtist>> = repository.getFavoriteArtistsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedTracks: StateFlow<List<DownloadedTrack>> = repository.getDownloadedTracksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localPlaylists: StateFlow<List<LocalPlaylist>> = repository.getLocalPlaylistsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPlays: StateFlow<List<RecentPlay>> = repository.getRecentPlaysFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Playlist details (when exploring a playlist) ---
    private val _activePlaylistItems = MutableStateFlow<List<LocalPlaylistItem>>(emptyList())
    val activePlaylistItems: StateFlow<List<LocalPlaylistItem>> = _activePlaylistItems.asStateFlow()

    // --- Download Progress tracker ---
    private val _downloadProgresses = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgresses: StateFlow<Map<String, Float>> = _downloadProgresses.asStateFlow()

    // --- Dynamic Accent Color (extracted from album art via Palette) ---
    private val _dominantColorArgb = MutableStateFlow(0xFFFA2D48.toInt())
    val dominantColorArgb: StateFlow<Int> = _dominantColorArgb.asStateFlow()

    // --- Lyrics State ---
    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    // --- Settings State ---
    private val _themeMode = MutableStateFlow("dark") // "light", "dark", "amoled", "glass"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themeAccent = MutableStateFlow("red") // "red", "blue", "green", "orange", "purple", "pink"
    val themeAccent: StateFlow<String> = _themeAccent.asStateFlow()

    private val _language = MutableStateFlow("fa") // "fa", "en"
    val language: StateFlow<String> = _language.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow("")
    val profilePhotoUri: StateFlow<String> = _profilePhotoUri.asStateFlow()

    private val _onboardingPreferencesSet = MutableStateFlow(false)
    val onboardingPreferencesSet: StateFlow<Boolean> = _onboardingPreferencesSet.asStateFlow()

    private val _preferredGenres = MutableStateFlow<Set<String>>(emptySet())
    val preferredGenres: StateFlow<Set<String>> = _preferredGenres.asStateFlow()

    private val _downloadLocation = MutableStateFlow("")
    val downloadLocation: StateFlow<String> = _downloadLocation.asStateFlow()

    private val _audioQuality = MutableStateFlow("high") // "low", "medium", "high", "lossless"
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _downloadQuality = MutableStateFlow("high") // "low", "medium", "high", "lossless"
    val downloadQuality: StateFlow<String> = _downloadQuality.asStateFlow()

    private val _cacheSizeMb = MutableStateFlow(128)
    val cacheSizeMb: StateFlow<Int> = _cacheSizeMb.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(false)
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    // --- Equalizer & Playback Enhancements ---
    private val _bassLevel = MutableStateFlow(50) // 0..100, 50 = neutral
    val bassLevel: StateFlow<Int> = _bassLevel.asStateFlow()

    private val _trebleLevel = MutableStateFlow(50)
    val trebleLevel: StateFlow<Int> = _trebleLevel.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSeconds = MutableStateFlow(3)
    val crossfadeDurationSeconds: StateFlow<Int> = _crossfadeDurationSeconds.asStateFlow()

    private val _wifiOnlyDownload = MutableStateFlow(false)
    val wifiOnlyDownload: StateFlow<Boolean> = _wifiOnlyDownload.asStateFlow()

    private val _fadeOnPause = MutableStateFlow(false)
    val fadeOnPause: StateFlow<Boolean> = _fadeOnPause.asStateFlow()

    private val _resumeOnBluetooth = MutableStateFlow(false)
    val resumeOnBluetooth: StateFlow<Boolean> = _resumeOnBluetooth.asStateFlow()

    private val _saveDownloadsPublic = MutableStateFlow(true)
    val saveDownloadsPublic: StateFlow<Boolean> = _saveDownloadsPublic.asStateFlow()

    // Set by MainActivity when the app is launched from a launcher shortcut
    var pendingShortcutDest: String? = null

    // --- Multi-server management ---
    val allServers: StateFlow<List<SavedServer>> = repository.getAllServersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search filter & trending ---
    private val _searchFilter = MutableStateFlow("all") // "all", "song", "album", "artist"
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    private val _trendingAlbums = MutableStateFlow<List<SubsonicAlbum>>(emptyList())
    val trendingAlbums: StateFlow<List<SubsonicAlbum>> = _trendingAlbums.asStateFlow()

    // --- Artist detail extras ---
    private val _artistTopTracks = MutableStateFlow<List<SubsonicSong>>(emptyList())
    val artistTopTracks: StateFlow<List<SubsonicSong>> = _artistTopTracks.asStateFlow()

    private val _artistSimilarArtists = MutableStateFlow<List<SubsonicArtist>>(emptyList())
    val artistSimilarArtists: StateFlow<List<SubsonicArtist>> = _artistSimilarArtists.asStateFlow()

    private val _artistAllSongs = MutableStateFlow<List<SubsonicSong>>(emptyList())
    val artistAllSongs: StateFlow<List<SubsonicSong>> = _artistAllSongs.asStateFlow()

    private val _isLoadingArtistAllSongs = MutableStateFlow(false)
    val isLoadingArtistAllSongs: StateFlow<Boolean> = _isLoadingArtistAllSongs.asStateFlow()

    // --- Album detail extras ---
    private val _relatedAlbums = MutableStateFlow<List<SubsonicAlbum>>(emptyList())
    val relatedAlbums: StateFlow<List<SubsonicAlbum>> = _relatedAlbums.asStateFlow()

    // --- Enriched artist info (Last.fm + MusicBrainz) ---
    private val _enrichedArtistInfo = MutableStateFlow<EnrichedArtistInfo?>(null)
    val enrichedArtistInfo: StateFlow<EnrichedArtistInfo?> = _enrichedArtistInfo.asStateFlow()

    // --- MusicBrainz artist detail (Wikipedia link + band members) ---
    private val _artistMbDetail = MutableStateFlow<MusicBrainzArtistDetail?>(null)
    val artistMbDetail: StateFlow<MusicBrainzArtistDetail?> = _artistMbDetail.asStateFlow()

    // --- Internet Radio Stations ---
    private val _internetRadioStations = MutableStateFlow<List<InternetRadioStation>>(emptyList())
    val internetRadioStations: StateFlow<List<InternetRadioStation>> = _internetRadioStations.asStateFlow()

    // --- Lyrics Translation ---
    private val _translatedLyrics = MutableStateFlow<String?>(null)
    val translatedLyrics: StateFlow<String?> = _translatedLyrics.asStateFlow()

    // --- Last.fm Account State ---

    // --- Transient one-shot UI messages (toasts) ---
    private val _transientMessage = MutableStateFlow<String?>(null)
    val transientMessage: StateFlow<String?> = _transientMessage.asStateFlow()

    // --- Profile stats ---
    val estimatedListeningMinutes: StateFlow<Int> = recentPlays
        .map { plays -> plays.sumOf { it.duration } / 60 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var searchJob: Job? = null

    init {
        // Observe auth status
        viewModelScope.launch {
            repository.getActiveServerFlow().collect { server ->
                _activeServer.value = server
                val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
                val isGuestLoggedIn = prefs.getBoolean("is_guest_logged_in", false)
                _isServerConfigured.value = server != null || isGuestLoggedIn
                if (server != null || isGuestLoggedIn) {
                    loadHomeContent()
                } else {
                    // Fallback to Demo mode home loading
                    loadHomeContent()
                }
            }
        }

        // Keep track of lyrics when track changes
        viewModelScope.launch {
            playbackConnection.currentTrack.collect { song ->
                _currentLyrics.value = null
                if (song != null) {
                    fetchLyricsForTrack(song)
                }
            }
        }

        // Extract dominant color from album art for dynamic app-wide accent
        viewModelScope.launch {
            playbackConnection.currentTrack.collect { song ->
                if (song == null) {
                    _dominantColorArgb.value = 0xFFFA2D48.toInt()
                    return@collect
                }
                try {
                    val ctx = getApplication<Application>()
                    val req = ImageRequest.Builder(ctx)
                        .data(getCoverArtUrl(song.coverArt))
                        .allowHardware(false)
                        .size(128)
                        .build()
                    val result = ctx.imageLoader.execute(req)
                    if (result is SuccessResult) {
                        val bmp = (result.drawable as? BitmapDrawable)?.bitmap
                        bmp?.let {
                            val palette = Palette.from(it).generate()
                            val swatch = palette.vibrantSwatch
                                ?: palette.dominantSwatch
                                ?: palette.mutedSwatch
                            swatch?.let { s -> _dominantColorArgb.value = s.rgb }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // Initialize Settings from preferences
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString("theme_mode", "dark") ?: "dark"
        _themeAccent.value = prefs.getString("theme_accent", "red") ?: "red"
        _language.value = prefs.getString("app_language", "fa") ?: "fa"
        _userName.value = prefs.getString("user_name", "") ?: ""
        _profilePhotoUri.value = prefs.getString("profile_photo_uri", "") ?: ""
        _onboardingPreferencesSet.value = prefs.getBoolean("onboarding_preferences_set", false)
        _preferredGenres.value = prefs.getStringSet("preferred_genres", emptySet()) ?: emptySet()
        _downloadLocation.value = prefs.getString("download_location", "") ?: ""
        _audioQuality.value = prefs.getString("audio_quality", "high") ?: "high"
        _downloadQuality.value = prefs.getString("download_quality", "high") ?: "high"
        repository.setStreamQuality(_audioQuality.value)
        repository.setDownloadQuality(_downloadQuality.value)
        _cacheSizeMb.value = prefs.getInt("cache_size_mb", 128)
        _bassLevel.value = prefs.getInt("bass_level", 50)
        _trebleLevel.value = prefs.getInt("treble_level", 50)
        _crossfadeEnabled.value = prefs.getBoolean("crossfade_enabled", false)
        _crossfadeDurationSeconds.value = prefs.getInt("crossfade_duration_seconds", 3)
        _wifiOnlyDownload.value = prefs.getBoolean("wifi_only_download", false)
        _fadeOnPause.value = prefs.getBoolean("fade_on_pause", false)
        _resumeOnBluetooth.value = prefs.getBoolean("resume_on_bluetooth", false)
        _saveDownloadsPublic.value = prefs.getBoolean("save_downloads_public", true)
        _autoBackupEnabled.value = prefs.getBoolean("auto_backup_enabled", false)
        loadTrendingContent()
        startAutoBackupLoop()
        loadInternetRadioStations()
    }

    private fun startAutoBackupLoop() {
        viewModelScope.launch {
            while (true) {
                delay(6 * 60 * 60 * 1000L) // every 6 hours
                if (_autoBackupEnabled.value) {
                    exportPlaylistsBackup()
                }
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        _autoBackupEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
    }

    // --- Server Auth Actions ---
    fun login(url: String, username: String, passwordOrToken: String, isTokenMode: Boolean = false) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _loginError.value = null
            val success = repository.saveServer(url, username, passwordOrToken, isTokenMode)
            _isAuthenticating.value = false
            if (!success) {
                _loginError.value = "اتصال ناموفق بود. مشخصات ورود را بررسی کنید."
            }
        }
    }

    fun bypassAuthAndGuestMode() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_guest_logged_in", true).apply()
            _isServerConfigured.value = true
            loadHomeContent()
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_guest_logged_in", false).apply()
            _isServerConfigured.value = false
            playbackConnection.togglePlayPause() // stop music if playing
        }
    }

    // --- Content Loading ---
    fun loadHomeContent() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            try {
                // Load random tracks
                _homeRandomSongs.value = repository.getRandomSongs()
                // Load newest albums
                _homeNewestAlbums.value = repository.getAlbumList("newest")
                // Load artists
                _artistsList.value = repository.getArtists()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    // --- Detail Nav Loaders ---
    fun loadArtistDetails(artist: SubsonicArtist) {
        _selectedArtistDetails.value = artist
        _selectedArtistAlbums.value = emptyList()
        _selectedArtistBio.value = null
        _artistTopTracks.value = emptyList()
        _artistSimilarArtists.value = emptyList()
        _artistAllSongs.value = emptyList()
        _enrichedArtistInfo.value = null
        _artistMbDetail.value = null
        viewModelScope.launch {
            _isLoadingDetail.value = true
            val details = repository.getArtistDetails(artist.id)
            val artistAlbums = details?.album ?: emptyList()
            if (details != null) {
                _selectedArtistAlbums.value = artistAlbums
            }
            val bio = repository.getArtistInfo(artist.id)
            _selectedArtistBio.value = bio
            _artistTopTracks.value = repository.getTopSongs(artist.name, artist.id)
            _artistSimilarArtists.value = repository.getSimilarArtists(artist.id)
            _isLoadingDetail.value = false

            _isLoadingArtistAllSongs.value = true
            _artistAllSongs.value = artistAlbums.flatMap { album ->
                repository.getAlbumDetails(album.id)?.song ?: emptyList()
            }
            _isLoadingArtistAllSongs.value = false
        }
        // Enrich with Last.fm + MusicBrainz in background
        viewModelScope.launch {
            try {
                _enrichedArtistInfo.value = repository.getEnrichedArtistInfo(artist.name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAlbumDetails(album: SubsonicAlbum) {
        _selectedAlbumDetails.value = album
        _selectedAlbumSongs.value = emptyList()
        _relatedAlbums.value = emptyList()
        viewModelScope.launch {
            _isLoadingDetail.value = true
            val details = repository.getAlbumDetails(album.id)
            if (details != null) {
                _selectedAlbumSongs.value = details.song ?: emptyList()
            }
            val artistId = album.artistId
            if (artistId != null) {
                val artistDetails = repository.getArtistDetails(artistId)
                _relatedAlbums.value = artistDetails?.album?.filter { it.id != album.id } ?: emptyList()
            }
            _isLoadingDetail.value = false
        }
    }

    fun loadPlaylistItems(playlistId: Long) {
        viewModelScope.launch {
            repository.getPlaylistItemsFlow(playlistId).collect {
                _activePlaylistItems.value = it
            }
        }
    }

    // --- Playback Controls Helpers ---
    fun playSongDirectly(song: SubsonicSong, queue: List<SubsonicSong>) {
        playbackConnection.playTrack(song, queue)
    }

    fun pausePlayback() {
        if (playbackConnection.isPlaying.value) playbackConnection.togglePlayPause()
    }

    fun playDownloadedTrack(track: com.example.data.local.DownloadedTrack) {
        val song = SubsonicSong(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration
        )
        playbackConnection.playTrack(song, listOf(song))
    }

    fun playAllSongsInQueue(songsList: List<SubsonicSong>) {
        if (songsList.isNotEmpty()) {
            playSongDirectly(songsList.first(), songsList)
        }
    }

    fun shufflePlayAllSongsInQueue(songsList: List<SubsonicSong>) {
        if (songsList.isNotEmpty()) {
            val shuffled = songsList.shuffled()
            playSongDirectly(shuffled.first(), shuffled)
        }
    }

    fun addToQueue(songs: List<SubsonicSong>) {
        if (songs.isEmpty()) return
        playbackConnection.addToQueue(songs)
        _transientMessage.value = "به صف پخش اضافه شد"
    }

    // --- Favorite Liking Toggles ---
    fun toggleFavoriteTrack(song: SubsonicSong) {
        viewModelScope.launch {
            repository.toggleFavoriteTrack(song)
        }
    }

    fun toggleFavoriteAlbum(album: SubsonicAlbum) {
        viewModelScope.launch {
            repository.toggleFavoriteAlbum(album)
        }
    }

    fun toggleFavoriteArtist(artist: SubsonicArtist) {
        viewModelScope.launch {
            repository.toggleFavoriteArtist(artist)
        }
    }

    // --- Live Search ---
    // --- Lyrics-content search results (LRCLIB) ---
    private val _lyricsSearchHits = MutableStateFlow<List<LyricsSearchHit>>(emptyList())
    val lyricsSearchHits: StateFlow<List<LyricsSearchHit>> = _lyricsSearchHits.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = SearchResult3(null, null, null)
            _lyricsSearchHits.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce API calls
            _isSearching.value = true
            val results = repository.search(query)
            _searchResults.value = applyFuzzyFallback(query, results)
            _isSearching.value = false

            // Lyrics-content search runs after main results so it never delays them
            try {
                val hits = repository.searchInLyrics(query)
                _lyricsSearchHits.value = hits.mapNotNull { t ->
                    val name = t.trackName ?: return@mapNotNull null
                    val snippet = t.plainLyrics
                        ?.lineSequence()
                        ?.firstOrNull { line -> line.contains(query, ignoreCase = true) }
                        ?.trim()
                        ?: ""
                    LyricsSearchHit(trackName = name, artistName = t.artistName ?: "", snippet = snippet)
                }
            } catch (_: Exception) {
                _lyricsSearchHits.value = emptyList()
            }
        }
    }

    // Play a lyrics-search hit by locating it in the user's own server library
    fun playLyricsHit(hit: LyricsSearchHit) {
        viewModelScope.launch {
            try {
                val candidates = repository.search(hit.trackName).song ?: emptyList()
                val song = candidates.firstOrNull { s ->
                    s.artist != null && hit.artistName.isNotBlank() && (
                        s.artist.contains(hit.artistName, ignoreCase = true) ||
                            hit.artistName.contains(s.artist, ignoreCase = true)
                        )
                } ?: candidates.firstOrNull { it.title.equals(hit.trackName, ignoreCase = true) }
                if (song != null) {
                    playSongDirectly(song, listOf(song))
                } else {
                    _transientMessage.value = "این آهنگ در سرور شما موجود نیست"
                }
            } catch (_: Exception) {
                _transientMessage.value = "این آهنگ در سرور شما موجود نیست"
            }
        }
    }

    // --- Typo-tolerant search: when the server finds little, fuzzy-match local pools ---
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            prev = curr.copyInto(IntArray(b.length + 1))
        }
        return prev[b.length]
    }

    private fun isFuzzyMatch(query: String, target: String): Boolean {
        val q = query.lowercase().trim()
        val t = target.lowercase()
        if (q.length < 3) return t.contains(q)
        if (t.contains(q)) return true
        val threshold = if (q.length <= 5) 1 else 2
        if (levenshtein(q, t) <= threshold) return true
        return t.split(' ', '-', '_').any { token ->
            token.length >= 3 && levenshtein(q, token) <= threshold
        }
    }

    private fun applyFuzzyFallback(query: String, results: SearchResult3): SearchResult3 {
        val existing = results.song ?: emptyList()
        if (existing.size >= 3) return results
        val pool = (_homeRandomSongs.value + _deviceSongs.value)
        val fuzzy = pool
            .filter { s ->
                isFuzzyMatch(query, s.title) || (s.artist?.let { isFuzzyMatch(query, it) } == true)
            }
            .filter { s -> existing.none { it.id == s.id } }
            .distinctBy { it.id }
            .take(20)
        return if (fuzzy.isEmpty()) results else results.copy(song = existing + fuzzy)
    }

    fun addSearchHistory(query: String) {
        if (query.isNotBlank() && !_searchHistory.value.contains(query)) {
            val list = _searchHistory.value.toMutableList()
            list.add(0, query)
            _searchHistory.value = list.take(10)
        }
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
    }

    // --- Offline Downloads Management ---
    private fun isOnWifi(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun startDownload(song: SubsonicSong) {
        viewModelScope.launch {
            if (repository.isTrackDownloaded(song.id)) return@launch

            if (_wifiOnlyDownload.value && !isOnWifi()) {
                _transientMessage.value = "دانلود فقط در حالت وای‌فای فعال است"
                return@launch
            }

            _downloadProgresses.value = _downloadProgresses.value.toMutableMap().apply {
                put(song.id, 0.01f)
            }

            val success = repository.downloadTrack(song) { progress ->
                _downloadProgresses.value = _downloadProgresses.value.toMutableMap().apply {
                    put(song.id, progress)
                }
            }

            _downloadProgresses.value = _downloadProgresses.value.toMutableMap().apply {
                remove(song.id)
            }
        }
    }

    fun startDownloadAll(songs: List<SubsonicSong>) {
        viewModelScope.launch {
            for (song in songs) {
                if (repository.isTrackDownloaded(song.id)) continue
                if (_wifiOnlyDownload.value && !isOnWifi()) {
                    _transientMessage.value = "دانلود فقط در حالت وای‌فای فعال است"
                    return@launch
                }
                _downloadProgresses.value = _downloadProgresses.value.toMutableMap().apply {
                    put(song.id, 0.01f)
                }
                repository.downloadTrack(song) { progress ->
                    _downloadProgresses.value = _downloadProgresses.value.toMutableMap().apply {
                        put(song.id, progress)
                    }
                }
                _downloadProgresses.value = _downloadProgresses.value.toMutableMap().apply {
                    remove(song.id)
                }
            }
            _transientMessage.value = "دانلود آلبوم به پایان رسید"
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            repository.deleteDownload(songId)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            val list = downloadedTracks.value
            for (track in list) {
                repository.deleteDownload(track.id)
            }
        }
    }

    // --- Playlist Controls ---
    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            repository.createLocalPlaylist(name, description)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, song: SubsonicSong) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, song)
        }
    }

    // Manual playlist creation from picked songs
    fun createPlaylistWithSongs(name: String, songs: List<SubsonicSong>) {
        viewModelScope.launch {
            val id = repository.createLocalPlaylist(name, null)
            songs.forEach { repository.addTrackToPlaylist(id, it) }
            _transientMessage.value = "پلی‌لیست «$name» با ${songs.size} آهنگ ساخته شد"
        }
    }

    // --- Spotify playlist import (public embed page, no API key) ---
    private val _spotifyImportStatus = MutableStateFlow<String?>(null)
    val spotifyImportStatus: StateFlow<String?> = _spotifyImportStatus.asStateFlow()
    private val _isImportingSpotify = MutableStateFlow(false)
    val isImportingSpotify: StateFlow<Boolean> = _isImportingSpotify.asStateFlow()

    fun clearSpotifyImportStatus() {
        _spotifyImportStatus.value = null
    }

    fun importSpotifyPlaylist(link: String) {
        if (_isImportingSpotify.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isImportingSpotify.value = true
            _spotifyImportStatus.value = null
            try {
                val playlistId = Regex("playlist/([A-Za-z0-9]+)").find(link)?.groupValues?.get(1)
                if (playlistId == null) {
                    _spotifyImportStatus.value = "لینک پلی‌لیست اسپاتیفای معتبر نیست"
                    return@launch
                }
                // The public embed page ships the playlist as JSON — no login or API key needed
                val url = "https://open.spotify.com/embed/playlist/$playlistId"
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
                    .build()
                val html = client.newCall(request).execute().use { it.body?.string() }
                if (html.isNullOrBlank()) {
                    _spotifyImportStatus.value = "دسترسی به اسپاتیفای ممکن نشد"
                    return@launch
                }

                var playlistName = "Spotify Playlist"
                val wanted = mutableListOf<Pair<String, String>>() // title to artist

                // Primary: parse the embedded __NEXT_DATA__ JSON
                val jsonStr = Regex(
                    "<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>",
                    RegexOption.DOT_MATCHES_ALL
                ).find(html)?.groupValues?.get(1)
                if (jsonStr != null) {
                    try {
                        val entity = org.json.JSONObject(jsonStr)
                            .optJSONObject("props")?.optJSONObject("pageProps")
                            ?.optJSONObject("state")?.optJSONObject("data")
                            ?.optJSONObject("entity")
                        if (entity != null) {
                            playlistName = entity.optString("name").ifBlank { playlistName }
                            val trackArr = entity.optJSONArray("trackList")
                            if (trackArr != null) {
                                for (i in 0 until trackArr.length()) {
                                    val t = trackArr.getJSONObject(i)
                                    val title = t.optString("title")
                                    val artist = t.optString("subtitle")
                                    if (title.isNotBlank()) wanted.add(title to artist)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                    // Fallback: generic scan of the JSON for title/subtitle pairs
                    if (wanted.isEmpty()) {
                        Regex("\"title\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"subtitle\"\\s*:\\s*\"(.*?)\"")
                            .findAll(jsonStr).forEach { m ->
                                val title = m.groupValues[1]
                                if (title.isNotBlank()) wanted.add(title to m.groupValues[2])
                            }
                    }
                }

                if (wanted.isEmpty()) {
                    _spotifyImportStatus.value = "آهنگی در این پلی‌لیست پیدا نشد (شاید پلی‌لیست خصوصی است)"
                    return@launch
                }

                // Match each Spotify track against the user's own server library
                val matched = mutableListOf<SubsonicSong>()
                for ((title, subtitle) in wanted) {
                    try {
                        val primaryArtist = subtitle.split(",").first().trim()
                        val candidates = repository.search(title).song ?: emptyList()
                        val hit = candidates.firstOrNull { s ->
                            s.artist != null && primaryArtist.isNotBlank() && (
                                s.artist.contains(primaryArtist, ignoreCase = true) ||
                                    primaryArtist.contains(s.artist, ignoreCase = true)
                                )
                        } ?: candidates.firstOrNull { it.title.equals(title, ignoreCase = true) }
                        if (hit != null && matched.none { it.id == hit.id }) matched.add(hit)
                    } catch (_: Exception) {}
                }

                if (matched.isEmpty()) {
                    _spotifyImportStatus.value = "هیچ‌کدام از ${wanted.size} آهنگ این پلی‌لیست در سرور شما پیدا نشد"
                } else {
                    val id = repository.createLocalPlaylist(playlistName, "ایمپورت از اسپاتیفای")
                    matched.forEach { repository.addTrackToPlaylist(id, it) }
                    _spotifyImportStatus.value = "✓ «$playlistName» ساخته شد — ${matched.size} از ${wanted.size} آهنگ در سرور پیدا شد"
                }
            } catch (e: Exception) {
                _spotifyImportStatus.value = "خطا در ایمپورت از اسپاتیفای"
            } finally {
                _isImportingSpotify.value = false
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    // --- MusicBrainz Artist Detail ---
    fun fetchArtistMbDetail(mbid: String) {
        viewModelScope.launch {
            try {
                _artistMbDetail.value = repository.getArtistDetailFromMusicBrainz(mbid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Internet Radio Stations ---
    private fun loadInternetRadioStations() {
        viewModelScope.launch {
            try {
                _internetRadioStations.value = repository.getInternetRadioStations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playSongDirectly(song: SubsonicSong) {
        playbackConnection.playTrack(song, listOf(song))
    }

    // --- Lyrics Translation ---
    fun translateLyrics(text: String) {
        viewModelScope.launch {
            try {
                val result = repository.translateLyrics(text)
                _translatedLyrics.value = result
                if (result == null) {
                    _transientMessage.value = "ترجمه در دسترس نیست — بعداً دوباره امتحان کنید"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _transientMessage.value = "خطا در ترجمه"
            }
        }
    }

    fun clearTranslatedLyrics() {
        _translatedLyrics.value = null
    }

    // --- Lyrics Loader ---
    private fun fetchLyricsForTrack(song: SubsonicSong) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            _currentLyrics.value = repository.getLyrics(song.artist ?: "", song.title, song.album, song.duration ?: 0)
            _isLoadingLyrics.value = false
        }
    }

    // --- Settings Preferences Persist ---
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setThemeAccent(accent: String) {
        _themeAccent.value = accent
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme_accent", accent).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", lang).apply()
    }

    fun setUserName(name: String) {
        _userName.value = name
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_name", name).apply()
    }

    fun setProfilePhotoUri(uri: String) {
        _profilePhotoUri.value = uri
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("profile_photo_uri", uri).apply()
    }

    fun setOnboardingPreferencesSet(done: Boolean) {
        _onboardingPreferencesSet.value = done
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_preferences_set", done).apply()
    }

    fun setPreferredGenres(genres: Set<String>) {
        _preferredGenres.value = genres
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("preferred_genres", genres).apply()
    }

    fun setDownloadLocation(path: String) {
        _downloadLocation.value = path
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("download_location", path).apply()
    }

    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
        repository.setStreamQuality(quality)
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("audio_quality", quality).apply()
    }

    fun setDownloadQuality(quality: String) {
        _downloadQuality.value = quality
        repository.setDownloadQuality(quality)
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("download_quality", quality).apply()
    }

    fun setCacheSizeMb(sizeMb: Int) {
        _cacheSizeMb.value = sizeMb
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("cache_size_mb", sizeMb).apply()
    }

    fun clearCache() {
        // Simulating cache deletion
        viewModelScope.launch {
            val downloadDir = File(getApplication<Application>().filesDir, "downloads")
            if (downloadDir.exists()) {
                downloadDir.listFiles()?.forEach { file ->
                    // Keep downloaded tracks but clear transient cache files if any
                    if (file.name.endsWith("_cache")) {
                        file.delete()
                    }
                }
            }
        }
    }

    fun getCoverArtUrl(coverArtId: String?): String {
        return repository.getCoverArtUrl(coverArtId)
    }

    // --- Equalizer Controls (applied live by MusicPlaybackService via SharedPreferences) ---
    fun setBassLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        _bassLevel.value = clamped
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("bass_level", clamped).apply()
    }

    fun setTrebleLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        _trebleLevel.value = clamped
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("treble_level", clamped).apply()
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("crossfade_enabled", enabled).apply()
    }

    fun setCrossfadeDuration(seconds: Int) {
        _crossfadeDurationSeconds.value = seconds
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("crossfade_duration_seconds", seconds).apply()
    }

    fun setWifiOnlyDownload(enabled: Boolean) {
        _wifiOnlyDownload.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("wifi_only_download", enabled).apply()
    }

    fun setFadeOnPause(enabled: Boolean) {
        _fadeOnPause.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("fade_on_pause", enabled).apply()
    }

    fun setResumeOnBluetooth(enabled: Boolean) {
        _resumeOnBluetooth.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("resume_on_bluetooth", enabled).apply()
    }

    fun setSaveDownloadsPublic(enabled: Boolean) {
        _saveDownloadsPublic.value = enabled
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("save_downloads_public", enabled).apply()
    }

    // --- Backup export/import via user-picked file (SAF) ---
    fun exportBackupToUri(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = repository.exportPlaylistsBackup()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                _transientMessage.value = "بکاپ در فایل ذخیره شد"
            } catch (e: Exception) {
                _transientMessage.value = "خطا در ذخیره فایل بکاپ"
            }
        }
    }

    fun importBackupFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                if (json.isNullOrBlank()) {
                    _transientMessage.value = "فایل بکاپ خالی است"
                    return@launch
                }
                val ok = repository.importPlaylistsBackup(json)
                _transientMessage.value = if (ok) "بازیابی از فایل انجام شد" else "فایل بکاپ نامعتبر است"
            } catch (e: Exception) {
                _transientMessage.value = "خطا در بازیابی از فایل"
            }
        }
    }

    // --- Multi-server management ---
    fun switchServer(url: String) {
        viewModelScope.launch {
            val success = repository.switchServer(url)
            if (success) loadHomeContent()
        }
    }

    fun removeServer(url: String) {
        viewModelScope.launch {
            repository.removeServer(url)
        }
    }

    fun addAnotherServer(url: String, username: String, passwordOrToken: String, isTokenMode: Boolean = false) {
        login(url, username, passwordOrToken, isTokenMode)
    }

    // --- Search filter & trending ---
    fun setSearchFilter(filter: String) {
        _searchFilter.value = filter
    }

    fun loadTrendingContent() {
        viewModelScope.launch {
            try {
                _trendingAlbums.value = repository.getAlbumList("frequent")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Backup & Restore ---
    fun exportPlaylistsBackup() {
        viewModelScope.launch {
            try {
                val json = repository.exportPlaylistsBackup()
                val backupDir = File(getApplication<Application>().filesDir, "backups")
                if (!backupDir.exists()) backupDir.mkdirs()
                val file = File(backupDir, "smusic_playlists_backup.json")
                file.writeText(json)
                _transientMessage.value = "پشتیبان‌گیری با موفقیت ذخیره شد"
            } catch (e: Exception) {
                _transientMessage.value = "خطا در پشتیبان‌گیری"
            }
        }
    }

    fun restorePlaylistsBackup() {
        viewModelScope.launch {
            try {
                val file = File(File(getApplication<Application>().filesDir, "backups"), "smusic_playlists_backup.json")
                if (!file.exists()) {
                    _transientMessage.value = "فایل پشتیبان یافت نشد"
                    return@launch
                }
                val json = file.readText()
                val success = repository.importPlaylistsBackup(json)
                _transientMessage.value = if (success) "پلی‌لیست‌ها با موفقیت بازیابی شدند" else "بازیابی ناموفق بود"
            } catch (e: Exception) {
                _transientMessage.value = "خطا در بازیابی"
            }
        }
    }

    fun consumeTransientMessage() {
        _transientMessage.value = null
    }

    private val _discoverMusicVideos = MutableStateFlow<List<MusicVideoItem>>(emptyList())
    val discoverMusicVideos: StateFlow<List<MusicVideoItem>> = _discoverMusicVideos.asStateFlow()

    private val _artistMusicVideos = MutableStateFlow<List<MusicVideoItem>>(emptyList())
    val artistMusicVideos: StateFlow<List<MusicVideoItem>> = _artistMusicVideos.asStateFlow()

    fun fetchArtistMusicVideos(artistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _artistMusicVideos.value = emptyList()
            try {
                val encoded = java.net.URLEncoder.encode(artistName, "UTF-8")
                val url = "https://itunes.apple.com/search?term=$encoded&media=musicVideo&limit=20&country=us"
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(url).build()
                val body = client.newCall(request).execute().use { it.body?.string() } ?: return@launch
                val arr = org.json.JSONObject(body).getJSONArray("results")
                val items = mutableListOf<MusicVideoItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val preview = obj.optString("previewUrl", "")
                    if (preview.isBlank()) continue
                    val thumb = obj.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                    items.add(MusicVideoItem(
                        id = obj.optLong("trackId", i.toLong()),
                        title = obj.optString("trackName", ""),
                        artistName = obj.optString("artistName", ""),
                        thumbnailUrl = thumb,
                        previewUrl = preview
                    ))
                }
                _artistMusicVideos.value = items
            } catch (_: Exception) {}
        }
    }

    private var _discoverVideoOffset = 0
    private var _discoverVideoSeedIndex = 0
    // iTunes caps each search term at ~200 results, so the feed rotates
    // across genre seeds to keep serving fresh videos indefinitely
    private val discoverVideoSeeds = listOf(
        "music video", "pop music video", "rock music video", "hip hop music video",
        "dance music video", "r&b music video", "electronic music video", "latin music video",
        "k-pop music video", "country music video", "indie music video", "persian music video"
    )
    private val _isLoadingMoreVideos = MutableStateFlow(false)
    val isLoadingMoreVideos: StateFlow<Boolean> = _isLoadingMoreVideos.asStateFlow()

    fun fetchMusicVideos() {
        _discoverVideoSeedIndex = 0
        _discoverVideoOffset = 0
        _discoverMusicVideos.value = emptyList()
        loadCatalogVideos()
        loadMusicVideosPage()
    }

    // Videos the admin posts to the Telegram channel (synced into the public
    // catalog). They are pinned to the TOP of the Discover music-video feed.
    private fun loadCatalogVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = catalogUrl() + "?t=" + System.currentTimeMillis()
                val body = okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(url).build())
                    .execute().use { it.body?.string() } ?: "{}"
                val arr = org.json.JSONObject(body).optJSONArray("videos") ?: org.json.JSONArray()
                val items = mutableListOf<MusicVideoItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val videoUrl = o.optString("videoUrl")
                    if (videoUrl.isBlank()) continue
                    items.add(
                        MusicVideoItem(
                            id = videoUrl.hashCode().toLong(),
                            title = o.optString("title"),
                            artistName = o.optString("artist"),
                            thumbnailUrl = o.optString("coverUrl"),
                            previewUrl = videoUrl
                        )
                    )
                }
                if (items.isNotEmpty()) {
                    val unique = items.distinctBy { it.id }
                    val existing = _discoverMusicVideos.value.filter { cur -> unique.none { it.id == cur.id } }
                    _discoverMusicVideos.value = unique + existing
                }
            } catch (_: Exception) {
            }
        }
    }

    fun fetchMoreMusicVideos() {
        if (_isLoadingMoreVideos.value) return
        loadMusicVideosPage()
    }

    private fun loadMusicVideosPage() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMoreVideos.value = true
            try {
                val client = okhttp3.OkHttpClient()
                var attempts = 0
                while (attempts < 3 && _discoverVideoSeedIndex < discoverVideoSeeds.size) {
                    val seed = discoverVideoSeeds[_discoverVideoSeedIndex]
                    val encoded = java.net.URLEncoder.encode(seed, "UTF-8")
                    val url = "https://itunes.apple.com/search?term=$encoded&media=musicVideo&limit=50&offset=$_discoverVideoOffset&country=us"
                    val request = okhttp3.Request.Builder().url(url).build()
                    val body = client.newCall(request).execute().use { it.body?.string() } ?: break
                    val arr = org.json.JSONObject(body).getJSONArray("results")
                    val existingIds = _discoverMusicVideos.value.map { it.id }.toSet()
                    val items = mutableListOf<MusicVideoItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val preview = obj.optString("previewUrl", "")
                        if (preview.isBlank()) continue
                        val id = obj.optLong("trackId", -1L)
                        if (id in existingIds) continue
                        val thumb = obj.optString("artworkUrl100", "")
                            .replace("100x100bb", "600x600bb")
                        items.add(
                            MusicVideoItem(
                                id = id,
                                title = obj.optString("trackName", ""),
                                artistName = obj.optString("artistName", ""),
                                thumbnailUrl = thumb,
                                previewUrl = preview
                            )
                        )
                    }
                    if (items.isNotEmpty()) {
                        _discoverVideoOffset += 50
                        _discoverMusicVideos.value = _discoverMusicVideos.value + items.distinctBy { it.id }.shuffled()
                        break
                    }
                    // Seed exhausted (or only duplicates) — advance to the next query
                    _discoverVideoSeedIndex++
                    _discoverVideoOffset = 0
                    attempts++
                }
            } catch (_: Exception) {
            } finally {
                _isLoadingMoreVideos.value = false
            }
        }
    }

    // Server-library match for the music video currently on screen
    private val _discoverVideoServerSong = MutableStateFlow<SubsonicSong?>(null)
    val discoverVideoServerSong: StateFlow<SubsonicSong?> = _discoverVideoServerSong.asStateFlow()
    private var videoMatchJob: kotlinx.coroutines.Job? = null

    fun matchVideoToServerSong(video: MusicVideoItem) {
        videoMatchJob?.cancel()
        _discoverVideoServerSong.value = null
        videoMatchJob = viewModelScope.launch {
            try {
                val cleanTitle = video.title.substringBefore("(").substringBefore("[").trim()
                if (cleanTitle.isBlank()) return@launch
                val songs = repository.search(cleanTitle).song ?: return@launch
                _discoverVideoServerSong.value = songs.firstOrNull { s ->
                    s.artist != null && (
                        s.artist.contains(video.artistName, ignoreCase = true) ||
                            video.artistName.contains(s.artist, ignoreCase = true)
                        )
                }
            } catch (_: Exception) {}
        }
    }

    // --- Story share card: cover + song name + app name (no URL) rendered to an image ---
    fun shareSongCard(song: SubsonicSong) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val coverUrl = repository.getCoverArtUrl(song.coverArt)
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .size(800)
                    .build()
                val result = context.imageLoader.execute(request)
                val cover = (result.drawable as? BitmapDrawable)?.bitmap

                val w = 1080
                val h = 1920
                val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                val filterPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

                // Background: center-cropped cover dimmed way down, else near-black
                if (cover != null) {
                    val scale = maxOf(w.toFloat() / cover.width, h.toFloat() / cover.height)
                    val m = android.graphics.Matrix()
                    m.setScale(scale, scale)
                    m.postTranslate((w - cover.width * scale) / 2f, (h - cover.height * scale) / 2f)
                    canvas.drawBitmap(cover, m, filterPaint)
                    canvas.drawColor(android.graphics.Color.argb(200, 8, 8, 12))
                } else {
                    canvas.drawColor(android.graphics.Color.rgb(16, 16, 20))
                }
                val gradientPaint = android.graphics.Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, h * 0.55f, 0f, h.toFloat(),
                        android.graphics.Color.argb(0, 0, 0, 0),
                        android.graphics.Color.argb(150, 0, 0, 0),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradientPaint)

                // Big rounded cover in the center
                if (cover != null) {
                    val size = 760f
                    val left = (w - size) / 2f
                    val top = 420f
                    val rect = android.graphics.RectF(left, top, left + size, top + size)
                    val clip = android.graphics.Path().apply {
                        addRoundRect(rect, 48f, 48f, android.graphics.Path.Direction.CW)
                    }
                    canvas.save()
                    canvas.clipPath(clip)
                    val cs = maxOf(size / cover.width, size / cover.height)
                    val cm = android.graphics.Matrix()
                    cm.setScale(cs, cs)
                    cm.postTranslate(left + (size - cover.width * cs) / 2f, top + (size - cover.height * cs) / 2f)
                    canvas.drawBitmap(cover, cm, filterPaint)
                    canvas.restore()
                    val border = android.graphics.Paint().apply {
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 3f
                        color = android.graphics.Color.argb(70, 255, 255, 255)
                        isAntiAlias = true
                    }
                    canvas.drawRoundRect(rect, 48f, 48f, border)
                }

                // Song title (up to two centered lines)
                val titlePaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 64f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                val titleLayout = android.text.StaticLayout.Builder
                    .obtain(song.title, 0, song.title.length, titlePaint, w - 160)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                    .setMaxLines(2)
                    .setEllipsize(android.text.TextUtils.TruncateAt.END)
                    .build()
                canvas.save()
                canvas.translate(80f, 1300f)
                titleLayout.draw(canvas)
                canvas.restore()

                // Artist
                val artistPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.argb(200, 255, 255, 255)
                    textSize = 44f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.drawText(
                    song.artist ?: "",
                    w / 2f,
                    1300f + titleLayout.height + 76f,
                    artistPaint
                )

                // App name only — deliberately no web address
                val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
                val appPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.rgb(250, 45, 72)
                    textSize = 42f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                canvas.drawText("♪ $appName", w / 2f, h - 110f, appPaint)

                saveAndShareCardBitmap(bmp)
            } catch (e: Exception) {
                _transientMessage.value = "خطا در ساخت کارت اشتراک‌گذاری"
            }
        }
    }

    private fun saveAndShareCardBitmap(bmp: android.graphics.Bitmap) {
        val context = getApplication<Application>()
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "share_card.png")
        java.io.FileOutputStream(file).use { out ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("share_card", uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(send, "اشتراک‌گذاری").apply {
            addFlags(
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        context.startActivity(chooser)
    }

    // Lyric card: one chosen lyric line as a big story image (cover, line, song, app name — no URL)
    fun shareLyricCard(song: SubsonicSong, line: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val coverUrl = repository.getCoverArtUrl(song.coverArt)
                val request = ImageRequest.Builder(context)
                    .data(coverUrl).allowHardware(false).size(600).build()
                val cover = (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap

                val w = 1080
                val h = 1920
                val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                val filterPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

                if (cover != null) {
                    val scale = maxOf(w.toFloat() / cover.width, h.toFloat() / cover.height)
                    val m = android.graphics.Matrix()
                    m.setScale(scale, scale)
                    m.postTranslate((w - cover.width * scale) / 2f, (h - cover.height * scale) / 2f)
                    canvas.drawBitmap(cover, m, filterPaint)
                    canvas.drawColor(android.graphics.Color.argb(215, 8, 8, 12))
                } else {
                    canvas.drawColor(android.graphics.Color.rgb(16, 16, 20))
                }

                // Small rounded cover up top
                if (cover != null) {
                    val size = 280f
                    val left = (w - size) / 2f
                    val top = 260f
                    val rect = android.graphics.RectF(left, top, left + size, top + size)
                    val clip = android.graphics.Path().apply {
                        addRoundRect(rect, 32f, 32f, android.graphics.Path.Direction.CW)
                    }
                    canvas.save()
                    canvas.clipPath(clip)
                    val cs = maxOf(size / cover.width, size / cover.height)
                    val cm = android.graphics.Matrix()
                    cm.setScale(cs, cs)
                    cm.postTranslate(left + (size - cover.width * cs) / 2f, top + (size - cover.height * cs) / 2f)
                    canvas.drawBitmap(cover, cm, filterPaint)
                    canvas.restore()
                }

                // The lyric line, big and centered (up to 6 wrapped lines)
                val quoted = "“$line”"
                val lyricPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 74f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                val lyricLayout = android.text.StaticLayout.Builder
                    .obtain(quoted, 0, quoted.length, lyricPaint, w - 180)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.25f)
                    .setMaxLines(6)
                    .setEllipsize(android.text.TextUtils.TruncateAt.END)
                    .build()
                canvas.save()
                canvas.translate(90f, 700f + ((760 - lyricLayout.height) / 2f).coerceAtLeast(0f))
                lyricLayout.draw(canvas)
                canvas.restore()

                // Song title · artist
                val metaPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.argb(200, 255, 255, 255)
                    textSize = 42f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val metaText = listOfNotNull(song.title.takeIf { it.isNotBlank() }, song.artist).joinToString(" · ")
                canvas.drawText(metaText, w / 2f, 1640f, metaPaint)

                // App name only — no web address
                val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
                val appPaint = android.text.TextPaint().apply {
                    color = android.graphics.Color.rgb(250, 45, 72)
                    textSize = 42f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                canvas.drawText("♪ $appName", w / 2f, h - 110f, appPaint)

                saveAndShareCardBitmap(bmp)
            } catch (e: Exception) {
                _transientMessage.value = "خطا در ساخت کارت متن"
            }
        }
    }

    // --- Songs stored on the device (MediaStore) ---
    private val _deviceSongs = MutableStateFlow<List<SubsonicSong>>(emptyList())
    val deviceSongs: StateFlow<List<SubsonicSong>> = _deviceSongs.asStateFlow()

    // MediaStore DATE_ADDED (epoch seconds) per song id, used by the sort menu
    private val _deviceSongDates = MutableStateFlow<Map<String, Long>>(emptyMap())
    val deviceSongDates: StateFlow<Map<String, Long>> = _deviceSongDates.asStateFlow()

    // Parent folder name per song id (Telegram, Download, Music, ...)
    private val _deviceSongFolders = MutableStateFlow<Map<String, String>>(emptyMap())
    val deviceSongFolders: StateFlow<Map<String, String>> = _deviceSongFolders.asStateFlow()

    fun loadDeviceSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                val media = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.TITLE,
                    android.provider.MediaStore.Audio.Media.ARTIST,
                    android.provider.MediaStore.Audio.Media.ALBUM,
                    android.provider.MediaStore.Audio.Media.DURATION,
                    android.provider.MediaStore.Audio.Media.SIZE,
                    android.provider.MediaStore.Audio.Media.DATE_ADDED,
                    android.provider.MediaStore.Audio.Media.DATA
                )
                val songs = mutableListOf<SubsonicSong>()
                val dates = mutableMapOf<String, Long>()
                val folderNames = mutableMapOf<String, String>()
                resolver.query(
                    media,
                    projection,
                    android.provider.MediaStore.Audio.Media.IS_MUSIC + " != 0",
                    null,
                    android.provider.MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATE_ADDED)
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val songId = "local_$id"
                        val contentUri = android.content.ContentUris.withAppendedId(media, id)
                        val rawArtist = cursor.getString(artistCol)
                        songs.add(
                            SubsonicSong(
                                id = songId,
                                title = cursor.getString(titleCol) ?: "",
                                artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") null else rawArtist,
                                album = cursor.getString(albumCol),
                                duration = (cursor.getLong(durationCol) / 1000L).toInt(),
                                size = cursor.getLong(sizeCol),
                                path = contentUri.toString()
                            )
                        )
                        dates[songId] = cursor.getLong(dateCol)
                        val filePath = cursor.getString(dataCol)
                        folderNames[songId] = filePath?.let { File(it).parentFile?.name }?.takeIf { it.isNotBlank() } ?: "سایر"
                    }
                }
                _deviceSongs.value = songs
                _deviceSongDates.value = dates
                _deviceSongFolders.value = folderNames
            } catch (_: Exception) {}
        }
    }

    // --- Apple Music free integrations (iTunes Search API + RSS, no Developer Token).
    // Used ONLY for cover artwork and the Discover top-songs preview tab; all real
    // song playback comes from the user's own server.
    private val _appleTopSongs = MutableStateFlow<List<ApplePreviewTrack>>(emptyList())
    val appleTopSongs: StateFlow<List<ApplePreviewTrack>> = _appleTopSongs.asStateFlow()

    private fun parseApplePreviewTracks(body: String): List<ApplePreviewTrack> {
        val arr = org.json.JSONObject(body).getJSONArray("results")
        val items = mutableListOf<ApplePreviewTrack>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val preview = obj.optString("previewUrl", "")
            if (preview.isBlank()) continue
            items.add(
                ApplePreviewTrack(
                    id = obj.optLong("trackId", i.toLong()),
                    title = obj.optString("trackName", ""),
                    artistName = obj.optString("artistName", ""),
                    artworkUrl = obj.optString("artworkUrl100", "").replace("100x100bb", "600x600bb"),
                    previewUrl = preview
                )
            )
        }
        return items
    }

    fun fetchAppleTopSongs(country: String = "us") {
        // Refetch when the list is empty OR previews never resolved (e.g. lookup failed)
        if (_appleTopSongs.value.any { it.previewUrl.isNotBlank() }) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                // Free Apple marketing RSS feed: most-played songs of the day.
                // Title, artist, and artwork all come straight from the feed so the
                // covers always show; the lookup call only resolves preview URLs.
                val rssUrl = "https://rss.applemarketingtools.com/api/v2/$country/music/most-played/50/songs.json"
                val rssBody = client.newCall(okhttp3.Request.Builder().url(rssUrl).build())
                    .execute().use { it.body?.string() } ?: return@launch
                val rssResults = org.json.JSONObject(rssBody).getJSONObject("feed").getJSONArray("results")
                val rssTracks = mutableListOf<ApplePreviewTrack>()
                for (i in 0 until rssResults.length()) {
                    val obj = rssResults.getJSONObject(i)
                    val id = obj.optString("id").toLongOrNull() ?: continue
                    rssTracks.add(
                        ApplePreviewTrack(
                            id = id,
                            title = obj.optString("name", ""),
                            artistName = obj.optString("artistName", ""),
                            artworkUrl = obj.optString("artworkUrl100", "").replace("100x100bb", "600x600bb"),
                            previewUrl = "" // filled from the lookup below
                        )
                    )
                }
                if (rssTracks.isEmpty()) return@launch
                // Show the chart immediately with covers; previews get merged in after
                _appleTopSongs.value = rssTracks

                val lookupUrl = "https://itunes.apple.com/lookup?id=${rssTracks.joinToString(",") { it.id.toString() }}&country=$country"
                val lookupBody = client.newCall(okhttp3.Request.Builder().url(lookupUrl).build())
                    .execute().use { it.body?.string() } ?: return@launch
                val lookupById = parseApplePreviewTracks(lookupBody).associateBy { it.id }
                _appleTopSongs.value = rssTracks.map { rss ->
                    val looked = lookupById[rss.id]
                    rss.copy(
                        previewUrl = looked?.previewUrl ?: "",
                        artworkUrl = rss.artworkUrl.ifBlank { looked?.artworkUrl.orEmpty() }
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // Official Apple cover art for tracks whose server entry has no artwork.
    // Keyed by "artist|title"; each pair is looked up at most once per process.
    private val _appleArtworkCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val appleArtworkCache: StateFlow<Map<String, String>> = _appleArtworkCache.asStateFlow()
    private val appleArtworkRequested = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun appleArtworkKey(artist: String?, title: String) = "${artist.orEmpty()}|$title".lowercase()

    fun requestAppleArtwork(artist: String?, title: String) {
        if (title.isBlank()) return
        val key = appleArtworkKey(artist, title)
        if (!appleArtworkRequested.add(key)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val term = java.net.URLEncoder.encode(listOfNotNull(artist, title).joinToString(" "), "UTF-8")
                val url = "https://itunes.apple.com/search?term=$term&media=music&entity=song&limit=1&country=us"
                val client = okhttp3.OkHttpClient()
                val body = client.newCall(okhttp3.Request.Builder().url(url).build())
                    .execute().use { it.body?.string() } ?: return@launch
                val arr = org.json.JSONObject(body).getJSONArray("results")
                if (arr.length() == 0) return@launch
                val art = arr.getJSONObject(0).optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                if (art.isNotBlank()) {
                    _appleArtworkCache.value = _appleArtworkCache.value + (key to art)
                }
            } catch (_: Exception) {}
        }
    }

    // --- Music quiz game (guess the song from your own library) ---
    private val _quizHighScore = MutableStateFlow(0)
    val quizHighScore: StateFlow<Int> = _quizHighScore.asStateFlow()

    fun loadQuizHighScore() {
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        _quizHighScore.value = prefs.getInt("quiz_high_score", 0)
    }

    fun updateQuizHighScore(score: Int) {
        if (score > _quizHighScore.value) {
            _quizHighScore.value = score
            getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
                .edit().putInt("quiz_high_score", score).apply()
        }
    }

    suspend fun getStreamUrlFor(song: SubsonicSong): String = repository.getStreamUrl(song)

    // --- Artist biography from Persian (fallback English) Wikipedia — free, no key ---
    private val _artistWikiBio = MutableStateFlow<String?>(null)
    val artistWikiBio: StateFlow<String?> = _artistWikiBio.asStateFlow()

    fun fetchArtistWikiBio(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _artistWikiBio.value = null
            if (name.isBlank()) return@launch
            try {
                val client = okhttp3.OkHttpClient()
                fun fetchLang(lang: String): String? = try {
                    val encoded = java.net.URLEncoder.encode(name.trim().replace(" ", "_"), "UTF-8")
                    val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encoded"
                    val body = client.newCall(
                        okhttp3.Request.Builder().url(url)
                            .header("User-Agent", "SMusicApp/1.0 (Android)")
                            .build()
                    ).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
                    body?.let {
                        val obj = org.json.JSONObject(it)
                        obj.optString("extract").takeIf { e ->
                            e.isNotBlank() && !obj.optString("type").contains("disambiguation")
                        }
                    }
                } catch (_: Exception) { null }
                _artistWikiBio.value = fetchLang("fa") ?: fetchLang("en")
            } catch (_: Exception) {}
        }
    }

    // --- Top charts source selection: Apple RSS per-country + Deezer genre charts (no key) ---
    private val _topChartsKey = MutableStateFlow("us")
    val topChartsKey: StateFlow<String> = _topChartsKey.asStateFlow()

    fun fetchTopCharts(key: String) {
        if (key == _topChartsKey.value && _appleTopSongs.value.any { it.previewUrl.isNotBlank() }) return
        _topChartsKey.value = key
        _appleTopSongs.value = emptyList()
        if (key.startsWith("deezer_")) {
            fetchDeezerChart(key.removePrefix("deezer_"))
        } else {
            fetchAppleTopSongs(key)
        }
    }

    private fun fetchDeezerChart(genreId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.deezer.com/chart/$genreId/tracks?limit=50"
                val body = okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(url).build())
                    .execute().use { it.body?.string() } ?: return@launch
                val arr = org.json.JSONObject(body).getJSONArray("data")
                val items = mutableListOf<ApplePreviewTrack>()
                for (i in 0 until arr.length()) {
                    val t = arr.getJSONObject(i)
                    val preview = t.optString("preview", "")
                    if (preview.isBlank()) continue
                    val album = t.optJSONObject("album")
                    items.add(
                        ApplePreviewTrack(
                            id = t.optLong("id", i.toLong()),
                            title = t.optString("title", ""),
                            artistName = t.optJSONObject("artist")?.optString("name").orEmpty(),
                            artworkUrl = album?.optString("cover_xl").takeUnless { it.isNullOrBlank() }
                                ?: album?.optString("cover_big").orEmpty(),
                            previewUrl = preview
                        )
                    )
                }
                _appleTopSongs.value = items
            } catch (_: Exception) {}
        }
    }

    // --- Podcasts (free iTunes Search API + public RSS feeds; full episodes, no key) ---
    private val _podcastSearchResults = MutableStateFlow<List<PodcastItem>>(emptyList())
    val podcastSearchResults: StateFlow<List<PodcastItem>> = _podcastSearchResults.asStateFlow()

    private val _topPodcasts = MutableStateFlow<List<PodcastItem>>(emptyList())
    val topPodcasts: StateFlow<List<PodcastItem>> = _topPodcasts.asStateFlow()

    private val _podcastEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val podcastEpisodes: StateFlow<List<PodcastEpisode>> = _podcastEpisodes.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(false)
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes.asStateFlow()

    private val _followedPodcasts = MutableStateFlow<List<PodcastItem>>(emptyList())
    val followedPodcasts: StateFlow<List<PodcastItem>> = _followedPodcasts.asStateFlow()

    private fun parsePodcastResults(body: String): List<PodcastItem> {
        val arr = org.json.JSONObject(body).getJSONArray("results")
        val items = mutableListOf<PodcastItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val feed = obj.optString("feedUrl", "")
            if (feed.isBlank()) continue
            items.add(
                PodcastItem(
                    id = obj.optLong("collectionId", i.toLong()),
                    name = obj.optString("collectionName", ""),
                    artistName = obj.optString("artistName", ""),
                    artworkUrl = obj.optString("artworkUrl600", "").ifBlank {
                        obj.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                    },
                    feedUrl = feed
                )
            )
        }
        return items
    }

    fun searchPodcasts(query: String) {
        if (query.isBlank()) {
            _podcastSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://itunes.apple.com/search?term=$encoded&media=podcast&limit=20"
                val body = okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(url).build())
                    .execute().use { it.body?.string() } ?: return@launch
                _podcastSearchResults.value = parsePodcastResults(body)
            } catch (_: Exception) {}
        }
    }

    fun fetchTopPodcasts(country: String = "us") {
        if (_topPodcasts.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val rssUrl = "https://rss.applemarketingtools.com/api/v2/$country/podcasts/top/25/podcasts.json"
                val rssBody = client.newCall(okhttp3.Request.Builder().url(rssUrl).build())
                    .execute().use { it.body?.string() } ?: return@launch
                val results = org.json.JSONObject(rssBody).getJSONObject("feed").getJSONArray("results")
                val ids = mutableListOf<String>()
                for (i in 0 until results.length()) {
                    results.getJSONObject(i).optString("id").takeIf { it.isNotBlank() }?.let { ids.add(it) }
                }
                if (ids.isEmpty()) return@launch
                // Lookup resolves feed URLs for the charted podcasts
                val lookupUrl = "https://itunes.apple.com/lookup?id=${ids.joinToString(",")}"
                val lookupBody = client.newCall(okhttp3.Request.Builder().url(lookupUrl).build())
                    .execute().use { it.body?.string() } ?: return@launch
                _topPodcasts.value = parsePodcastResults(lookupBody)
            } catch (_: Exception) {}
        }
    }

    fun loadPodcastEpisodes(podcast: PodcastItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingEpisodes.value = true
            _podcastEpisodes.value = emptyList()
            try {
                val body = okhttp3.OkHttpClient()
                    .newCall(okhttp3.Request.Builder().url(podcast.feedUrl).build())
                    .execute().use { it.body?.string() } ?: return@launch
                val episodes = mutableListOf<PodcastEpisode>()
                val parser = android.util.Xml.newPullParser()
                parser.setFeature(org.xmlpull.v1.XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(java.io.StringReader(body))
                var inItem = false
                var title = ""
                var audioUrl = ""
                var duration = 0
                var pubDate = ""
                var image = ""
                var event = parser.eventType
                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && episodes.size < 50) {
                    when (event) {
                        org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                            "item" -> { inItem = true; title = ""; audioUrl = ""; duration = 0; pubDate = ""; image = "" }
                            "title" -> if (inItem) title = parser.nextText().trim()
                            "enclosure" -> if (inItem) audioUrl = parser.getAttributeValue(null, "url") ?: ""
                            "itunes:duration" -> if (inItem) {
                                val raw = parser.nextText().trim()
                                duration = raw.toIntOrNull() ?: raw.split(":")
                                    .mapNotNull { it.toIntOrNull() }
                                    .fold(0) { acc, part -> acc * 60 + part }
                            }
                            "pubDate" -> if (inItem) pubDate = parser.nextText().trim().take(16)
                            "itunes:image" -> if (inItem) image = parser.getAttributeValue(null, "href") ?: ""
                        }
                        org.xmlpull.v1.XmlPullParser.END_TAG -> if (parser.name == "item") {
                            inItem = false
                            if (title.isNotBlank() && audioUrl.isNotBlank()) {
                                episodes.add(PodcastEpisode(title, audioUrl, duration, pubDate, image))
                            }
                        }
                    }
                    event = parser.next()
                }
                _podcastEpisodes.value = episodes
            } catch (_: Exception) {
            } finally {
                _isLoadingEpisodes.value = false
            }
        }
    }

    fun playPodcastEpisode(episode: PodcastEpisode, podcast: PodcastItem) {
        val song = SubsonicSong(
            id = "podcast_${episode.audioUrl.hashCode()}",
            title = episode.title,
            artist = podcast.name,
            album = podcast.artistName,
            coverArt = episode.imageUrl.ifBlank { podcast.artworkUrl },
            duration = episode.durationSec,
            path = episode.audioUrl
        )
        playSongDirectly(song, listOf(song))
    }

    fun toggleFollowPodcast(podcast: PodcastItem) {
        val current = _followedPodcasts.value
        _followedPodcasts.value = if (current.any { it.id == podcast.id }) {
            current.filterNot { it.id == podcast.id }
        } else {
            current + podcast
        }
        persistFollowedPodcasts()
    }

    private fun persistFollowedPodcasts() {
        try {
            val arr = org.json.JSONArray()
            _followedPodcasts.value.forEach { p ->
                arr.put(
                    org.json.JSONObject()
                        .put("id", p.id).put("name", p.name).put("artistName", p.artistName)
                        .put("artworkUrl", p.artworkUrl).put("feedUrl", p.feedUrl)
                )
            }
            getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
                .edit().putString("followed_podcasts", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun loadFollowedPodcasts() {
        try {
            val json = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
                .getString("followed_podcasts", null) ?: return
            val arr = org.json.JSONArray(json)
            val items = mutableListOf<PodcastItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(
                    PodcastItem(
                        id = o.optLong("id"),
                        name = o.optString("name"),
                        artistName = o.optString("artistName"),
                        artworkUrl = o.optString("artworkUrl"),
                        feedUrl = o.optString("feedUrl")
                    )
                )
            }
            _followedPodcasts.value = items
        } catch (_: Exception) {}
    }

    // --- Telegram as free unlimited cloud storage (user's own bot, no key) ---
    private val _telegramConnected = MutableStateFlow(false)
    val telegramConnected: StateFlow<Boolean> = _telegramConnected.asStateFlow()

    private val _telegramSongs = MutableStateFlow<List<TelegramSong>>(emptyList())
    val telegramSongs: StateFlow<List<TelegramSong>> = _telegramSongs.asStateFlow()

    private val _telegramStatus = MutableStateFlow<String?>(null)
    val telegramStatus: StateFlow<String?> = _telegramStatus.asStateFlow()

    private val _isUploadingTelegram = MutableStateFlow(false)
    val isUploadingTelegram: StateFlow<Boolean> = _isUploadingTelegram.asStateFlow()

    private fun tgPrefs() =
        getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)

    private fun tgToken(): String? = tgPrefs().getString("telegram_bot_token", null)
    private fun tgChatId(): String? = tgPrefs().getString("telegram_chat_id", null)

    fun loadTelegramState() {
        _telegramConnected.value = !tgToken().isNullOrBlank() && !tgChatId().isNullOrBlank()
        loadTelegramSongs()
    }

    fun clearTelegramStatus() { _telegramStatus.value = null }

    // Validate the bot token and auto-discover the chat id from recent updates.
    // The user must first press Start / send any message to their bot.
    fun connectTelegram(botToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _telegramStatus.value = "در حال اتصال..."
            try {
                val token = botToken.trim()
                val client = okhttp3.OkHttpClient()
                val me = client.newCall(
                    okhttp3.Request.Builder().url("https://api.telegram.org/bot$token/getMe").build()
                ).execute().use { it.body?.string() } ?: ""
                if (!org.json.JSONObject(me).optBoolean("ok")) {
                    _telegramStatus.value = "توکن بات نامعتبر است"
                    return@launch
                }
                val updates = client.newCall(
                    okhttp3.Request.Builder().url("https://api.telegram.org/bot$token/getUpdates").build()
                ).execute().use { it.body?.string() } ?: ""
                val arr = org.json.JSONObject(updates).optJSONArray("result")
                var chatId: String? = null
                if (arr != null) {
                    for (i in arr.length() - 1 downTo 0) {
                        val msg = arr.getJSONObject(i).optJSONObject("message")
                            ?: arr.getJSONObject(i).optJSONObject("channel_post")
                        val id = msg?.optJSONObject("chat")?.optLong("id")
                        if (id != null && id != 0L) { chatId = id.toString(); break }
                    }
                }
                if (chatId == null) {
                    _telegramStatus.value = "ابتدا در تلگرام به بات خود پیام /start بدهید، سپس دوباره وصل شوید"
                    return@launch
                }
                tgPrefs().edit()
                    .putString("telegram_bot_token", token)
                    .putString("telegram_chat_id", chatId)
                    .apply()
                _telegramConnected.value = true
                _telegramStatus.value = "✓ متصل شد"
            } catch (e: Exception) {
                _telegramStatus.value = "خطا در اتصال به تلگرام"
            }
        }
    }

    fun disconnectTelegram() {
        tgPrefs().edit()
            .remove("telegram_bot_token")
            .remove("telegram_chat_id")
            .apply()
        _telegramConnected.value = false
    }

    fun uploadTelegramSong(uri: android.net.Uri, title: String, artist: String, coverUrl: String) {
        if (_isUploadingTelegram.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isUploadingTelegram.value = true
            _telegramStatus.value = "در حال آپلود..."
            try {
                val token = tgToken(); val chatId = tgChatId()
                if (token == null || chatId == null) {
                    _telegramStatus.value = "ابتدا تلگرام را متصل کنید"
                    return@launch
                }
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() }
                if (bytes == null) {
                    _telegramStatus.value = "فایل خوانده نشد"
                    return@launch
                }
                // Bot API caps downloads (getFile) at 20MB, so warn but still upload
                val fileName = (title.ifBlank { "audio" }) + ".mp3"
                val body = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("title", title)
                    .addFormDataPart("performer", artist)
                    .addFormDataPart(
                        "audio", fileName,
                        okhttp3.RequestBody.create("audio/mpeg".toMediaTypeOrNull(), bytes)
                    )
                    .build()
                val resp = okhttp3.OkHttpClient.Builder()
                    .callTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                    .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
                    .build()
                    .newCall(
                        okhttp3.Request.Builder()
                            .url("https://api.telegram.org/bot$token/sendAudio")
                            .post(body).build()
                    ).execute().use { it.body?.string() } ?: ""
                val json = org.json.JSONObject(resp)
                if (!json.optBoolean("ok")) {
                    _telegramStatus.value = "آپلود ناموفق: " + json.optString("description")
                    return@launch
                }
                val audio = json.getJSONObject("result").optJSONObject("audio")
                    ?: json.getJSONObject("result").optJSONObject("document")
                if (audio == null) {
                    _telegramStatus.value = "پاسخ نامعتبر تلگرام"
                    return@launch
                }
                val song = TelegramSong(
                    fileId = audio.getString("file_id"),
                    title = title.ifBlank { audio.optString("file_name", "آهنگ") },
                    artist = artist,
                    coverUrl = coverUrl,
                    durationSec = audio.optInt("duration", 0),
                    sizeBytes = audio.optLong("file_size", bytes.size.toLong())
                )
                _telegramSongs.value = listOf(song) + _telegramSongs.value
                persistTelegramSongs()
                _telegramStatus.value = "✓ آپلود شد"
            } catch (e: Exception) {
                _telegramStatus.value = "خطا در آپلود"
            } finally {
                _isUploadingTelegram.value = false
            }
        }
    }

    fun removeTelegramSong(fileId: String) {
        _telegramSongs.value = _telegramSongs.value.filterNot { it.fileId == fileId }
        persistTelegramSongs()
    }

    private fun persistTelegramSongs() {
        try {
            val arr = org.json.JSONArray()
            _telegramSongs.value.forEach { s ->
                arr.put(
                    org.json.JSONObject()
                        .put("fileId", s.fileId).put("title", s.title).put("artist", s.artist)
                        .put("coverUrl", s.coverUrl).put("durationSec", s.durationSec)
                        .put("sizeBytes", s.sizeBytes)
                )
            }
            tgPrefs().edit().putString("telegram_songs", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadTelegramSongs() {
        try {
            val json = tgPrefs().getString("telegram_songs", null) ?: return
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<TelegramSong>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TelegramSong(
                        fileId = o.getString("fileId"),
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        coverUrl = o.optString("coverUrl"),
                        durationSec = o.optInt("durationSec", 0),
                        sizeBytes = o.optLong("sizeBytes", 0)
                    )
                )
            }
            _telegramSongs.value = list
        } catch (_: Exception) {}
    }

    // Telegram file links expire, so resolve a fresh one right before playing
    fun playTelegramSong(song: TelegramSong, queue: List<TelegramSong>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = tgToken() ?: return@launch
                val client = okhttp3.OkHttpClient()
                val fileResp = client.newCall(
                    okhttp3.Request.Builder()
                        .url("https://api.telegram.org/bot$token/getFile?file_id=${song.fileId}")
                        .build()
                ).execute().use { it.body?.string() } ?: ""
                val json = org.json.JSONObject(fileResp)
                if (!json.optBoolean("ok")) {
                    _telegramStatus.value = "دریافت فایل ناموفق (شاید بزرگ‌تر از ۲۰ مگابایت است)"
                    return@launch
                }
                val path = json.getJSONObject("result").getString("file_path")
                val url = "https://api.telegram.org/file/bot$token/$path"
                val subsonicSong = SubsonicSong(
                    id = "tg_${song.fileId}",
                    title = song.title,
                    artist = song.artist,
                    coverArt = song.coverUrl.ifBlank { null },
                    duration = song.durationSec,
                    path = url
                )
                playSongDirectly(subsonicSong, listOf(subsonicSong))
            } catch (e: Exception) {
                _telegramStatus.value = "خطا در پخش از تلگرام"
            }
        }
    }

    // --- Shared online catalog: a public catalog.json (kept up to date by a
    // GitHub Actions worker from the Telegram channel) with direct audio URLs.
    // The client only reads a public JSON and plays direct links — no token. ---
    private val _catalogSongs = MutableStateFlow<List<CatalogSong>>(emptyList())
    val catalogSongs: StateFlow<List<CatalogSong>> = _catalogSongs.asStateFlow()

    private val _catalogStatus = MutableStateFlow<String?>(null)
    val catalogStatus: StateFlow<String?> = _catalogStatus.asStateFlow()

    private val _isCatalogBusy = MutableStateFlow(false)
    val isCatalogBusy: StateFlow<Boolean> = _isCatalogBusy.asStateFlow()

    // Default public catalog (separate public repo), overridable in prefs
    private val defaultCatalogUrl =
        "https://raw.githubusercontent.com/sahandse/himusic-catalog/main/catalog.json"

    fun catalogUrl(): String =
        tgPrefs().getString("catalog_url", null)?.takeIf { it.isNotBlank() } ?: defaultCatalogUrl

    fun setCatalogUrl(url: String) {
        tgPrefs().edit().putString("catalog_url", url.trim()).apply()
        fetchCatalog()
    }

    fun clearCatalogStatus() { _catalogStatus.value = null }

    fun loadCatalogState() { fetchCatalog() }

    fun fetchCatalog() {
        viewModelScope.launch(Dispatchers.IO) {
            _isCatalogBusy.value = true
            try {
                // Cache-buster so the newest catalog is always fetched
                val url = catalogUrl() + "?t=" + System.currentTimeMillis()
                val body = okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(url).build())
                    .execute().use { it.body?.string() } ?: "{}"
                val arr = org.json.JSONObject(body).optJSONArray("songs") ?: org.json.JSONArray()
                val list = mutableListOf<CatalogSong>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val audio = o.optString("audioUrl")
                    if (audio.isBlank()) continue
                    list.add(
                        CatalogSong(
                            title = o.optString("title"),
                            artist = o.optString("artist"),
                            coverUrl = o.optString("coverUrl"),
                            audioUrl = audio,
                            durationSec = o.optInt("durationSec", 0)
                        )
                    )
                }
                // Dedupe by URL — duplicate entries in catalog.json would crash
                // LazyColumn item keys and clutter the list
                _catalogSongs.value = list.distinctBy { it.audioUrl }
            } catch (e: Exception) {
                _catalogStatus.value = "خطا در دریافت کاتالوگ"
            } finally {
                _isCatalogBusy.value = false
            }
        }
    }

    private fun catalogToSubsonic(s: CatalogSong) = SubsonicSong(
        id = "cat_${s.audioUrl.hashCode()}",
        title = s.title,
        artist = s.artist,
        coverArt = s.coverUrl.ifBlank { null },
        duration = s.durationSec,
        path = s.audioUrl
    )

    fun playCatalogSong(song: CatalogSong, queue: List<CatalogSong>) {
        val songs = queue.ifEmpty { listOf(song) }
        val subsonicQueue = songs.map { catalogToSubsonic(it) }
        val startIndex = songs.indexOfFirst { it.audioUrl == song.audioUrl }.coerceAtLeast(0)
        playSongDirectly(subsonicQueue[startIndex], subsonicQueue)
    }

    // Radio: shuffle the whole catalog and play endlessly.
    fun radioPlayCatalog(songs: List<CatalogSong>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        playSongDirectly(catalogToSubsonic(shuffled.first()), shuffled.map { catalogToSubsonic(it) })
    }

    // Share a catalog song as a story card (reuses the existing card renderer).
    fun shareCatalogSong(song: CatalogSong) = shareSongCard(catalogToSubsonic(song))

    // --- Catalog likes (stored locally on the device) ---
    private val _catalogLikes = MutableStateFlow<Set<String>>(
        tgPrefs().getStringSet("catalog_likes", emptySet()) ?: emptySet()
    )
    val catalogLikes: StateFlow<Set<String>> = _catalogLikes.asStateFlow()

    fun toggleCatalogLike(audioUrl: String) {
        val next = _catalogLikes.value.toMutableSet()
        if (!next.add(audioUrl)) next.remove(audioUrl)
        _catalogLikes.value = next
        tgPrefs().edit().putStringSet("catalog_likes", next).apply()
    }

    override fun onCleared() {
        super.onCleared()
        playbackConnection.cleanUp()
    }
}

// ViewModelFactory helper to easily instanciate with Custom constructor injection
class MusicViewModelFactory(
    private val application: Application,
    private val repository: MusicRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MusicViewModel(application, repository, playbackConnection) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
