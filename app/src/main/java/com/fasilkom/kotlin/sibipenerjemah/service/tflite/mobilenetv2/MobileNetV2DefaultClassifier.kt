package com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2

import android.app.Activity
import com.fasilkom.kotlin.sibipenerjemah.service.common.Device

class MobileNetV2DefaultClassifier: MobileNetV2Classifier {

     constructor(
         activity: Activity,
         device: Device,
         numThreads: Int
    ) : super(activity, device, numThreads)

    override fun getLabelPath(): String {
        return "labels.txt"
    }

    override fun getModelPath(): String {
//        return "tflite/Weight_MobileNet_V2_ekstraksi_fitur.tflite"
        return "tflite/majority/Weight_MobileNet_V2_20191020_054829_K_1-5_Epoch_50_endcut.tflite"
    }

    override fun getNumOfClasses(): Int {
        return 1280
    }
}