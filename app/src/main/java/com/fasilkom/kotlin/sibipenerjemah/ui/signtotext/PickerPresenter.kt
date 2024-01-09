package com.fasilkom.kotlin.sibipenerjemah.ui.signtotext

import android.util.Log
import com.fasilkom.kotlin.sibipenerjemah.network.dummy.DummyProvider
import com.fasilkom.kotlin.sibipenerjemah.network.dummy.TranslatorResponse
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProvider
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProviderInterface
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException


class PickerPresenter(
    val view: PickerActivity,
    val modelProvider: ModelProviderInterface
) : PickerContract.Presenter{

    val TAG = "PickerPresenter"

    override fun loadClassifier() {
        Log.d("PickerPresenter", "Yeah Loaded Boy!")
        modelProvider.loadClassifier(view.applicationContext, view)
    }

    override fun executeOnlineClassifier() {
        DummyProvider().services.getTranslateResult().enqueue(object : Callback<TranslatorResponse> {
            override fun onResponse(
                call: Call<TranslatorResponse>,
                response: Response<TranslatorResponse>
            ) {
                if (response.code() == 200) {
                    response.body()?.translationResult?.let {
                        Log.d(TAG, "Result: " + it)
                    }
                } else {
                    Log.d(TAG, "response != 200")
                }
            }

            override fun onFailure(call: Call<TranslatorResponse>, t: Throwable) {
                Log.d(TAG, "Failed: " + t.message)
            }
        })
    }

    override fun executeOfflineClassifier(videoFile: File, listener: ModelProvider.TranslateListener) {
        try {
            Log.d("PickerPresenter", "Filename : " + videoFile)
            modelProvider.runClassifier(videoFile, listener, OpenCVService.Crop(20, 20 , 20, 20)) // DELETE CROPSIZE
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
