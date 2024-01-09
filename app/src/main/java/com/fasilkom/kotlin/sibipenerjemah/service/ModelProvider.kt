package com.fasilkom.kotlin.sibipenerjemah.service

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.util.Log
import android.util.TimingLogger
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf.CRFClassifier
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.crf.CRFClassifierFactory
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.lstm.LSTMClassifier
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.lstm.LSTMClassifierFactory
import com.fasilkom.kotlin.sibipenerjemah.service.tensorflow.lstm.LSTMResult
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.utils.Converter.convertByteToFloat
import com.opencsv.CSVWriter
import id.ac.ui.cs.skripsi.misael_jonathan.sibikotlin.Tensorflow.MOBILENETV2.MobileNetV2Classifier
import id.ac.ui.cs.skripsi.misael_jonathan.sibikotlin.Tensorflow.MOBILENETV2.MobileNetV2Factory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

/** Tensorflow-Android Implementation
 * Soon to be replaced by Tensorflow-Lite, but Conditional Random Field (CRF) model is not
 * migratable. There'll be partial implementation of Tensorflow-Android in Tensorflow-Lite
 * Created by : Misael Jonathan */

class ModelProvider : ModelProviderInterface{

    private val NUM_OF_CLASSES = 84
    private val NUM_OF_CLASSES_EXTRAKSI = 1280

    private var translateListener: TranslateListener? = null
    private var frameClassifier: CRFClassifier? = null

    private var imageClassifier: MobileNetV2Classifier? = null
    private var textClassifier: LSTMClassifier? = null
    private var timingLogger: TimingLogger? = null

    /* Default */
    private val MODEL_MOBILENETV2_DIR = "protobuf/Weight_MobileNet_V2_20191020_054829_K_1-5_Epoch_50.pb"
    private val MODEL_CRF_DIR = "protobuf/Weight_CRF_20191218_181608_K_5-5_Epoch_45.pb"
    private val MODEL_LSTM_DIR = "protobuf/Weight_LSTM_P1_20191104_210250_K_2-5_Epoch_150.pb"

    /* 84 label, with majority vote rule */
    private val MODEL_MOBILENETV2_PIPUT_DIR = "protobufv2/Weight_MobileNet_V2_20191020_054829_K_1-5_Epoch_50.pb"
    private val MODEL_CRF_PIPUT_DIR = "protobufv2/Weight_TCRF_84_label_based_test.pb"
    private val MODEL_LSTM_PIPUT_DIR = "protobufv2/Weight_LSTM.pb"

    /* Mobilenet normalization config*/
    private val MODEL_MOBILENETV2_INPUT_NORMALIZED = "protobuftest/Weight_MobileNet_V2_20200924_041343_K_1-5_Epoch_1_input_normalize.pb"
    private val MODEL_MOBILENETV2_WITH_NORMALIZE = "protobuftest/Weight_MobileNet_V2_20200924_092450_K_2-5_Epoch_5_with_normalize.pb"


    /* TensorFlow-Android */
    @Throws(IOException::class)
    override fun loadClassifier(
        context: Context,
        activity: Activity
    ) {
        imageClassifier = MobileNetV2Factory.create(context.assets)
        frameClassifier = CRFClassifierFactory.create(context.assets)
        textClassifier = LSTMClassifierFactory.create(context.assets)
        Log.d("ModelProvider", "Model Loaded")
    }


    @Throws(IOException::class)
    override fun runClassifier(
        videoFile: File,
        listener: TranslateListener,
        cropSize: OpenCVService.Crop
    ) {
        translateListener = listener
        timingLogger = TimingLogger("ALL", "Start")

        Log.d("ModelProvider", "Start")

        /* Run classifier using TensorFlow-Android */
        Single.just(OpenCVService.javacvConvert(videoFile, cropSize))
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.newThread())
            .subscribe { result ->
//                Converter.convertResultToPrimitiveArray(result, result.size, NUM_OF_CLASSES_EXTRAKSI)
//                timingLogger!!.addSplit("End: MobileNetV2")
//                translateListener!!.onClassifyDoneShowTranslation(result.toString())
//                writeToExternalFile(result)
                runMobileNetV2InParallel(convertByteToFloat(result))
            }

        timingLogger!!.addSplit("All Done")
        timingLogger!!.dumpToLog()
    }

    override fun runObservableClassifier(
        videoFile: File,
        listener: TranslateListener,
        cropSize: OpenCVService.Crop
    ): Observable<Pair<String, ArrayList<FloatArray>>> {
        TODO("Not yet implemented")
    }

    override fun runRestLiteClassifier(mobileNetV2Result: FloatArray): String {
        TODO("Not yet implemented")
    }

    override fun isClassifierLoaded(): Boolean {
        return (imageClassifier != null).and(textClassifier != null).and(frameClassifier != null)
    }

    private fun runMobileNetV2InSequence(inputData: ArrayList<FloatArray>) {
        Log.d("ModelProvider", "Start MobileNetV2 in sequence")
        imageClassifier?.predictAndExtractDuration(inputData, 0)
            ?.subscribeOn(Schedulers.computation())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeWith(
                object : DisposableSingleObserver<ArrayList<FloatArray>>() {
                    override fun onError(e: Throwable?) {
                        Log.d("ModelProvider", "MobileNetV2 Failed")
                    }

                    override fun onSuccess(t: ArrayList<FloatArray>) {
//                        writeToExternalFile(t)
                        runRestClassifier(imageClassifier!!.convertResultToPrimitiveArray(t, t.size, 1280))
//                        translateListener!!.onClassifyDoneShowTranslation("Dummy")
                    }
                }
            )
    }


    private fun runMobileNetV2InParallel(inputData: ArrayList<FloatArray>) {
        timingLogger!!.addSplit("End: Preprocess")
        Log.d("ModelProvider", "Start MobileNetV2")

        Single.zip(
            imageClassifier?.predict_Real(inputData.subList(0, inputData.size / 2), 0),
            imageClassifier?.predict_Real(inputData.subList(inputData.size / 2, inputData.size),1),
            BiFunction<ArrayList<FloatArray>, ArrayList<FloatArray>, FloatArray> { floats, floats2 ->
                floats.addAll(floats2)
                imageClassifier?.convertResultToPrimitiveArray(floats, floats.size, 1280)
            }
        )
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(
                object : DisposableSingleObserver<FloatArray>() {
                    override fun onSuccess(dataOutput: FloatArray) {
                        for (i in 0..100) {
                            println("mob $i : ${dataOutput[i]}")
                        }
                        runRestClassifier(dataOutput)
                    }

                    override fun onError(@NonNull e: Throwable) {

                    }
                }
            )
    }

    private fun runMobileNetV2AllImage(inputData: ArrayList<FloatArray>) {
        timingLogger!!.addSplit("End: Preprocess")

        var allInputData = inputData[0]

        for (i in 1..inputData.size-1) {
            allInputData = allInputData.plus(inputData[i])
        }

        imageClassifier!!.predictAll(allInputData, 0, inputData.size)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(
                object : DisposableSingleObserver<FloatArray>() {
                    override fun onSuccess(t: FloatArray) {
                        runRestClassifier(t)
                    }

                    override fun onError(e: Throwable?) {

                    }
                }
            )
    }


    private fun writeToExternalFile(data: ArrayList<FloatArray>) {

        val filePath = Environment.getExternalStorageDirectory()
        val dir = File(filePath.absolutePath + "/SIBI/MobileNetV2_Tflite.txt")
        val writer = CSVWriter(FileWriter(dir.toString()))
        for (i in 0 .. data.size-1) {
            for ( j in 0 .. data.get(i).size-1) {
                writer.writeNext(arrayOf("idx_${i+1}", "${data.get(i)[j]}"))
            }
        }

        writer.close()
    }

    @Throws(IOException::class)
    private fun runRestClassifier(mobileNetV2Result: FloatArray) {
        Log.d("ModelProvider", "Start Rest Classifier")
        timingLogger!!.addSplit("End: MobileNetV2")
        /* ==== CRF ==== */
        val crfResults: ArrayList<ArrayList<FloatArray>> =
            frameClassifier!!.predict(
                mobileNetV2Result
            )
        val lstmFinalResult: ArrayList<Array<LSTMResult?>> =
            ArrayList<Array<LSTMResult?>>()

        timingLogger!!.addSplit("End: CRF")
        Log.d("ModelProvider", "Start End CRF")

        /* ==== LSTM ==== */
        val result = StringBuilder()
        for (sentenceIdx in crfResults.indices) {
            val lstmResults: Array<LSTMResult?> =
                textClassifier!!.predictWord_Real(crfResults[sentenceIdx])
            for (q in lstmResults.indices) {
//                Log.d(
//                    "Hasil " + (q + 1),
//                    ">>" + lstmResults[q]?.result + " - " + lstmResults[q]?.confidence
//                )
                result.append(lstmResults.get(q)?.word + " ")
            }
            lstmFinalResult.add(lstmResults)
        }
        Log.d("ModelProvider", "Start End LSTM")
        timingLogger!!.addSplit("End: LSTM")
        timingLogger!!.dumpToLog()

        Runtime.getRuntime().gc()
        translateListener!!.onClassifyDoneShowTranslation(result.toString())
    }


    interface TranslateListener {
        fun onClassifyDoneShowTranslation(translation: String?)
    }

    init {
        Log.d("ModelProvider", "======= Instance Created")
    }

    companion object {
        val TAG = "ModelProvider"
    }
}
