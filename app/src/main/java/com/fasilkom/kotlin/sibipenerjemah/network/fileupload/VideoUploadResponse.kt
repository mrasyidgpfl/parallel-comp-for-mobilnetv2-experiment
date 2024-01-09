package com.fasilkom.kotlin.sibipenerjemah.network.fileupload

import com.google.gson.annotations.SerializedName

/** Network Response
 * Assuming server only return translation result */
data class VideoUploadResponse(
    @SerializedName("translation")
    var translationResult: String? = null
)