package com.zerozoa.psik.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    // text-embedding-004 고정 출력 차원 — 모델 변경 시 이 상수만 수정
    private static final int EMBEDDING_DIM = 768;

    // text-embedding-004 토큰 한도(~2048 토큰) 초과 방지용 문자 수 상한선
    // 한국어 1자는 약 1.5~2 토큰이므로 8,000자면 안전 구간
    private static final int MAX_TEXT_LENGTH = 8000;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.embedding-url}")
    private String embeddingUrl;

    /**
     * 텍스트를 벡터로 변환 (Gemini text-embedding-004)
     *
     * [RAG 개념] 임베딩(Embedding)이란 텍스트의 의미를 숫자 배열로 압축한 것.
     * "히알루론산은 보습에 좋다" → [0.12, -0.33, 0.87, ...] (768개 float)
     * 의미가 비슷한 문장일수록 배열 값이 유사해짐.
     * 이 유사도를 pgvector가 계산해 관련 성분을 검색하는 것이 RAG의 핵심.
     *
     * @param text 임베딩할 텍스트
     * @return "[0.1,-0.3,...]" — pgvector가 인식하는 vector 문자열 포맷
     */
    public String embed(String text) {
        // 빈 텍스트는 의미 없는 벡터를 생성하므로 API 호출 전에 차단
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "임베딩할 텍스트가 비어있습니다.");
        }

        // 토큰 한도 초과 방지: 상한선 넘으면 잘라서 전송
        String truncated = text.length() > MAX_TEXT_LENGTH
                ? text.substring(0, MAX_TEXT_LENGTH)
                : text;

        Map<String, Object> requestBody = Map.of(
                "model", "models/text-embedding-004",
                "content", Map.of("parts", List.of(Map.of("text", truncated)))
        );

        log.debug("[Embedding] 요청 — 텍스트 길이: {}자", truncated.length());

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(embeddingUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            float[] vector = parseEmbedding(response);
            log.debug("[Embedding] 완료 — 벡터 차원: {}", vector.length);

            return toVectorString(vector);

        } catch (BusinessException e) {
            throw e; // 이미 처리된 예외는 그대로 전파
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("[Embedding] Rate Limit 초과 — 잠시 후 재시도 필요");
                throw new BusinessException(ErrorCode.GEMINI_RATE_LIMIT_EXCEEDED);
            }
            log.error("[Embedding] API 호출 실패 — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "임베딩 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 성분 엔티티의 모든 필드를 결합해 임베딩용 텍스트 생성.
     *
     * [품질 원칙] 임베딩 텍스트가 풍부할수록 유사도 검색 정확도가 높아짐.
     * 효과 태그, 주의사항, 피부 고민까지 포함해야
     * "레티놀 자극성 있어?", "건성 피부에 뭐가 좋아?" 같은 질문도 정확히 검색됨.
     */
    public String buildIngredientText(Ingredient ingredient) {
        return """
                성분명: %s
                유형: %s
                설명: %s
                효과 요약: %s
                효과 태그: %s
                주의사항: %s
                관련 피부 고민: %s
                """.formatted(
                ingredient.getName(),
                ingredient.getType() != null ? ingredient.getType().name() : "",
                nullSafe(ingredient.getDescription()),
                nullSafe(ingredient.getEffectSummary()),
                String.join(", ", ingredient.getEffects()),
                String.join(", ", ingredient.getCautions()),
                ingredient.getSkinConcerns().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", "))
        );
    }

    /**
     * Gemini 임베딩 응답 JSON에서 float[] 벡터 추출.
     *
     * 응답 구조: { "embedding": { "values": [0.1, -0.3, ...] } }
     *
     * 차원 검증을 하는 이유: 모델명 오타나 API 버전 변경으로
     * 예상과 다른 차원의 벡터가 반환될 경우 DB 저장 시 조용히 실패할 수 있음.
     * 조기에 잡아야 디버깅이 쉬움.
     */
    private float[] parseEmbedding(String response) {
        try {
            JsonNode values = objectMapper.readTree(response)
                    .path("embedding")
                    .path("values");

            // 반환 차원이 예상(768)과 다르면 모델 설정 오류 가능성
            if (values.size() != EMBEDDING_DIM) {
                log.error("[Embedding] 차원 불일치 — 예상: {}, 실제: {}", EMBEDDING_DIM, values.size());
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "임베딩 차원이 올바르지 않습니다.");
            }

            float[] result = new float[EMBEDDING_DIM];
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                result[i] = (float) values.get(i).asDouble();
            }
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Embedding] 응답 파싱 실패 — response: {}", response);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "임베딩 응답 파싱 중 오류가 발생했습니다.");
        }
    }

    /**
     * float[] → "[0.1,-0.3,...]" 변환.
     *
     * pgvector는 이 문자열 포맷을 vector 타입으로 자동 인식.
     * String.join 대신 StringBuilder를 쓰는 이유:
     * 768개 원소를 Float 객체로 박싱하지 않고 primitive float 그대로 처리해 GC 부담 최소화.
     */
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}