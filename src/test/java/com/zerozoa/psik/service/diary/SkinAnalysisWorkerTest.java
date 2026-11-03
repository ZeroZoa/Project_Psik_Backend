package com.zerozoa.psik.service.diary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerozoa.psik.service.ai.GeminiService;
import com.zerozoa.psik.service.diary.SkinAnalysisTxOps.Checkpoint;
import com.zerozoa.psik.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkinAnalysisWorkerTest {

    private static final long ID = 1L;
    private static final String IMAGE_URL = "http://localhost:8080/uploads/analysis/a.webp";
    private static final String VALID_PAYLOAD =
            "{\"skinAnalysisId\":1,\"imageUrl\":\"" + IMAGE_URL + "\",\"mimeType\":\"image/jpeg\"}";

    @Mock SkinAnalysisTxOps txOps;
    @Mock GeminiService geminiService;
    @Mock FileStorageService fileStorageService;

    SkinAnalysisWorker worker;

    @BeforeEach
    void setUp() {
        worker = new SkinAnalysisWorker(
                new ObjectMapper(), txOps, geminiService, fileStorageService,
                new SkinAnalysisFailureHandler(txOps, fileStorageService));
    }

    @Test
    @DisplayName("깨진 JSON 메시지는 처리 완료(true)로 폐기하고 아무 처리도 하지 않는다")
    void malformedJson_ack() {
        // given
        String payload = "{not-json";

        // when
        boolean acked = worker.handle(payload);

        // then
        assertThat(acked).isTrue();
        verifyNoInteractions(txOps, geminiService, fileStorageService);
    }

    @Test
    @DisplayName("이미 처리된 건은 Gemini 호출 없이 ack한다 (멱등성)")
    void alreadyDone_skipAndAck() {
        // given
        String payload = VALID_PAYLOAD;
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.ALREADY_DONE);

        // when
        boolean acked = worker.handle(payload);

        // then
        assertThat(acked).isTrue();
        verifyNoInteractions(geminiService);
        verify(txOps, never()).markFailed(anyLong());
        verify(txOps, never()).markCompleted(anyLong(), any());
    }

    @Test
    @DisplayName("접수 후 5분이 지난 건은 처리하지 않고 FAILED + 이미지 삭제 후 ack한다")
    void expired_failAndAck() {
        // given
        String payload = VALID_PAYLOAD;
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.EXPIRED);

        // when
        boolean acked = worker.handle(payload);

        // then
        verify(txOps).markFailed(ID);
        verify(fileStorageService).delete(IMAGE_URL);
        assertThat(acked).isTrue();
        verifyNoInteractions(geminiService);
    }

    @Test
    @DisplayName("얼굴이 인식된 정상 응답이면 COMPLETED 처리 후 ack한다")
    void faceDetected_complete() throws Exception {
        // given
        String payload = VALID_PAYLOAD;
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.PROCEED);
        when(fileStorageService.readBytes(IMAGE_URL)).thenReturn(tinyPng());
        when(geminiService.analyzeSkin(any(byte[].class), anyString())).thenReturn(
                "{\"faceDetected\":true,\"acneScore\":80,\"wrinkleScore\":70,"
                        + "\"toneScore\":60,\"oilScore\":50,\"summary\":\"양호\"}");

        // when
        boolean acked = worker.handle(payload);

        // then
        verify(txOps).markCompleted(eq(ID), any(JsonNode.class));
        verify(txOps, never()).markFailed(anyLong());
        verify(fileStorageService, never()).delete(anyString());
        assertThat(acked).isTrue();
    }

    @Test
    @DisplayName("얼굴이 없으면 FAILED 처리 + 이미지 삭제 후 ack한다")
    void noFace_failAndAck() throws Exception {
        // given
        String payload = VALID_PAYLOAD;
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.PROCEED);
        when(fileStorageService.readBytes(IMAGE_URL)).thenReturn(tinyPng());
        when(geminiService.analyzeSkin(any(byte[].class), anyString())).thenReturn(
                "{\"faceDetected\":false,\"acneScore\":0,\"wrinkleScore\":0,"
                        + "\"toneScore\":0,\"oilScore\":0,\"summary\":\"\"}");

        // when
        boolean acked = worker.handle(payload);

        // then
        verify(txOps).markFailed(ID);
        verify(fileStorageService).delete(IMAGE_URL);
        verify(txOps, never()).markCompleted(anyLong(), any());
        assertThat(acked).isTrue();
    }

    @Test
    @DisplayName("Gemini 호출이 예외로 실패하면 FAILED 처리 + 이미지 삭제 후 ack한다")
    void geminiFails_failAndAck() throws Exception {
        // given
        String payload = VALID_PAYLOAD;
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.PROCEED);
        when(fileStorageService.readBytes(IMAGE_URL)).thenReturn(tinyPng());
        when(geminiService.analyzeSkin(any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("gemini down"));

        // when
        boolean acked = worker.handle(payload);

        // then
        verify(txOps).markFailed(ID);
        verify(fileStorageService).delete(IMAGE_URL);
        assertThat(acked).isTrue();
    }

    @Test
    @DisplayName("실패 처리(DB) 자체가 예외면 false(5xx)로 재전달을 요청한다")
    void markFailedThrows_nack() {
        // given
        String payload = VALID_PAYLOAD;
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.EXPIRED);
        doThrow(new RuntimeException("db down")).when(txOps).markFailed(ID);

        // when
        boolean acked = worker.handle(payload);

        // then
        assertThat(acked).isFalse();
    }

    // ───────── 헬퍼 ─────────

    private byte[] tinyPng() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
