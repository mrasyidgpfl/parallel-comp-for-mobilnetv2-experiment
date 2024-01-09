package com.fasilkom.kotlin.sibipenerjemah.service.tflite.crf

import android.app.Activity
import com.fasilkom.kotlin.sibipenerjemah.service.common.Device
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf.CRFResult
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf.CRFUtils
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.utils.FileUtils
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2.MobileNetV2Classifier
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.nio.MappedByteBuffer
import java.util.*

class CRFClassifier {

    private val MODEL_CRF_DIR = "protobuf/Weight_CRF_20191218_181608_K_5-5_Epoch_45.pb"
    private val MODEL_CRF_PIPUT_DIR = "protobufv2/Weight_TCRF_84_label_based_test.pb"

    // Default Values
    private val NUM_OF_CRF_FEATURE = 4 // TODO: 4 OR 84

    private val NUM_OF_CLASSES = 84
    private val NUM_OF_CLASSES_EXTRAKSI = 1280

    private var numOfFrames: Int? = null
    private var NUM_OF_MOBNET_FEATURES: Int = 1280

    /** The loaded TensorFlow Lite model.  */
    private var tfliteModel: MappedByteBuffer? = null

    /** Optional GPU delegate for acceleration.  */
    private var gpuDelegate: GpuDelegate? = null

    /** An instance of the driver class to run model inference with Tensorflow Lite.  */
    protected var tflite: Interpreter? = null

    /** Options for configuring the Interpreter.  */
    private val tfliteOptions =
        Interpreter.Options()

    /** Labels corresponding to the output of the vision model.  */
    private var labels: List<String>? = null

    private var outputs: FloatArray? = null

    // Inner Class
    inner class FrameSequence(var mse: Float, var sequence: FloatArray)

    constructor(
        activity: Activity,
        device: Device,
        numThreads: Int,
        numOfFrames: Int
    ) {
        tfliteModel = FileUtil.loadMappedFile(activity, MODEL_CRF_DIR)
        this.numOfFrames = numOfFrames

        when (device) {
            Device.GPU -> {
                gpuDelegate = GpuDelegate()
                tfliteOptions.addDelegate(gpuDelegate)
            }
            Device.CPU -> {
                tfliteOptions.setNumThreads(numThreads)
            }
        }
        tflite = Interpreter(tfliteModel!!, tfliteOptions)
        labels = FileUtils.getLabels(activity.assets, "labels_mobilenetv2.txt")
    }

    fun recognize(inputData: FloatArray): ArrayList<ArrayList<FloatArray>> {
        // 1 = batch size
//        val result = Array(1) { FloatArray(numOfFrames!! * NUM_OF_CRF_FEATURE) } // TODO : Check this <<

        val output = ArrayList<ArrayList<FloatArray>>()
        outputs = FloatArray(numOfFrames!! * NUM_OF_CRF_FEATURE)

        tflite!!.run(inputData, outputs)

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

    private fun concatFrameSequence(frameSequences: Array<FrameSequence?>): FloatArray {

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

    private fun <T> removeFromArray(arr: Array<FrameSequence?>, idx: Int): Array<FrameSequence?> {

        // shifting elements
        for (j in idx until arr.size - 1) {
            arr[j] = arr[j + 1]
        }

        return Arrays.copyOfRange(arr, 0, arr.size - 1)
    }

    companion object {

        fun create(
            activity: Activity,
            device: Device,
            numThreads: Int,
            numOfFrames: Int
        ): CRFClassifier {
            return CRFClassifier(activity, device, numThreads, numOfFrames)
        }
    }
}