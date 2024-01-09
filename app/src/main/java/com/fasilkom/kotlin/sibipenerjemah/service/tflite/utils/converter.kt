package com.fasilkom.kotlin.sibipenerjemah.service.tflite.utils

import java.nio.FloatBuffer
import java.util.ArrayList

object Converter {
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

    fun convertByteToFloat(arr: ArrayList<ByteArray>): ArrayList<FloatArray> {
        val result = ArrayList<FloatArray>()
        for (layer in arr) {
            val temp = FloatArray(224 * 224 * 3)
            for (i in layer.indices) {
                temp[i] = (layer[i].toInt() and 0xff).toFloat()
            }
            result.add(temp)
        }
        return result
    }

    fun convertByteToFloatBuffer(arr: ArrayList<ByteArray>): ArrayList<FloatBuffer> {
        val result = ArrayList<FloatBuffer>()
        for (layer in arr) {
            val temp = FloatBuffer.allocate(224*224*3)
            for (i in layer.indices) {
                temp.put((layer[i].toInt() and 0xff).toFloat())
            }

            temp.rewind()

            result.add(temp)
        }

        return result
    }
}