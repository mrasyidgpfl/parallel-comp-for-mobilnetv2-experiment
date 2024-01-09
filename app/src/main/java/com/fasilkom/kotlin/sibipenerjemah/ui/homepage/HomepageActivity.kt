package com.fasilkom.kotlin.sibipenerjemah.ui.homepage

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fasilkom.kotlin.sibipenerjemah.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class HomepageActivity: AppCompatActivity() {

    var TAG = "HomePageActivity"
    val presenter: HomepageContract.Presenter by inject { parametersOf(this) }

    val sibiFragment: SibiFragment = SibiFragment.newInstance("SIBI")
    val bisindoFragment: SibiFragment = SibiFragment.newInstance("BISINDO")
    val fm = supportFragmentManager
    var activeFrag: SibiFragment = sibiFragment
    private var navigation: BottomNavigationView? = null
    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    fm.beginTransaction().hide(activeFrag).show(sibiFragment).commit()
                    //                    setNavMenuItemThemeColors(getResources().getColor(R.color.mColor));
                    activeFrag = sibiFragment
                }
                R.id.navigation_dashboard -> {
                    fm.beginTransaction().hide(activeFrag).show(bisindoFragment).commit()
                    //                    setNavMenuItemThemeColors(getResources().getColor(R.color.mColor));
                    activeFrag = bisindoFragment
                }
            }
            loadFragment(activeFrag)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        navigation = findViewById(R.id.navigation)
        navigation?.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        setNavMenuItemThemeColors(resources.getColor(R.color.mColor))
        loadFragment(activeFrag)

        presenter.loadClassifier()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "On Resume was called")
        Loader.load(opencv_java::class.java)
    }

    private fun loadFragment(fragment: Fragment?): Boolean {
        //switching fragment
        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.container_Fragment, fragment)
                .commit()
            return true
        }
        return false
    }

    fun setNavMenuItemThemeColors(color: Int) {
        //Setting default colors for menu item Text and Icon
        val navDefaultTextColor = resources.getColor(R.color.defaultColor)
        val navDefaultIconColor = resources.getColor(R.color.defaultColor)

        //Defining ColorStateList for menu item Text
        val navMenuTextList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_pressed)
            ), intArrayOf(
                color,
                navDefaultTextColor,
                navDefaultTextColor,
                navDefaultTextColor,
                navDefaultTextColor
            )
        )

        //Defining ColorStateList for menu item Icon
        val navMenuIconList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_pressed)
            ), intArrayOf(
                color,
                navDefaultIconColor,
                navDefaultIconColor,
                navDefaultIconColor,
                navDefaultIconColor
            )
        )
        navigation!!.itemTextColor = navMenuTextList
        navigation!!.itemIconTintList = navMenuIconList
    }
}