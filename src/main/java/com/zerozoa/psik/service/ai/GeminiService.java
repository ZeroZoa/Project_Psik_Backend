package com.zerozoa.psik.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private static final Pattern JSON_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private static final String ANALYSIS_PROMPT = """
    당신은 피부과 전문의 수준의 AI 피부 분석가입니다.
    이미지를 관찰하여 아래 기준으로 분석 결과를 산출하세요.
    
    - faceDetected: 이미지에 1명의 사람 얼굴이 명확히 보이면 true, 얼굴이 없거나 여러 명이면 false
    - faceDetected가 false인 경우: 나머지 필드는 전부 0 또는 빈 문자열로 채우세요.
    - faceDetected가 true인 경우:
      - acneScore, wrinkleScore, toneScore, oilScore: 0(나쁨/심함) ~ 100(좋음/없음) 사이의 정수
      - summary: 전체 피부 상태 한줄 요약 + 예상나이 (한국어, 60자 이내)
    """;

    /**
     * 이미지 바이트 배열을 Gemini API로 전송하여 피부 분석 결과를 반환
     * @param imageBytes 분석할 이미지 바이트 배열
     * @param mimeType 이미지 MIME 타입 (예: image/jpeg)
     * @return Gemini API 분석 결과 JSON 문자열
     */
    //출력형식을 자연어로 처리하는 방식 -> API요청 파라미터로 제약 조건 추가
    public String analyzeSkin(byte[] imageBytes, String mimeType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Gemini API 요청 바디 구성
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", ANALYSIS_PROMPT),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Image
                                ))
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 2048,
                        "thinkingConfig", Map.of("thinkingBudget", 0),
                        "responseMimeType", "application/json",
                        "responseSchema", Map.of(
                                "type", "OBJECT",
                                "properties", Map.of(
                                        "faceDetected", Map.of("type", "BOOLEAN"),
                                        "acneScore", Map.of("type", "INTEGER"),
                                        "wrinkleScore", Map.of("type", "INTEGER"),
                                        "toneScore", Map.of("type", "INTEGER"),
                                        "oilScore", Map.of("type", "INTEGER"),
                                        "summary", Map.of("type", "STRING")
                                ),
                                "required", List.of(
                                        "faceDetected", "acneScore", "wrinkleScore",
                                        "toneScore", "oilScore", "summary"
                                )
                        )
                )
        );

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            // 진단용 — 응답이 실제로 도착하는지, candidates가 비어있는 세이프티 차단 등인지 확인하기 위해
            // 파싱 성공/실패와 무관하게 원본 응답을 항상 남긴다. 원인 확인되면 제거할 것.
            log.info("[Gemini][DEBUG] 피부분석 원본 응답: {}", response);

            // Gemini 응답에서 실제 텍스트 추출
            return extractTextFromResponse(response);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("[Gemini] Rate Limit 초과 - 잠시 후 다시 시도해주세요.");
                throw new BusinessException(ErrorCode.GEMINI_RATE_LIMIT_EXCEEDED);
            }
            log.error("[Gemini] API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Gemini API 호출 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[Gemini] API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Gemini API 호출 중 오류가 발생했습니다.");
        }
    }

    /**
     * RAG 챗봇용 텍스트 생성 — 시스템 프롬프트 + 사용자 질문을 Gemini에 전달
     * analyzeSkin()과 다른 점:
     * - 이미지 없이 텍스트만 전달
     * - systemInstruction으로 AI 역할(성분 전문가)과 답변 범위를 고정
     * - temperature 0.2: 낮을수록 일관된 답변, 높을수록 창의적 답변
     *   챗봇 특성상 사실 기반 답변이 중요하므로 낮게 설정
     *
     * @param systemPrompt AI 역할과 참고할 성분 데이터를 담은 시스템 지시문
     * @param userMessage  사용자가 입력한 질문
     * @return Gemini가 생성한 답변 텍스트
     */
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("role", "user",
                                "parts", List.of(Map.of("text", userMessage)))
                ),
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 1024,
                        "thinkingConfig", Map.of("thinkingBudget", 0)
                )
        );

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            return extractTextFromResponse(response); // 기존 private 메서드 재사용

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("[Gemini Chat] Rate Limit 초과 — 잠시 후 재시도 필요");
                throw new BusinessException(ErrorCode.GEMINI_RATE_LIMIT_EXCEEDED);
            }
            log.error("[Gemini Chat] API 호출 실패 — status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "챗봇 응답 생성 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[Gemini Chat] 처리 중 오류: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "챗봇 응답 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * Gemini API 응답 JSON에서 실제 텍스트 내용 추출
     * @param response Gemini API 원본 응답 JSON 문자열
     * @return 분석 결과 텍스트
     */
    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            text = text.trim();

            // 프롬프트가 CoT 설명 텍스트를 코드펜스 앞에 쓰라고 지시하므로,
            // "전체가 펜스로 시작하는지"가 아니라 텍스트 안의 마지막 펜스 블록을 찾아야 한다.
            Matcher matcher = JSON_FENCE_PATTERN.matcher(text);
            String lastFenced = null;
            while (matcher.find()) {
                lastFenced = matcher.group(1);
            }

            return lastFenced != null ? lastFenced.trim() : text;
        } catch (Exception e) {
            log.error("[Gemini] 응답 파싱 실패: {}", response);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Gemini API 호출 중 오류가 발생했습니다.");
        }
    }
}