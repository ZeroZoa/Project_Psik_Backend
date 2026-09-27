package com.zerozoa.psik.service.diary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.JpegWriter;
import com.zerozoa.psik.dto.diary.SkinAnalysisRequestedEvent;
import com.zerozoa.psik.service.ai.GeminiService;
import com.zerozoa.psik.service.storage.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkinAnalysisWorker {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private final SkinAnalysisTxOps txOps;
    private final GeminiService geminiService;
    private final FileStorageService fileStorageService;

    @Value("${gcp.pubsub.skin-analysis-subscription}")
    private String subscriptionName;

    @PostConstruct
    public void subscribe() {
        pubSubTemplate.subscribe(subscriptionName, this::onMessage);
    }

    private void onMessage(BasicAcknowledgeablePubsubMessage message) {
        String payload = message.getPubsubMessage().getData().toStringUtf8();
        try {
            SkinAnalysisRequestedEvent event = objectMapper.readValue(payload, SkinAnalysisRequestedEvent.class);
            process(event);
        } catch (Exception e) {
            log.error("[SkinAnalysisWorker] 메시지 처리 실패 - payload={}", payload, e);
            message.nack(); // 재전달 유도
            return;
        }
        message.ack();
    }

    private void process(SkinAnalysisRequestedEvent event) {
        // Pub/Sub은 at-least-once라 같은 메시지가 중복 도착할 수 있음 — 멱등성 체크
        if (!txOps.isPending(event.skinAnalysisId())) {
            log.info("[SkinAnalysisWorker] 이미 처리됨, 스킵 - skinAnalysisId={}", event.skinAnalysisId());
            return;
        }

        try {
            byte[] storedBytes = fileStorageService.readBytes(event.imageUrl());
            byte[] imageBytes = ImmutableImage.loader()
                    .fromBytes(storedBytes)
                    .bound(512, 512)
                    .bytes(new JpegWriter().withCompression(85));

            String resultJson = geminiService.analyzeSkin(imageBytes, "image/jpeg");
            JsonNode result = objectMapper.readTree(resultJson);

            if (!result.path("faceDetected").asBoolean(false)) {
                txOps.markFailed(event.skinAnalysisId());
                fileStorageService.delete(event.imageUrl());
                return;
            }

            txOps.markCompleted(event.skinAnalysisId(), result);

        } catch (Exception e) {
            // Gemini 실패든 파싱 실패든 원인 불문하고 한 곳에서 정리 (백로그 3번과 동일 원칙)
            log.error("[SkinAnalysisWorker] 분석 실패 - skinAnalysisId={}", event.skinAnalysisId(), e);
            txOps.markFailed(event.skinAnalysisId());
            fileStorageService.delete(event.imageUrl());
        }
    }
}