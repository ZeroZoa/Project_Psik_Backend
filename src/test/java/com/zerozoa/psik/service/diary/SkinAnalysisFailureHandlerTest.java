package com.zerozoa.psik.service.diary;

import com.zerozoa.psik.service.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SkinAnalysisFailureHandlerTest {

    private static final Long ID = 1L;
    private static final String IMAGE_URL = "https://storage.example.com/analysis/a.webp";

    @Mock SkinAnalysisTxOps txOps;
    @Mock FileStorageService fileStorageService;
    @InjectMocks SkinAnalysisFailureHandler handler;

    @Test
    @DisplayName("fail: FAILED로 전환한 뒤 사진을 삭제한다")
    void fail_marksFailedThenDeletesImage() {
        // when
        handler.fail(ID, IMAGE_URL);

        // then
        InOrder inOrder = inOrder(txOps, fileStorageService);
        inOrder.verify(txOps).markFailed(ID);
        inOrder.verify(fileStorageService).delete(IMAGE_URL);
    }

    @Test
    @DisplayName("fail: FAILED 전환(DB)이 실패하면 사진은 삭제하지 않는다")
    void fail_markFailedThrows_imageKept() {
        // given
        doThrow(new RuntimeException("db down")).when(txOps).markFailed(ID);

        // when & then
        try {
            handler.fail(ID, IMAGE_URL);
        } catch (RuntimeException expected) {
            // 예외는 호출자(워커)가 nack으로 처리한다
        }
        verifyNoInteractions(fileStorageService);
    }
}
