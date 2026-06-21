package com.zerozoa.psik.service;

import com.zerozoa.psik.domain.contents.Ingredient;
import com.zerozoa.psik.domain.member.SkinConcern;
import com.zerozoa.psik.repository.contents.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RagService {

    // 유사도 하한선: 0.6 미만이면 관련 없는 성분으로 판단해 제외
    // 값이 높을수록 엄격 (1.0 = 완전 동일, 0.0 = 전혀 무관)
    private static final double SIMILARITY_THRESHOLD = 0.6;

    // LLM에 넘길 최대 성분 수 — 많을수록 정확하나 토큰 비용 증가
    // 실무에서는 5~10개가 일반적인 균형점
    private static final int TOP_K = 5;

    private final EmbeddingService embeddingService;
    private final IngredientRepository ingredientRepository;
    private final GeminiService geminiService;
    private final MemberService memberService;

    /**
     * RAG 파이프라인 실행 — 질문에 대한 성분 기반 답변 반환
     *
     * [RAG 흐름]
     * 1. 질문을 벡터로 변환
     * 2. pgvector로 유사 성분 검색
     * 3. 검색 결과를 컨텍스트로 만들어 Gemini에 전달
     * 4. Gemini가 컨텍스트 기반으로 답변 생성
     *
     * @param question   사용자 질문
     * @param memberUuid 로그인 사용자 UUID
     */
    public String answer(String question, UUID memberUuid) {

        List<SkinConcern> skinConcerns = memberService.getByUuid(memberUuid).getSkinConcerns();

        // 질문 임베딩 (텍스트 → 768차원 벡터)
        String queryVector = embeddingService.embed(question);

        // pgvector 유사도 검색 — threshold 이상인 성분만 반환
        List<Ingredient> similar = ingredientRepository
                .findSimilarIngredients(queryVector, SIMILARITY_THRESHOLD, TOP_K);

        // 유사 성분이 없으면 LLM 호출 없이 즉시 반환 (불필요한 API 비용 방지)
        if (similar.isEmpty()) {
            log.info("[RAG] 유사 성분 없음 — question: {}", question);
            return "죄송합니다, 해당 질문과 관련된 성분 정보를 찾지 못했습니다.";
        }

        log.debug("[RAG] 검색 성분 {}개 — {}",
                similar.size(),
                similar.stream().map(Ingredient::getName).collect(Collectors.joining(", ")));

        // 검색된 성분들을 LLM이 읽기 좋은 텍스트로 조합
        String context = buildContext(similar);

        // 시스템 프롬프트 구성 후 Gemini 답변 생성
        return geminiService.chat(buildSystemPrompt(context, skinConcerns), question);
    }

    /**
     * 검색된 성분 목록을 LLM 컨텍스트용 텍스트로 조합
     * 효과와 주의사항까지 포함해야 "이 성분 자극적이야?" 같은 질문도 정확히 답변 가능
     */
    private String buildContext(List<Ingredient> ingredients) {
        return ingredients.stream()
                .map(i -> "- %s: %s (효과: %s / 주의사항: %s)".formatted(
                        i.getName(),
                        nullSafe(i.getDescription()),
                        String.join(", ", i.getEffects()),
                        String.join(", ", i.getCautions())
                ))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Gemini에 전달할 시스템 프롬프트 구성
     *
     * 핵심 제약 2가지:
     * 1. 반드시 [성분 데이터베이스] 안의 정보만 사용 → 환각(hallucination) 방지
     * 2. 사용자 피부 고민 주입 → 개인화 답변
     */
    private String buildSystemPrompt(String context, List<SkinConcern> skinConcerns) {
        String concerns = skinConcerns.isEmpty()
                ? "정보 없음"
                : skinConcerns.stream()
                .map(SkinConcern::getDescription)  // "WRINKLE" 대신 "주름" 으로 전달
                .collect(Collectors.joining(", "));

        return """
                당신은 화장품 성분 전문 AI 상담사 '픽이'입니다.
                반드시 아래 [성분 데이터베이스]에 있는 정보만 활용해 답변하세요.
                데이터베이스에 없는 내용은 "해당 성분에 대한 정보가 없습니다"라고 답하세요.
                답변은 친근하고 이해하기 쉽게, 3~5문장으로 요약해서 답하세요.

                [사용자 피부 고민]: %s

                [성분 데이터베이스]:
                %s
                """.formatted(concerns, context);
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}