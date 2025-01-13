package com.example.aplikacjafitness

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

import java.io.Console

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus){
            hideSystemUI()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun hideSystemUI() {
        // Ukrywa pasek nawigacyjny (dolny pasek) oraz pasek statusu
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    protected fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        Log.i("BaseActivity", "Setting up BottomNavigationView")

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.main -> {
                    Log.i("BaseActivity", "Main item clicked")
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    true
                }
//                R.id.workout -> {
//                    Log.i("BaseActivity", "Workout item clicked")
//                    if (this !is Progress) {
//                        startActivity(Intent(this, Progress::class.java))
//                    }
//                    true
//                }

                R.id.map -> {
                    Log.i("BaseActivity", "Map item clicked")
                    if (this !is MapActivity) {
                        startActivity(Intent(this, MapActivity::class.java))
                    }
                    true
                }
                R.id.summary -> {
                    Log.i("BaseActivity", "Summary item clicked")
                    if (this !is SummaryActivity) {
                        startActivity(Intent(this, SummaryActivity::class.java))
                    }
                    true
                }

                R.id.profile -> {
                    Log.i("BaseActivity", "Profile item clicked")
                    if (this !is Profile) {
                        startActivity(Intent(this, Profile::class.java))
                    }
                    true
                }
                else -> false
            }
        }
    }

}