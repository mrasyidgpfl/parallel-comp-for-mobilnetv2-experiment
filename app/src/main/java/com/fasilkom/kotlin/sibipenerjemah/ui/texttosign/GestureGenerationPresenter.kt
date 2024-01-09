package com.example.sibinative.ui

import android.util.Log
import com.fasilkom.kotlin.sibipenerjemah.data.model.KataEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.lang.Exception

class GestureGenerationPresenter: GestureGenerationContract.Presenter {

    lateinit var view: GestureGenerationContract.View
    var kamus = listOf<KataEntity>()

    override fun getKata(query: String, firstLetter: String): List<KataEntity> {
        if (kamus.isEmpty()){
            val json:String
            val array:ArrayList<String> = arrayListOf()
            val kataResult:ArrayList<KataEntity> = arrayListOf()

            try {
                val inputStream: InputStream = view.getActivity().assets.open("KamusSIBI.json")
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()

                json = String(buffer)

                val list = JSONArray(json)

                for (i in 0 until list.length()) {
                    val kata: JSONObject = list.getJSONObject(i)
                    val word = kata.getString("word")
                    val keterangan = kata.getString("keterangan")
                    if (!array.contains(word)){
                        array.add(word)
                        kataResult.add(KataEntity(word,keterangan))
                    }
                }

            } catch (e: Exception){
                Log.v("Error",e.toString())
            }

            this.kamus =  kataResult.toList()
        }

        return kamus.filter {
            it.text.first().toString().contains(firstLetter.toLowerCase()) && it.text.toLowerCase().contains(query.toLowerCase())
        }
    }

    override fun onAttach(view: GestureGenerationContract.View) {
        this.view = view
    }


}