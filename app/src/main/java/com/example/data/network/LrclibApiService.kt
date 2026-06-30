package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface LrclibApiService {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") durationSeconds: Int? = null
    ): LrclibTrack

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String
    ): List<LrclibTrack>
}
