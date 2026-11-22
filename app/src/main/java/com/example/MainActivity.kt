package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.StudioViewModel
import com.example.ui.VoiceStudioScreen
import com.example.ui.theme.AiVoiceStudioTheme
import com.example.ui.theme.StudioVoid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.studio_void)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val studioViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[StudioViewModel::class.java]

        setContent {
            AiVoiceStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioVoid
                ) {
                    VoiceStudioScreen(viewModel = studioViewModel)
                }
            }
        }
    }
}
