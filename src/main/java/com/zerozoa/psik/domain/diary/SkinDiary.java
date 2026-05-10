package com.zerozoa.psik.domain.diary;

import com.zerozoa.psik.domain.common.BaseTimeEntity;
import com.zerozoa.psik.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant; // LocalDate -> Instant로 변경
import java.util.ArrayList;
import java.util.List;

/**
 * 피부 다이어리 엔티티
 * 회원이 날짜별로 피부 상태를 기록하며, (member_id, record_date) 유니크 제약으로 하루 1개 작성
 * recordDate는 UTC Instant 기준이며, 프론트에서 날짜 변환 처리
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "skin_diary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_skin_diary_member_date",
                        columnNames = {"member_id", "record_date"}
                )
        }
)
public class SkinDiary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skin_diary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 기록 날짜 (UTC Instant — LocalDate 대신 Instant 사용하여 타임존 이슈 방지) */
    @Column(name = "record_date", nullable = false)
    private Instant recordDate;

    @Column(name = "skin_score", nullable = false)
    private Integer skinScore;

    @Column(name = "sleep_time_minutes")
    private Integer sleepTimeMinutes;

    @Column(name = "water_intake_ml")
    private Integer waterIntakeMl;

    /** 식단 목록 — 값 타입 컬렉션으로 생명주기를 SkinDiary에 종속 */
    @ElementCollection
    @CollectionTable(
            name = "skin_diary_diet",
            joinColumns = @JoinColumn(name = "skin_diary_id")
    )
    @Column(name = "food_name")
    private List<String> diet = new ArrayList<>();

    /** 해당 날짜에 사용한 화장품 목록 (orphanRemoval로 교체 시 자동 삭제) */
    @OneToMany(mappedBy = "skinDiary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkinDiaryProduct> usedCosmetics = new ArrayList<>();

    @Builder
    public SkinDiary(Member member, Instant recordDate, Integer skinScore,
                     Integer sleepTimeMinutes, Integer waterIntakeMl, List<String> diet) {
        this.member = member;
        this.recordDate = recordDate;
        this.skinScore = skinScore;
        this.sleepTimeMinutes = sleepTimeMinutes;
        this.waterIntakeMl = waterIntakeMl;
        if (diet != null) {
            this.diet = diet;
        }
    }

    public void addProduct(SkinDiaryProduct skinDiaryProduct) {
        this.usedCosmetics.add(skinDiaryProduct);
    }

    public void updateDiary(Integer skinScore, Integer sleepTimeMinutes,
                            Integer waterIntakeMl, List<String> newDiet) {
        this.skinScore = skinScore;
        this.sleepTimeMinutes = sleepTimeMinutes;
        this.waterIntakeMl = waterIntakeMl;

        this.diet.clear();
        if (newDiet != null) {
            this.diet.addAll(newDiet);
        }
    }

    /** 사용 화장품 목록 전체 교체 — orphanRemoval로 기존 항목 자동 삭제됨 */
    public void updateCosmetics(List<SkinDiaryProduct> newCosmetics) {
        this.usedCosmetics.clear();
        if (newCosmetics != null) {
            this.usedCosmetics.addAll(newCosmetics);
        }
    }
}