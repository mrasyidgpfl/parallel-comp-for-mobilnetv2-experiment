package com.fasilkom.kotlin.sibipenerjemah.ui.texttosign.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sibinative.ui.fragments.KamusFragment
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.data.model.KataEntity
import kotlinx.android.synthetic.main.kamus_item_row.view.*

class KamusAdapter(val context: Context, val fragment: KamusFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items : ArrayList<KataEntity> = arrayListOf()

    fun setItems(items:List<KataEntity>){
        this.items = ArrayList(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return KamusViewHolder(LayoutInflater.from(context).inflate(R.layout.kamus_item_row, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as KamusViewHolder).bindView(position)
    }

    inner class KamusViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        val text = view.text
        val layout = view.layout

        fun bindView(position: Int){
            this.setIsRecyclable(false)
            val item = this@KamusAdapter.items.get(position) as KataEntity
            text.text = item.text
            layout.setOnClickListener {
                fragment.onItemClick(item)
            }
        }
    }
}