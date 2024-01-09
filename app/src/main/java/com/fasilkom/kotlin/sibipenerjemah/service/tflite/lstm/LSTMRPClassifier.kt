package com.fasilkom.kotlin.sibipenerjemah.service.tflite.lstm

import android.app.Activity
import com.fasilkom.kotlin.sibipenerjemah.service.common.Device

class LSTMRPClassifier: LSTMClassifier {

    constructor(
        activity: Activity,
        device: Device,
        numThreads: Int,
        numOfTimeStep: Long,
        numOfFeatures: Long
    ) : super(activity, device, numThreads, numOfTimeStep, numOfFeatures)

    override fun getModelPath(): String {
        return "randompixel/Weight_LSTM_P1_20201222_071052_K_1-5_Epoch_7.tflite"
    }

    override fun getNumOfClasses(): Int {
        return 83
    }
}