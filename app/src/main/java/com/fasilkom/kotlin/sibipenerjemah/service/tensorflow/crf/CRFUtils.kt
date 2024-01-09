package com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf

object CRFUtils {

    fun customMSE(frameFeature: FloatArray, idx: Int, featureSize: Int): Float {

        var sum = 0f

        for (i in 0 until featureSize) {
            val valueOfArrA = frameFeature[idx * i]
            val valueOfArrB = frameFeature[(idx + 1) * i]

            sum += Math.pow((valueOfArrA - valueOfArrB).toDouble(), 2.0).toFloat()
        }

        return sum / featureSize
    }
}
