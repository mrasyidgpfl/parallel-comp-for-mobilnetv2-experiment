package com.fasilkom.kotlin.sibipenerjemah.ui.camera

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.databinding.ActivityCameraBinding
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProvider
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import com.fasilkom.kotlin.sibipenerjemah.ui.result.CameraResultActivity
import kotlinx.android.synthetic.main.activity_camera.*
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity: AppCompatActivity(), CameraContract.View {

    val presenter: CameraContract.Presenter by inject { parametersOf(this) }

    var newfile: File? = null
    var filePath: File = Environment.getExternalStorageDirectory()
    var dir: File = File(filePath.getAbsolutePath().toString() + "/SIBI/")
    var cropSize: OpenCVService.Crop? = null

    private var cameraBinding: ActivityCameraBinding? = null
    private var isRecording = false
    private var videoCapture: VideoCapture? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var recordButton: ImageButton

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraBinding = DataBindingUtil.setContentView(this, R.layout.activity_camera) as ActivityCameraBinding
        cameraExecutor = Executors.newSingleThreadExecutor()
        recordButton = findViewById<ImageButton>(R.id.imgCapture)

        setOverlayLayoutSize()
        setCameraListener()
        setViewListener()

        if (allPermissionsGranted()) {
            println("Permission Granted")
            cameraBinding?.viewFinder?.post(Runnable { startCamera() }) //start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        Loader.load(opencv_java::class.java)
        Log.i(TAG, "On Resume was called")
    }

    private fun setViewListener() {
        val textViewTutorial = cameraBinding?.textViewTutorial
        cameraBinding?.imgHelp?.setOnClickListener {
            when (textViewTutorial?.visibility) {
                View.GONE -> textViewTutorial.visibility = View.VISIBLE
                View.VISIBLE -> textViewTutorial.visibility = View.GONE
            }
        }
        cameraBinding?.textViewGarisBantu?.setOnClickListener {
            when (imgSibiBody?.visibility) {
                View.GONE -> {
                    imgSibiBody.visibility = View.VISIBLE
                    textViewGarisBantu.background = ContextCompat.getDrawable(this, R.drawable.bg_button_border_green)
                }
                View.VISIBLE -> {
                    imgSibiBody.visibility = View.GONE
                    textViewGarisBantu.background = ContextCompat.getDrawable(this, R.drawable.bg_button_border_disabled)
                }
            }
        }
    }

    private fun setOverlayLayoutSize() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        val topLayer = findViewById<View>(R.id.view_top_layer).layoutParams.height
        val statusBarHeight = getStatusBarHeight()
        val navigationBarHeight = when (showNavigationBar(resources)) {
            true -> 0
            false -> 2 * statusBarHeight
        }

        Log.d("OverlaySize", "$height : $width : $topLayer")
        Log.d("StatusBarHeight", "${getStatusBarHeight()}")

//        cameraBinding?.imgHelp?.layoutParams?.height = 1080
        cameraBinding?.overlay?.layoutParams?.height = height - width - topLayer - statusBarHeight + navigationBarHeight
        cropSize = OpenCVService.Crop(topLayer, height-width-topLayer - statusBarHeight + navigationBarHeight, width, height)
    }

    private fun setCameraListener() {
        val ot: View.OnTouchListener = object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return false
            }
        }
        cameraBinding?.imgCapture?.isClickable = false
        cameraBinding?.imgCapture?.setOnClickListener(object : View.OnClickListener {
            @SuppressLint("RestrictedApi")
            override fun onClick(v: View?) {
                if (!isRecording) {
                    // Run Classifier
                    startRecord()

                    Log.d("OverlaySize2", "${cameraBinding?.viewFinder?.width} : ${cameraBinding?.viewFinder?.height}")


                } else if (isRecording) {
                    isRecording = false
                    videoCapture!!.stopRecording()
                    cameraBinding!!.imgCapture.setImageResource(R.drawable.ic_camera_not_recording)
                }
            }
        })
        cameraBinding?.imgCapture?.setOnTouchListener(ot)
    }

    private fun showNavigationBar(resources: Resources): Boolean {
        val id: Int = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && resources.getBoolean(id)
    }

    private fun getNavigationBarHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val usableHeight = metrics.heightPixels
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val realHeight = metrics.heightPixels
            return if (realHeight > usableHeight) realHeight - usableHeight else 0
        }
        return 0
    }


    fun getCurrentDateNaming(): String? {
        val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return fileNameDateFormat.format(Date())
    }

    fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @SuppressLint("RestrictedApi")
    private fun startRecord() {
        /* Original */
        isRecording = true
        cameraBinding!!.imgCapture.setImageResource(R.drawable.ic_stop_recording)

        // Reset
        cameraBinding!!.textViewTranslate.text = ""

        // Video File Setup
        if (!dir.exists()) {
            dir.mkdir()
        }
        newfile = File(dir, VIDEO_FILE_NAME)
        if (newfile!!.exists()) newfile!!.delete()
        newfile!!.setReadable(true)

        val videoCapture = videoCapture ?: return

        val option = VideoCapture.OutputFileOptions.Builder(newfile!!).build()
        videoCapture.startRecording(
            option,
            ContextCompat.getMainExecutor(this),
            object : VideoCapture.OnVideoSavedCallback {

            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                onClassifyingShowLoadingState()
                runClassifier()
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                println("Error: $message")
                println("Cause: $cause")
                Toast.makeText(this@CameraActivity, "Error: Jangan terlalu cepat kliknya", Toast.LENGTH_LONG).show()
            }
        })
    }

    /** This function is built for survey purpose, might be needed one day */
    @SuppressLint("RestrictedApi")
    private fun startRecordSurveyVersion() {

        val dir: File = File(filePath.getAbsolutePath().toString() + "/SIBI/Video")
        isRecording = true
        cameraBinding!!.imgCapture.setImageResource(R.drawable.ic_stop_recording)

        if (!dir.exists()) {
            dir.mkdir()
        }
        newfile = File(dir, "SIBI_" + getCurrentDateNaming().toString() + ".mp4")
        if (newfile!!.exists()) newfile!!.delete()
        newfile!!.setReadable(true)

        val videoCapture = videoCapture ?: return

        val option = VideoCapture.OutputFileOptions.Builder(newfile!!).build()
        videoCapture.startRecording(
            option,
            ContextCompat.getMainExecutor(this@CameraActivity),
            object : VideoCapture.OnVideoSavedCallback {

                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    val cameraResultIntent = Intent(
                        this@CameraActivity,
                        CameraResultActivity::class.java
                    )
                    cameraResultIntent.putExtra(
                        CameraResultActivity.argPathCamera,
                        newfile
                    )
                    startActivity(cameraResultIntent)
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    println("Error: $message")
                    println("Cause: $cause")
                    Toast.makeText(this@CameraActivity, "Error: Jangan terlalu cepat kliknya", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun runClassifier() {
        val cropSize = cropSize ?: return
        val file: File = File(filePath.getAbsolutePath() + "/SIBI/IbuBudi_Siapa_Nama_Mu.mp4")

        presenter.executeOfflineClassifier(
            newfile!!,
            object: ModelProvider.TranslateListener {
                    override fun onClassifyDoneShowTranslation(translation: String?) {
                        Log.d("CameraActivity", "RESULT OFFLINE : " + translation)

                        runOnUiThread {
                            cameraBinding?.run {
                                progressBarCircular.visibility = View.GONE
                                imgCapture.isEnabled = true
                                imgCapture.setImageResource(R.drawable.ic_camera_not_recording)
                                textViewTranslate.text = translation
                            }
                        }
                    }
                },
            cropSize)
    }

//    private fun runClassifier() {
//        val cropSize = cropSize ?: return
//        val filePath = Environment.getExternalStorageDirectory()
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/20181013_161446.mp4")
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/20181013_092112.mp4")
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/20181014_101633.mp4")
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/20181014_092734.mp4")
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/20181014_134925.mp4")
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/20181014_164208.mp4")
//
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/IbuBudi_Siapa_Nama_Mu.mp4")
////                val file: File = File(filePath.getAbsolutePath() + "/SIBI/SIBI_PIPUT.mp4")
//
//        /* ----- Offline ----- */
//
//        // Delay to wait UI changes applied
//        val handler = Handler()
//        handler.postDelayed(Runnable {
//            presenter.executeOfflineClassifier(
//                newfile!!, // It is supposed to use reference from outputFileResults
//                object: ModelProvider.TranslateListener {
//                    override fun onClassifyDoneShowTranslation(translation: String?) {
//                        Log.d("CameraActivity", "RESULT OFFLINE : " + translation)
//
//                        runOnUiThread {
//                            cameraBinding?.run {
//                                progressBarCircular.visibility = View.GONE
//                                imgCapture.isEnabled = true
//                                imgCapture.setImageResource(R.drawable.ic_camera_not_recording)
//                                textViewTranslate.text = translation
//                            }
//                        }
//                    }
//                },
//                cropSize
//            )
//        }, DELAY_TIME)
//
////                Log.d(TAG, "SavedUri : ${Uri.fromFile(newfile)}")
////                Log.d(TAG, "SavedUri : ${contentResolver != null}")
//
//        /* ----- Online ----- */
////                val requestFile: RequestBody = RequestBody.create(
////                    MediaType.parse(getMimeType(outputFileResults.savedUri!!)),
////                    newfile!!
////                )
////
////                val body = MultipartBody.Part.createFormData("video", newfile!!.name, requestFile)
////                presenter.executeOnlineClassifier(body, cropSize)
//    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                }

            videoCapture = VideoCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture)

            } catch(exc: Exception) {
                Log.e("CameraActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onClassifyingShowLoadingState() {
//        cameraBinding?.progressBarCircular?.visibility = View.VISIBLE
        recordButton.isEnabled = false
        recordButton.setImageResource(R.drawable.ic_camera_disabled)
    }

    override fun onTranslationShowProgressPercentage(percentage: String) {
        progressBarLinear.progress = Integer.parseInt(percentage)
    }

    override fun onTranslationShowProgressResult(result: String) {
        textViewTranslate.text = result
        progressBarLinear.progress = 0
        imgCapture.isEnabled = true
        imgCapture.setImageResource(R.drawable.ic_camera_not_recording)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraBinding?.viewFinder?.post(Runnable { startCamera() })
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        presenter.closeClassifier()
    }

    fun getMimeType(uri: Uri): String? {
        var mimeType: String? = null
        mimeType = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.getType(uri)
        } else {
            val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(
                uri
                    .toString()
            )
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.toLowerCase()
            )
        }
        return mimeType
    }

    companion object {
        private val DELAY_TIME: Long = 100
        private val REQUEST_CODE_PERMISSIONS = 101
        private val REQUIRED_PERMISSIONS = arrayOf(
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_EXTERNAL_STORAGE"
        )
        private val VIDEO_FILE_NAME = "SIBI_VIDEO.mp4"
        private val TAG = "CameraActivity"
    }
}