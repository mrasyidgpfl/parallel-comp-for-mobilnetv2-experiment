package com.fasilkom.kotlin.sibipenerjemah.service

import android.app.Activity
import android.content.Context
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import io.reactivex.rxjava3.core.Observable
import java.io.File
import java.util.ArrayList

interface ModelProviderInterface {

    fun loadClassifier(context: Context, activity: Activity)
    fun isClassifierLoaded(): Boolean
    fun runClassifier(
        videoFile: File,
        listener: ModelProvider.TranslateListener,
        cropSize: OpenCVService.Crop
    )
    fun runObservableClassifier(
        videoFile: File,
        listener: ModelProvider.TranslateListener,
        cropSize: OpenCVService.Crop
    ): Observable<Pair<String, ArrayList<FloatArray>>>

    fun runRestLiteClassifier(mobileNetV2Result: FloatArray): String
}