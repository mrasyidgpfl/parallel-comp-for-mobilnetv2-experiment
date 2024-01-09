package com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.lstm

import android.os.Trace
import android.util.Log
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.lang.Float
import java.util.*
import kotlin.Comparator

class LSTMClassifier(

    // Config values.
    private val inputName: String,
    private val outputName: String,
    private val numOfTimeStep: Long,
    private val numOfFeatures: Long,

    // Pre-allocated buffers.
    private val labels: List<String>,
    private val tensorFlowInference: TensorFlowInferenceInterface

) {

    private val inputSize: Int = 0
    private val outputs: FloatArray
    private val outputNames: Array<String>

    private val results: PriorityQueue<LSTMResult>
        get() {
            val outputQueue = createOutputQueue()
            for (i in outputs.indices) {
                outputQueue.add(
                    LSTMResult(
                        labels[i + 1],
                        (i + 1).toString() + " : " + labels[i + 1],
                        outputs[i]
                    )
                )
            }
            return outputQueue
        }

    init {
        val numClasses =
            tensorFlowInference.graph().operation(outputName).output<Int>(0).shape().size(1).toInt()
        println("HELLO : $numClasses")
        this.outputs = FloatArray(numClasses)
        this.outputNames = arrayOf(outputName)
    }

    private fun classifyTextToOutputs(inputData: FloatArray) {

        Trace.beginSection("Start Classifier")

        // Copy the input data to TensorFlow
        Trace.beginSection("Feed")
        tensorFlowInference.feed(inputName, inputData, 1, numOfTimeStep, numOfFeatures)
        Trace.endSection()

        // Run the inference call
        Trace.beginSection("Run")
        tensorFlowInference.run(outputNames)
        Trace.endSection()

        // Copy the output Tensor back into the output array
        Trace.beginSection("Fetch")
        tensorFlowInference.fetch(outputName, outputs)
        Trace.endSection()

        Trace.endSection()
    }

    fun predictWord_Real(inputData: ArrayList<FloatArray>): Array<LSTMResult?> {

        val numOfWord = inputData.size
        Log.d("OutputLSTM", "numOfWord: $numOfWord")

        val output = arrayOfNulls<LSTMResult?>(numOfWord)

        for (wordIdx in inputData.indices) {
            Log.d("OutputLSTM", "Length: " + inputData[wordIdx].size)

            classifyTextToOutputs(inputData[wordIdx])
            output[wordIdx] = results.poll()

            Log.d("OutputLSTM", output[wordIdx]?.result)

        }

        return output
    }

    private fun createOutputQueue(): PriorityQueue<LSTMResult> {
        // Find the best classifications.
        return PriorityQueue<LSTMResult>(
            labels.size,
            Comparator<LSTMResult> { lhs, rhs ->
                // Intentionally reversed to put high confidence at the head of the queue.
                Float.compare(rhs.confidence, lhs.confidence)
            })
    }
}
