package com.fasilkom.kotlin.sibipenerjemah.service.tflite.lstm

import android.app.Activity
import com.fasilkom.kotlin.sibipenerjemah.service.common.Device
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2.MobileNetV2Classifier

class LSTMDefaultClassifier: LSTMClassifier {

    constructor(
        activity: Activity,
        device: Device,
        numThreads: Int,
        numOfTimeStep: Long,
        numOfFeatures: Long
    ) : super(activity, device, numThreads, numOfTimeStep, numOfFeatures)

    override fun getModelPath(): String {
//        private val MODEL_LSTM_DIR = "tflite/Weight_LSTM_P1_20191104_210250_K_2-5_Epoch_150.tflite"
//        private val MODEL_LSTM_PIPUT_DIR = "tflite/majority/Weight_LSTM.tflite" //DEFAULT
        return "tflite/majority/Weight_LSTM.tflite"
    }

    override fun getNumOfClasses(): Int {
        return 83
    }
}