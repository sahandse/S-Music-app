package com.example.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

interface MyMemoryApi {
    // MyMemory rejects "auto" as a source language — the langpair must always
    // name explicit ISO codes like "en|fa"
    @GET("get")
    suspend fun translate(
        @Query("q") text: String,
        @Query("langpair") langpair: String = "en|fa"
    ): MyMemoryResponse
}

@JsonClass(generateAdapter = true)
data class MyMemoryResponse(
    val responseData: MyMemoryResponseData? = null
)

@JsonClass(generateAdapter = true)
data class MyMemoryResponseData(
    val translatedText: String? = null
)
