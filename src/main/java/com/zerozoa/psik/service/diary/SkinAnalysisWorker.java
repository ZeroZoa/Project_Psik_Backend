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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkinAnalysisWorker {

    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private final SkinAnalysisTxOps txOps;
    private final GeminiService geminiService;
    private final FileStorageService fileStorageService;

    @Value("${gcp.pubsub.skin-analysis-subscription}")
    private String subscriptionName;

    // GCS 읽기/Gemini 호출처럼 블로킹 I/O가 발생하는 작업 전용 풀.
    // 공용 ForkJoinPool을 쓰면 여기서 멈춘 작업이 무관한 다른 병렬 작업의 스레드까지 잠식할 수 있어 분리.
    // 대기열도 짧게(4) 제한 — 무제한 큐를 쓰면 대기 시간까지 PROCESS_TIMEOUT_SECONDS에 포함되어
    // 실제로는 멈춘 게 아니라 밀린 것뿐인 요청까지 타임아웃으로 잘못 처리됨. 큐까지 꽉 차면
    // RejectedExecutionException을 그대로 던져서 onMessage()의 catch가 nack 처리하게 하고,
    // Pub/Sub의 재전달(backoff)에 맡긴다.
    private final ExecutorService analysisExecutor = new ThreadPoolExecutor(
            4, 4,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(4),
            r -> {
                Thread t = new Thread(r, "skin-analysis-worker");
                t.setDaemon(true);
                return t;
            }
    );

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
        // 멱등성 체크(DB 조회)도 스레드풀 밖에서 하면 여기서 멈출 때 타임아웃 보호를 못 받음 —
        // 그래서 isPending()까지 통째로 같은 Future 안에 넣어 전체 구간을 60초로 제한한다.
        Future<Boolean> future;
        try {
            future = submitAnalysisTask(event);
        } catch (RejectedExecutionException e) {
            // 워커 풀(스레드 4 + 대기열 4)이 꽉 찬 상태 — 여기서 그냥 기다리면 대기 시간이
            // PROCESS_TIMEOUT_SECONDS를 갉아먹으므로, 즉시 포기하고 Pub/Sub 재전달에 맡긴다.
            log.warn("[SkinAnalysisWorker] 워커 풀 포화로 재시도 위임 - skinAnalysisId={}", event.skinAnalysisId());
            throw e; // onMessage()의 catch(Exception)가 nack 처리
        }

        try {
            future.get(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            // GCS 읽기/이미지 리사이즈/Gemini 호출 중 어디서 멈추든 60초 안에 강제 실패 처리.
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

    private Future<Boolean> submitAnalysisTask(SkinAnalysisRequestedEvent event) {
        // 진단용 — 이 메시지가 실제로 워커 풀에서 작업을 시작했는지(즉 여기까지 도달했는지)를
        // 확인하기 위한 로그. 원인 확인되면 제거할 것.
        log.info("[SkinAnalysisWorker][DEBUG] 작업 제출 - skinAnalysisId={}", event.skinAnalysisId());

        return analysisExecutor.submit(() -> {
            log.info("[SkinAnalysisWorker][DEBUG] 작업 시작 - skinAnalysisId={}", event.skinAnalysisId());

            // Pub/Sub은 at-least-once라 같은 메시지가 중복 도착할 수 있음 — 멱등성 체크
            if (!txOps.isPending(event.skinAnalysisId())) {
                log.info("[SkinAnalysisWorker] 이미 처리됨, 스킵 - skinAnalysisId={}", event.skinAnalysisId());
                return false;
            }

            byte[] storedBytes = fileStorageService.readBytes(event.imageUrl());
            log.info("[SkinAnalysisWorker][DEBUG] GCS 읽기 완료 - skinAnalysisId={}", event.skinAnalysisId());

            byte[] imageBytes = ImmutableImage.loader()
                    .fromBytes(storedBytes)
                    .bound(512, 512)
                    .bytes(new JpegWriter().withCompression(85));
            log.info("[SkinAnalysisWorker][DEBUG] 이미지 리사이즈 완료 - skinAnalysisId={}", event.skinAnalysisId());

            String resultJson = geminiService.analyzeSkin(imageBytes, "image/jpeg");
            JsonNode result = objectMapper.readTree(resultJson);

            if (!result.path("faceDetected").asBoolean(false)) {
                txOps.markFailed(event.skinAnalysisId());
                fileStorageService.delete(event.imageUrl());
                return true;
            }

            txOps.markCompleted(event.skinAnalysisId(), result);
            return true;
        });
    }
}