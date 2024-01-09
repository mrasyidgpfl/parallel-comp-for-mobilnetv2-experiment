package com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2

import android.app.Activity
import android.graphics.Bitmap
import com.fasilkom.kotlin.sibipenerjemah.service.common.Device
import com.fasilkom.kotlin.sibipenerjemah.service.common.LiteModelType
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.utils.FileUtils
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer

// Needed GPU : https://github.com/tensorflow/tensorflow/issues/24986
// https://www.tensorflow.org/lite/performance/gpu_advanced#supported_ops

abstract class MobileNetV2Classifier {

    /** The loaded TensorFlow Lite model.  */
    private var tfliteModel: MappedByteBuffer? = null

    /** Image size  */
    private val MODEL_INPUT_SIZE = 224

    /** Optional GPU delegate for acceleration.  */
    private var gpuDelegate: GpuDelegate? = null

    /** An instance of the driver class to run model inference with Tensorflow Lite.  */
    protected var tflite: Interpreter? = null

    /** Options for configuring the Interpreter.  */
    private val tfliteOptions =
        Interpreter.Options()

    /** Labels corresponding to the output of the vision model.  */
    private var labels: List<String>? = null

    /** Input image TensorBuffer.  */
    private var inputImageBuffer: TensorImage? = null

    /** Output probability TensorBuffer.  */
    private var outputProbabilityBuffer: TensorBuffer? = null

    /** Processer to apply post processing of the output probability.  */
    private var probabilityProcessor: TensorProcessor? = null

    inner class Recognition {

    }

    constructor(
        activity: Activity,
        device: Device,
        numThreads: Int
    ) {
        val compatList = CompatibilityList()
        tfliteModel = FileUtil.loadMappedFile(activity, getModelPath())
        when (device) {
            Device.GPU -> {
                gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                //gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                tfliteOptions.addDelegate(gpuDelegate)
            }
            Device.CPU -> {
                tfliteOptions.setNumThreads(numThreads)
            }
        }
        tflite = Interpreter(tfliteModel!!, tfliteOptions)
        labels = FileUtils.getLabels(activity.assets, "labels_mobilenetv2.txt")
    }

    fun recognize(bitmap: Bitmap): Array<FloatArray> {
        val result = Array (BATCH_SIZE) { FloatArray(getNumOfClasses()) }

        inputImageBuffer = TensorImage(DataType.FLOAT32)
        inputImageBuffer?.load(bitmap)

        tflite!!.run(inputImageBuffer!!.buffer, result)
        return result
    }

    fun getOutputImage(output: ByteBuffer): Bitmap? {
        output.rewind()
        val outputWidth = MODEL_INPUT_SIZE
        val outputHeight = MODEL_INPUT_SIZE
        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(outputWidth * outputHeight)
        for (i in 0 until outputWidth * outputHeight) {
            val a = 0xFF
            val r = output.float * 255.0f
            val g = output.float * 255.0f
            val b = output.float * 255.0f
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
        return bitmap
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

    /** Gets the name of the model file stored in Assets.  */
    protected abstract fun getModelPath(): String

    /** Gets the name of the label file stored in Assets.  */
    protected abstract fun getLabelPath(): String

    /** Gets num of classes/features */
    protected abstract fun getNumOfClasses(): Int

    companion object {

        private const val BATCH_SIZE = 1 // process only 1 image at a time
        private const val BYTES_PER_CHANNEL = 4 // float size
        private const val PIXEL_SIZE = 3 // rgb
        private const val NUM_OF_THREAD = 4

        private val device = Device.GPU

        fun create(
            activity: Activity,
            modelType: LiteModelType
        ): MobileNetV2Classifier {

            return when (modelType) {
                LiteModelType.Reguler ->  MobileNetV2DefaultClassifier(activity, device, NUM_OF_THREAD)
                LiteModelType.RandomPixel ->  MobileNetV2RPClassifier(activity, device, NUM_OF_THREAD)
            }
        }
    }
}