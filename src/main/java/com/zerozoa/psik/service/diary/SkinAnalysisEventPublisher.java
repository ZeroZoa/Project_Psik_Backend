package com.zerozoa.psik.service.diary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.zerozoa.psik.dto.diary.SkinAnalysisRequestedEvent;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisEventPublisher {

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gcp.pubsub.skin-analysis-topic}")
    private String topicName;

    /**
     * 분석 요청 이벤트를 Pub/Sub에 발행
     * publish()는 CompletableFuture를 반환하므로 결과를 기다려야 실제 발행 실패
     * (네트워크/쿼터/IAM 오류 등)를 감지할 수 있다 — 기다리지 않으면 SkinAnalysis(PENDING)
     * row만 커밋되고 메시지는 안 가는 좀비 상태를 절대 막을 수 없다.
     * @param event 발행할 분석 요청 이벤트
     * @throws BusinessException 발행 실패 시 {@link ErrorCode#INTERNAL_SERVER_ERROR}
     */
    public void publish(SkinAnalysisRequestedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            pubSubTemplate.publish(topicName, payload).get(5, TimeUnit.SECONDS);
            log.info("[SkinAnalysis] Pub/Sub 발행 - skinAnalysisId={}", event.skinAnalysisId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw publishFailed(event, e);
        } catch (Exception e) {
            throw publishFailed(event, e);
        }
    }

    private BusinessException publishFailed(SkinAnalysisRequestedEvent event, Exception cause) {
        log.error("[SkinAnalysis] Pub/Sub 발행 실패 - skinAnalysisId={}", event.skinAnalysisId(), cause);
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "분석 요청 접수 중 오류가 발생했습니다.");
    }
}
