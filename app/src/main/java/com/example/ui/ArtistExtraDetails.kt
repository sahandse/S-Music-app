package com.example.ui

data class ArtistExtraDetails(
    val country: String,
    val birthYearOrActive: String,
    val instagram: String? = null,
    val twitter: String? = null,
    val youtube: String? = null,
    val spotify: String? = null
)
