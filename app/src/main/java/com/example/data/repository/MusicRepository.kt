package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.network.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val dao: MusicDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Holds the active server credentials in memory to avoid constant DB queries in interceptor
    @Volatile
    private var cachedServer: SavedServer? = null

    private val authInterceptor = Interceptor { chain ->
        var request = chain.request()
        val server = cachedServer ?: run {
            // Blocking read from DB if memory cache is empty
            val s = kotlinx.coroutines.runBlocking { dao.getActiveServer() }
            cachedServer = s
            s
        }

        if (server != null) {
            val serverUri = server.url.toHttpUrlOrNull()
            if (serverUri != null) {
                val newUrl = request.url.newBuilder()
                    .scheme(serverUri.scheme)
                    .host(serverUri.host)
                    .port(serverUri.port)
                    .setQueryParameter("u", server.username)
                    .setQueryParameter("t", server.token)
                    .setQueryParameter("s", server.salt)
                    .setQueryParameter("v", "1.16.1")
                    .setQueryParameter("c", "smusic")
                    .setQueryParameter("f", "json")
                    .build()
                request = request.newBuilder().url(newUrl).build()
            }
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val apiService: SubsonicApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://placeholder.com") // Replaced dynamically by interceptor
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SubsonicApiService::class.java)
    }

    init {
        // Hydrate cache on startup
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launchSilently {
            cachedServer = dao.getActiveServer()
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.launchSilently(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        try {
            this.launchSilentlyInternal(block)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.launchSilentlyInternal(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        launch(Dispatchers.IO) {
            block()
        }
    }

    // --- Authentication ---
    suspend fun saveServer(url: String, username: String, passwordOrToken: String, isTokenMode: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        val salt = UUID.randomUUID().toString().substring(0, 8)
        val token = if (isTokenMode) {
            passwordOrToken
        } else {
            md5(passwordOrToken + salt)
        }

        val server = SavedServer(
            url = cleanUrl,
            username = username,
            token = token,
            salt = salt,
            isActive = true
        )

        dao.deactivateAllServers()
        dao.insertServer(server)
        cachedServer = server

        // Verify server connection by sending a ping
        try {
            val response = apiService.ping().response
            if (response.status == "ok") {
                true
            } else {
                dao.clearServers()
                cachedServer = null
                false
            }
        } catch (e: Exception) {
            dao.clearServers()
            cachedServer = null
            false
        }
    }

    suspend fun isServerConfigured(): Boolean = withContext(Dispatchers.IO) {
        dao.getActiveServer() != null
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        dao.clearServers()
        cachedServer = null
    }

    fun getActiveServerFlow(): Flow<SavedServer?> = dao.getActiveServerFlow()

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    // --- Core Media & Streaming ---
    suspend fun getStreamUrl(song: SubsonicSong): String = withContext(Dispatchers.IO) {
        // If it's a demo song, return its mock stream url
        if (song.id.startsWith("demo_")) {
            return@withContext DemoData.getDemoAudioUrl(song.id)
        }

        // If downloaded offline, return its local file path URI
        val downloaded = dao.getDownloadedTrack(song.id)
        if (downloaded != null) {
            val file = File(downloaded.localFilePath)
            if (file.exists()) {
                return@withContext file.absolutePath
            }
        }

        // Generate full streaming URL with auth params appended
        val server = cachedServer ?: dao.getActiveServer()
            ?: return@withContext DemoData.getDemoAudioUrl(song.id)

        val cleanUrl = server.url.trimEnd('/')
        "$cleanUrl/rest/stream.view?id=${song.id}&u=${server.username}&t=${server.token}&s=${server.salt}&v=1.16.1&c=smusic&f=json"
    }

    fun getCoverArtUrl(coverArtId: String?): String {
        if (coverArtId == null) return "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"
        if (coverArtId.startsWith("http://") || coverArtId.startsWith("https://")) return coverArtId

        val server = cachedServer ?: return "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"
        val cleanUrl = server.url.trimEnd('/')
        return "$cleanUrl/rest/getCoverArt.view?id=$coverArtId&size=512&u=${server.username}&t=${server.token}&s=${server.salt}&v=1.16.1&c=smusic&f=json"
    }

    // --- Subsonic API Data Fetchers ---
    suspend fun getArtists(): List<SubsonicArtist> = withContext(Dispatchers.IO) {
        if (cachedServer == null) return@withContext DemoData.artists
        try {
            val response = apiService.getArtists().response
            val list = mutableListOf<SubsonicArtist>()
            response.artists?.index?.forEach { index ->
                index.artist?.let { list.addAll(it) }
            }
            if (list.isEmpty()) DemoData.artists else list
        } catch (e: Exception) {
            DemoData.artists
        }
    }

    suspend fun getArtistDetails(id: String): ArtistDetail? = withContext(Dispatchers.IO) {
        if (id.startsWith("demo_") || cachedServer == null) {
            val artist = DemoData.artists.find { it.id == id } ?: return@withContext null
            val alb = DemoData.albums.filter { it.artistId == id }
            return@withContext ArtistDetail(id = artist.id, name = artist.name, coverArt = artist.coverArt, album = alb)
        }
        try {
            apiService.getArtist(id).response.artist
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getArtistInfo(id: String): ArtistInfo? = withContext(Dispatchers.IO) {
        if (id.startsWith("demo_") || cachedServer == null) {
            val artistName = DemoData.artists.find { it.id == id }?.name ?: "This Artist"
            return@withContext ArtistInfo(
                biography = "بیوگرافی هنری و خلاقانه $artistName:\n\nاین هنرمند برجسته یکی از تاثیرگذارترین چهره‌های موسیقی در سبک خود است. با تلفیق ملودی‌های مدرن و ریشه‌های موسیقی اصیل، توانسته آثاری خلق کند که مرزهای زمان را درنوردیده‌اند. آلبوم‌های پرفروش او گواهی بر خلاقیت بی‌پایان و درک عمیق هنری وی دارند.\n\n$artistName with their legendary sound and deep, evocative songwriting, has shaped the soundscapes of a generation. Seamlessly merging traditional roots with cutting-edge production styles, they have delivered timeless albums and electrifying performances worldwide."
            )
        }
        try {
            val resp = apiService.getArtistInfo2(id).response
            resp.artistInfo2 ?: resp.artistInfo
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAlbumDetails(id: String): AlbumDetail? = withContext(Dispatchers.IO) {
        if (id.startsWith("demo_") || cachedServer == null) {
            val album = DemoData.albums.find { it.id == id } ?: return@withContext null
            val songs = DemoData.songs.filter { it.albumId == id }
            return@withContext AlbumDetail(
                id = album.id,
                name = album.name,
                artist = album.artist,
                artistId = album.artistId,
                coverArt = album.coverArt,
                songCount = songs.size,
                duration = songs.sumOf { it.duration ?: 0 },
                song = songs
            )
        }
        try {
            apiService.getAlbum(id).response.album
        } catch (e: Exception) {
            null
        }
    }

    suspend fun search(query: String): SearchResult3 = withContext(Dispatchers.IO) {
        if (cachedServer == null) {
            val filteredSongs = DemoData.songs.filter { it.title.contains(query, ignoreCase = true) || it.artist?.contains(query, ignoreCase = true) == true }
            val filteredAlbums = DemoData.albums.filter { it.name.contains(query, ignoreCase = true) || it.artist?.contains(query, ignoreCase = true) == true }
            val filteredArtists = DemoData.artists.filter { it.name.contains(query, ignoreCase = true) }
            return@withContext SearchResult3(artist = filteredArtists, album = filteredAlbums, song = filteredSongs)
        }
        try {
            apiService.search3(query).response.searchResult3 ?: SearchResult3(null, null, null)
        } catch (e: Exception) {
            SearchResult3(null, null, null)
        }
    }

    suspend fun getAlbumList(type: String): List<SubsonicAlbum> = withContext(Dispatchers.IO) {
        if (cachedServer == null) return@withContext DemoData.albums
        try {
            apiService.getAlbumList2(type).response.albumList?.album
                ?: apiService.getAlbumList2(type).response.albumList2?.album
                ?: DemoData.albums
        } catch (e: Exception) {
            DemoData.albums
        }
    }

    suspend fun getRandomSongs(): List<SubsonicSong> = withContext(Dispatchers.IO) {
        if (cachedServer == null) return@withContext DemoData.songs
        try {
            apiService.getRandomSongs().response.randomSongs?.song ?: DemoData.songs
        } catch (e: Exception) {
            DemoData.songs
        }
    }

    suspend fun getLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        // Try getting from DemoData first if it matches
        val demoSong = DemoData.songs.find { it.artist == artist && it.title == title }
        if (demoSong != null) {
            return@withContext DemoData.demoLyrics[demoSong.id]
        }

        if (cachedServer == null) return@withContext null
        try {
            val response = apiService.getLyrics(artist, title).response
            response.lyricsList?.firstOrNull()?.value
        } catch (e: Exception) {
            null
        }
    }

    // --- Favorites Synchronization & DB ---
    fun getFavoriteTracksFlow(): Flow<List<FavoriteTrack>> = dao.getFavoriteTracksFlow()
    fun getFavoriteAlbumsFlow(): Flow<List<FavoriteAlbum>> = dao.getFavoriteAlbumsFlow()
    fun getFavoriteArtistsFlow(): Flow<List<FavoriteArtist>> = dao.getFavoriteArtistsFlow()

    suspend fun toggleFavoriteTrack(song: SubsonicSong) = withContext(Dispatchers.IO) {
        val isFavorite = dao.isTrackFavorite(song.id)
        if (isFavorite) {
            dao.deleteFavoriteTrack(song.id)
            if (!song.id.startsWith("demo_") && cachedServer != null) {
                try { apiService.unstar(id = song.id) } catch (e: Exception) {}
            }
        } else {
            dao.insertFavoriteTrack(
                FavoriteTrack(
                    id = song.id,
                    title = song.title,
                    artist = song.artist ?: "هنرمند ناشناس",
                    album = song.album ?: "آلبوم ناشناس",
                    duration = song.duration ?: 0,
                    coverArtUrl = getCoverArtUrl(song.coverArt)
                )
            )
            if (!song.id.startsWith("demo_") && cachedServer != null) {
                try { apiService.star(id = song.id) } catch (e: Exception) {}
            }
        }
    }

    suspend fun isTrackFavorite(id: String): Boolean = dao.isTrackFavorite(id)

    suspend fun toggleFavoriteAlbum(album: SubsonicAlbum) = withContext(Dispatchers.IO) {
        val isFavorite = dao.isAlbumFavorite(album.id)
        if (isFavorite) {
            dao.deleteFavoriteAlbum(album.id)
            if (!album.id.startsWith("demo_") && cachedServer != null) {
                try { apiService.unstar(albumId = album.id) } catch (e: Exception) {}
            }
        } else {
            dao.insertFavoriteAlbum(
                FavoriteAlbum(
                    id = album.id,
                    name = album.name,
                    artist = album.artist ?: "هنرمند ناشناس",
                    songCount = album.songCount ?: 0,
                    coverArtUrl = getCoverArtUrl(album.coverArt)
                )
            )
            if (!album.id.startsWith("demo_") && cachedServer != null) {
                try { apiService.star(albumId = album.id) } catch (e: Exception) {}
            }
        }
    }

    suspend fun isAlbumFavorite(id: String): Boolean = dao.isAlbumFavorite(id)

    suspend fun toggleFavoriteArtist(artist: SubsonicArtist) = withContext(Dispatchers.IO) {
        val isFavorite = dao.isArtistFavorite(artist.id)
        if (isFavorite) {
            dao.deleteFavoriteArtist(artist.id)
            if (!artist.id.startsWith("demo_") && cachedServer != null) {
                try { apiService.unstar(artistId = artist.id) } catch (e: Exception) {}
            }
        } else {
            dao.insertFavoriteArtist(
                FavoriteArtist(
                    id = artist.id,
                    name = artist.name,
                    albumCount = artist.albumCount ?: 0,
                    coverArtUrl = getCoverArtUrl(artist.coverArt)
                )
            )
            if (!artist.id.startsWith("demo_") && cachedServer != null) {
                try { apiService.star(artistId = artist.id) } catch (e: Exception) {}
            }
        }
    }

    suspend fun isArtistFavorite(id: String): Boolean = dao.isArtistFavorite(id)

    // --- Offline Mode & File Downloads ---
    fun getDownloadedTracksFlow(): Flow<List<DownloadedTrack>> = dao.getDownloadedTracksFlow()

    suspend fun isTrackDownloaded(id: String): Boolean = dao.isTrackDownloaded(id)

    suspend fun downloadTrack(song: SubsonicSong, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        if (dao.isTrackDownloaded(song.id)) return@withContext true

        val prefs = context.getSharedPreferences("smusic_prefs", Context.MODE_PRIVATE)
        val customLoc = prefs.getString("download_location", "") ?: ""
        val downloadDir = if (customLoc.isNotBlank()) {
            File(customLoc)
        } else {
            File(context.filesDir, "downloads")
        }
        if (!downloadDir.exists()) downloadDir.mkdirs()

        val fileName = "${song.id}.${song.suffix ?: "mp3"}"
        val localFile = File(downloadDir, fileName)

        // Fetch streaming binary from network
        try {
            val url = getStreamUrl(song)
            val request = Request.Builder().url(url).build()
            val client = if (song.id.startsWith("demo_")) OkHttpClient() else okHttpClient
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(localFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    onProgress(totalBytesRead.toFloat() / contentLength)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Save cover art path if any
            var coverArtPath: String? = null
            song.coverArt?.let { artId ->
                try {
                    val coverArtFile = File(downloadDir, "${song.id}_art.png")
                    val artUrl = getCoverArtUrl(artId)
                    val artResponse = OkHttpClient().newCall(Request.Builder().url(artUrl).build()).execute()
                    if (artResponse.isSuccessful) {
                        artResponse.body?.byteStream()?.use { input ->
                            FileOutputStream(coverArtFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        coverArtPath = coverArtFile.absolutePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Save record in local db
            val record = DownloadedTrack(
                id = song.id,
                title = song.title,
                artist = song.artist ?: "هنرمند ناشناس",
                album = song.album ?: "آلبوم ناشناس",
                duration = song.duration ?: 0,
                localFilePath = localFile.absolutePath,
                sizeBytes = localFile.length(),
                coverArtPath = coverArtPath
            )
            dao.insertDownloadedTrack(record)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            if (localFile.exists()) localFile.delete()
            false
        }
    }

    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        val record = dao.getDownloadedTrack(id)
        if (record != null) {
            val file = File(record.localFilePath)
            if (file.exists()) file.delete()
            record.coverArtPath?.let { artPath ->
                val artFile = File(artPath)
                if (artFile.exists()) artFile.delete()
            }
            dao.deleteDownloadedTrack(id)
        }
    }

    // --- Playlists ---
    fun getLocalPlaylistsFlow(): Flow<List<LocalPlaylist>> = dao.getLocalPlaylistsFlow()

    fun getPlaylistItemsFlow(playlistId: Long): Flow<List<LocalPlaylistItem>> = dao.getPlaylistItemsFlow(playlistId)

    suspend fun createLocalPlaylist(name: String, description: String? = null): Long = withContext(Dispatchers.IO) {
        dao.insertLocalPlaylist(LocalPlaylist(name = name, description = description))
    }

    suspend fun addTrackToPlaylist(playlistId: Long, song: SubsonicSong) = withContext(Dispatchers.IO) {
        val item = LocalPlaylistItem(
            playlistId = playlistId,
            trackId = song.id,
            title = song.title,
            artist = song.artist ?: "هنرمند ناشناس",
            album = song.album ?: "آلبوم ناشناس",
            duration = song.duration ?: 0,
            coverArtUrl = getCoverArtUrl(song.coverArt)
        )
        dao.insertPlaylistItem(item)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylistItems(playlistId)
        dao.deleteLocalPlaylist(playlistId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    // --- History / Recent Plays ---
    fun getRecentPlaysFlow(): Flow<List<RecentPlay>> = dao.getRecentPlaysFlow()

    suspend fun insertRecentPlay(song: SubsonicSong) = withContext(Dispatchers.IO) {
        dao.insertRecentPlay(
            RecentPlay(
                trackId = song.id,
                title = song.title,
                artist = song.artist ?: "هنرمند ناشناس",
                album = song.album ?: "آلبوم ناشناس",
                duration = song.duration ?: 0,
                coverArtUrl = getCoverArtUrl(song.coverArt)
            )
        )
    }
}
