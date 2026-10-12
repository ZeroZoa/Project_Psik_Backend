package com.zerozoa.psik.service.diary;

import com.zerozoa.psik.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 피부 분석을 실패로 종료하는 공통 처리.
 * 워커(처리 실패), 서비스(발행 실패), 스위퍼(오래된 PENDING)가 같은 방식으로 정리하도록 한 곳에 모았다.
 */
@Component
@RequiredArgsConstructor
public class SkinAnalysisFailureHandler {

    private final SkinAnalysisTxOps txOps;
    private final FileStorageService fileStorageService;

    /** 분석을 FAILED로 종료하고 업로드된 사진을 정리한다 */
    public void fail(Long skinAnalysisId, String imageUrl) {
        txOps.markFailed(skinAnalysisId);
        fileStorageService.delete(imageUrl);
    }
}
