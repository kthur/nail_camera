package com.example.nailnutri.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NailResultDao {

    @Query("SELECT * FROM nail_analysis_results ORDER BY date DESC")
    fun getAllHistoryFlow(): Flow<List<NailAnalysisResultEntity>>

    @Query("SELECT * FROM nail_analysis_results")
    suspend fun getAllHistoryDirect(): List<NailAnalysisResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: NailAnalysisResultEntity)

    @Query("DELETE FROM nail_analysis_results WHERE id = :id")
    suspend fun deleteResultById(id: String)

    @Query("DELETE FROM nail_analysis_results")
    suspend fun clearHistory()

    // ─────────────────────────────────────────────
    //  Session Database Actions
    // ─────────────────────────────────────────────

    @Query("SELECT * FROM session_reports ORDER BY createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<SessionReportEntity>>

    @Query("SELECT * FROM session_reports")
    suspend fun getAllSessionsDirect(): List<SessionReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionReportEntity)

    @Query("DELETE FROM session_reports WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM session_reports")
    suspend fun clearSessions()
}
