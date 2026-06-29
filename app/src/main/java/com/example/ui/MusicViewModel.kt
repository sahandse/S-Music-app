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
import com.example.data.network.SearchResult3
import com.example.data.network.SubsonicAlbum
import com.example.data.network.SubsonicArtist
import com.example.data.network.SubsonicSong
import com.example.data.network.ArtistInfo
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    private val _introShown = MutableStateFlow(false)
    val introShown: StateFlow<Boolean> = _introShown.asStateFlow()

    private val _downloadLocation = MutableStateFlow("")
    val downloadLocation: StateFlow<String> = _downloadLocation.asStateFlow()

    private val _audioQuality = MutableStateFlow("high") // "low", "medium", "high", "lossless"
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _cacheSizeMb = MutableStateFlow(128)
    val cacheSizeMb: StateFlow<Int> = _cacheSizeMb.asStateFlow()

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

        // Initialize Settings from preferences
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString("theme_mode", "dark") ?: "dark"
        _themeAccent.value = prefs.getString("theme_accent", "red") ?: "red"
        _language.value = prefs.getString("app_language", "fa") ?: "fa"
        _userName.value = prefs.getString("user_name", "") ?: ""
        _introShown.value = prefs.getBoolean("intro_shown", false)
        _downloadLocation.value = prefs.getString("download_location", "") ?: ""
        _audioQuality.value = prefs.getString("audio_quality", "high") ?: "high"
        _cacheSizeMb.value = prefs.getInt("cache_size_mb", 128)
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
        viewModelScope.launch {
            _isLoadingDetail.value = true
            val details = repository.getArtistDetails(artist.id)
            if (details != null) {
                _selectedArtistAlbums.value = details.album ?: emptyList()
            }
            val bio = repository.getArtistInfo(artist.id)
            _selectedArtistBio.value = bio
            _isLoadingDetail.value = false
        }
    }

    fun loadAlbumDetails(album: SubsonicAlbum) {
        _selectedAlbumDetails.value = album
        _selectedAlbumSongs.value = emptyList()
        viewModelScope.launch {
            _isLoadingDetail.value = true
            val details = repository.getAlbumDetails(album.id)
            if (details != null) {
                _selectedAlbumSongs.value = details.song ?: emptyList()
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

    fun playAllSongsInQueue(songsList: List<SubsonicSong>) {
        if (songsList.isNotEmpty()) {
            playSongDirectly(songsList.first(), songsList)
        }
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
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = SearchResult3(null, null, null)
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce API calls
            _isSearching.value = true
            val results = repository.search(query)
            _searchResults.value = results
            _isSearching.value = false
        }
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
    fun startDownload(song: SubsonicSong) {
        viewModelScope.launch {
            if (repository.isTrackDownloaded(song.id)) return@launch

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

    // --- Lyrics Loader ---
    private fun fetchLyricsForTrack(song: SubsonicSong) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            _currentLyrics.value = repository.getLyrics(song.artist ?: "", song.title)
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

    fun setIntroShown(shown: Boolean) {
        _introShown.value = shown
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("intro_shown", shown).apply()
    }

    fun setDownloadLocation(path: String) {
        _downloadLocation.value = path
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("download_location", path).apply()
    }

    fun performOnboardingLogin(name: String) {
        viewModelScope.launch {
            setUserName(name)
            setIntroShown(true)
            // Preconfigured credentials:
            // Username: sahandse, Password: 09190159560, Server: https://soundtron.ir
            login(
                url = "https://soundtron.ir",
                username = "sahandse",
                passwordOrToken = "09190159560",
                isTokenMode = false
            )
        }
    }

    fun setAudioQuality(quality: String) {
        _audioQuality.value = quality
        val prefs = getApplication<Application>().getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("audio_quality", quality).apply()
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
