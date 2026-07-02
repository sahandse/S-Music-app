package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MusicBrainzArtistSearchResponse(
    val artists: List<MusicBrainzArtist>? = null,
    val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class MusicBrainzArtist(
    val id: String? = null,
    val name: String? = null,
    val country: String? = null,
    val type: String? = null,
    @Json(name = "life-span") val lifeSpan: MusicBrainzLifeSpan? = null,
    val tags: List<MusicBrainzTag>? = null,
    val genres: List<MusicBrainzTag>? = null,
    val disambiguation: String? = null,
    val score: Int? = null
)

@JsonClass(generateAdapter = true)
data class MusicBrainzLifeSpan(
    val begin: String? = null,
    val end: String? = null,
    val ended: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class MusicBrainzTag(
    val name: String? = null,
    val count: Int? = null
)

data class EnrichedArtistInfo(
    val country: String?,
    val foundedYear: String?,
    val type: String?,
    val genres: List<String>,
    val mbid: String?
)

@JsonClass(generateAdapter = true)
data class MusicBrainzArtistDetail(
    val id: String? = null,
    val name: String? = null,
    @Json(name = "relations") val relations: List<MusicBrainzRelation>? = null
)

@JsonClass(generateAdapter = true)
data class MusicBrainzRelation(
    val type: String? = null,
    val url: MusicBrainzUrl? = null,
    val artist: MusicBrainzArtist? = null
)

@JsonClass(generateAdapter = true)
data class MusicBrainzUrl(
    val resource: String? = null
)
