package com.zerozoa.psik.service.diary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.JpegWriter;
import com.zerozoa.psik.dto.diary.SkinAnalysisRequestedEvent;
import com.zerozoa.psik.service.ai.GeminiService;
import com.zerozoa.psik.service.diary.SkinAnalysisTxOps.Checkpoint;
import com.zerozoa.psik.service.storage.FileStorageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkinAnalysisWorker {

    private static final long PROCESS_TIMEOUT_SECONDS = 60;                  // 한 번의 처리 시도 상한
    private static final Duration PROCESS_DEADLINE = Duration.ofMinutes(5);  // 접수 후 이 시간이 지나면 재시도 없이 실패

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private final SkinAnalysisTxOps txOps;
    private final GeminiService geminiService;
    private final FileStorageService fileStorageService;

    @Value("${gcp.pubsub.skin-analysis-subscription}")
    private String subscriptionName;

    // 블로킹 I/O(GCS, Gemini) 전용 풀. 대기열을 제한해 대기 시간이 타임아웃에 섞이지 않게 하고,
    // 가득 차면 RejectedExecutionException → nack으로 재전달한다.
    private final ExecutorService analysisExecutor = new ThreadPoolExecutor(
            4, 4, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(4),
            r -> {
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

    void onMessage(BasicAcknowledgeablePubsubMessage message) {
        String payload = message.getPubsubMessage().getData().toStringUtf8();

        SkinAnalysisRequestedEvent event;
        try {
            event = objectMapper.readValue(payload, SkinAnalysisRequestedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[SkinAnalysisWorker] 복구 불가능한 메시지, 폐기 - payload={}", payload, e);
            message.ack();      // 다시 받아도 못 읽는 메시지
            return;
        }

        try {
            process(event);
        } catch (Exception e) {
            log.warn("[SkinAnalysisWorker] 일시적 처리 실패, 재전달 요청 - skinAnalysisId={}", event.skinAnalysisId(), e);
            message.nack();     // 풀 포화, DB 일시 장애 등
            return;
        }
        message.ack();
    }

    private void process(SkinAnalysisRequestedEvent event) {
        Future<Boolean> future;
        try {
            future = analysisExecutor.submit(() -> analyze(event));
        } catch (RejectedExecutionException e) {
            log.warn("[SkinAnalysisWorker] 워커 풀 포화로 재시도 위임 - skinAnalysisId={}", event.skinAnalysisId());
            throw e;
        }

        try {
            future.get(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {     // 타임아웃 포함, 어디서 실패하든 같은 정리
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            future.cancel(true);
            log.error("[SkinAnalysisWorker] 분석 실패 - skinAnalysisId={}", event.skinAnalysisId(), e);
            fail(event);
        }
    }

    private boolean analyze(SkinAnalysisRequestedEvent event) throws Exception {
        Checkpoint checkpoint = txOps.check(event.skinAnalysisId(), PROCESS_DEADLINE);
        if (checkpoint == Checkpoint.ALREADY_DONE) {
            log.info("[SkinAnalysisWorker] 이미 처리됨, 스킵 - skinAnalysisId={}", event.skinAnalysisId());
            return false;
        }
        if (checkpoint == Checkpoint.EXPIRED) {
            log.warn("[SkinAnalysisWorker] 처리 기한 초과 - skinAnalysisId={}", event.skinAnalysisId());
            fail(event);
            return false;
        }

        byte[] imageBytes = ImmutableImage.loader()
                .fromBytes(fileStorageService.readBytes(event.imageUrl()))
                .bound(512, 512)
                .bytes(new JpegWriter().withCompression(85));

        JsonNode result = objectMapper.readTree(geminiService.analyzeSkin(imageBytes, "image/jpeg"));

        if (result.path("faceDetected").asBoolean(false)) {
            txOps.markCompleted(event.skinAnalysisId(), result);
        } else {
            fail(event);            // 얼굴 없음/여러 명 — 거절
        }
        return true;
    }

    private void fail(SkinAnalysisRequestedEvent event) {
        txOps.markFailed(event.skinAnalysisId());
        fileStorageService.delete(event.imageUrl());
    }
}
