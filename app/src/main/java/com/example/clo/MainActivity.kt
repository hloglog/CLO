package com.example.clo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // FriendProfileActivity에서 전달된 Fragment 정보 처리
        val fragmentToShow = intent.getStringExtra("fragment")
        if (fragmentToShow != null) {
            when (fragmentToShow) {
                "home" -> bottomNavigationView.selectedItemId = R.id.navigation_home
                "search" -> bottomNavigationView.selectedItemId = R.id.navigation_search
                "profile" -> bottomNavigationView.selectedItemId = R.id.navigation_profile
            }
        }

        // TODO: 로그인 상태 확인 및 필요 시 LoginActivity로 이동 로직 추가 (현재는 SplashActivity에서 처리)
    }
}