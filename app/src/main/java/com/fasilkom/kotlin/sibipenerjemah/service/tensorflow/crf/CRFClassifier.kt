package com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf

import android.os.Trace

import org.tensorflow.contrib.android.TensorFlowInferenceInterface

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

class CRFClassifier(
    private val inputName: String,
    private val outputName: String,
    private val NUM_OF_FRAMES: Int,
    private val NUM_OF_MOBNET_FEATURES: Int,
    private val NUM_OF_CRF_FEATURE: Int, // label size (can be 4 or 84)
    private val tensorFlowInference: TensorFlowInferenceInterface
) {


    // Pre-allocated buffers.
    private val labels: List<String>? = null
    private var outputs: FloatArray? = null
    private val outputNames: Array<String>

    // Inner Class
    inner class FrameSequence(var mse: Float, var sequence: FloatArray)

    init {
        this.outputNames = arrayOf(outputName)
    }

    fun classify(inputData: FloatArray) {

        outputs = FloatArray(NUM_OF_FRAMES * NUM_OF_CRF_FEATURE)

        Trace.beginSection("Start Classifier")

        // Copy the input data to TensorFlow
        Trace.beginSection("Feed")
        tensorFlowInference.feed(
            inputName,
            inputData,
            1,
            inputData.size / NUM_OF_MOBNET_FEATURES.toLong(),
            NUM_OF_MOBNET_FEATURES.toLong()
        )
        Trace.endSection()

        // Run the inference call
        Trace.beginSection("Run")
        tensorFlowInference.run(outputNames)
        Trace.endSection()

        // Copy the output Tensor back into the output array
        Trace.beginSection("Fetch")
        tensorFlowInference.fetch(outputName, outputs!!)
        Trace.endSection()

        Trace.endSection()
    }

    @Throws(IOException::class)
    fun predict(inputData: FloatArray): ArrayList<ArrayList<FloatArray>> {

        val output = ArrayList<ArrayList<FloatArray>>()

        classify(inputData)

        val res = CRFResult(outputs, NUM_OF_CRF_FEATURE)

        val packages = res.packagesOfNonTransition
        val eachSentencePackage = ArrayList<FloatArray>()

        for (pack in packages) {

            var frameFeature = FloatArray(13 * NUM_OF_MOBNET_FEATURES)
            val flattenFrameData = Arrays.copyOfRange(
                inputData,
                pack.startIdx * NUM_OF_MOBNET_FEATURES,
                (pack.startIdx + pack.length) * NUM_OF_MOBNET_FEATURES
            )


            frameFeature = equalizeFrameLength(inputData, flattenFrameData, pack)

            eachSentencePackage.add(frameFeature)

        }

        output.add(eachSentencePackage)

        return output
    }
    private fun equalizeFrameLength(
        inputData: FloatArray,
        flattenFrameData: FloatArray,
        pack: CRFResult.Package
    ): FloatArray {

        var frameFeature = FloatArray(13 * NUM_OF_MOBNET_FEATURES)

        /* Expand frame length */
        if (pack.length <= 13) {

            val lastFrameData = Arrays.copyOfRange(
                inputData,
                (pack.startIdx + pack.length - 1) * NUM_OF_MOBNET_FEATURES,
                (pack.startIdx + pack.length) * NUM_OF_MOBNET_FEATURES
            )

            System.arraycopy(flattenFrameData, 0, frameFeature, 0, flattenFrameData.size)

            for (frameIdx in pack.length..12) {

                for (featureIdx in 0 until NUM_OF_MOBNET_FEATURES) {

                    frameFeature[frameIdx * NUM_OF_MOBNET_FEATURES + featureIdx] =
                        lastFrameData[featureIdx]
                }

            }
        /* Cut frame length */
        } else {

            var frameSequences = arrayOfNulls<FrameSequence>(pack.length)

            /* Init first frame */
            val firstFrameData =
                flattenFrameData.copyOfRange(0 * NUM_OF_MOBNET_FEATURES, 1 * NUM_OF_MOBNET_FEATURES)
            frameSequences[0] = FrameSequence(java.lang.Float.MAX_VALUE, firstFrameData)


            /* The rest of the frames */
            for (frameIdx in 1 until pack.length) {

                val mse = CRFUtils.customMSE(flattenFrameData, frameIdx, NUM_OF_MOBNET_FEATURES)
                val iterFrameData = Arrays.copyOfRange(
                    flattenFrameData,
                    frameIdx * NUM_OF_MOBNET_FEATURES,
                    (frameIdx + 1) * NUM_OF_MOBNET_FEATURES
                )
                frameSequences[frameIdx] = FrameSequence(mse, iterFrameData)
            }

            /* remove lowest value until frame equalized */
            var removeCount = 0

            while (removeCount < pack.length - 13) {

                var lowestValIdx = 0
                for (frameIdx in 1 until frameSequences.size) {

                    if (frameSequences[frameIdx]?.mse!! < frameSequences[lowestValIdx]?.mse!!) {

                        lowestValIdx = frameIdx
                    }
                }

                frameSequences = removeFromArray<FrameSequence>(frameSequences, lowestValIdx)

                removeCount++
            }

            frameFeature = concatFrameSequence(frameSequences)
            //            Log.d("Equalizing", "frame feature: " + frameFeature.length);

        }

        //        Log.d("Length-nya", "Final: " + frameFeature.length);

        return frameFeature
    }

    fun concatFrameSequence(frameSequences: Array<FrameSequence?>): FloatArray {

        val concatSequence = FloatArray(13 * NUM_OF_MOBNET_FEATURES)

        for (frameIdx in frameSequences.indices) {

            System.arraycopy(
                frameSequences[frameIdx]?.sequence!!,
                0,
                concatSequence,
                frameIdx * NUM_OF_MOBNET_FEATURES,
                NUM_OF_MOBNET_FEATURES
            )
        }

        return concatSequence
    }

    fun <T> removeFromArray(arr: Array<FrameSequence?>, idx: Int): Array<FrameSequence?> {

        // shifting elements
        for (j in idx until arr.size - 1) {
            arr[j] = arr[j + 1]
        }

        return Arrays.copyOfRange(arr, 0, arr.size - 1)
    }
}
