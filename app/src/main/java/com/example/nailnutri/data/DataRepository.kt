package com.example.nailnutri.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

interface DataRepository {
    // Keep compatibility with template's simple data flow if needed, but we will replace its usage.
    val data: Flow<List<String>>

    val history: Flow<List<NailAnalysisResult>>
    val apiKey: Flow<String>
    val isMockMode: Flow<Boolean>

    suspend fun saveResult(result: NailAnalysisResult)
    suspend fun clearHistory()
    suspend fun deleteResult(id: String)
    suspend fun setApiKey(key: String)
    suspend fun setMockMode(enabled: Boolean)
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("nail_nutri_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _history = MutableStateFlow<List<NailAnalysisResult>>(emptyList())
    override val history = _history.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    override val apiKey = _apiKey.asStateFlow()

    private val _isMockMode = MutableStateFlow(true) // Default to Mock Mode for testing
    override val isMockMode = _isMockMode.asStateFlow()

    // Temporary compatibility data flow
    override val data: Flow<List<String>> = flowFromHistory()

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

        _apiKey.value = prefs.getString("api_key", "") ?: ""
        _isMockMode.value = prefs.getBoolean("is_mock_mode", true)
    }

    private fun flowFromHistory(): Flow<List<String>> = kotlinx.coroutines.flow.flow {
        history.collect { list ->
            emit(list.map { "${it.date}: ${it.symptoms.joinToString()}" })
        }
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

    private fun saveHistoryList(list: List<NailAnalysisResult>) {
        val sortedList = list.sortedByDescending { it.date }
        _history.value = sortedList
        val historyJson = json.encodeToString(sortedList)
        prefs.edit().putString("scan_history", historyJson).apply()
    }
}
