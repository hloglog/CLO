package com.example.clo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavOptions
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // bottomNavigationView.setupWithNavController(navController) // 기존 코드 제거

        // 커스텀 네비게이터 클릭 리스너 추가
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    navController.navigate(R.id.menu_home, null, NavOptions.Builder().setLaunchSingleTop(true).build())
                    true
                }
                R.id.menu_search -> {
                    navController.navigate(R.id.menu_search, null, NavOptions.Builder().setLaunchSingleTop(true).build())
                    true
                }
                R.id.menu_mypage -> {
                    navController.navigate(R.id.menu_mypage, null, NavOptions.Builder().setLaunchSingleTop(true).build())
                    true
                }
                else -> false
            }
        }

        // 앱 시작 시 홈 아이콘이 선택되도록 설정
        bottomNavigationView.selectedItemId = R.id.menu_home

        // FriendProfileActivity에서 전달된 Fragment 정보 처리
        val fragmentToShow = intent.getStringExtra("fragment")
        if (fragmentToShow != null) {
            when (fragmentToShow) {
                "home" -> bottomNavigationView.selectedItemId = R.id.menu_home
                "search" -> bottomNavigationView.selectedItemId = R.id.menu_search
                "closet" -> {
                    bottomNavigationView.selectedItemId = R.id.menu_closet
                    navController.navigate(R.id.menu_closet, null, NavOptions.Builder().setLaunchSingleTop(true).build())
                }
                "profile" -> bottomNavigationView.selectedItemId = R.id.menu_mypage
            }
        }

        // TODO: 로그인 상태 확인 및 필요 시 LoginActivity로 이동 로직 추가 (현재는 SplashActivity에서 처리)
    }
}