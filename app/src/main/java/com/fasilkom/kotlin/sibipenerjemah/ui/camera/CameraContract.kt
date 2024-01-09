package com.fasilkom.kotlin.sibipenerjemah.ui.camera

import com.fasilkom.kotlin.sibipenerjemah.service.ModelProvider
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProviderInterface
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import okhttp3.MultipartBody
import java.io.File

interface CameraContract {

    interface View {
        fun onClassifyingShowLoadingState()
        fun onTranslationShowProgressPercentage(percentage: String)
        fun onTranslationShowProgressResult(result: String)
    }

    interface Presenter {
        val modelProvider: ModelProviderInterface

        fun executeOnlineClassifier(
            body: MultipartBody.Part,
            cropSize: OpenCVService.Crop
        )
        fun executeOfflineClassifier(
            videoFile: File,
            listener: ModelProvider.TranslateListener,
            cropSize: OpenCVService.Crop
        )
        fun loadClassifier()
        fun closeClassifier()
    }
}