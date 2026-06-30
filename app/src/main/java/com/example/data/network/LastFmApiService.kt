package com.example.data.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LastFmApiService {

    @GET(".")
    suspend fun getArtistInfo(
        @Query("method") method: String = "artist.getInfo",
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("autocorrect") autocorrect: Int = 1,
        @Query("lang") lang: String = "en",
        @Query("format") format: String = "json"
    ): LastFmArtistResponse

    @GET(".")
    suspend fun getTrackInfo(
        @Query("method") method: String = "track.getInfo",
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("format") format: String = "json"
    ): LastFmTrackInfoResponse

    @FormUrlEncoded
    @POST(".")
    suspend fun getMobileSession(
        @Field("method") method: String = "auth.getMobileSession",
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("format") format: String = "json"
    ): LastFmSessionResponse

    @FormUrlEncoded
    @POST(".")
    suspend fun scrobble(
        @Field("method") method: String = "track.scrobble",
        @Field("track[0]") track: String,
        @Field("artist[0]") artist: String,
        @Field("album[0]") album: String,
        @Field("timestamp[0]") timestamp: Long,
        @Field("api_key") apiKey: String,
        @Field("sk") sessionKey: String,
        @Field("api_sig") apiSig: String,
        @Field("format") format: String = "json"
    ): LastFmScrobbleResponse

    @FormUrlEncoded
    @POST(".")
    suspend fun updateNowPlaying(
        @Field("method") method: String = "track.updateNowPlaying",
        @Field("track") track: String,
        @Field("artist") artist: String,
        @Field("album") album: String,
        @Field("api_key") apiKey: String,
        @Field("sk") sessionKey: String,
        @Field("api_sig") apiSig: String,
        @Field("format") format: String = "json"
    ): okhttp3.ResponseBody
}
