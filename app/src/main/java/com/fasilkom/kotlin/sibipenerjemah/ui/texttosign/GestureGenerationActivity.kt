package com.fasilkom.kotlin.sibipenerjemah.ui.texttosign

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import com.example.sibinative.ui.GestureGenerationContract
import com.example.sibinative.ui.fragments.KamusDetailFragment
import com.fasilkom.kotlin.sibipenerjemah.R
import com.fasilkom.kotlin.sibipenerjemah.ui.texttosign.adapter.TabAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.ui.SibiGesturesGeneration.UnityPlayerActivity
import com.unity3d.player.UnityPlayer
import kotlinx.android.synthetic.main.activity_gesture_generation.*
import org.koin.android.ext.android.inject


class GestureGenerationActivity : UnityPlayerActivity(), GestureGenerationContract.View {

    private lateinit var tabLayout: TabLayout
    private lateinit var tabAdapter: TabAdapter
    lateinit var viewPager: ViewPager
    lateinit var mSettings: SharedPreferences

    val presenter: GestureGenerationContract.Presenter by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gesture_generation)

        presenter.onAttach(this)

        setupUI(findViewById<View>(R.id.tab_layout).parent as View)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        tabAdapter =
            TabAdapter(supportFragmentManager)

        viewPager.adapter = tabAdapter
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                hideKeyboard()

                if (tab.position!=1){
                    viewPager.visibility = View.VISIBLE
                    input_text_layout.visibility = View.GONE
                    container.visibility = View.GONE
                }
                else{
                    container.visibility = View.VISIBLE
                    input_text_layout.visibility = View.VISIBLE
                    viewPager.visibility = View.GONE
                    try {
                        UnityPlayer.UnitySendMessage("TextProcessing","triggerModel", mSettings.getString("character","jasper"))
                    }
                    catch (e:Exception){
                        Log.e("Error",e.toString())
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        val isStacked = intent.getBooleanExtra("isStacked",false)

        if (isStacked){
            viewPager.setCurrentItem(0, false)
            input_text_layout.visibility = View.GONE
            container.visibility = View.GONE
        }
        else{
            viewPager.setCurrentItem(1, false)
            container.visibility = View.VISIBLE
            input_text_layout.visibility = View.VISIBLE
        }

        mSettings = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        val input:EditText = findViewById(R.id.input_text)
        input.setOnEditorActionListener { v, _, _ ->
            hideKeyboard()
            UnityPlayer.UnitySendMessage(
                "TextProcessing",
                "setSliderSpeedValue",
                mSettings.getString("sliderSpeed","0.7")
            )
            UnityPlayer.UnitySendMessage(
                "TextProcessing",
                "triggerAnimation",
                v.text.toString().replace(",","")
            )
            input_text_layout.visibility = View.INVISIBLE
            true
        }
        val container:FrameLayout = findViewById(R.id.container)
        val lp: WindowManager.LayoutParams = WindowManager.LayoutParams(750,630)
        container.addView(mUnityPlayer.view, 0, lp)
        mUnityPlayer.requestFocus()

        UnityPlayer.UnitySendMessage("TextProcessing","triggerModel", mSettings.getString("character","jasper"))
    }

    fun toKamusDetailFragment(text:String, deskripsi:String){
        UnityPlayer.UnitySendMessage("TextProcessing","triggerAnimation", "")
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        val kamusDetailFragment: Fragment =
            KamusDetailFragment.newInstance(
                text = text, description = deskripsi
            )
        fragmentTransaction
            .replace(
                R.id.frame_container,
                kamusDetailFragment
            )

        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()

        val scale: Float = applicationContext.resources.displayMetrics.density
        val pixels = (450 * scale + 0.5f).toInt()

        container.layoutParams.height = pixels
        container.requestLayout()

        kamus_app_bar.visibility = View.VISIBLE
        kamus_toolbar.title = text
        container.visibility = View.VISIBLE
        app_bar.visibility = View.GONE
        tabLayout.visibility = View.GONE
        frame_container.visibility = View.VISIBLE
        viewPager.visibility=View.GONE


        kamus_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

    }

    fun popKamusDetailFragment(){
        UnityPlayer.UnitySendMessage("TextProcessing","triggerAnimation", "")
        container.layoutParams.height = 0
        container.requestLayout()

        val transition = Slide()
        transition.slideEdge = Gravity.START
        transition.duration = 100
        transition.addTarget(kamus_app_bar)
        transition.addTarget(container)
        TransitionManager.beginDelayedTransition(home, transition)

        kamus_app_bar.visibility = View.GONE
        container.visibility = View.GONE
        app_bar.visibility = View.VISIBLE
        tabLayout.visibility = View.VISIBLE
        frame_container.visibility = View.GONE
        viewPager.visibility=View.VISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupUI(view: View) {
        if (view !is EditText || view !is TextInputEditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                false
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    //called from Unity3D
    fun showInput(param:String){
        runOnUiThread {
            if(tabLayout.selectedTabPosition==1){
                input_text_layout.visibility = View.VISIBLE
                input_text.setText("")
            }
        }
    }

    fun hideKeyboard() {
        val imm: InputMethodManager =
            this.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view: View? = this.currentFocus
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    fun quitUnityPlayer(){
        mUnityPlayer.quit()
    }

    fun customBackPressed(){
        runOnUiThread {
            onBackPressed()
        }
    }

    override fun getActivity(): Activity {
        return this
    }
}
