package com.example.nailnutri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.nailnutri.data.DefaultDataRepository
import com.example.nailnutri.theme.NailNutriTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val repository = DefaultDataRepository(applicationContext)

    enableEdgeToEdge()
    setContent {
      val darkTheme by repository.isDarkTheme.collectAsState(initial = false)
      val dynamicColor by repository.useDynamicColor.collectAsState(initial = false)
      val onboardingDone by repository.onboardingDone.collectAsState(initial = false)

      NailNutriTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainNavigation(
            repository = repository,
            initialRoute = if (onboardingDone) Home else Onboarding
          )
        }
      }
    }
  }
}
