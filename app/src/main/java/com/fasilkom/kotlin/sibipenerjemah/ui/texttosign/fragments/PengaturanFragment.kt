package com.example.sibinative.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.ui.texttosign.GestureGenerationActivity
import kotlinx.android.synthetic.main.pengaturan_fragment.*


class PengaturanFragment : Fragment() {

    var selectedCharacter = ""
    var progress = 0
    lateinit var mSettings:SharedPreferences

    companion object {
        fun newInstance() =
            PengaturanFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.pengaturan_fragment, container, false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSettings = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        progress = (mSettings.getString("sliderSpeed","0.7")!!.toFloat()*10).toInt()
        selectedCharacter = mSettings.getString("character","jasper")!!

        seek_bar.progress = progress
        seek_bar.refreshDrawableState()
        seek_bar_label.setText(progress.toString())

        seek_bar_label.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                var text = p0.toString()

                if(!text.isEmpty()){
                    if (text.toInt()>10){
                        seek_bar_label.setText("10")
                        text = "10"
                    }

                    seek_bar.progress = text.toInt()
                    seek_bar_label.setSelection(text.toInt().toString().length)
                    simpan()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        if (selectedCharacter=="jasper"){
            model1RadioButton.isChecked = true
        }
        else if (selectedCharacter=="pearl"){
            model2RadioButton.isChecked = true
        }

        seek_bar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                seek_bar_label.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seek: SeekBar) {
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                if (seek_bar.progress == 0){
                    seek_bar.progress = 1
                }
                progress = seek_bar.progress
                seek_bar_label.setText(progress.toString())
                simpan()
            }
        })

        model1RadioButton.setOnClickListener {
            selectedCharacter = "jasper"
            simpan()
        }

        model2RadioButton.setOnClickListener {
            selectedCharacter = "pearl"
            simpan()
        }

        simpan.setOnClickListener {
            simpan(true)
        }
    }

    fun simpan(back: Boolean = false){
        val editor = mSettings.edit()
        editor.putString("sliderSpeed",(progress.div(10.0).toString()))
        editor.putString("character",selectedCharacter)
        editor.apply()

        if (back){
            (activity as GestureGenerationActivity).viewPager.setCurrentItem(1, true)
        }
    }


}
