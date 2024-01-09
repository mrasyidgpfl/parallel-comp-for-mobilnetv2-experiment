package com.fasilkom.kotlin.sibipenerjemah.network.dummy

import retrofit2.Call
import retrofit2.http.GET

interface TranslatorService {
    @GET("v3/ec68fb4f-05e0-4f54-bc57-77b450d4bebc")
    fun getTranslateResult(): Call<TranslatorResponse>
}