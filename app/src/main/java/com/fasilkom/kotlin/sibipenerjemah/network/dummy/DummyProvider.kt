package com.fasilkom.kotlin.sibipenerjemah.network.dummy

import com.fasilkom.kotlin.sibipenerjemah.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Dummy Provider
 * Use this class to simulate or mock server-call.
 * Endpoint, attributes, all need to be created first on mocky.io */
class DummyProvider {
    private val client = OkHttpClient().newBuilder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://run.mocky.io/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val services: TranslatorService = retrofit.create(TranslatorService::class.java)
}