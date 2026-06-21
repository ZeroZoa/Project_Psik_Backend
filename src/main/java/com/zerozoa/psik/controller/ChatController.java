package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.chat.ChatRequest;
import com.zerozoa.psik.dto.chat.ChatResponse;
import com.zerozoa.psik.global.util.SecurityUtils;
import com.zerozoa.psik.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Tag(name = "Chat API", description = "성분 기반 RAG 챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

    /**
     * 성분 기반 RAG 챗봇 답변
     * 로그인 필수 — 피부 고민 컨텍스트 자동 반영
     *
     * @param principal JWT 인증 객체
     */
    @Operation(summary = "챗봇 질문", description = "성분 데이터베이스 기반으로 질문에 답변합니다.")
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Valid @RequestBody ChatRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);

        log.info("[Chat] 질문 수신 — memberUuid: {}", memberUuid);
        String answer = ragService.answer(request.message(), memberUuid);

        return ResponseEntity.ok(new ChatResponse(answer));
    }
}