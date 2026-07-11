package com.example.nailnutri.data

import kotlinx.serialization.Serializable

@Serializable
data class SessionReport(
    val id: String,
    val createdAt: String,
    val label: String,              // 사용자 지정 세션 이름 (e.g. "2024-07-11 오전 세션")
    val resultIds: List<String>,    // 포함된 NailAnalysisResult ID 목록
    val topDeficiencies: List<String>, // 상위 결핍 영양소 이름
    val overallScore: Int           // 0~100 건강 점수 (결핍 영양소 수 기반)
)
