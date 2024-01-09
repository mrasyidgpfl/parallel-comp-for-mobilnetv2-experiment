package com.fasilkom.kotlin.sibipenerjemah.ui.result

import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.service.TestingModelProvider
import com.fasilkom.kotlin.sibipenerjemah.service.opencv.OpenCVService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.observers.DisposableObserver
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_result.*
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.FFmpegLogCallback
import org.bytedeco.opencv.opencv_java
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class ResultActivity: AppCompatActivity() {

    var cropSize: OpenCVService.Crop? = null

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initializeRxJavaErrorHandler()
        setOverlayLayoutSize()
        initializeFFmpegLog()

        if (allPermissionsGranted()) {
            runModelClassifier()
        } else {
            ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

    }

    private fun initializeFFmpegLog() {
        FFmpegLogCallback.set()
        av_log_set_level(AV_LOG_DEBUG);
    }

    private fun initializeRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler { e ->
            if (e is UndeliverableException) {
                textViewResult.text = "Error : $e"
            }

            if (e is IOException) {
                return@setErrorHandler
            }

            if (e is FileNotFoundException) {
                textViewResult.text = "Error : $e"
            }
        }
    }

    private fun runModelClassifier() {
        /* Read All Files */
        val filePath = Environment.getExternalStorageDirectory()
        val modelProvider = TestingModelProvider()
        modelProvider.loadClassifier(this.applicationContext, this)

        // Value dibawah cuma dummy aja, tidak terpakai
        modelProvider.runClassifierObservable(
            assets,
            File(filePath.getAbsolutePath() + "/SIBI/IbuBudi_Siapa_Nama_Mu.mp4"),
            cropSize!!
        )
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<String>() {
                override fun onNext(t: String?) {
                    textViewResult.text = t
                }

                override fun onError(e: Throwable?) {
                    textViewResult.text = "Error : $e"
                }

                override fun onComplete() {
                    textViewResult.text = "All Video Completed"
                    progressBarCircular.visibility = View.GONE
                }
            })
    }

    fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
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
        overlay?.layoutParams?.height = height - width - topLayer - statusBarHeight + navigationBarHeight
        cropSize = OpenCVService.Crop(topLayer, height-width-topLayer - statusBarHeight + navigationBarHeight, width, height)
    }

    private fun showNavigationBar(resources: Resources): Boolean {
        val id: Int = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && resources.getBoolean(id)
    }

    override fun onResume() {
        super.onResume()
        Loader.load(opencv_java::class.java)
        Log.i(ResultActivity.TAG, "On Resume was called")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                runModelClassifier()
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

    companion object {
        private val DELAY_TIME: Long = 100
        private val REQUEST_CODE_PERMISSIONS = 101
        private val REQUIRED_PERMISSIONS = arrayOf(
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
        )
        private val VIDEO_FILE_NAME = "SIBI_VIDEO.mp4"
        private val TAG = "CameraActivity"
    }
}