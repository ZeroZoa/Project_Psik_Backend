package com.zerozoa.psik.service.diary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.zerozoa.psik.service.ai.GeminiService;
import com.zerozoa.psik.service.diary.SkinAnalysisTxOps.Checkpoint;
import com.zerozoa.psik.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock PubSubTemplate pubSubTemplate;
    @Mock SkinAnalysisTxOps txOps;
    @Mock GeminiService geminiService;
    @Mock FileStorageService fileStorageService;
    @Mock BasicAcknowledgeablePubsubMessage message;

    SkinAnalysisWorker worker;

    @BeforeEach
    void setUp() {
        worker = new SkinAnalysisWorker(
                pubSubTemplate, new ObjectMapper(), txOps, geminiService, fileStorageService);
    }

    @Test
    @DisplayName("깨진 JSON 메시지는 ack로 폐기하고 아무 처리도 하지 않는다")
    void malformedJson_ack() {
        // given
        givenPayload("{not-json");

        // when
        worker.onMessage(message);

        // then
        verify(message).ack();
        verify(message, never()).nack();
        verifyNoInteractions(txOps, geminiService, fileStorageService);
    }

    @Test
    @DisplayName("이미 처리된 건은 Gemini 호출 없이 ack한다 (멱등성)")
    void alreadyDone_skipAndAck() {
        // given
        givenPayload(VALID_PAYLOAD);
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.ALREADY_DONE);

        // when
        worker.onMessage(message);

        // then
        verify(message).ack();
        verifyNoInteractions(geminiService);
        verify(txOps, never()).markFailed(anyLong());
        verify(txOps, never()).markCompleted(anyLong(), any());
    }

    @Test
    @DisplayName("접수 후 5분이 지난 건은 처리하지 않고 FAILED + 이미지 삭제 후 ack한다")
    void expired_failAndAck() {
        // given
        givenPayload(VALID_PAYLOAD);
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.EXPIRED);

        // when
        worker.onMessage(message);

        // then
        verify(txOps).markFailed(ID);
        verify(fileStorageService).delete(IMAGE_URL);
        verify(message).ack();
        verifyNoInteractions(geminiService);
    }

    @Test
    @DisplayName("얼굴이 인식된 정상 응답이면 COMPLETED 처리 후 ack한다")
    void faceDetected_complete() throws Exception {
        // given
        givenPayload(VALID_PAYLOAD);
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.PROCEED);
        when(fileStorageService.readBytes(IMAGE_URL)).thenReturn(tinyPng());
        when(geminiService.analyzeSkin(any(byte[].class), anyString())).thenReturn(
                "{\"faceDetected\":true,\"acneScore\":80,\"wrinkleScore\":70,"
                        + "\"toneScore\":60,\"oilScore\":50,\"summary\":\"양호\"}");

        // when
        worker.onMessage(message);

        // then
        verify(txOps).markCompleted(eq(ID), any(JsonNode.class));
        verify(txOps, never()).markFailed(anyLong());
        verify(fileStorageService, never()).delete(anyString());
        verify(message).ack();
    }

    @Test
    @DisplayName("얼굴이 없으면 FAILED 처리 + 이미지 삭제 후 ack한다")
    void noFace_failAndAck() throws Exception {
        // given
        givenPayload(VALID_PAYLOAD);
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.PROCEED);
        when(fileStorageService.readBytes(IMAGE_URL)).thenReturn(tinyPng());
        when(geminiService.analyzeSkin(any(byte[].class), anyString())).thenReturn(
                "{\"faceDetected\":false,\"acneScore\":0,\"wrinkleScore\":0,"
                        + "\"toneScore\":0,\"oilScore\":0,\"summary\":\"\"}");

        // when
        worker.onMessage(message);

        // then
        verify(txOps).markFailed(ID);
        verify(fileStorageService).delete(IMAGE_URL);
        verify(txOps, never()).markCompleted(anyLong(), any());
        verify(message).ack();
    }

    @Test
    @DisplayName("Gemini 호출이 예외로 실패하면 FAILED 처리 + 이미지 삭제 후 ack한다")
    void geminiFails_failAndAck() throws Exception {
        // given
        givenPayload(VALID_PAYLOAD);
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.PROCEED);
        when(fileStorageService.readBytes(IMAGE_URL)).thenReturn(tinyPng());
        when(geminiService.analyzeSkin(any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("gemini down"));

        // when
        worker.onMessage(message);

        // then
        verify(txOps).markFailed(ID);
        verify(fileStorageService).delete(IMAGE_URL);
        verify(message).ack();
        verify(message, never()).nack();
    }

    @Test
    @DisplayName("실패 처리(DB) 자체가 예외면 nack으로 재전달을 요청한다")
    void markFailedThrows_nack() {
        // given
        givenPayload(VALID_PAYLOAD);
        when(txOps.check(eq(ID), any(Duration.class))).thenReturn(Checkpoint.EXPIRED);
        doThrow(new RuntimeException("db down")).when(txOps).markFailed(ID);

        // when
        worker.onMessage(message);

        // then
        verify(message).nack();
        verify(message, never()).ack();
    }

    // ───────── 헬퍼 ─────────

    private void givenPayload(String json) {
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(json))
                .build();
        when(message.getPubsubMessage()).thenReturn(pubsubMessage);
    }

    private byte[] tinyPng() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
