package com.fasilkom.kotlin.sibipenerjemah.service

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.os.Environment
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
import com.opencsv.CSVWriter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.*
import java.util.*

import com.fasilkom.kotlin.sibipenerjemah.utils.WordSequenceAligner
import kotlin.math.pow

/** ModelProviderDummy
 * Only use this class to run datatest. Do not forget to change the intent to "ResultActivity" in
 * SibiFragment.kt. */
class TestingModelProvider: ModelProviderInterface {

    private val actualLabel: HashMap<String, String> = hashMapOf(
    "1. Siapa Nama Mu" to "72 54 52",
    "2. Dimana Alamat Rumahmu" to "22 47 2 63 52",
    "3. Dimana Sekolahmu" to "22 47 69 52",
    "4. Bolehkah Saya Minta Nomor Teleponmu" to "16 35 65 49 56 76 52",
    "5. Film Apa Yang Sedang Di Putar" to "25 5 83 68 23 62",
    "6. Jam Berapa Film Ini Di Putar" to "33 14 25 31 23 62",
    "7. Berapa Harga Karcis Film Ini" to "14 26 37 25 31",
    "8. Dimana Film Ini Di Putar" to "22 47 25 31 23 62",
    "9. Apa Nama Sayuran Itu" to "5 54 66 4 32",
    "10. Berapa Harga Sayuran Itu" to "14 26 66 4 32",
    "11. Apakah Harga Sayuran Ini Boleh Ditawar" to "5 35 26 66 4 31 16 23 75",
    "12. Berapa Jumlah Yang Harus Saya Bayar" to "14 34 83 27 65 12",
    "13. Kami Ingin Pergi ke Kota Tua, Naik Bis Apa" to "36 30 60 38 42 77 53 18 5",
    "14. Berapa Harga Karcis Yang Harus Saya Bayar" to "14 26 37 83 27 65 12",
    "15. Kami Harus Turun Dimana" to "36 27 78 22 47",
    "16. Adakah Cara Lain Kita Pergi Ke Kota Tua" to "1 35 19 44 41 60 38 42 77",
    "17. Saya Ingin Membuka Tabungan, Bagaimana Caranya" to "65 30 48 17 73 4 7 19 57",
    "18. Bagaimana Cara Menabung" to "7 19 48 73",
    "19. Di mana Kami Bisa Mengambil Tabungan" to "22 47 36 15 48 3 73 4",
    "20. Bagaimana Cara Mengirim Uang Melalui Bank" to "7 19 48 40 79 45 28 9",
    "21. Selamat Natal dan Tahun Baru" to "70 55 20 74 10",
    "22. Selamat Idul Fitri, Mohon Maaf Lahir dan Batin" to "70 29 51 46 43 20 11",
    "23. Selamat Ulang Tahun" to "70 80 74",
    "24. Semoga Panjang Umur" to "67 50 59 82",
    "25. Saya Sering Sakit Kepala, Saya Harus Periksa ke Bagian Mana" to "65 71 64 39 65 27 61 38 8 4 47",
    "26. Saya Ingin Ke Dokter Umum, Siapa Nama Dokternya" to "65 30 38 24 81 72 54 57",
    "27. Jam Berapa Dokter Datang" to "33 14 24 21",
    "28. Di Apotek Mana Obat ini Bisa Dibeli" to "22 6 47 58 31 15 23 13"
    )

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

    val filePath = Environment.getExternalStorageDirectory()
    val sibiVideoDir = filePath.getAbsolutePath() + "/SIBI/Video"
    // val labelDir = filePath.getAbsolutePath() + "/SIBI/Dataset_Video_Shuffle_K_2.txt"

    /* CSVWriter */
    // private var mwriter : CSVWriter? = null
    // private var cwriter : CSVWriter? = null
    // private var lwriter : CSVWriter? = null
    private var awriter : CSVWriter? = null

    var cStartTime: Long = 0
    var cEndTime: Long = 0
    var lStartTime: Long = 0
    var lEndTime: Long = 0

    var allProcessStartTime: Long = 0
    var allProcessEndTime: Long = 0

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

        /* Please check lagi modelnya setelah pakai random pixel */
        tfliteMobileNetV2Classifier = MobileNetV2Classifier.create(activity, modelType)
        frameClassifier = CRFClassifierFactory.create(context.assets) // Not Tflite
        tfliteLSTMClassifier = LSTMClassifier.create(activity, modelType)
    }

    override fun runClassifier(
            videoFile: File,
            listener: ModelProvider.TranslateListener,
            cropSize: OpenCVService.Crop
    ) {
        OpenCVService.setMobileNetV2Classifier(tfliteMobileNetV2Classifier!!)
        translateListener = listener
    }

    override fun runObservableClassifier(
            videoFile: File,
            listener: ModelProvider.TranslateListener,
            cropSize: OpenCVService.Crop
    ): Observable<Pair<String, ArrayList<FloatArray>>> {
        TODO("Not yet implemented")
    }

    override fun runRestLiteClassifier(mobileNetV2Result: FloatArray): String {
        TODO("Not yet implemented")
    }

    /* This is the main function to run the classifier and loop through all datatest */
    fun runClassifierObservable(
            assetManager: AssetManager,
            videoFile: File,
            cropSize: OpenCVService.Crop
    ): Observable<String> {
        OpenCVService.setMobileNetV2Classifier(tfliteMobileNetV2Classifier!!)

        /* Create File */
        val timeLogDir = "/SIBI/TimeLog"
        //val mfileDir = filePath.getAbsolutePath() + "$timeLogDir/mobilenet.csv"
        //val cfileDir = filePath.getAbsolutePath() + "$timeLogDir/crf.csv"
        //val lfileDir = filePath.getAbsolutePath() + "$timeLogDir/lstm.csv"
        // All
        val afileDir = filePath.getAbsolutePath() + "$timeLogDir/CPU8Battery.csv"
        val file = File(filePath.getAbsolutePath() + timeLogDir)
        if (!file.exists()) file.mkdir()

        /* Initialize CSVWriter */
        //mwriter = CSVWriter(FileWriter(mfileDir))
        //cwriter = CSVWriter(FileWriter(cfileDir))
        //lwriter = CSVWriter(FileWriter(lfileDir))
        awriter = CSVWriter(FileWriter(afileDir))
        allProcessStartTime = System.nanoTime()

        // mwriter!!.writeNext(arrayOf<String>("kalimat","time"))
        // cwriter!!.writeNext(arrayOf<String>("kalimat","time"))
        // lwriter!!.writeNext(arrayOf<String>("kalimat","time"))
        awriter!!.writeNext(arrayOf<String>("video","kalimat","ref_labels","hyp_labels","accuracy","error_rate","preprocess", "mobilenet", "total_frame", "crf", "lstm", "total", "check"))

        var namedThread = Schedulers.newThread()

        /* Read All Video Test */
        var dir = File(sibiVideoDir)
        var videos = dir.listFiles()
        var idx = 1

        println("Start Running Fold 1")

        return Observable.create (ObservableOnSubscribe<String> { emitter ->

            println("Reader Excecuted")

            /* ======= Reading Data From Fold ======= */
//            val DATA_FOLD = "Dataset_Video_Shuffle_K_1.txt"
//            val br = BufferedReader(InputStreamReader(assetManager.open(DATA_FOLD)))
//            var line = br.readLine()
//            var idx = 0
//
//            while (line != null) {
//
//                if (idx > 1) {
//                    break
//                }
//
//                try {
//                    val videoFileDir = sibiVideoDir + line + ".mp4"
////                    println("Kalimat : $videoFileDir")
//
//                    mStartTime = System.nanoTime()
//                    val result = OpenCVService.javacvConvertLite(File(videoFileDir), cropSize)
//                    mEndTime = System.nanoTime()
//                    val mDuration = mEndTime - mStartTime
//                    mwriter!!.writeNext(arrayOf(line, mDuration.toString()))
//
//                    runRestLiteClassifier(
//                        line,
//                        Converter.convertResultToPrimitiveArray(
//                            result,
//                            result.size,
//                            NUM_OF_CLASSES_EXTRAKSI
//                        )
//                    )®
//
//                    emitter.onNext("On Working : ($idx/???)")
//
//                    idx++
//                    line = br.readLine()
//
//                } catch (e: FileNotFoundException) {
//                    emitter.onError(e)
//                }
//
//            }
//            emitter.onComplete()
//
//            awriter!!.close()
//            mwriter!!.close()
//            cwriter!!.close()
//            lwriter!!.close()

            /* ======= Reading All Video Files From Folder =======*/
            for (file in videos) {
                Log.d("size_vids", videos.size.toString())
                try {
                    /* Di bawah ini val dari result, pDuration, dan mDuration dari Data Class
                       Perubahan berpengaruh ke runClassifier di ModelProviderLite, jadi menggunakan Data Class.
                       Result dan Size disimpan pada/dalam Objek Data Class.
                     */
                    var checkerStart = System.nanoTime()
                    val (result, pDuration, mDuration, totalFrame) = OpenCVService.javacvConvertLite(file, cropSize)
                    // mwriter!!.writeNext(arrayOf(file.name, mDuration.toString()))
                    var checkerEnd = System.nanoTime()
                    var checkerDuration = checkerEnd - checkerStart

                    runRestLiteClassifier(
                            file.name,
                            Converter.convertResultToPrimitiveArray(
                                    result,
                                    result.size,
                                    NUM_OF_CLASSES_EXTRAKSI
                            ),
                            mDuration, pDuration, totalFrame, checkerDuration
                    )

                    println("Reading each file")
                    Log.d("reading_tag", "Baca videos per file")

                    emitter.onNext("On Working : ($idx/${videos.size})")
                    Log.d("waktu_mobilenet", mDuration.toString())

                    var mDurationInMs = mDuration / 10.0.pow(9.0)
                    var sMDurationInMs = "$mDurationInMs ms"

                    println("Waktu_Mobilenet $sMDurationInMs")
                } catch (e: FileNotFoundException) {
                    emitter.onError(e)
                }

            }
            emitter.onComplete()

            allProcessEndTime = System.nanoTime()

            val allProcessTime = (allProcessEndTime - allProcessStartTime) / 60000000000

            awriter!!.writeNext(arrayOf<String>("Total Time for this batch: ", allProcessTime.toString()))
            awriter!!.close()
            // mwriter!!.close()
            // cwriter!!.close()
            // lwriter!!.close()
        })
    }

    @Throws(IOException::class)
    private fun runRestLiteClassifier(it: String, mobileNetV2Result: FloatArray, mDuration: Long, pDuration: Long, totalFrame: Int, cTDuration: Long) {

        cStartTime = System.nanoTime()
        /* ==== CRF ==== */
        val crfResults: ArrayList<ArrayList<FloatArray>> =
                frameClassifier!!.predict(
                        mobileNetV2Result
                )
        cEndTime = System.nanoTime()
        val cDuration = cEndTime - cStartTime
        // cwriter!!.writeNext(arrayOf(it, cDuration.toString()))

        val lstmFinalResult: ArrayList<Array<LSTMResult?>> =
                ArrayList<Array<LSTMResult?>>()


        lStartTime = System.nanoTime()
        /* ==== LSTM ==== */
        val result = StringBuilder()
        Log.d("result", result.toString())

        var previousWord: String? = ""

        for (sentenceIdx in crfResults.indices) {
            val lstmResults: Array<LSTMResult?> =
                    tfliteLSTMClassifier!!.recognize(crfResults[sentenceIdx])
            for (q in lstmResults.indices) {
                if (lstmResults.get(q)?.word != previousWord) {
                    Log.d(
                            "Hasil " + (q + 1),
                            ">> ${lstmResults[q]?.word} : " + lstmResults[q]?.result + " - " + lstmResults[q]?.confidence
                    )
                    result.append(lstmResults.get(q)?.result + " ")
                    previousWord = lstmResults.get(q)?.word
                }
            }

            lstmFinalResult.add(lstmResults)
        }

        lEndTime = System.nanoTime()
        val lDuration = lEndTime - lStartTime
        // lwriter!!.writeNext(arrayOf(it, lDuration.toString()))

        /* Accuracy */
//        var kalimat = it.split("/")[1]

        var kalimat = it  // TODO: ni cuma buat test

        //println("actual label : ${actualLabel[kalimat]}")
        //println(kalimat)
        //println("predicted label : $result")

        Log.d("Kalimat", kalimat)

        var predictedLabel = result.trim().split(" ")
        var label = actualLabel[kalimat]!!.trim().split(" ")

        var aligner = WordSequenceAligner()
        val ser = aligner.sentenceAccuracy(label.toString(), predictedLabel.toString())
        val wer = aligner.wordErrorRate(label.toString(), predictedLabel.toString())
        println("SER $ser")
        println("WER $wer")

        var pDurationInMs = pDuration / 10.0.pow(9.0)
        var sPDurationInMs = "$pDurationInMs s"

        var mDurationInMs = mDuration / 10.0.pow(9.0)
        var sMDurationInMs = "$mDurationInMs s"

        var cRFDurationInMs = cDuration / 10.0.pow(9.0)
        var sCRFDurationInMs = "$cRFDurationInMs s"

        var lstmDurationInMs = lDuration / 10.0.pow(9.0)
        var sLSTMDurationInMs = "$lstmDurationInMs s"

        var cTDuration = cTDuration / 10.0.pow(9.0)

        var totalTimeInMs = pDurationInMs + mDurationInMs + cRFDurationInMs + lstmDurationInMs
        var sTotalTimeInMs = "$totalTimeInMs s"

        var checkerTimeInMs = cTDuration + cRFDurationInMs + lstmDurationInMs
        var sCheckerDuration = "$checkerTimeInMs s"

        awriter!!.writeNext(arrayOf<String>(it, totalFrame.toString(), kalimat, result.toString(), actualLabel[kalimat]!!, ser, wer, sPDurationInMs, sMDurationInMs, sCRFDurationInMs, sLSTMDurationInMs, sTotalTimeInMs, sCheckerDuration))

        //awriter!!.writeNext(arrayOf<String>(it,result.toString()))

        //Runtime.getRuntime().gc()
        //translateListener!!.onClassifyDoneShowTranslation(result.toString())

    }

    /*
    fun calculateAccuracy(predictedLabel: List<String>, actualLabel: List<String>): Int {

        val totalKata = actualLabel.size
        var i = 0
        var akurasi = 0

        while (i < totalKata && i < predictedLabel.size) {
            if (predictedLabel[i].equals(actualLabel[i])) {
                akurasi += 1
            } else {
                if (i+1 < totalKata && predictedLabel[i].equals(actualLabel[i+1])) {
                    akurasi += 1
                }
            }
            i++
        }

        return akurasi
    }
    */

    fun close() {
        tfliteMobileNetV2Classifier?.close()
        tfliteLSTMClassifier?.close()
    }
}

