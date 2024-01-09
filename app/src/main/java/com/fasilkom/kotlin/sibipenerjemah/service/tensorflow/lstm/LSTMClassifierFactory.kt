package com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.lstm

import android.content.res.AssetManager
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.utils.FileUtils
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.IOException

object LSTMClassifierFactory {

    /* Directory of models (next will use internal memory) */
    private const val MODEL_DIR = "protobuf/Weight_LSTM_P1_20191104_210250_K_2-5_Epoch_150.pb"
    private const val MODEL_PIPUT_DIR = "protobufv2/Weight_LSTM.pb"

    /* Const */
    private const val INPUT_NAME = "input_1"
    private const val OUTPUT_NAME = "dense_1/Softmax"
    private const val NUM_OF_TIME_STEP = 13
    private const val NUM_OF_FEATURES = 1280
    private const val LABEL_DIR = "labels.txt"

    @Throws(IOException::class)
    fun create(
        assetManager: AssetManager
    ): LSTMClassifier {
        val labels = FileUtils.getLabels(assetManager, LABEL_DIR)

        return LSTMClassifier(
            INPUT_NAME,
            OUTPUT_NAME,
            java.lang.Long.valueOf(NUM_OF_TIME_STEP.toLong()),
            java.lang.Long.valueOf(NUM_OF_FEATURES.toLong()),
            labels,
            TensorFlowInferenceInterface(assetManager, MODEL_PIPUT_DIR)
        )
    }
}