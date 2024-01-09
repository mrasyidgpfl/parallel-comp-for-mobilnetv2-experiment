package com.fasilkom.kotlin.sibipenerjemah.ui.texttosign.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.sibinative.ui.fragments.KamusFragment
import com.example.sibinative.ui.fragments.PengaturanFragment
import com.example.sibinative.ui.fragments.TranslateFragment

class TabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm){
    override fun getItem(position: Int): Fragment {
        if (position==0){
            return KamusFragment.newInstance()
        }
        else if (position==1){
            return TranslateFragment.newInstance()
        }
        else{
            return PengaturanFragment.newInstance()
        }
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
        if (position==0){
            return "Kamus"
        }
        else if (position==1){
            return "Penerjemah"
        }
        else{
            return "Pengaturan"
        }
    }

}