package com.fasilkom.kotlin.sibipenerjemah.ui.homepage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager.widget.ViewPager
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.ui.camera.CameraActivity

class TutorialDialogFragment : DialogFragment() {

    var vpTutorial: ViewPager? = null
    var imgClose: ImageView? = null
    var btnNext: ImageButton? = null
    var btnBack: ImageButton? = null
    var tvPage: TextView? = null
    var btnStartCamera: Button? = null
    val firstTutorial = FragmentPopupTutorial.newInstance("first")
    val secondTutorial = FragmentPopupTutorial.newInstance("second")
    val thirdTutorial = FragmentPopupTutorial.newInstance("third")
    var currentItem = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.sibi_popup_alert, container, false)
        vpTutorial = view.findViewById(R.id.viewpager)
        imgClose = view.findViewById(R.id.closePopupButton)
        btnBack = view.findViewById(R.id.buttonBack)
        btnNext = view.findViewById(R.id.buttonNext)
        tvPage = view.findViewById(R.id.tvPage)
        btnStartCamera = view.findViewById(R.id.startCamera)

        val adapter = CustomAdapter(getChildFragmentManager())
        adapter.addFragment("tutorial1", firstTutorial)
        adapter.addFragment("tutorial2", secondTutorial)
        adapter.addFragment("tutorial3", thirdTutorial)
        vpTutorial?.setAdapter(adapter)
        btnBack?.setVisibility(View.INVISIBLE)
        btnStartCamera?.setOnClickListener(View.OnClickListener {
            val intent = Intent(getActivity(), CameraActivity::class.java)
            startActivity(intent)
        })
        btnNext?.setOnClickListener(View.OnClickListener {
            currentItem++
            checkVisibility()
            vpTutorial!!.setCurrentItem(currentItem)
        })
        btnBack?.setOnClickListener(View.OnClickListener {
            currentItem--
            checkVisibility()
            vpTutorial?.setCurrentItem(currentItem)
        })
        imgClose?.setOnClickListener(View.OnClickListener {
            currentItem = 0
            vpTutorial?.setCurrentItem(currentItem)
            getDialog()?.dismiss()
        })
        return view
    }

    open fun checkVisibility() {
        if (currentItem == 0) {
            btnBack!!.visibility = View.INVISIBLE
            btnNext!!.visibility = View.VISIBLE
            tvPage!!.visibility = View.VISIBLE
            tvPage!!.text = (currentItem + 1).toString() + "/3"
            btnStartCamera!!.visibility = View.GONE
        } else if (currentItem == 2) {
            btnNext!!.visibility = View.INVISIBLE
            btnBack!!.visibility = View.VISIBLE
            tvPage!!.visibility = View.GONE
            btnStartCamera!!.visibility = View.VISIBLE
        } else {
            btnNext!!.visibility = View.VISIBLE
            btnBack!!.visibility = View.VISIBLE
            tvPage!!.visibility = View.VISIBLE
            tvPage!!.text = (currentItem + 1).toString() + "/3"
            btnStartCamera!!.visibility = View.GONE
        }
    }
}