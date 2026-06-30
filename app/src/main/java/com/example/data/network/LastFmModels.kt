package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LastFmArtistResponse(val artist: LastFmArtist? = null)

@JsonClass(generateAdapter = true)
data class LastFmArtist(
    val name: String? = null,
    val listeners: String? = null,
    val playcount: String? = null,
    val bio: LastFmBio? = null,
    val tags: LastFmTags? = null,
    val similar: LastFmSimilarWrapper? = null,
    val image: List<LastFmImage>? = null
)

@JsonClass(generateAdapter = true)
data class LastFmBio(
    val summary: String? = null,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class LastFmTags(val tag: List<LastFmTag>? = null)

@JsonClass(generateAdapter = true)
data class LastFmTag(val name: String? = null, val url: String? = null)

@JsonClass(generateAdapter = true)
data class LastFmSimilarWrapper(val artist: List<LastFmSimilarArtist>? = null)

@JsonClass(generateAdapter = true)
data class LastFmSimilarArtist(
    val name: String? = null,
    val url: String? = null,
    val image: List<LastFmImage>? = null
)

@JsonClass(generateAdapter = true)
data class LastFmImage(
    @Json(name = "#text") val text: String? = null,
    val size: String? = null
)

@JsonClass(generateAdapter = true)
data class LastFmSessionResponse(val session: LastFmSession? = null)

@JsonClass(generateAdapter = true)
data class LastFmSession(val name: String? = null, val key: String? = null)

@JsonClass(generateAdapter = true)
data class LastFmTrackInfoResponse(val track: LastFmTrackInfo? = null)

@JsonClass(generateAdapter = true)
data class LastFmTrackInfo(
    val name: String? = null,
    val listeners: String? = null,
    val playcount: String? = null,
    val toptags: LastFmTags? = null,
    val wiki: LastFmBio? = null
)

@JsonClass(generateAdapter = true)
data class LastFmScrobbleResponse(val scrobbles: LastFmScrobbleResult? = null)

@JsonClass(generateAdapter = true)
data class LastFmScrobbleResult(
    @Json(name = "@attr") val attr: LastFmScrobbleAttr? = null
)

@JsonClass(generateAdapter = true)
data class LastFmScrobbleAttr(val accepted: Int? = null, val ignored: Int? = null)
