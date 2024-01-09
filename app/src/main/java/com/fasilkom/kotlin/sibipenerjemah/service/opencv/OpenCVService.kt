package com.fasilkom.kotlin.sibipenerjemah.service.opencv

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import com.fasilkom.kotlin.sibipenerjemah.service.tflite.mobilenetv2.MobileNetV2Classifier
//import com.google.firebase.crashlytics.FirebaseCrashlytics
//import com.google.firebase.perf.FirebasePerformance
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.*
import java.lang.Exception
import java.util.*
import io.reactivex.rxjava3.core.Observable


object OpenCVService {

    /* Skin Mask untuk remove background */
    private val lower = Scalar(30.0, 60.0, 60.0)
    private val upper = Scalar(90.0, 255.0, 255.0)
    private val lower2 = Scalar(0.0, 48.0, 80.0)
    private val upper2 = Scalar(20.0, 255.0, 255.0)

    /* Template Matrix */
    private var hsv: Mat? = null
    private var skinMask: Mat? = null
    private var whiteMatrix: Mat? = null

    /* Params */
    private val anchor = Point((-1).toDouble(), (-1).toDouble())
    private val kernelEllipseOf3 = Imgproc.getStructuringElement(
        Imgproc.MORPH_ELLIPSE,
        Size(3.0, 3.0)
    )
    private val kernelEllipseOf5 = Imgproc.getStructuringElement(
        Imgproc.MORPH_ELLIPSE,
        Size(5.0, 5.0)
    )
    private val OUTPUT_SIZE = Size(224.0, 224.0)

    private lateinit var dir: File
    private lateinit var frameDir: File

    private lateinit var mobileNetV2Classifier: MobileNetV2Classifier

    fun setMobileNetV2Classifier(mobileNetV2Classifier: MobileNetV2Classifier) {
        this.mobileNetV2Classifier = mobileNetV2Classifier
    }

    @Throws(IOException::class)
    fun javacvConvert(filename: File, cropSize: Crop): ArrayList<ByteArray> {

        val filePath = Environment.getExternalStorageDirectory()
        dir = File(filePath.absolutePath + "/SIBI/")
        frameDir = File(filePath.absolutePath + "/SIBI/Frames")

        // Error: mp4 should have been
        val inputStream: InputStream = FileInputStream(filename)
        val grabber = FFmpegFrameGrabber(inputStream)
        val converterToMat = ToMat()

        hsv = Mat()
        skinMask = Mat()
        grabber.start()

        val outputData: ArrayList<ByteArray> = ArrayList<ByteArray>()

        while (true) {
            val nthFrame = grabber.grabImage() ?: break
            var mat = converterToMat.convertToOrgOpenCvCoreMat(nthFrame)

            mat = preprocessImageGetSkin(mat, cropSize)
            outputData.add(matToFloatArray(mat))

        }

        grabber.stop()
        grabber.release()

        return outputData
    }

    /**
     *  @ResultsOfPreprocessAndMobileNet
     *  Data Class dibuat untuk me-return nilai-nilai results (return) MobileNet, waktu preprocess,
     *  waktu penjalanan MobileNetV2.
     *  Selain itu Data Class di bawah juga menyimpan nilai-nilai yang dibutuhkan untuk fungsi lain seperti:
     *  Size dari MobileNet dan Result MobileNet.
     */
    data class ResultsOfPreprocessAndMobileNet(var mobilenetResult: ArrayList<FloatArray>, var pTime: Long, var mTime: Long, var totalFrame: Int) {
        var size: Int = mobilenetResult.size
        var result: ArrayList<FloatArray> = mobilenetResult
    }

    @Throws(IOException::class)
    fun javacvConvertLite(filename: File, cropSize: Crop): ResultsOfPreprocessAndMobileNet {

//        val tracer = FirebasePerformance.getInstance().newTrace("Preprocessing and MobileNetV2")
//        tracer.start()

        val converterToMat = ToMat()
        var bmp: Bitmap? = null
        val outputData: ArrayList<FloatArray> = ArrayList<FloatArray>()

        var pDuration = 0L;
        var mDuration = 0L;
        var totalFrame = 0

        try {
            val filePath = Environment.getExternalStorageDirectory()
            dir = File(filePath.absolutePath + "/SIBI/")

            //Log.d("dir_o", dir.toString())
            frameDir = File(filePath.absolutePath + "/SIBI/Frames")

            var inputStream: InputStream? = FileInputStream(filename)
            //Log.d("cidIS", inputStream.toString())

            val grabber = FFmpegFrameGrabber(inputStream) // OK

            hsv = Mat()
            //Log.d("cidx", "hsv")
            skinMask = Mat()
            //Log.d("cidx", "skinMask")
            //
            grabber.startUnsafe()
            //Log.d("cidx", "start")
            var idx = 1

            while (true) {
                //Log.d("cidx", idx.toString())
                idx += 2
                val nthFrame = grabber.grabImage() ?: break
                grabber.grabImage() ?: break
                var pStartTime = System.nanoTime()
                var mat = converterToMat.convertToOrgOpenCvCoreMat(nthFrame)
                mat = trial4PreprocessImageGetSkin(mat, cropSize, idx++)
                bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bmp)
                var pEndTime = System.nanoTime()
                pDuration += pEndTime - pStartTime

                var mStartTime = System.nanoTime()
                var mobnet = mobileNetV2Classifier.recognize(bmp)[0]
                var mEndTime = System.nanoTime()
                totalFrame += 1
                mDuration += mEndTime - mStartTime

                //Log.d("mobnet", mobnet.size.toString())
                //Log.d("mobnet", mobnet.toString())
                outputData.add(mobnet)
                Log.d("waktu_mobnet", mDuration.toString())
                Log.d("output_mob_net", mobnet.toString())

                /* Clean Components */
                bmp.recycle()
                bmp = null
                mat.release()

            }

//            tracer.putAttribute("frameCount", idx.toString())

            grabber.stop()
            grabber.release()
//            tracer.stop()
        } catch (e: Exception) {
            println("ERROR OCCURED : $e")
//            FirebaseCrashlytics.getInstance().recordException(e)
        }

        return ResultsOfPreprocessAndMobileNet(outputData, pDuration, mDuration, totalFrame)
    }

    fun javacvObservableConvertion(filename: File, cropSize: Crop): Observable<Pair<String, ArrayList<FloatArray>>> {

        return Observable.create { emitter ->

            val converterToMat = ToMat()
            var bmp: Bitmap? = null
            val outputData: ArrayList<FloatArray> = ArrayList<FloatArray>()
            val dummyArray = arrayListOf<FloatArray>()

            try {
                val inputStream: InputStream? = FileInputStream(filename)
                val grabber = FFmpegFrameGrabber(inputStream)

//                println("lengthInFrame" + grabber.lengthInFrames)
//                println("lengthInVideoFrame" + grabber.lengthInVideoFrames)

                hsv = Mat()
                skinMask = Mat()
                grabber.start()

                var idx = 1

                while (true) {
                    /* 15 FPS, grabImage twice */
                    val nthFrame = grabber.grabImage() ?: break
                    grabber.grabImage() ?: break
                    idx += 2

                    var mat = converterToMat.convertToOrgOpenCvCoreMat(nthFrame)

                    mat = trial4PreprocessImageGetSkin(mat, cropSize, idx)

                    bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mat, bmp)

                    outputData.add(mobileNetV2Classifier.recognize(bmp)[0])

                    /* Clean Components */
                    bmp.recycle()
                    bmp = null
                    mat.release()

                    /* Calculate Progression */
                    val percent = idx.toDouble().div(grabber.lengthInFrames) * 100 * 0.95 // max 95%
//                    println("Percent : $percent with $idx out of ${grabber.lengthInFrames}")
                    emitter.onNext(percent.toInt().toString() to dummyArray)
                }

                grabber.stop()
                grabber.release()
            } catch (e: Exception) {
//                FirebaseCrashlytics.getInstance().recordException(e)
            }

            /* emit actual data */
            emitter.onNext("999" to outputData)
            emitter.onComplete()

        }

    }

    /** Use this function to check Bitmap as PNG (access through ExternalDirectories)*/
    fun writeBitmapToExternalFile(mat: Mat, bmp: Bitmap) {
        var pixel = 0
        for (i in 0..mat.rows()-1) {
            for (j in 0..mat.cols()-1) {

                val data: DoubleArray = mat.get(i, j) //Stores element in an array
                var rgb = ""

                /* mat */
                for (k in 0 until 3)  //Runs for the available number of channels
                {
                    rgb = "$rgb, ${data[k]}"
                }

                val pixelValues = IntArray(224 * 224)
                bmp.getPixels(pixelValues, 0, bmp.width, 0, 0, bmp.width, bmp.height)

                /* bitmap */
                val pixelValue = pixelValues[pixel++]
                val R = (pixelValue shr 16 and 0xFF)
                val G = (pixelValue shr 8 and 0xFF)
                val B = (pixelValue and 0xFF)
                println("[$i][$j] : ${rgb} == $R, $G, $B")
            }
        }

        val filePath = Environment.getExternalStorageDirectory()
        val dir = File(filePath.absolutePath + "/SIBI/sibi_bmp.png")
        try {
            FileOutputStream(dir).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Imgcodecs.imwrite("${OpenCVService.dir}/sibi_mat.png", mat)
    }

    /** MatToFloatArray
     * This function used in ModelProvider (TensorFlow-Android version).
     * to convert OpenCV matrix to FloatArray*/
    fun matToFloatArray(
        mat: Mat
    ): ByteArray {
        val outputArray = ByteArray(224 * 224 * 3)
        mat[0, 0, outputArray]
        return outputArray
    }

    /** No Preprocess */
    private fun extractAndResizeOnly(inmat: Mat, cropSize: Crop, idx: Int): Mat {
        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

        Imgcodecs.imwrite("${OpenCVService.frameDir}/frame_${String.format("%03d", idx)}.png", mat)

        /* Resize Image */
        Imgproc.resize(
            mat,
            mat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        return mat
    }

    /** Remove Background Remove Process, Last 4 opening closing */
    fun trial4PreprocessImageGetSkin(inmat: Mat, cropSize: Crop, idx: Int): Mat {

        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

//        if (inmat.width() > inmat.height()) {
//            Core.rotate(inmat, inmat, Core.ROTATE_90_CLOCKWISE)
//        }
//
//        var mat = inmat.submat(
//            ((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt(),
//            ((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt() + inmat.width(),
//            0,
//            inmat.width())

        if (whiteMatrix == null) {
            whiteMatrix = Mat(mat.width(), mat.width(), CvType.CV_8UC1, Scalar(255.0))
        }

        /* Convert to HSV */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

//        /* Get skin mask*/
//        Core.inRange(hsv, lower, upper, skinMask)
//
//        /* Subtract Mat[255,255] with skinMask to invert its value*/
//        Core.subtract(whiteMatrix, skinMask, skinMask)
//
//        /* Bitwise_and to get cropped image */
//        Core.bitwise_and(mat, mat, mat, skinMask)
//
//        /* Proses Segmentasi Kulit */
//        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        Core.inRange(hsv, lower2, upper2, skinMask)

        /* Opening Closing */
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)
        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 8)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

//        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf5, anchor, 3)
//        Imgproc.erode(skinMask, skinMask, kernelEllipseOf5, anchor, 3)

//        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 5)
//        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

        /* Gaussian Blur */
        Imgproc.GaussianBlur(skinMask, skinMask, Size(3.0, 3.0), 0.0)

        val skin = Mat()
        Core.bitwise_and(mat, mat, skin, skinMask)

        /* Resize Image */
        Imgproc.resize(
            skin,
            mat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

//        Imgcodecs.imwrite("${OpenCVService.dir}/frame_${String.format("%03d", idx)}.png", mat)

        return mat
    }

    /** Remove Background Remove Process, Last 2 opening closing.
     * D/Hasil 1: >>72 : Siapa - 0.99612445
     * D/Hasil 2: >>54 : Nama - 0.834225
     * D/Hasil 3: >>52 : -Mu - 0.9968887 */
    fun trial5PreprocessImageGetSkin(inmat: Mat, cropSize: Crop): Mat {

        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

        if (whiteMatrix == null) {
            whiteMatrix = Mat(mat.width(), mat.width(), CvType.CV_8UC1, Scalar(255.0))
        }

        /* Convert to HSV */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

//        /* Get skin mask*/
//        Core.inRange(hsv, lower, upper, skinMask)
//
//        /* Subtract Mat[255,255] with skinMask to invert its value*/
//        Core.subtract(whiteMatrix, skinMask, skinMask)
//
//        /* Bitwise_and to get cropped image */
//        Core.bitwise_and(mat, mat, mat, skinMask)
//
//        /* Proses Segmentasi Kulit */
//        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        Core.inRange(hsv, lower2, upper2, skinMask)

        /* Opening Closing */
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)
        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 8)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf5, anchor, 3)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf5, anchor, 3)

//        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 5)
//        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

        /* Gaussian Blur */
        Imgproc.GaussianBlur(skinMask, skinMask, Size(3.0, 3.0), 0.0)

        val skin = Mat()
        Core.bitwise_and(mat, mat, skin, skinMask)

        /* Resize Image */
        Imgproc.resize(
            skin,
            mat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        return mat
    }

    /** Resize 864, remove green, remove last 2 opening closing */
    fun trial6PreprocessImageGetSkin(inmat: Mat, cropSize: Crop): Mat {

        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

        /* Resize Image */
        Imgproc.resize(
            mat,
            mat,
            Size(864.0, 864.0),
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        if (whiteMatrix == null) {
            whiteMatrix = Mat(mat.width(), mat.width(), CvType.CV_8UC1, Scalar(255.0))
        }

        /* Convert to HSV */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

//        /* Get skin mask*/
//        Core.inRange(hsv, lower, upper, skinMask)
//
//        /* Subtract Mat[255,255] with skinMask to invert its value*/
//        Core.subtract(whiteMatrix, skinMask, skinMask)
//
//        /* Bitwise_and to get cropped image */
//        Core.bitwise_and(mat, mat, mat, skinMask)
//
//        /* Proses Segmentasi Kulit */
//        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        Core.inRange(hsv, lower2, upper2, skinMask)

        /* Opening Closing */
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)
        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 8)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf5, anchor, 3)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf5, anchor, 3)

//        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 5)
//        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

        /* Gaussian Blur */
        Imgproc.GaussianBlur(skinMask, skinMask, Size(3.0, 3.0), 0.0)

        val skin = Mat()
        Core.bitwise_and(mat, mat, skin, skinMask)

        /* Resize Image */
        Imgproc.resize(
            skin,
            mat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        return mat
    }

    /** Resize 864, remove background, lower opening closing iteration */
    fun trial7PreprocessImageGetSkin(inmat: Mat, cropSize: Crop): Mat {

        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

        /* Resize Image */
        Imgproc.resize(
            mat,
            mat,
            Size(864.0, 864.0),
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        if (whiteMatrix == null) {
            whiteMatrix = Mat(mat.width(), mat.width(), CvType.CV_8UC1, Scalar(255.0))
        }

        /* Convert to HSV */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

//        /* Get skin mask*/
//        Core.inRange(hsv, lower, upper, skinMask)
//
//        /* Subtract Mat[255,255] with skinMask to invert its value*/
//        Core.subtract(whiteMatrix, skinMask, skinMask)
//
//        /* Bitwise_and to get cropped image */
//        Core.bitwise_and(mat, mat, mat, skinMask)
//
//        /* Proses Segmentasi Kulit */
//        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        Core.inRange(hsv, lower2, upper2, skinMask)

        /* Opening Closing */
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 1)
        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 4)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)

        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf5, anchor, 1)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf5, anchor, 1)

        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 3)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)

        /* Gaussian Blur */
        Imgproc.GaussianBlur(skinMask, skinMask, Size(3.0, 3.0), 0.0)

        val skin = Mat()
        Core.bitwise_and(mat, mat, skin, skinMask)

        /* Resize Image */
        Imgproc.resize(
            skin,
            mat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        return mat
    }

    /** Resize 864, remove background, lower opening closing iteration, remove last 4 opening-closing  */
    fun trial8PreprocessImageGetSkin(inmat: Mat, cropSize: Crop): Mat {

        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

        /* Resize Image */
        Imgproc.resize(
            mat,
            mat,
            Size(864.0, 864.0),
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        if (whiteMatrix == null) {
            whiteMatrix = Mat(mat.width(), mat.width(), CvType.CV_8UC1, Scalar(255.0))
        }

        /* Convert to HSV */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

//        /* Get skin mask*/
//        Core.inRange(hsv, lower, upper, skinMask)
//
//        /* Subtract Mat[255,255] with skinMask to invert its value*/
//        Core.subtract(whiteMatrix, skinMask, skinMask)
//
//        /* Bitwise_and to get cropped image */
//        Core.bitwise_and(mat, mat, mat, skinMask)
//
//        /* Proses Segmentasi Kulit */
//        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        Core.inRange(hsv, lower2, upper2, skinMask)

        /* Opening Closing */
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 1)
        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 4)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)

//        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf5, anchor, 1)
//        Imgproc.erode(skinMask, skinMask, kernelEllipseOf5, anchor, 1)

//        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 3)
//        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)

        /* Gaussian Blur */
        Imgproc.GaussianBlur(skinMask, skinMask, Size(3.0, 3.0), 0.0)

        val skin = Mat()
        Core.bitwise_and(mat, mat, skin, skinMask)

        /* Resize Image */
        Imgproc.resize(
            skin,
            mat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        return mat
    }


    /** DEFAULT PREPROCESS METHOD
     * PLEASE KEEP IN MIND
     * There are 2 types of image cropping method:
     * -> 1st crop method : to crop landscape images (ex: greenscreen dataset)
     * -> 2nd crop method : to crop portrait images (ex: actual image taken from smartphone camera)
     * */
    fun preprocessImageGetSkin(inmat: Mat, cropSize: Crop): Mat {

//        TimingLogger timingLogger = new TimingLogger("Preprocesss", "Start");

        /** === 1st crop method : Data GreenScreen === */
        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

//        Log.d("MatSize", "1 : ${inmat.height()} dan ${inmat.width()}")
//        Log.d("MatSize", "2 :${cropSize.width} dan ${cropSize.bottom} dan ${cropSize.top} dan ${cropSize.width + cropSize.top}")

        /** ======================= */
        /** === 2nd crop method : Data Hasil Rekam Module Kamera === */

//        if (inmat.width() > inmat.height()) {
//            Core.rotate(inmat, inmat, Core.ROTATE_90_CLOCKWISE)
//        }

//        var mat = inmat.submat(
//            ((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt(),
//            ((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt() + inmat.width(),
//            0,
//            inmat.width())

        if (whiteMatrix == null) {
            whiteMatrix = Mat(mat.width(), mat.width(), CvType.CV_8UC1, Scalar(255.0))
        }

        /** ======================= */

        /* Convert to HSV */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        /* Get skin mask*/
        Core.inRange(hsv, lower, upper, skinMask)

        /* Subtract Mat[255,255] with skinMask to invert its value*/
        Core.subtract(whiteMatrix, skinMask, skinMask)

        /* Bitwise_and to get cropped image */
        Core.bitwise_and(mat, mat, mat, skinMask)

        /* Proses Segmentasi Kulit */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        Core.inRange(hsv, lower2, upper2, skinMask)

        /* Opening Closing */
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 3)
        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 8)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf5, anchor, 3)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf5, anchor, 3)

        Imgproc.dilate(skinMask, skinMask, kernelEllipseOf3, anchor, 5)
        Imgproc.erode(skinMask, skinMask, kernelEllipseOf3, anchor, 5)

        /* Gaussian Blur */
        Imgproc.GaussianBlur(skinMask, skinMask, Size(3.0, 3.0), 0.0)

        val skin = Mat()
        Core.bitwise_and(mat, mat, skin, skinMask)

        /* Resize Image */
        Imgproc.resize(
            skin,
            mat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

//        var startTime: Long = System.nanoTime()
//        Core.normalize(
//            mat,
//            mat,
//            -1.0,
//            1.0,
//            Core.NORM_MINMAX
//        )
//        var endTime: Long = System.nanoTime()
//        val duration = endTime - startTime

        return mat
    }

    fun preprocessImageGetFullBody(inmat: Mat, cropSize: Crop): Mat {

//        TimingLogger timingLogger = new TimingLogger("Preprocesss", "Start");

        val left: Int = (inmat.width() - inmat.height()) / 2
        val right: Int = (inmat.width() + inmat.height()) / 2

        val mat = inmat.submat(0, inmat.height(), left, right)

        /* BALIKIN */
//        if (inmat.width() > inmat.height()) {
//            Core.rotate(inmat, inmat, Core.ROTATE_90_CLOCKWISE)
//        }

        val whiteMatrix = whiteMatrix ?: Mat(mat.width(), mat.width(), CvType.CV_8UC1, Scalar(255.0))
//        val whiteMatrix = whiteMatrix ?: Mat(inmat.width(), inmat.width(), CvType.CV_8UC1, Scalar(255.0))

//        var mat = inmat.submat(
//            cropSize.top+1,
//            cropSize.width + cropSize.top + 1,
//            0,
//            cropSize.width)

//        Log.d("InMatSizeCal", "${inmat.width()} : ${inmat.height()}" )
//        Log.d("MatSizeCal", "${((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt()} : ${((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt() + inmat.width()} : 0 : ${inmat.width()}" )

        /* BALIKIN */
//        var mat = inmat.submat(
//            ((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt(),
//            ((cropSize.top+1) * (inmat.height().toDouble() / cropSize.height)).toInt() + inmat.width(),
//            0,
//            inmat.width())

        /* Convert to HSV */
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)

        /* Get skin mask*/
        Core.inRange(hsv, lower, upper, skinMask)

        /* Subtract Mat[255,255] with skinMask to invert its value*/
        Core.subtract(whiteMatrix, skinMask, skinMask)

        /* Bitwise_and to get cropped image */
        Core.bitwise_and(mat, mat, mat, skinMask)

        val outmat = Mat()

        /* Resize Image */
        Imgproc.resize(
            mat,
            outmat,
            OUTPUT_SIZE,
            0.0,
            0.0,
            Imgproc.INTER_AREA
        )

        return outmat
    }

    fun loadImagesToByteArray(file: File): ByteArray {
        val mat = Imgcodecs.imread(file.absolutePath)
        return matToFloatArray(mat)
    }

    class Crop(val top: Int, val bottom: Int, val width: Int, val height: Int) {
        fun getJoined(): String {
            return "$top,$bottom,$width,$height"
        }
    }
}