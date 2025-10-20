package com.example.alertmate

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.alertmate.fragment.NewsFragment
import com.example.alertmate.home.HomeFragment
import com.example.alertmate.fragment.ProfileFragment
import com.example.alertmate.fragment.TipsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val role = intent.getStringExtra("role")

        // Show admin button only if admin
        if (role == "admin") {
            bottomNav.menu.findItem(R.id.bott_admin).isVisible = true
        } else {
            bottomNav.menu.findItem(R.id.bott_admin).isVisible = false
        }

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
                R.id.bott_profile -> loadFragment(ProfileFragment())
                R.id.bott_news -> loadFragment(NewsFragment())
                R.id.bott_admin -> loadFragment(AdminFragment())
                else -> false
            }
            true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

    }

    // helper function to replace fragments
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
