package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApiService {
    @GET("json/stations/topclick/{limit}")
    suspend fun getTopStations(@Path("limit") limit: Int = 40): List<RadioStation>

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String,
        @Query("limit") limit: Int = 40,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioStation>
}
