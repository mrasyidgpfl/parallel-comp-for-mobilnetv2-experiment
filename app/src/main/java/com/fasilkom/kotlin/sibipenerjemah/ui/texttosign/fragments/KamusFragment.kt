package com.example.sibinative.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.data.model.KataEntity
import com.fasilkom.kotlin.sibipenerjemah.ui.texttosign.GestureGenerationActivity
import com.fasilkom.kotlin.sibipenerjemah.ui.texttosign.adapter.KamusAdapter
import kotlinx.android.synthetic.main.kamus_fragment.view.*


class KamusFragment : Fragment() {

    lateinit var adapter: KamusAdapter
    lateinit var loadingState:ConstraintLayout
    lateinit var recyclerView:RecyclerView
    lateinit var inputText: EditText
    var list = listOf<KataEntity>()

    companion object {
        fun newInstance() = KamusFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.kamus_fragment, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        loadingState = view.findViewById(R.id.loading_layer)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity().applicationContext)
        adapter =
            KamusAdapter(requireActivity(), this)
        inputText = view.input_text

        view.input_text.setOnEditorActionListener { v, _, _ ->
            (activity as GestureGenerationActivity).hideKeyboard()
            val filteredList = (activity as GestureGenerationActivity).presenter.getKata(v.text.toString(),view.huruf_input_text.text.toString())
            adapter.setItems(filteredList)
            true
        }

        view.huruf_input_text.setOnEditorActionListener { v, actionId, event ->
            (activity as GestureGenerationActivity).hideKeyboard()
            val filteredList = (activity as GestureGenerationActivity).presenter.getKata(view.input_text.text.toString(),v.text.toString())
            adapter.setItems(filteredList)
            true
        }

        list = (activity as GestureGenerationActivity).presenter.getKata()
        adapter.setItems(list)
        recyclerView.adapter = adapter

        recyclerView.scrollToPosition(activity?.intent!!.getIntExtra("scrollPosition",0))

        return view
    }

    fun onItemClick(kataEntity: KataEntity){
        (activity as GestureGenerationActivity).toKamusDetailFragment(kataEntity.text, kataEntity.description)

    }
}
