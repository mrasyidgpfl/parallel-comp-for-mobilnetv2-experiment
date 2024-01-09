package com.fasilkom.kotlin.sibipenerjemah.ui.camera

import android.util.Log
import com.fasilkom.kotlin.sibipenerjemah.network.fileupload.VideoUploadProvider
import com.fasilkom.kotlin.sibipenerjemah.network.fileupload.VideoUploadResponse
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProvider
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProviderInterface
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProviderLite
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.utils.Converter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class CameraPresenter(
    val view: CameraActivity,
    override var modelProvider: ModelProviderInterface
) : CameraContract.Presenter {

    override fun loadClassifier() {
        modelProvider.loadClassifier(view.applicationContext, view)
    }

    override fun executeOnlineClassifier(
        body: MultipartBody.Part,
        cropSize: OpenCVService.Crop
    ) {

        /* ----- Dummy ----- */
//        TranslatorProvider().services.getTranslateResult().enqueue(object :
//            Callback<TranslatorResponse> {
//            override fun onResponse(
//                call: Call<TranslatorResponse>,
//                response: Response<TranslatorResponse>
//            ) {
//                if (response.code() == 200) {
//                    response.body().translationResult.let {
//                        Log.d(TAG, "Result: " + "aa")
//                    }
//                } else {
//                    Log.d(TAG, "response != 200")
//                }
//            }
//
//            override fun onFailure(call: Call<TranslatorResponse>, t: Throwable) {
////                Log.d(TAG, "Failed: " + t.message)
//            }
//        })

        /* ----- Real Server -----*/
        val cropAxis = createPartFromString(cropSize.getJoined())
        VideoUploadProvider().services.onFileUpload(cropAxis, body).enqueue(object : Callback<VideoUploadResponse> {
            override fun onResponse(
                call: Call<VideoUploadResponse>,
                response: Response<VideoUploadResponse>
            ) {
                if (response.code() == 200) {
                    response.body()?.translationResult.let {
                        Log.d(TAG, "Result: " + it)
                    }
                } else {
                    Log.d(TAG, "response != 200")
                }
            }

            override fun onFailure(call: Call<VideoUploadResponse>, t: Throwable) {
                Log.d(TAG, "Failed: " + t.message)
            }
        })

    }

    private fun createPartFromString(param: String): RequestBody{
        return RequestBody.create(MediaType.parse("multipart/form-data"), param)
    }

    override fun executeOfflineClassifier(
        videoFile: File,
        listener: ModelProvider.TranslateListener,
        cropSize: OpenCVService.Crop
    ) {

        modelProvider.runObservableClassifier(videoFile, listener, cropSize)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<Pair<String, ArrayList<FloatArray>>>() {
                override fun onNext(t: Pair<String, ArrayList<FloatArray>>?) {
                    if (t?.first == "999") {
                        val data = t.second
                        val result = modelProvider.runRestLiteClassifier(
                            Converter.convertResultToPrimitiveArray(data, data.size, 1280)
                        )
//                        view.onTranslationShowProgressPercentage("Menerjemahkan : 100%")
                        view.onTranslationShowProgressResult(result)
                    } else {
                        view.onTranslationShowProgressPercentage(t!!.first)
                    }
                }

                override fun onError(e: Throwable?) {
                    TODO("Not yet implemented")
                }

                override fun onComplete() {
                    // Do Nothing
                }
            })

        return
    }

    override fun closeClassifier() {
        if (modelProvider is ModelProviderLite) {
            (modelProvider as ModelProviderLite).close()
        }
    }

    companion object {
        private val TAG = "CameraPresenter"
    }
}