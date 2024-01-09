package com.fasilkom.kotlin.sibipenerjemah.service.tflite.lstm

import android.app.Activity
import com.fasilkom.kotlin.sibipenerjemah.service.common.Device
import com.fasilkom.kotlin.sibipenerjemah.service.common.LiteModelType
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.lstm.LSTMResult
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.utils.FileUtils
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2.MobileNetV2Classifier
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2.MobileNetV2DefaultClassifier
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2.MobileNetV2RPClassifier
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.lang.Float
import java.nio.MappedByteBuffer
import java.util.*
import kotlin.Comparator

abstract class LSTMClassifier {

    // Pre-allocated buffers.
    private val labels: List<String>

    // Config values.
    private var numOfTimeStep: Long? = null
    private var numOfFeatures: Long? = null

    private val inputSize: Int = 0
    private val outputs: Array<FloatArray>

    /** The loaded TensorFlow Lite model.  */
    private var tfliteModel: MappedByteBuffer? = null

    /** Optional GPU delegate for acceleration.  */
    private var gpuDelegate: GpuDelegate? = null

    /** An instance of the driver class to run model inference with Tensorflow Lite.  */
    protected var tflite: Interpreter? = null

    /** Options for configuring the Interpreter.  */
    private val tfliteOptions =
        Interpreter.Options()

    constructor(
        activity: Activity,
        device: Device,
        numThreads: Int,
        numOfTimeStep: Long,
        numOfFeatures: Long
    ) {
        this.outputs = Array(1) { FloatArray(getNumOfClasses()) }
        this.numOfTimeStep = numOfTimeStep
        this.numOfFeatures = numOfFeatures

        tfliteModel = FileUtil.loadMappedFile(activity, getModelPath())

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
        labels = FileUtils.getLabels(activity.assets, "labels.txt")
    }

    private val results: PriorityQueue<LSTMResult>
        get() {
            val outputQueue = createOutputQueue()
            for (i in outputs[0].indices) {
                outputQueue.add(
                    LSTMResult(
                        labels[i + 1],
                        (i + 1).toString(),
                        outputs[0][i]
                    )
                )
            }
            return outputQueue
        }

    fun recognize(inputData: ArrayList<FloatArray>): Array<LSTMResult?> {

        val numOfWord = inputData.size
//        Log.d("OutputLSTM", "numOfWord: $numOfWord")

        val output = arrayOfNulls<LSTMResult?>(numOfWord)

        for (wordIdx in inputData.indices) {
//            Log.d("OutputLSTM", "Length: " + inputData[wordIdx].size)

            tflite!!.run(inputData[wordIdx], outputs)
            output[wordIdx] = results.poll()

//            Log.d("OutputLSTM", "${output[wordIdx]?.word} : ${output[wordIdx]?.result}")

        }

        return output
    }

    /** Gets the name of the model file stored in Assets.  */
    protected abstract fun getModelPath(): String

    protected abstract fun getNumOfClasses(): Int

    private fun createOutputQueue(): PriorityQueue<LSTMResult> {
        // Find the best classifications.
        return PriorityQueue<LSTMResult>(
            labels.size,
            Comparator<LSTMResult> { lhs, rhs ->
                // Intentionally reversed to put high confidence at the head of the queue.
                Float.compare(rhs.confidence, lhs.confidence)
            })
    }

    fun close() {
        if (tflite != null) {
            tflite!!.close()
            tflite = null
        }
        if (gpuDelegate != null) {
            gpuDelegate!!.close()
            gpuDelegate = null
        }
        tfliteModel = null
    }



    companion object {
        private val NUM_OF_TIME_STEP: Long = 13
        private val NUM_OF_FEATURES: Long = 1280
        private const val NUM_OF_THREAD = 4

        private val device = Device.CPU

        fun create(
            activity: Activity,
            modelType: LiteModelType
        ): LSTMClassifier {

            return when (modelType) {
                LiteModelType.Reguler -> LSTMDefaultClassifier(
                    activity, device, NUM_OF_THREAD, NUM_OF_TIME_STEP, NUM_OF_FEATURES
                )
                LiteModelType.RandomPixel -> LSTMRPClassifier(
                    activity, device, NUM_OF_THREAD, NUM_OF_TIME_STEP, NUM_OF_FEATURES
                )
            }
        }
    }
}