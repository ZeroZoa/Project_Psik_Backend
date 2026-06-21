package com.zerozoa.psik.service;

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

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private static final String ANALYSIS_PROMPT = """
    당신은 피부과 전문의 수준의 AI 피부 분석가입니다.
    이미지를 관찰한 후, 반드시 논리적인 분석 과정을 거쳐 점수를 산출해야 합니다.
    
    [단계 1: 이미지 유효성 검사]
    이 이미지가 1명의 사람 얼굴 사진인지 확인해. 얼굴이 잘 탐지되지 않거나, 여러 명이라면 다른 텍스트 없이 반드시 아래 JSON만 반환해:
    {"error": "FACE_NOT_DETECTED"}
    
    [단계 2: 분석 과정 작성 (Chain of Thought)]
    정상적인 얼굴 이미지라면, 점수를 매기기 전에 텍스트로 먼저 각 항목(여드름, 주름, 피부톤/모공, 유분)에 대한 피부 상태를 꼼꼼히 관찰하고 시각적 특징과 점수 산출 근거를 서술해.
    
    [단계 3: 최종 결과 JSON 반환]
    위의 분석을 바탕으로 0~100 사이의 점수를 계산해. (0점: 나쁨/심함, 100점: 좋음/없음)
    텍스트 분석 작성이 모두 끝나면, 마지막에 반드시 아래의 JSON 형식'만' 코드 블록(```json ... ```) 안에 작성해서 반환해.
    
    ```json
    {
      "acne_score": 여드름 점수 (정수),
      "wrinkle_score": 주름 점수 (정수),
      "tone_score": 피부톤 점수 (정수),
      "oil_score": 유분 점수 (정수),
      "summary": "전체 피부 상태 한줄 요약 + 예상나이까지 (한국어, 60자 이내)"
    }
    ```
    """;

    /**
     * 이미지 바이트 배열을 Gemini API로 전송하여 피부 분석 결과를 반환
     * @param imageBytes 분석할 이미지 바이트 배열
     * @param mimeType 이미지 MIME 타입 (예: image/jpeg)
     * @return Gemini API 분석 결과 JSON 문자열
     */
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
                        "thinkingConfig", Map.of(
                                "thinkingBudget", 0 // thinking 비활성화
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
                    .block();

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
     *
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
            if (text.startsWith("```")) {
                text = text.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }

            return text;
        } catch (Exception e) {
            log.error("[Gemini] 응답 파싱 실패: {}", response);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Gemini API 호출 중 오류가 발생했습니다.");
        }
    }
}