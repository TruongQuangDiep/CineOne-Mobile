package com.example.cineone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cineone.navigation.AppNavGraph
import com.example.cineone.ui.theme.AppThemePreferences
import com.example.cineone.ui.theme.CineBackgroundDark
import com.example.cineone.ui.theme.CineOneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppThemePreferences.init(applicationContext)
        setContent {
            CineOneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CineBackgroundDark
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
