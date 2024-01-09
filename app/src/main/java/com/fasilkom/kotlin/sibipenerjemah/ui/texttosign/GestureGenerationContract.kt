package com.example.sibinative.ui

import android.app.Activity
import com.fasilkom.kotlin.sibipenerjemah.data.model.KataEntity

interface GestureGenerationContract {
    interface View {
        fun getActivity():Activity
    }
    interface Presenter {
        fun getKata(query: String = "",firstLetter: String = "" ):List<KataEntity>
        fun onAttach(view: View)
    }
}