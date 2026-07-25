package com.example.nailnutri.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nailnutri.data.DataRepository
import com.example.nailnutri.data.NailAnalysisResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: DataRepository
) : ViewModel() {

    val history: StateFlow<List<NailAnalysisResult>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isMockMode = repository.isMockMode
    val apiKey = repository.apiKey
    val useOnDeviceVision = repository.useOnDeviceVision
    val useGemma = repository.useGemma
    val gemmaModelPath = repository.gemmaModelPath
    val isDarkTheme = repository.isDarkTheme
    val useDynamicColor = repository.useDynamicColor
    val reminderEnabled = repository.reminderEnabled

    fun saveResult(result: NailAnalysisResult) {
        viewModelScope.launch { repository.saveResult(result) }
    }

    fun deleteResult(id: String) {
        viewModelScope.launch { repository.deleteResult(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch { repository.setApiKey(key) }
    }

    fun setMockMode(enabled: Boolean) {
        viewModelScope.launch { repository.setMockMode(enabled) }
    }

    fun setGemmaModelPath(path: String) {
        viewModelScope.launch { repository.setGemmaModelPath(path) }
    }

    fun setUseGemma(enabled: Boolean) {
        viewModelScope.launch { repository.setUseGemma(enabled) }
    }

    fun setUseOnDeviceVision(enabled: Boolean) {
        viewModelScope.launch { repository.setUseOnDeviceVision(enabled) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkTheme(enabled) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setUseDynamicColor(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setReminderEnabled(enabled) }
    }

    fun setOnboardingDone(done: Boolean) {
        viewModelScope.launch { repository.setOnboardingDone(done) }
    }

    class Factory(private val repository: DataRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
