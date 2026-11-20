package com.example.nailnutri.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.nailnutri.data.db.DbMapper
import com.example.nailnutri.data.db.NailDatabase
import com.example.nailnutri.data.db.NailResultDao
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "nail_nutri_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            // Graceful fallback for JVM unit tests or environments where Android Keystore is unavailable
            prefs
        }
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val dao: NailResultDao = NailDatabase.getDatabase(context).nailResultDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val history: Flow<List<NailAnalysisResult>> = dao.getAllHistoryFlow().map { list ->
        list.map { DbMapper.toDomain(it) }
    }

    override val sessions: Flow<List<SessionReport>> = dao.getAllSessionsFlow().map { list ->
        list.map { DbMapper.toDomain(it) }
    }

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
        loadSettings()
        migrateIfNeeded()
    }

    private fun loadSettings() {
        val savedKey = try {
            securePrefs.getString("api_key", null)
        } catch (e: Throwable) {
            null
        } ?: prefs.getString("api_key", "") ?: ""
        _apiKey.value = savedKey
        _isMockMode.value = prefs.getBoolean("is_mock_mode", true)
        _gemmaModelPath.value = prefs.getString("gemma_model_path", "/data/local/tmp/gemma.bin") ?: "/data/local/tmp/gemma.bin"
        _useGemma.value = prefs.getBoolean("use_gemma", false)
        _useOnDeviceVision.value = prefs.getBoolean("use_on_device_vision", false)
        _onboardingDone.value = prefs.getBoolean("onboarding_done", false)
        _isDarkTheme.value = prefs.getBoolean("is_dark_theme", false)
        _useDynamicColor.value = prefs.getBoolean("use_dynamic_color", false)
        _reminderEnabled.value = prefs.getBoolean("reminder_enabled", false)
    }

    private fun migrateIfNeeded() {
        if (prefs.contains("scan_history") || prefs.contains("session_reports")) {
            scope.launch {
                var historyMigrated = false
                var sessionsMigrated = false

                if (prefs.contains("scan_history")) {
                    val historyJson = prefs.getString("scan_history", "[]") ?: "[]"
                    try {
                        val list = json.decodeFromString<List<NailAnalysisResult>>(historyJson)
                        list.forEach { dao.insertResult(DbMapper.toEntity(it)) }
                        historyMigrated = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    historyMigrated = true
                }

                if (prefs.contains("session_reports")) {
                    val sessionsJson = prefs.getString("session_reports", "[]") ?: "[]"
                    try {
                        val list = json.decodeFromString<List<SessionReport>>(sessionsJson)
                        list.forEach { dao.insertSession(DbMapper.toEntity(it)) }
                        sessionsMigrated = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    sessionsMigrated = true
                }

                val editor = prefs.edit()
                if (historyMigrated) editor.remove("scan_history")
                if (sessionsMigrated) editor.remove("session_reports")
                editor.apply()
            }
        }
    }

    override suspend fun saveResult(result: NailAnalysisResult) {
        dao.insertResult(DbMapper.toEntity(result))
    }

    override suspend fun deleteResult(id: String) {
        try {
            val entity = dao.getResultById(id)
            if (entity != null && entity.imagePath.isNotBlank()) {
                val file = File(entity.imagePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.deleteResultById(id)
    }

    override suspend fun clearHistory() {
        try {
            val list = dao.getAllHistoryDirect()
            list.forEach { entity ->
                if (entity.imagePath.isNotBlank()) {
                    val file = File(entity.imagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.clearHistory()
    }

    override suspend fun setApiKey(key: String) {
        var encrypted = false
        try {
            securePrefs.edit().putString("api_key", key).apply()
            encrypted = (securePrefs !== prefs)
        } catch (e: Throwable) {
            prefs.edit().putString("api_key", key).apply()
        }
        if (encrypted && key.isNotEmpty()) {
            prefs.edit().remove("api_key").apply()
        }
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

    override suspend fun saveSession(report: SessionReport) {
        dao.insertSession(DbMapper.toEntity(report))
    }

    override suspend fun deleteSession(id: String) {
        dao.deleteSessionById(id)
    }

    override suspend fun clearSessions() {
        dao.clearSessions()
    }
}
