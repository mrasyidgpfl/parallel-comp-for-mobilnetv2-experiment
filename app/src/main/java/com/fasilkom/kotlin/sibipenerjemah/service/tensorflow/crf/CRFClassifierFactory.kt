package com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf

import android.content.res.AssetManager
import android.util.Log

import org.tensorflow.contrib.android.TensorFlowInferenceInterface

import java.io.IOException

object CRFClassifierFactory {

    /* Directory of models (next will use internal memory) */
    private const val MODEL_DIR = "protobuf/Weight_CRF_20191218_181608_K_5-5_Epoch_45.pb"
    private const val MODEL_PIPUT_DIR = "protobufv2/Weight_TCRF_84_label_based_val.pb" // DEFAULT
    private const val MODEL_RP_DIR = "randompixel/Weight_TCRF_20201221_054415_K_1-5_Epoch_93.pb"

    /* Const */
    private const val INPUT_NAME = "input_1"
    private const val OUTPUT_NAME = "crf_1/truediv"
    private const val NUM_OF_FRAMES = 480
    private const val NUM_OF_FEATURES = 1280
    private const val NUM_OF_CRF_FEATURE = 84

    @Throws(IOException::class)
    fun create(
        assetManager: AssetManager
    ): CRFClassifier {

        Log.d("LoadModel", "Start Frame Classifier")
        return CRFClassifier(
            INPUT_NAME,
            OUTPUT_NAME,
            NUM_OF_FRAMES,
            NUM_OF_FEATURES,
            NUM_OF_CRF_FEATURE,
            TensorFlowInferenceInterface(assetManager, MODEL_PIPUT_DIR)
        )
    }
}
