package com.zerozoa.skinner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
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
        이 이미지가 사람의 얼굴 사진인지 먼저 확인해줘.
        얼굴이 아니라면 반드시 아래 JSON만 반환해:
        {"error": "FACE_NOT_DETECTED"}
        
        얼굴이 맞다면 피부 전문가 관점에서 분석 후 아래 JSON만 반환해:
        {
          "acne_score": 여드름 점수 0~100(여드름이 많으면 0, 적으면 100에 가깝게),
          "wrinkle_score": 주름 점수 0~100(팔자, 눈가, 피부 탄력을 종합해서 주름이 없으면 100, 많으면 0에 가깝게),
          "tone_score": 피부톤 점수 0~100(피부 흉터나, 패인 상처, 모공이 많으면 0, 없으면 100에 가깝게),
          "oil_score": 유분 점수 0~100(번들거리고, 개기름이 많으면 0에 가깝게, 없고 유수분 밸런스가 맞으면 100에 가깝게),
          "summary": "전체 피부 상태 한줄 요약 (한국어, 50자 이내)"
        }
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
                        "maxOutputTokens", 2048,    // 512 → 2048으로 증가
                        "thinkingConfig", Map.of(
                                "thinkingBudget", 0     // thinking 비활성화 (토큰 절약)
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
            throw new RuntimeException("Gemini API 호출 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("[Gemini] API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("Gemini API 호출 중 오류가 발생했습니다.", e);
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

            // gemini-2.5 등 일부 모델이 ```json ... ``` 마크다운으로 감싸서 응답하는 경우 제거
            text = text.trim();
            if (text.startsWith("```")) {
                text = text.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }

            return text;
        } catch (Exception e) {
            log.error("[Gemini] 응답 파싱 실패: {}", response);
            throw new RuntimeException("Gemini 응답 파싱 중 오류가 발생했습니다.", e);
        }
    }
}