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
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkinAnalysisWorker {

    private static final long PROCESS_TIMEOUT_SECONDS = 30;

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private final SkinAnalysisTxOps txOps;
    private final GeminiService geminiService;
    private final FileStorageService fileStorageService;

    @Value("${gcp.pubsub.skin-analysis-subscription}")
    private String subscriptionName;

    // GCS 읽기/Gemini 호출처럼 블로킹 I/O가 발생하는 작업 전용 풀.
    // 공용 ForkJoinPool을 쓰면 여기서 멈춘 작업이 무관한 다른 병렬 작업의 스레드까지 잠식할 수 있어 분리.
    private final ExecutorService analysisExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "skin-analysis-worker");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void subscribe() {
        pubSubTemplate.subscribe(subscriptionName, this::onMessage);
    }

    @PreDestroy
    public void shutdown() {
        analysisExecutor.shutdownNow();
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

        Future<JsonNode> future = analysisExecutor.submit(() -> {
            byte[] storedBytes = fileStorageService.readBytes(event.imageUrl());
            byte[] imageBytes = ImmutableImage.loader()
                    .fromBytes(storedBytes)
                    .bound(512, 512)
                    .bytes(new JpegWriter().withCompression(85));

            String resultJson = geminiService.analyzeSkin(imageBytes, "image/jpeg");
            return objectMapper.readTree(resultJson);
        });

        try {
            JsonNode result = future.get(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!result.path("faceDetected").asBoolean(false)) {
                txOps.markFailed(event.skinAnalysisId());
                fileStorageService.delete(event.imageUrl());
                return;
            }

            txOps.markCompleted(event.skinAnalysisId(), result);

        } catch (TimeoutException e) {
            // GCS 읽기/이미지 리사이즈/Gemini 호출 중 어디서 멈추든 30초 안에 강제 실패 처리.
            // cancel(true)로 인터럽트를 시도하지만, 블로킹 소켓 I/O는 인터럽트에 반응 안 할 수도 있어
            // 스레드 자체는 못 끊길 수 있음 — 그래서 이 작업들을 전용 풀로 격리해뒀음(전역 풀 오염 방지).
            future.cancel(true);
            log.error("[SkinAnalysisWorker] 처리 시간 초과 - skinAnalysisId={}", event.skinAnalysisId());
            txOps.markFailed(event.skinAnalysisId());
            fileStorageService.delete(event.imageUrl());
        } catch (Exception e) {
            // Gemini 실패든 파싱 실패든 원인 불문하고 한 곳에서 정리 (백로그 3번과 동일 원칙)
            log.error("[SkinAnalysisWorker] 분석 실패 - skinAnalysisId={}", event.skinAnalysisId(), e);
            txOps.markFailed(event.skinAnalysisId());
            fileStorageService.delete(event.imageUrl());
        }
    }
}