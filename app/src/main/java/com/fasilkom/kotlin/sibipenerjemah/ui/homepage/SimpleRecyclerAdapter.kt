package com.fasilkom.kotlin.sibipenerjemah.ui.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

class SimpleRecyclerAdapter<T>(
    var mainData: MutableList<T>,
    @field:LayoutRes var layoutRes: Int,
    var listener: OnViewHolder<T>
) :
    RecyclerView.Adapter<SimpleRecyclerAdapter.SimpleViewHolder>() {

    class SimpleViewHolder @JvmOverloads constructor(
        itemView: View?,
        listener: OnViewHolder<*>?,
        adapter: SimpleRecyclerAdapter<*>? = null
    ) :
        RecyclerView.ViewHolder(itemView!!) {
        private var listener: OnViewHolder<*>? = null
        var layoutBinding: ViewDataBinding? = null
        var adapter: SimpleRecyclerAdapter<*>? = null

        init {
            try {
                this.listener = listener
                this.adapter = adapter
                layoutBinding = DataBindingUtil.bind(itemView!!)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SimpleViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return SimpleViewHolder(view, listener, this)
    }

    override fun onBindViewHolder(
        holder: SimpleViewHolder,
        position: Int
    ) {
        val t = mainData!![position]
        listener.onBindView(holder, t)
    }

    override fun getItemCount(): Int {
        return if (mainData == null) 0 else mainData!!.size
    }

    fun getItemBy(t: T): T? {
        for (mainDatum in mainData) {
            if (mainDatum == t) {
                return mainDatum
            }
        }
        return null
    }

    fun getItemAt(position: Int): T {
        return mainData[position]
    }

    fun addItem(t: T) {
        addItemAt(t, mainData.size)
    }

    fun addItemAt(t: T, i: Int) {
        mainData.add(i, t)
    }

    fun addAllItemRelyingPassByValue(t: MutableList<T>) {
        mainData = t
    }

    fun addAllItem(t: List<T>) {
        mainData.addAll(t)
    }

    fun addAllItemWithIndex(position: Int, t: List<T>) {
        mainData.addAll(position, t)
    }

    fun removeItem(t: T) {
        mainData.remove(t)
    }

    fun remove(t: List<T>) {
        for (t1 in t) {
            mainData.remove(t1)
        }
    }

    fun removeAt(position: Int) {
        mainData.removeAt(position)
    }

    fun removeAll() {
        mainData.clear()
    }

    interface OnViewHolder<T> {
        fun onBindView(holder: SimpleViewHolder?, item: T)
    }

}
