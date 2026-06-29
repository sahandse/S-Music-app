package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface SubsonicApiService {
    @GET("rest/ping.view")
    suspend fun ping(): SubsonicResponseWrapper

    @GET("rest/getArtists.view")
    suspend fun getArtists(): SubsonicResponseWrapper

    @GET("rest/getArtist.view")
    suspend fun getArtist(@Query("id") id: String): SubsonicResponseWrapper

    @GET("rest/getArtistInfo2.view")
    suspend fun getArtistInfo2(@Query("id") id: String): SubsonicResponseWrapper

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(@Query("id") id: String): SubsonicResponseWrapper

    @GET("rest/getStarred2.view")
    suspend fun getStarred2(): SubsonicResponseWrapper

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(): SubsonicResponseWrapper

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(@Query("id") id: String): SubsonicResponseWrapper

    @GET("rest/createPlaylist.view")
    suspend fun createPlaylist(
        @Query("name") name: String,
        @Query("songId") songId: List<String>
    ): SubsonicResponseWrapper

    @GET("rest/deletePlaylist.view")
    suspend fun deletePlaylist(@Query("id") id: String): SubsonicResponseWrapper

    @GET("rest/updatePlaylist.view")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("songIndexToRemove") removeIndex: Int? = null,
        @Query("songIdToAdd") addId: String? = null
    ): SubsonicResponseWrapper

    @GET("rest/search3.view")
    suspend fun search3(@Query("query") query: String): SubsonicResponseWrapper

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int = 20
    ): SubsonicResponseWrapper

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(@Query("size") size: Int = 20): SubsonicResponseWrapper

    @GET("rest/getLyrics.view")
    suspend fun getLyrics(
        @Query("artist") artist: String,
        @Query("title") title: String
    ): SubsonicResponseWrapper

    @GET("rest/star.view")
    suspend fun star(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null
    ): SubsonicResponseWrapper

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null
    ): SubsonicResponseWrapper
}
