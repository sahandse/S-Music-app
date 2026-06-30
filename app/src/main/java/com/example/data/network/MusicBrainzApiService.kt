package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface MusicBrainzApiService {

    @GET("artist")
    suspend fun searchArtist(
        @Query("query") query: String,
        @Query("limit") limit: Int = 1,
        @Query("fmt") format: String = "json"
    ): MusicBrainzArtistSearchResponse
}
