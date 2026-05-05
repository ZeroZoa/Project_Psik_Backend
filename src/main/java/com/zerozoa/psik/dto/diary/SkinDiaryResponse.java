package com.zerozoa.psik.dto.diary;

import com.zerozoa.psik.domain.diary.SkinDiary;
import com.zerozoa.psik.dto.contents.ProductDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//스킨 다이어리 응답 DTO
public record SkinDiaryResponse(
        Long skinDiaryId,
        Instant recordDate,
        Integer skinScore,
        Integer sleepTimeMinutes,
        Integer waterIntakeMl,
        List<String> diet,
        List<ProductDto> usedCosmetics,
        Instant createdAt,
        Instant updatedAt
) {
    public static SkinDiaryResponse from(SkinDiary skinDiary) {

        List<ProductDto> cosmeticsList = skinDiary.getUsedCosmetics() != null
                ? skinDiary.getUsedCosmetics().stream()
                .map(mapping -> ProductDto.from(mapping.getProduct()))
                .toList()
                : Collections.emptyList();

        List<String> dietList = skinDiary.getDiet() != null
                ? new ArrayList<>(skinDiary.getDiet())
                : Collections.emptyList();

        return new SkinDiaryResponse(
                skinDiary.getId(),
                skinDiary.getRecordDate(),
                skinDiary.getSkinScore(),
                skinDiary.getSleepTimeMinutes(),
                skinDiary.getWaterIntakeMl(),
                dietList,
                cosmeticsList,
                skinDiary.getCreatedAt(),
                skinDiary.getUpdatedAt()
        );
    }
}