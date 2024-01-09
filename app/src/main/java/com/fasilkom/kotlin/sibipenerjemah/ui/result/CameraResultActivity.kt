package com.fasilkom.kotlin.sibipenerjemah.ui.result

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.widget.MediaController
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.databinding.ActivityCameraResultBinding
import kotlinx.android.synthetic.main.activity_camera_result.*
import java.io.File


class CameraResultActivity : AppCompatActivity() {
    private var cameraResultBinding: ActivityCameraResultBinding? = null
    private var videoUri: Uri? = null
    private var videoFile: File? = null
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraResultBinding = DataBindingUtil.setContentView(this, R.layout.activity_camera_result)
        val intent = intent
        videoFile = intent.extras!![argPathCamera] as File?
        initLayout()
        initListener()
    }

    private fun initLayout() {
        videoUri = Uri.fromFile(videoFile)
        val mc = MediaController(this@CameraResultActivity)
        mc.setAnchorView(cameraResultBinding!!.videoRecorded)
        mc.setMediaPlayer(cameraResultBinding!!.videoRecorded)
        cameraResultBinding!!.videoRecorded.setVideoURI(videoUri)
        cameraResultBinding!!.videoRecorded.setMediaController(mc)
        cameraResultBinding!!.videoRecorded.requestFocus()
        cameraResultBinding!!.videoRecorded.start()
    }

    private fun initListener() {
        buttonSave.setOnClickListener {
            finish()
        }

        buttonDelete.setOnClickListener {
            try {
                showAlertDialog()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: delete failed (${e.message})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAlertDialog() {
        val alertDialog: AlertDialog? = let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton("yes",
                    DialogInterface.OnClickListener { dialog, id ->
                        videoFile?.delete()
                        finish()
                    })
                setNegativeButton("cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        // do nothing
                    })
                setMessage("Yakin mau dihapus?")
            }

            // Create the AlertDialog
            builder.create()
        }

        val window: Window? = alertDialog?.window
        window?.setGravity(Gravity.BOTTOM)

        alertDialog?.show()
    }

    companion object {
        const val argPathCamera = "ARG_GET_VIDEO_PATH"
    }
}