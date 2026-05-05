package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.diary.SkinAnalysisResponse;
import com.zerozoa.psik.global.util.SecurityUtils;
import com.zerozoa.psik.service.SkinAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

// 피부 분석 관련 API 컨트롤러
@Tag(name = "Skin Analysis API", description = "피부 이미지 분석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diaries/{diaryId}/analysis")
public class SkinAnalysisController {

    private final SkinAnalysisService skinAnalysisService;

    /**
     * 피부 이미지 업로드 및 AI 분석 요청
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param diaryId 분석 결과를 연결할 SkinDiary의 ID
     * @param image 분석할 피부 이미지 (multipart/form-data)
     * @return 200 OK - 피부 분석 결과 SkinAnalysisResponse
     * @see SkinAnalysisService#analyze(UUID, Long, MultipartFile)
     */
    @Operation(summary = "피부 분석 요청", description = "이미지를 업로드하면 Gemini AI가 피부를 분석합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SkinAnalysisResponse> analyze(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long diaryId,
            @RequestPart("image") MultipartFile image
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(skinAnalysisService.analyze(memberUuid, diaryId, image));
    }

    /**
     * 다이어리의 피부 분석 결과 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param diaryId 조회할 SkinDiary의 ID
     * @return 200 OK - 피부 분석 결과 SkinAnalysisResponse
     * @see SkinAnalysisService#getAnalysis(UUID, Long)
     */
    @Operation(summary = "피부 분석 결과 조회", description = "다이어리의 피부 분석 결과를 조회합니다.")
    @GetMapping
    public ResponseEntity<SkinAnalysisResponse> getAnalysis(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable Long diaryId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(skinAnalysisService.getAnalysis(memberUuid, diaryId));
    }
}