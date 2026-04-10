package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.domain.contents.IngredientType;
import com.zerozoa.skinner.domain.member.SkinConcern;
import com.zerozoa.skinner.dto.contents.IngredientDetailResponse;
import com.zerozoa.skinner.dto.contents.IngredientResponse;
import com.zerozoa.skinner.dto.contents.RecommendedGroupResponse;
import com.zerozoa.skinner.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//Ingredient 관련 API 컨트롤러
@Slf4j
@Tag(name = "Ingredient API", description = "성분/제품 정보 조회 API")
@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    /**
     * 성분 목록 조회
     * @param keyword 검색어
     * @param type 성분 타입 필터
     * @param pageable 페이징 정보 (기본값: size=10, sort=id,desc)
     * @return 200 OK - 검색된 성분 목록 (Page)
     * @see IngredientService#getIngredients(String, IngredientType, Pageable)
     */
    @Operation(summary = "성분 목록 조회", description = "성분 리스트를 조회")
    @GetMapping
    public ResponseEntity<Page<IngredientResponse>> getIngredients(
            @Parameter(description = "검색어 (이름, 설명 포함)") @RequestParam(required = false) String keyword,
            @Parameter(description = "성분 타입 필터 (GENERAL, OTC, PRESCRIPTION, OVERSEAS)") @RequestParam(required = false) IngredientType type,
            @Parameter(description = "페이징 설정 (기본: 10개, ID 역순)") @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("[API] Get Ingredients - keyword: {}, type: {}, page: {}", keyword, type, pageable.getPageNumber());

        // 서비스 계층에서 이미 DTO 변환이 완료된 Page 객체를 반환받음
        return ResponseEntity.ok(ingredientService.getIngredients(keyword, type, pageable));
    }

    /**
     * 특정 성분의 상세 정보를 조회
     * @param id 조회할 성분의 id
     * @return 200 OK
     * @see IngredientService#getIngredientDetail(Long)
     */
    @Operation(summary = "성분 상세 조회", description = "성분의 상세 정보(효과, 주의사항, 추천 제품 등)를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<IngredientDetailResponse> getIngredientDetail(
            @Parameter(description = "성분 ID") @PathVariable Long id
    ) {
        log.info("[API] Get Ingredient Detail - id: {}", id);

        return ResponseEntity.ok(ingredientService.getIngredientDetail(id));
    }

    /**
     * 피부고민에 해당하는 성분 추천
     * @param skinConcerns 성분을 추천받기위한 피부고민
     * @return 200 OK
     * @see IngredientService#getRecommendedIngredients(List)
     */
    @Operation(summary = "피부 고민별 성분 추천")
    @GetMapping("/recommended")
    public ResponseEntity<List<RecommendedGroupResponse>> getRecommended(
            @RequestParam List<SkinConcern> skinConcerns
    ) {
        log.info("[API] Get Recommended Ingredients - skinConcerns: {}", skinConcerns);

        List<RecommendedGroupResponse> result = skinConcerns.stream()
                .map(concern -> new RecommendedGroupResponse(
                        concern.name(),
                        concern.getDescription(),
                        ingredientService.getRecommendedIngredients(List.of(concern))
                ))
                .toList();

        return ResponseEntity.ok(result);
    }
}