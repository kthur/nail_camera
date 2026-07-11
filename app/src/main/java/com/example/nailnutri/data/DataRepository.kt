package com.example.nailnutri.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

interface DataRepository {
    val history: Flow<List<NailAnalysisResult>>
    val sessions: Flow<List<SessionReport>>
    val apiKey: Flow<String>
    val isMockMode: Flow<Boolean>
    val gemmaModelPath: Flow<String>
    val useGemma: Flow<Boolean>
    val useOnDeviceVision: Flow<Boolean>
    val onboardingDone: Flow<Boolean>
    val isDarkTheme: Flow<Boolean>
    val useDynamicColor: Flow<Boolean>
    val reminderEnabled: Flow<Boolean>

    suspend fun saveResult(result: NailAnalysisResult)
    suspend fun clearHistory()
    suspend fun deleteResult(id: String)
    suspend fun setApiKey(key: String)
    suspend fun setMockMode(enabled: Boolean)
    suspend fun setGemmaModelPath(path: String)
    suspend fun setUseGemma(enabled: Boolean)
    suspend fun setUseOnDeviceVision(enabled: Boolean)
    suspend fun setOnboardingDone(done: Boolean)
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setUseDynamicColor(enabled: Boolean)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun saveSession(report: SessionReport)
    suspend fun deleteSession(id: String)
    suspend fun clearSessions()
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("nail_nutri_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _history = MutableStateFlow<List<NailAnalysisResult>>(emptyList())
    override val history = _history.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionReport>>(emptyList())
    override val sessions = _sessions.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    override val apiKey = _apiKey.asStateFlow()

    private val _isMockMode = MutableStateFlow(true)
    override val isMockMode = _isMockMode.asStateFlow()

    private val _gemmaModelPath = MutableStateFlow("/data/local/tmp/gemma.bin")
    override val gemmaModelPath = _gemmaModelPath.asStateFlow()

    private val _useGemma = MutableStateFlow(false)
    override val useGemma = _useGemma.asStateFlow()

    private val _useOnDeviceVision = MutableStateFlow(false)
    override val useOnDeviceVision = _useOnDeviceVision.asStateFlow()

    private val _onboardingDone = MutableStateFlow(false)
    override val onboardingDone = _onboardingDone.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    override val isDarkTheme = _isDarkTheme.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(false)
    override val useDynamicColor = _useDynamicColor.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(false)
    override val reminderEnabled = _reminderEnabled.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val historyJson = prefs.getString("scan_history", "[]") ?: "[]"
        try {
            val list = json.decodeFromString<List<NailAnalysisResult>>(historyJson)
            _history.value = list.sortedByDescending { it.date }
        } catch (e: Exception) {
            _history.value = emptyList()
        }

        val sessionsJson = prefs.getString("session_reports", "[]") ?: "[]"
        try {
            _sessions.value = json.decodeFromString<List<SessionReport>>(sessionsJson).sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            _sessions.value = emptyList()
        }

        _apiKey.value = prefs.getString("api_key", "") ?: ""
        _isMockMode.value = prefs.getBoolean("is_mock_mode", true)
        _gemmaModelPath.value = prefs.getString("gemma_model_path", "/data/local/tmp/gemma.bin") ?: "/data/local/tmp/gemma.bin"
        _useGemma.value = prefs.getBoolean("use_gemma", false)
        _useOnDeviceVision.value = prefs.getBoolean("use_on_device_vision", false)
        _onboardingDone.value = prefs.getBoolean("onboarding_done", false)
        _isDarkTheme.value = prefs.getBoolean("is_dark_theme", false)
        _useDynamicColor.value = prefs.getBoolean("use_dynamic_color", false)
        _reminderEnabled.value = prefs.getBoolean("reminder_enabled", false)
    }

    override suspend fun saveResult(result: NailAnalysisResult) {
        val currentList = _history.value.toMutableList()
        // Remove duplicate if exists, then add at start
        currentList.removeAll { it.id == result.id }
        currentList.add(0, result)
        saveHistoryList(currentList)
    }

    override suspend fun deleteResult(id: String) {
        val currentList = _history.value.toMutableList()
        currentList.removeAll { it.id == id }
        saveHistoryList(currentList)
    }

    override suspend fun clearHistory() {
        saveHistoryList(emptyList())
    }

    override suspend fun setApiKey(key: String) {
        prefs.edit().putString("api_key", key).apply()
        _apiKey.value = key
    }

    override suspend fun setMockMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_mock_mode", enabled).apply()
        _isMockMode.value = enabled
    }

    override suspend fun setGemmaModelPath(path: String) {
        prefs.edit().putString("gemma_model_path", path).apply()
        _gemmaModelPath.value = path
    }

    override suspend fun setUseGemma(enabled: Boolean) {
        prefs.edit().putBoolean("use_gemma", enabled).apply()
        _useGemma.value = enabled
    }

    override suspend fun setUseOnDeviceVision(enabled: Boolean) {
        prefs.edit().putBoolean("use_on_device_vision", enabled).apply()
        _useOnDeviceVision.value = enabled
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean("onboarding_done", done).apply()
        _onboardingDone.value = done
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
        _isDarkTheme.value = enabled
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("use_dynamic_color", enabled).apply()
        _useDynamicColor.value = enabled
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminder_enabled", enabled).apply()
        _reminderEnabled.value = enabled
    }

    private fun saveHistoryList(list: List<NailAnalysisResult>) {
        val sortedList = list.sortedByDescending { it.date }
        _history.value = sortedList
        val historyJson = json.encodeToString(sortedList)
        prefs.edit().putString("scan_history", historyJson).apply()
    }

    override suspend fun saveSession(report: SessionReport) {
        val list = _sessions.value.toMutableList()
        list.removeAll { it.id == report.id }
        list.add(0, report)
        saveSessionList(list)
    }

    override suspend fun deleteSession(id: String) {
        val list = _sessions.value.toMutableList()
        list.removeAll { it.id == id }
        saveSessionList(list)
    }

    override suspend fun clearSessions() {
        saveSessionList(emptyList())
    }

    private fun saveSessionList(list: List<SessionReport>) {
        val sorted = list.sortedByDescending { it.createdAt }
        _sessions.value = sorted
        prefs.edit().putString("session_reports", json.encodeToString(sorted)).apply()
    }
}
