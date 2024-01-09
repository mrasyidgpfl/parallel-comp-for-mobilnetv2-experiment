package com.fasilkom.kotlin.sibipenerjemah.network.fileupload

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


/** Network Service Class
 * Connect to each endpoint of the server by defining functions here.
 * Currently we only have 1 functionality from the server (onFileUpload) */
interface VideoUploadService {

    /* Upload video file, return translation result*/
    @Multipart
    @POST("/translate")
    fun onFileUpload(
        @Part("crop_axis") cropAxis: RequestBody?,
        @Part file: MultipartBody.Part?
    ): Call<VideoUploadResponse>
}