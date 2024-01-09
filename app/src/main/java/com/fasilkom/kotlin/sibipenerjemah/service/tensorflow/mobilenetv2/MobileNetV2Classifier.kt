package id.ac.ui.cs.skripsi.misael_jonathan.sibikotlin.Tensorflow.MOBILENETV2

import android.content.res.AssetManager
import android.os.Environment
import android.os.Trace
import android.util.Log
import com.opencsv.CSVWriter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.File
import java.io.FileWriter
import java.nio.FloatBuffer
import java.util.*

class MobileNetV2Classifier(

    // Config values.
    private val inputName: String,
    private val outputName: String,
    private val INPUT_SIZE: Int,
    private val NUM_OF_CHANNEL: Int,
    private val NUM_OF_CLASSES: Int,

    // Unused
    private val labels: List<String>,
    private val tensorFlowInference: ArrayList<TensorFlowInferenceInterface>
) {

    // Pre-allocated buffers.
    private val outputNames: Array<String>

    private fun classifyImage(inputData: FloatArray, inferenceIdx: Int): FloatArray {
        val classifierOutput = FloatArray(1280)
        Trace.beginSection("Start Classifier")

        // Copy the input data to TensorFlow
        Trace.beginSection("Feed")
        tensorFlowInference[inferenceIdx].feed(
            inputName,
            inputData,
            1,
            INPUT_SIZE.toLong(),
            INPUT_SIZE.toLong(),
            NUM_OF_CHANNEL.toLong()
        )
        Trace.endSection()

        // Run the inference call
        Trace.beginSection("Run")
        tensorFlowInference[inferenceIdx].run(outputNames)
        Trace.endSection()

        // Copy the output Tensor back into the output array
        Trace.beginSection("Fetch")
        tensorFlowInference[inferenceIdx].fetch(outputName, classifierOutput)
        Trace.endSection()
        Trace.endSection()

        return classifierOutput
    }

    private fun classifyAllImage(inputData: FloatArray, inferenceIdx: Int, totalFrame: Int): FloatArray {
        val classifierOutput = FloatArray(1280 * totalFrame)
        Log.d("MobileNetV2", "InputSize : ${inputData.size} dan TotalFrame: ${totalFrame}}")
        Trace.beginSection("Start Classifier")

        // Copy the input data to TensorFlow
        Trace.beginSection("Feed")
        tensorFlowInference[inferenceIdx].feed(
            inputName,
            inputData,
            1,
            INPUT_SIZE.toLong(),
            INPUT_SIZE.toLong(),
            NUM_OF_CHANNEL.toLong()
        )
        Trace.endSection()

        // Run the inference call
        Trace.beginSection("Run")
        tensorFlowInference[inferenceIdx].run(outputNames)
        Trace.endSection()

        // Copy the output Tensor back into the output array
        Trace.beginSection("Fetch")
        tensorFlowInference[inferenceIdx].fetch(outputName, classifierOutput)
        Trace.endSection()
        Trace.endSection()

        return classifierOutput
    }

    private fun classifyImageBuffer(inputData: FloatBuffer, inferenceIdx: Int): FloatBuffer {
        val classifierOutput = FloatBuffer.allocate(1280)
        Trace.beginSection("Start Classifier")

        // Copy the input data to TensorFlow
        Trace.beginSection("Feed")
        tensorFlowInference[inferenceIdx].feed(
            inputName,
            inputData,
            1,
            INPUT_SIZE.toLong(),
            INPUT_SIZE.toLong(),
            NUM_OF_CHANNEL.toLong()
        )
        Trace.endSection()

        // Run the inference call
        Trace.beginSection("Run")
        tensorFlowInference[inferenceIdx].run(outputNames)
        Trace.endSection()

        // Copy the output Tensor back into the output array
        Trace.beginSection("Fetch")
        tensorFlowInference[inferenceIdx].fetch(outputName, classifierOutput)
        Trace.endSection()
        Trace.endSection()

        return classifierOutput
    }

    fun predict_Real(
        inputData: List<FloatArray>,
        inferenceIdx: Int
    ): Single<ArrayList<FloatArray>> {
        return Single.create<ArrayList<FloatArray>> { emitter ->

            val result =
                ArrayList<FloatArray>()
            for (frameIdx in inputData.indices) {
                result.add(classifyImage(inputData[frameIdx], inferenceIdx))
            }
            Log.d("MobileNetV2", "Selesai Gan !!!!!!!!")
            emitter.onSuccess(result)
        }
            .subscribeOn(Schedulers.computation())
    }

    fun predictBuffer(
        inputData: List<FloatBuffer>,
        inferenceIdx: Int
    ): Single<ArrayList<FloatBuffer>> {
        return Single.create<ArrayList<FloatBuffer>> { emitter ->

            val result =
                ArrayList<FloatBuffer>()
            for (frameIdx in inputData.indices) {
                result.add(classifyImageBuffer(inputData[frameIdx], inferenceIdx))
            }
            Log.d("MobileNetV2", "Selesai Gan !!!!!!!!")
            emitter.onSuccess(result)
        }
            .subscribeOn(Schedulers.computation())
    }

    fun predictAndExtractDuration(
        inputData: List<FloatArray>,
        inferenceIdx: Int
    ): Single<ArrayList<FloatArray>> {
        return Single.create<ArrayList<FloatArray>> { emitter ->

            val filePath = Environment.getExternalStorageDirectory()
            val dir = File(filePath.absolutePath + "/SIBI/MobileNetV2/old_model/old_model_5.txt")
            val writer = CSVWriter(FileWriter(dir.toString()))
            var startTime: Long
            var endTime: Long

            val result =
                ArrayList<FloatArray>()
            for (frameIdx in inputData.indices) {
                println("Frame : $frameIdx")
                startTime = System.nanoTime()
                result.add(classifyImage(inputData[frameIdx], inferenceIdx))
                endTime = System.nanoTime()

                val duration = endTime - startTime

                writer.writeNext(arrayOf("frame_$frameIdx", "$duration"))
            }
            writer.close()
            Log.d("MobileNetV2", "Selesai Gan !!!!!!!!")
            emitter.onSuccess(result)
        }
            .subscribeOn(Schedulers.computation())
    }

    fun predictAll(
        inputData: FloatArray,
        inferenceIdx: Int,
        totalFrame: Int
    ): Single<FloatArray> {
        return Single.create<FloatArray> { emitter ->
            val result = classifyAllImage(inputData, inferenceIdx, totalFrame)
            emitter.onSuccess(result)
        }
            .subscribeOn(Schedulers.computation())
    }


    fun convertResultToPrimitiveArray(
        result: ArrayList<FloatArray>, frameNumber: Int, numOfClasses: Int
    ): FloatArray {
        val convertedResult = FloatArray(frameNumber * numOfClasses)
        for (i in result.indices) {
            System.arraycopy(
                result[i],
                0,
                convertedResult,
                i * numOfClasses,
                numOfClasses
            )
        }
        return convertedResult
    }

    fun convertBufferResultToPrimitiveArray(
        result: ArrayList<FloatBuffer>, frameNumber: Int, numOfClasses: Int
    ): FloatArray {
        println("Start Convert Buffer -> Array")
        val convertedResult = FloatArray(frameNumber * numOfClasses)
        for (i in result.indices) {
            System.arraycopy(
                result[i].array(),
                0,
                convertedResult,
                i * numOfClasses,
                numOfClasses
            )
        }
        println("Selesai Convert Buffer -> Array")
        return convertedResult
    }

    init {
        // 224
        // 3
        // 1280
        outputNames = arrayOf(outputName)
    }
}