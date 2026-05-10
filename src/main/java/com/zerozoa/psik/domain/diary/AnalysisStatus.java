package com.zerozoa.psik.domain.diary;

/**
 * 피부 분석 상태 Enum
 * SkinAnalysis 생성 시 PENDING으로 시작하며, AI 분석 완료 시 COMPLETED, 실패 시 FAILED로 전환
 */
public enum AnalysisStatus {
    PENDING, // 분석 대기
    COMPLETED, // 분석 완료
    FAILED // 분석 실패
}