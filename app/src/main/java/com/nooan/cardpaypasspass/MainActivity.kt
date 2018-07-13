package com.nooan.cardpaypasspass

import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*

interface OnFragmentInteractionListener {
    fun onFragmentInteraction(uri: Uri)
}

class MainActivity : AppCompatActivity(), OnFragmentInteractionListener {
    override fun onFragmentInteraction(uri: Uri) {
        
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_emulator_main -> {
                startFragmentTransaction(EmulatorTerminalMainFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_logs -> {
                startFragmentTransaction(LogsFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_reads -> {
                startFragmentTransaction(ReadFileFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private fun startFragmentTransaction(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, fragment)
                .commit()
    }
}
