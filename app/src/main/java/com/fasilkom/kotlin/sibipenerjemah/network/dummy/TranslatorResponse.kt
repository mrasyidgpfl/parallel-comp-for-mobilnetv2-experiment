package com.fasilkom.kotlin.sibipenerjemah.network.dummy

import com.google.gson.annotations.SerializedName

data class TranslatorResponse(
    @SerializedName("translation")
    var translationResult: String? = null
)