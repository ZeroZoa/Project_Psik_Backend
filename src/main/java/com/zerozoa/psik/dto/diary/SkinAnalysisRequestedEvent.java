package com.zerozoa.psik.dto.diary;

public record SkinAnalysisRequestedEvent(
        Long skinAnalysisId,
        String imageUrl,
        String mimeType
) {}
