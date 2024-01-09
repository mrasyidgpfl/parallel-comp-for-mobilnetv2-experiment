package com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf

import java.util.*

class CRFResult(
    var sequence: FloatArray?,
    var labelSize: Int
) {

    val roundedSequence: FloatArray
        get() {

            val res = FloatArray(480)
            val con = labelSize-1

            var idx = 0

            var logOutput = ""

            when (labelSize) {
                LABEL_SIZE_4 -> {
                    /* For 4 label (algo widhi, check idx 0 vs 3) */
                    run {
                        var i = 0
                        while (i < sequence!!.size - 1) {
                            res[idx++] = (if (sequence!![i] < sequence!![i + con]) 1 else 0).toFloat()

                            logOutput += res[idx - 1].toInt().toString() + " "
                            i += (con+1)

                        }
                    }
                }
                LABEL_SIZE_84 -> {
                    /* For 84 label (algo piput, argmax all idx)*/
                    run {
                        var i = 0
                        var innerIdx = 0
                        var currMaxVal = -1f
                        var currMaxIdx = -1f

                        while (i < sequence!!.size) {
                            if (sequence!![i] > currMaxVal) {
                                currMaxIdx = innerIdx.toFloat()
                                currMaxVal = sequence!![i]
                            }

                            i++
                            innerIdx++

                            if (innerIdx == 84) {
                                res[idx++] = currMaxIdx
                                currMaxVal = -1f
                                currMaxIdx = -1f
                                innerIdx = 0
                                logOutput += res[idx - 1].toInt().toString() + " "
                            }
                        }
                    }
                }
            }


            println("Output : $logOutput")

            return res
        }

    val packagesOfNonTransition: ArrayList<Package>
        get() {

            val packages = ArrayList<Package>()
            val THRESHOLD = 1 // 1 is the latest threshold (2 nov 2020)

            val targetVal = 1f // possible value : 3f (for 4 label), (dynamic)f (for 84 label)
            var streak = 0

            val roundedSequence = roundedSequence

            /* Two Rule */
            for (i in 1..roundedSequence.size-3) {
                var status = false

                for (j in 1..2) {
                    if (roundedSequence[i-1] == roundedSequence[i+j]) {
                        status = true
                    }
                }

                if (status && roundedSequence[i] == 0.toFloat()
                    && roundedSequence[i] != roundedSequence[i-1]
                ) {
                    roundedSequence[i] = roundedSequence[i-1]
                }
            }


            for (i in roundedSequence.indices) {

                if (roundedSequence[i] != 0f) {
                    streak += 1

                } else {

                    if (streak > THRESHOLD) {
//                        println("masuk package : ${roundedSequence[i-1]}")
                        packages.add(Package(i - streak + 1, streak))
                    }

                    streak = 1
                }
            }

            return packages
        }

    inner class Package(val startIdx: Int, val length: Int)

    companion object {
        private val LABEL_SIZE_4 = 4
        private val LABEL_SIZE_84 = 84
    }
}
