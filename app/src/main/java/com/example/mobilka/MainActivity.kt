package com.example.mobilka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.mobilka.data.AppTheme
import com.example.mobilka.data.FirebaseRepo
import com.example.mobilka.data.SettingsManager
import com.example.mobilka.navigation.AppNavigation
import com.example.mobilka.navigation.Screen
import com.example.mobilka.ui.theme.MobilkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализируем менеджер настроек
        val settingsManager = SettingsManager.getInstance(this)
        
        enableEdgeToEdge()
        setContent {
            // Подписываемся на изменения темы
            val currentTheme by settingsManager.theme.collectAsState()
            val isDarkTheme = currentTheme == AppTheme.DARK
            
            MobilkaTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Определяем начальный экран в зависимости от авторизации Firebase
                    val startDestination = if (FirebaseRepo.instance.isLoggedIn) {
                        Screen.Subscriptions.route
                    } else {
                        Screen.Auth.route
                    }
                    
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}

