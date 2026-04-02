package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.diary.SkinDiaryRequest;
import com.zerozoa.skinner.dto.diary.SkinDiaryResponse;
import com.zerozoa.skinner.global.util.SecurityUtils;
import com.zerozoa.skinner.service.SkinDiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

//스킨 다이어리 관련 API 컨트롤러
@Slf4j
@Tag(name = "Skin Diary API", description = "스킨 다이어리 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diaries")
public class SkinDiaryController {

    private final SkinDiaryService skinDiaryService;

    /**
     * 다이어리 작성
     * [POST] /api/diaries
     */
    @Operation(summary = "다이어리 작성", description = "해당 날짜의 스킨 다이어리를 작성합니다.")
    @PostMapping
    public ResponseEntity<SkinDiaryResponse> createDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Valid @RequestBody SkinDiaryRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        SkinDiaryResponse response = skinDiaryService.createDiary(memberUuid, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //단건 조회 - 특정 일자
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

    //목록 조회 - 월별 캘린더용
    @Operation(summary = "월별 조회", description = "특정 년/월의 다이어리 목록을 조회합니다.")
    @GetMapping("/monthly")
    public ResponseEntity<List<SkinDiaryResponse>> getMonthlyDiaries(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        List<SkinDiaryResponse> responses = skinDiaryService.getMonthlyDiaries(memberUuid, year, month);

        return ResponseEntity.ok(responses);
    }

    //다이어리 수정
    @Operation(summary = "다이어리 수정", description = "기존 다이어리를 수정합니다.")
    @PutMapping("/{diaryId}")
    public ResponseEntity<SkinDiaryResponse> updateDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable("diaryId") Long diaryId,
            @Valid @RequestBody SkinDiaryRequest request
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        SkinDiaryResponse response = skinDiaryService.updateDiary(memberUuid, diaryId, request);

        return ResponseEntity.ok(response);
    }

    //다이어리 삭제
    @Operation(summary = "다이어리 삭제", description = "다이어리를 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @PathVariable("diaryId") Long diaryId
    ) {
        UUID memberUuid = SecurityUtils.extractMemberUuid(principal);
        skinDiaryService.deleteDiary(memberUuid, diaryId);

        return ResponseEntity.noContent().build();
    }

    // 기간별 조회 (최근 30일 그래프용)
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