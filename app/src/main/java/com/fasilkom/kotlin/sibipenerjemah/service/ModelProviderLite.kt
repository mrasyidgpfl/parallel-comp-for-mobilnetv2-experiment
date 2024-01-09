package com.fasilkom.kotlin.sibipenerjemah.service

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.TimingLogger
import com.fasilkom.kotlin.sibipenerjemah.service.common.LiteModelType
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf.CRFClassifier
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf.CRFClassifierFactory
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.lstm.LSTMResult
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.lstm.LSTMClassifier
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2.MobileNetV2Classifier
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.utils.Converter
import io.reactivex.rxjava3.core.Observable
//import com.google.firebase.perf.FirebasePerformance
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.util.ArrayList

class ModelProviderLite: ModelProviderInterface {

    private val NUM_OF_CLASSES = 84
    private val NUM_OF_CLASSES_EXTRAKSI = 1280

    private var tfliteMobileNetV2Classifier: MobileNetV2Classifier? = null
    private var tfliteCRFClassifier: com.fasilkom.kotlin.sibipenerjemah.service.tflite.crf.CRFClassifier? = null
    private var tfliteLSTMClassifier: LSTMClassifier? = null

    private var translateListener: ModelProvider.TranslateListener? = null
    private var frameClassifier: CRFClassifier? = null

    private var timingLogger: TimingLogger? = null

    override fun isClassifierLoaded(): Boolean {
        return (tfliteMobileNetV2Classifier != null).and(frameClassifier != null).and(tfliteLSTMClassifier != null)
    }

    /** Load Classifier
     * > CRF is not migratable yet, so TensorFlow-Android still in use only for CRF
     * Classifier :
     * > Classifier.Device.CPU -> 1 - 4 thread(s) allowed
     * > Classifier.Device.GPU -> 1 thread only
     * */
    override fun loadClassifier(context: Context, activity: Activity) {

        /* Change model type to use different models preset.
        * CRF need to be change manually from its factory class */
        val modelType = LiteModelType.Reguler

        tfliteMobileNetV2Classifier = MobileNetV2Classifier.create(activity, modelType)
        frameClassifier = CRFClassifierFactory.create(context.assets) // Not Tflite
        tfliteLSTMClassifier = LSTMClassifier.create(activity, modelType)
    }

    /** Run Classifier
     * > Run Preprocess and MobileNetV2, then CRF and LSTM
     * > Listener only needed for UI update (can be removed) */
    override fun runClassifier(
        videoFile: File,
        listener: ModelProvider.TranslateListener,
        cropSize: OpenCVService.Crop
    ) {
        translateListener = listener
        timingLogger = TimingLogger("ALL", "Start")
        OpenCVService.setMobileNetV2Classifier(tfliteMobileNetV2Classifier!!)

        Log.d("ModelProvider", "Start")

        /* Run all tflite */
        Single.just(OpenCVService.javacvConvertLite(videoFile, cropSize))
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.newThread())
            .subscribe { result ->
                /* result adalah Objek dari Data Class
                   "result" mobilenet dan size-nya didapatkan dari Objek Data Class bernama result ini.
                */
                runRestLiteClassifier(Converter.convertResultToPrimitiveArray(result.result, result.size, NUM_OF_CLASSES_EXTRAKSI))
            }
    }

    /** Run Observable Classifier
     * Run Preprocess and MobileNetV2, then CRF and LSTM.
     * This is the new implementation to replace the runClassifier() function.*/
    override fun runObservableClassifier(
        videoFile: File,
        listener: ModelProvider.TranslateListener,
        cropSize: OpenCVService.Crop
    ): Observable<Pair<String, ArrayList<FloatArray>>> {
        // Map <Percent, <ListInputMobileNetV2>>
        translateListener = listener
        timingLogger = TimingLogger("ALL", "Start")
        OpenCVService.setMobileNetV2Classifier(tfliteMobileNetV2Classifier!!)

        Log.d("ModelProvider", "Start")

        return OpenCVService.javacvObservableConvertion(videoFile, cropSize)
    }

    /** Run Rest Classifier (CRF and LSTM)
     * Run CRF and LSTM, translation will be shown to user by using 'return' or listener */
    @Throws(IOException::class)
    override fun runRestLiteClassifier(mobileNetV2Result: FloatArray): String {
        timingLogger!!.addSplit("End: MobileNetV2")
//        val tracer = FirebasePerformance.getInstance().newTrace("CRF and LSTM")
//        tracer.start()

        /* ==== CRF ==== */
        val crfResults: ArrayList<ArrayList<FloatArray>> =
            frameClassifier!!.predict(
                mobileNetV2Result
            )
        val lstmFinalResult: ArrayList<Array<LSTMResult?>> =
            ArrayList<Array<LSTMResult?>>()

        timingLogger!!.addSplit("End: CRF")

        /* ==== LSTM ==== */
        val result = StringBuilder()
        var previousWord: String? = ""
        for (sentenceIdx in crfResults.indices) {
            val lstmResults: Array<LSTMResult?> =
                tfliteLSTMClassifier!!.recognize(crfResults[sentenceIdx])
            for (q in lstmResults.indices) {
                if (lstmResults.get(q)?.word != previousWord) {
                    Log.d(
                        "Hasil " + (q + 1),
                        ">>" + lstmResults[q]?.result + " - " + lstmResults[q]?.confidence
                    )
                    result.append(lstmResults.get(q)?.word + " ")
                    previousWord = lstmResults.get(q)?.word
                }
            }

            lstmFinalResult.add(lstmResults)
        }
//        tracer.stop()
        timingLogger!!.addSplit("End: LSTM")
        timingLogger!!.dumpToLog()

        Runtime.getRuntime().gc()
        return result.toString()
    }

    fun close() {
        tfliteMobileNetV2Classifier?.close()
        tfliteLSTMClassifier?.close()
    }
}