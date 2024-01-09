package com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.utils

import android.content.res.AssetManager

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList

object FileUtils {

    @Throws(IOException::class)
    fun getLabels(assetManager: AssetManager, fileName: String): List<String> {

        val labels = ArrayList<String>()
        val br = BufferedReader(InputStreamReader(assetManager.open(fileName)))

        var line: String

        while (true) {
            line = br.readLine() ?: break
            labels.add(line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        }

        br.close()

        return labels
    }

    @Throws(IOException::class)
    fun getInputData(assetManager: AssetManager, fileName: String): FloatArray {

        val br = BufferedReader(InputStreamReader(assetManager.open(fileName)))

        var line: String

        val token = ArrayList<Float>()

        while (true) {

            line = br.readLine() ?: break
            token.add(java.lang.Float.valueOf(line))
        }

        return convertFloats(token)
    }

    fun convertFloats(floats: List<Float>) //CHECK
            : FloatArray {
        val ret = FloatArray(floats.size)
        val iterator = floats.iterator()
        for (i in ret.indices) {
            ret[i] = iterator.next()
        }
        return ret
    }
}
