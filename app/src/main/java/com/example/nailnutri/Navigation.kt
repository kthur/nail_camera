package com.example.nailnutri

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.nailnutri.data.DataRepository
import com.example.nailnutri.ui.home.HomeScreen
import com.example.nailnutri.ui.onboarding.OnboardingScreen
import com.example.nailnutri.ui.scan.CameraScanScreen
import com.example.nailnutri.ui.result.AnalysisResultScreen
import com.example.nailnutri.ui.history.HistoryScreen
import com.example.nailnutri.ui.settings.SettingsScreen
import com.example.nailnutri.ui.sensor.SensorDashboardScreen
import com.example.nailnutri.ui.sensor.AnemiaScanScreen
import com.example.nailnutri.ui.sensor.PpgScanScreen
import com.example.nailnutri.ui.sensor.LfaScanScreen
import com.example.nailnutri.ui.sensor.SleepAudioScreen
import com.example.nailnutri.ui.sensor.VoiceAnalysisScreen
import com.example.nailnutri.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(repository: DataRepository, initialRoute: NavKey = Home) {
  val backStack = rememberNavBackStack(initialRoute)
  val scope = rememberCoroutineScope()
  val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(repository))

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Onboarding> {
          OnboardingScreen(
            onComplete = {
              scope.launch { repository.setOnboardingDone(true) }
              backStack.removeLastOrNull()
              backStack.add(Home)
            }
          )
        }
        entry<Home> {
          HomeScreen(
            viewModel = mainViewModel,
            onNavigate = { route -> backStack.add(route) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<CameraScan> {
          CameraScanScreen(
            viewModel = mainViewModel,
            onBackClick = { backStack.removeLastOrNull() },
            onAnalysisComplete = { resultId ->
              backStack.removeLastOrNull()
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = true))
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<AnalysisResult> { key ->
          AnalysisResultScreen(
            resultId = key.resultId,
            isNewScan = key.isNewScan,
            viewModel = mainViewModel,
            onBackClick = { backStack.removeLastOrNull() },
            onNavigateToHome = {
              while (backStack.size > 1) { backStack.removeLastOrNull() }
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<History> {
          HistoryScreen(
            viewModel = mainViewModel,
            onResultClick = { resultId ->
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = false))
            },
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Settings> {
          SettingsScreen(
            viewModel = mainViewModel,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<SensorDashboard> {
          SensorDashboardScreen(
            onNavigate = { route -> backStack.add(route) },
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<AnemiaScan> {
          AnemiaScanScreen(
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            onAnalysisComplete = { resultId ->
              backStack.removeLastOrNull()
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = true))
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<PpgScan> {
          PpgScanScreen(
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            onAnalysisComplete = { resultId ->
              backStack.removeLastOrNull()
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = true))
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<LfaScan> {
          LfaScanScreen(
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            onAnalysisComplete = { resultId ->
              backStack.removeLastOrNull()
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = true))
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<SleepScan> {
          SleepAudioScreen(
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            onAnalysisComplete = { resultId ->
              backStack.removeLastOrNull()
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = true))
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<VoiceScan> {
          VoiceAnalysisScreen(
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            onAnalysisComplete = { resultId ->
              backStack.removeLastOrNull()
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = true))
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
