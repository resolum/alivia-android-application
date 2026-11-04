package com.resolum.alivia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import com.resolum.alivia.core.navigation.AppNavHost
import com.resolum.alivia.core.ui.theme.AliviaTheme
import com.resolum.alivia.core.ui.theme.Background

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AliviaTheme {
                Box(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
                    AppNavHost()
                }
            }
        }
    }
}
