package com.leonxlnx.imagesorter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.leonxlnx.imagesorter.ui.AppRoot
import com.leonxlnx.imagesorter.ui.theme.ImageSorterTheme
import com.leonxlnx.imagesorter.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = (application as ImageSorterApp).settingsRepository
        setContent {
            val themeFlow = settingsRepository.theme.collectAsState(initial = ThemeMode.System)
            val mode by themeFlow
            ImageSorterTheme(themeMode = mode) {
                AppRoot()
            }
        }
    }
}
