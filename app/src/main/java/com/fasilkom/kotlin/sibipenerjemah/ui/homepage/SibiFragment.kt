package com.fasilkom.kotlin.sibipenerjemah.ui.homepage

import android.app.Dialog
import android.content.Intent
import android.content.res.TypedArray
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.databinding.CardItemBinding
import com.fasilkom.kotlin.sibipenerjemah.databinding.FragmentSibiBinding
import com.fasilkom.kotlin.sibipenerjemah.ui.camera.CameraActivity
import com.fasilkom.kotlin.sibipenerjemah.ui.result.ResultActivity
import com.google.android.material.snackbar.Snackbar
import java.util.*

class SibiFragment : Fragment() {
    var fragType: String? = null
    private var sibiBinding: FragmentSibiBinding? = null
    private var cardList: ArrayList<CardModel>? = null
    private var cardAdapter: SimpleRecyclerAdapter<CardModel>? = null

    private val dialogTutorial = TutorialDialogFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        sibiBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_sibi, container, false)
        initLayout()
        return sibiBinding?.getRoot()
    }

    private fun initLayout() {
        if (fragType.equals("SIBI", ignoreCase = true)) {
            initSibiLayout()
        } else {
            initBisindoLayout()
        }
        initData()
    }

    private fun initSibiLayout() {
        //setting sibi title
        sibiBinding?.fragImgBg?.setImageResource(R.drawable.ic_background_title_sibi)
        sibiBinding?.TitleFrag?.setText(R.string.sibiText)
        sibiBinding?.subtitleFrag?.setText(R.string.sibiSubText)
        sibiBinding?.dictButton?.setTextColor(resources.getColor(R.color.colorOren))
    }

    private fun initBisindoLayout() {
        //setting bisindo title
        sibiBinding?.fragImgBg?.setImageResource(R.drawable.ic_background_bisindo_title)
        sibiBinding?.TitleFrag?.setText(R.string.bisindoText)
        sibiBinding?.subtitleFrag?.setText(R.string.bisindoSubText)
        sibiBinding?.dictButton?.setTextColor(resources.getColor(R.color.colorBlue))
    }

    private fun initData() {
        val title =
            resources.getStringArray(R.array.cardTitleName)
        val desc =
            resources.getStringArray(R.array.cardSubtitleText)
        val img: TypedArray
        img = if (fragType.equals("SIBI", ignoreCase = true)) {
            resources.obtainTypedArray(R.array.cardImgSibi)
        } else {
            resources.obtainTypedArray(R.array.cardImgBisindo)
        }
        cardList = ArrayList()
        for (i in title.indices) {
            val card = CardModel(title[i], desc[i], img.getResourceId(i, 0))
            cardList!!.add(card)
        }
        if (cardAdapter == null) {
            cardAdapter = SimpleRecyclerAdapter(cardList!!, R.layout.card_item, object : SimpleRecyclerAdapter.OnViewHolder<CardModel> {

                override fun onBindView(
                    holder: SimpleRecyclerAdapter.SimpleViewHolder?,
                    item: CardModel
                ) {
                    val cardBinding = holder?.layoutBinding as CardItemBinding

                    cardBinding.TitleFrag.setText(item.cardName)
                    cardBinding.cardImg.setImageResource(item.cardImg!!)

                    setupButtonStartCamera(cardBinding, item)
                }
            })
        }
        sibiBinding?.rcCard?.setLayoutManager(LinearLayoutManager(context))
        sibiBinding?.rcCard?.setAdapter(cardAdapter)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        val snackbar = mCreateSnackbar(view)
        snackbar.setAction(R.string.ok) { v: View? -> snackbar.dismiss() }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun mCreateSnackbar(view: View): Snackbar {
        return Snackbar.make(view, R.string.unavailable_feature, Snackbar.LENGTH_LONG)
    }

    private fun showDialogStartRecording(typeDialog: String) {
        val dialog = Dialog(context!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.sibi_dialog_start_recording)
        val tvHeader = dialog.findViewById<TextView>(R.id.headerPopup)
        val tvSubHeader = dialog.findViewById<TextView>(R.id.subPopup)
        val startNoTutorial =
            dialog.findViewById<Button>(R.id.startNoTutorial)
        val startWithTutorial = dialog.findViewById<Button>(R.id.startWithTutorial)
        val imgClose =
            dialog.findViewById<ImageView>(R.id.closePopupButton)
        if (typeDialog.equals("Isyarat Ke Dalam Teks", ignoreCase = true)) {
            tvHeader.setText(R.string.IsyaratToTextHeader)
            tvSubHeader.setText(R.string.IsyaratToTextSub)
        } else {
            tvHeader.setText(R.string.TextToIsyaratHeader)
            tvSubHeader.setText(R.string.TextToIsyaratSub)
        }
        startWithTutorial.setOnClickListener {
            dialogTutorial.show(
                childFragmentManager,
                "test1"
            )
        }

        startNoTutorial.setOnClickListener {
            val intent = Intent(activity, ResultActivity::class.java) // TODO: Ganti ini balik
//            val intent = Intent(activity, CameraActivity::class.java) // TODO: Ganti ini balik
            startActivity(intent)
        }

        imgClose.setOnClickListener { dialog.dismiss() }
        imgClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupButtonStartCamera(cardBinding: CardItemBinding, item: CardModel) {
        if (fragType.equals("SIBI", ignoreCase = true)) {
            if (item.cardName.equals("Isyarat ke Dalam Teks", ignoreCase = true)) {
                cardBinding.btnStart.setOnClickListener(View.OnClickListener {
                    showDialogStartRecording(
                        item.cardName
                    )
                })
            } else {
                cardBinding.btnStart.setEnabled(false)
                cardBinding.btnStart.setBackground(resources.getDrawable(R.drawable.bg_button_not_active))
            }
        } else {
            cardBinding.btnStart.setEnabled(false)
            cardBinding.btnStart.setBackground(resources.getDrawable(R.drawable.bg_button_not_active))
        }
    }

    companion object {
        fun newInstance(typeFrag: String?): SibiFragment {
            val fragment = SibiFragment()
            fragment.fragType = typeFrag
            return fragment
        }
    }
}