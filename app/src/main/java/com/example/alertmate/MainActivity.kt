package com.example.alertmate

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Load HomeFragment first when activity starts
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            bottomNav.selectedItemId = R.id.bott_home
        }

        // Set up Bottom Navigation
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bott_home -> loadFragment(HomeFragment())
                R.id.bott_tips -> loadFragment(TipsFragment())
                R.id.bott_alert -> loadFragment(AlertFragment())
                R.id.bott_profile -> loadFragment(ProfileFragment())
                R.id.bott_news -> loadFragment(NewsFragment())
                else -> false
            }
            true
        }
    }

    // helper function to replace fragments
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
