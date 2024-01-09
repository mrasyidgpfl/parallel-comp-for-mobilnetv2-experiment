package com.fasilkom.kotlin.sibipenerjemah.ui.signtotext

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.service.ModelProvider
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import java.io.File

class PickerActivity : AppCompatActivity(), PickerContract.View {

    val TAG = "PickerActivity"
    val presenter: PickerContract.Presenter by inject {parametersOf(this)}
    var filePath = Environment.getExternalStorageDirectory()
    var dir: File = File(filePath.getAbsolutePath() + "/SIBI")

    private val REQUEST_CODE_PERMISSIONS = 101
    private val REQUIRED_PERMISSIONS = arrayOf(
        "android.permission.CAMERA",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_EXTERNAL_STORAGE"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        presenter.loadClassifier()

        if (allPermissionsGranted()) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            initSpinner()
            initClickListener()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun initSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerVideoSelection)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.sibiVideoSelection, R.layout.spinner_color_layout
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                Toast.makeText(
                    applicationContext,
                    "" + position + " : " + parent.getItemAtPosition(position),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun initClickListener() {
        /* TODO : Ganti jadi actual pilih kalimat*/
        val videoDir = "$dir/Video/"
        val directory = File(videoDir)
        val videoUri = directory.listFiles()[0].listFiles()[0].listFiles()[5]

        val executeButton =
            findViewById<Button>(R.id.buttonExecute)
        executeButton.setOnClickListener {
            presenter.executeOfflineClassifier(videoUri, object: ModelProvider.TranslateListener {
                override fun onClassifyDoneShowTranslation(translation: String?) {
                    Log.d("PickerActivity", "RESULT OFFLINE : " + translation)
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "On Resume was called")
        Loader.load(opencv_java::class.java)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
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
}