package com.zerozoa.psik.controller;

import com.zerozoa.psik.dto.diary.SkinDiaryRequest;
import com.zerozoa.psik.dto.diary.SkinDiaryResponse;
import com.zerozoa.psik.global.util.SecurityUtils;
import com.zerozoa.psik.service.SkinDiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Slf4j
@Validated
@Tag(name = "Skin Diary API", description = "스킨 다이어리 생성/조회/수정/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diaries")
public class SkinDiaryController {

    private final SkinDiaryService skinDiaryService;

    /**
     * 다이어리 작성
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param request 다이어리 작성 요청
     * @return 201 Created
     * @see SkinDiaryService#createDiary(UUID, SkinDiaryRequest) 
     */
    @Operation(summary = "다이어리 작성", description = "해당 날짜의 스킨 다이어리를 작성합니다.")
    @PostMapping
    public ResponseEntity<SkinDiaryResponse> createDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Valid @RequestBody SkinDiaryRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        log.info("[API] 다이어리 작성 요청 - memberUuid={}", memberUuid);
        SkinDiaryResponse response = skinDiaryService.createDiary(memberUuid, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    /**
     * 다이어리 단건 조회 - 특정 일자
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param recordDate 조회할 날짜 (ISO-8601 UTC 형식, 예: 2024-04-01T00:00:00Z)
     * @return 200 OK
     * @see SkinDiaryService#getDiaryByDate(UUID, Instant) 
     */
    @Operation(summary = "단건 조회", description = "특정 날짜의 다이어리를 조회합니다.")
    @GetMapping("/daily")
    public ResponseEntity<SkinDiaryResponse> getDiaryByDate(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestParam("date") Instant recordDate
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        SkinDiaryResponse response = skinDiaryService.getDiaryByDate(memberUuid, recordDate);

        return ResponseEntity.ok(response);
    }
    
    
    /**
     * 다이어리 목록 조회 - 월별 캘린더용
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param year 연도
     * @param month 월
     * @return 200 OK
     * @see SkinDiaryService#getMonthlyDiaries(UUID, int, int) 
     */
    @Operation(summary = "월별 조회", description = "특정 년/월의 다이어리 목록을 조회합니다.")
    @GetMapping("/monthly")
    public ResponseEntity<List<SkinDiaryResponse>> getMonthlyDiaries(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestParam("year") @Min(2000) @Max(2100) int year,
            @RequestParam("month") @Min(1) @Max(12) int month
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        List<SkinDiaryResponse> responses = skinDiaryService.getMonthlyDiaries(memberUuid, year, month);

        return ResponseEntity.ok(responses);
    }
    
    
    /**
     * 다이어리 수정
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param diaryId 수정할 다이어리의 diaryId
     * @param request 수정할 다이어리 내용
     * @return 200 OK
     * @see SkinDiaryService#updateDiary(UUID, Long, SkinDiaryRequest) 
     */
    @Operation(summary = "다이어리 수정", description = "기존 다이어리를 수정합니다.")
    @PutMapping("/{diaryId}")
    public ResponseEntity<SkinDiaryResponse> updateDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable("diaryId") Long diaryId,
            @Valid @RequestBody SkinDiaryRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        log.info("[API] 다이어리 수정 요청 - memberUuid={}, diaryId={}", memberUuid, diaryId);
        SkinDiaryResponse response = skinDiaryService.updateDiary(memberUuid, diaryId, request);

        return ResponseEntity.ok(response);
    }

    
    /**
     * 다이어리 삭제
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param diaryId 삭제할 다이어리의 diaryId
     * @return 204 No Content
     * @see SkinDiaryService#deleteDiary(UUID, Long)
     */
    @Operation(summary = "다이어리 삭제", description = "다이어리를 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable("diaryId") Long diaryId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        log.warn("[API] 다이어리 삭제 요청 - memberUuid={}, diaryId={}", memberUuid, diaryId);
        skinDiaryService.deleteDiary(memberUuid, diaryId);

        return ResponseEntity.noContent().build();
    }

    
    /**
     * 그래프용 기간별 다이어리 조회
     * @param principal Spring Security Context에 저장된 인증 객체 (JWT 필터에서 주입)
     * @param from 조회 시작 일시 (ISO-8601 UTC 형식)
     * @param to 조회 종료 일시 (ISO-8601 UTC 형식)
     * @return 200 OK
     * @see SkinDiaryService#getDiariesByRange(UUID, Instant, Instant) 
     */
    @Operation(summary = "기간별 다이어리 조회", description = "from~to 사이의 다이어리 목록을 조회합니다.")
    @GetMapping("/range")
    public ResponseEntity<List<SkinDiaryResponse>> getDiariesByRange(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestParam("from") Instant from,
            @RequestParam("to") Instant to
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        return ResponseEntity.ok(skinDiaryService.getDiariesByRange(memberUuid, from, to));
    }
}