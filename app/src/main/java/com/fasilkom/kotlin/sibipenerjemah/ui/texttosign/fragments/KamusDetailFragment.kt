package com.example.sibinative.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.ui.texttosign.GestureGenerationActivity
import com.unity3d.player.UnityPlayer


class KamusDetailFragment : Fragment() {
    private var text: String? = null
    private var description: String? = null
    lateinit var descriptionText:TextView
    lateinit var playButton:Button
    lateinit var mSettings: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            text = it.getString("text")
            description = it.getString("description")
        }
        mSettings = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_kamus_detail, container, false)
        descriptionText = view.findViewById<TextView>(R.id.descriptionText)
        descriptionText.text = description

        playButton = view.findViewById(R.id.playButton)
        playButton.setOnClickListener {
            UnityPlayer.UnitySendMessage("TextProcessing","setSliderSpeedValue", mSettings.getString("sliderSpeed","0.7"))
            UnityPlayer.UnitySendMessage("TextProcessing","triggerAnimation", text)
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(text: String, description: String) =
            KamusDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("text", text)
                    putString("description", description)
                }
            }
    }

    override fun onDestroyView() {
        (activity as GestureGenerationActivity).popKamusDetailFragment()
        super.onDestroyView()
    }
}
