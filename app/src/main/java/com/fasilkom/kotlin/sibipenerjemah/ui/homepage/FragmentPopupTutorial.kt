package com.fasilkom.kotlin.sibipenerjemah.ui.homepage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.databinding.FragmentTutorialBinding

class FragmentPopupTutorial : Fragment() {
    private var tutorialBinding: FragmentTutorialBinding? = null
    var fragPos: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        tutorialBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_tutorial, container, false)
        initLayout()
        return tutorialBinding?.getRoot()
    }

    private fun initLayout() {
        if (fragPos.equals("first", ignoreCase = true)) {
            tutorialBinding?.imageTutorial?.setImageDrawable(resources.getDrawable(R.drawable.ic_first_tutorial))
            tutorialBinding?.textTutorial?.setText(resources.getString(R.string.firstTutorial))
        } else if (fragPos.equals("second", ignoreCase = true)) {
            tutorialBinding?.imageTutorial?.setImageDrawable(resources.getDrawable(R.drawable.ic_rekam_dengan_jelas))
            tutorialBinding?.textTutorial?.setText(resources.getString(R.string.secondTutorial))
        } else {
            tutorialBinding?.imageTutorial?.setImageDrawable(resources.getDrawable(R.drawable.ic_menjadi_teks))
            tutorialBinding?.textTutorial?.setText(resources.getString(R.string.thirdTutorial))
        }
    }

    companion object {
        fun newInstance(fragPosition: String?): FragmentPopupTutorial {
            val fragment = FragmentPopupTutorial()
            fragment.fragPos = fragPosition
            return fragment
        }
    }
}
