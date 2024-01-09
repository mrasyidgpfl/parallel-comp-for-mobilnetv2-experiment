package com.fasilkom.kotlin.sibipenerjemah.network.fileupload

import com.fasilkom.kotlin.sibipenerjemah.BuildConfig
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Network Provider, still on testing phase because there's no server.
 * Currently using ngrok to mock actual online server. */
class VideoUploadProvider {
    private val SERVER_URL = "https://0ef0d4615cb9.ngrok.io"
    private val LOCAL_URL = "https://192.168.1.10:5000/"
    private val EMU_LOCAL_URL = "https://10.0.2.2:5000/"

    private val client = OkHttpClient().newBuilder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT,
            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .allEnabledTlsVersions()
                .allEnabledCipherSuites()
                .build()))
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(SERVER_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val services: VideoUploadService = retrofit.create(VideoUploadService::class.java)
}