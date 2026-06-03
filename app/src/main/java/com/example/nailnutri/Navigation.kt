package com.example.nailnutri

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.nailnutri.data.DataRepository
import com.example.nailnutri.ui.home.HomeScreen
import com.example.nailnutri.ui.scan.CameraScanScreen
import com.example.nailnutri.ui.result.AnalysisResultScreen
import com.example.nailnutri.ui.history.HistoryScreen
import com.example.nailnutri.ui.settings.SettingsScreen

@Composable
fun MainNavigation(repository: DataRepository) {
  val backStack = rememberNavBackStack(Home)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Home> {
          HomeScreen(
            repository = repository,
            onNavigate = { route -> backStack.add(route) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<CameraScan> {
          CameraScanScreen(
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            onAnalysisComplete = { resultId ->
              // Pop scan screen, then push result
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
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            onNavigateToHome = {
              // Clear stack down to Home
              while (backStack.size > 1) {
                backStack.removeLastOrNull()
              }
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<History> {
          HistoryScreen(
            repository = repository,
            onResultClick = { resultId ->
              backStack.add(AnalysisResult(resultId = resultId, isNewScan = false))
            },
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Settings> {
          SettingsScreen(
            repository = repository,
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
