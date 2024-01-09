package com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2

import android.app.Activity
import com.fasilkom.kotlin.sibipenerjemah.service.common.Device

class MobileNetV2RPClassifier: MobileNetV2Classifier {

    constructor(
        activity: Activity,
        device: Device,
        numThreads: Int
    ) : super(activity, device, numThreads)

    override fun getLabelPath(): String {
        return "labels.txt"
    }

    override fun getModelPath(): String {
        return "randompixel/Weight_MobileNet_V2_20201216_065751_K_1-5_Epoch_25_feature_extract.tflite"
    }

    override fun getNumOfClasses(): Int {
        return 1280
    }
}