package com.fasilkom.kotlin.sibipenerjemah.ui.signtotext

import com.fasilkom.kotlin.sibipenerjemah.service.ModelProvider
import java.io.File

interface PickerContract {
    interface View {

    }
    interface Presenter {
        fun executeOnlineClassifier()
        fun executeOfflineClassifier(videoFile: File, listener: ModelProvider.TranslateListener)
        fun loadClassifier()
    }
}