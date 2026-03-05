package com.zerozoa.skinner.domain.diary;

import com.zerozoa.skinner.domain.common.BaseTimeEntity;
import com.zerozoa.skinner.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant; // LocalDate -> Instant로 변경
import java.util.ArrayList;
import java.util.List;

//스킨 다이어리 엔티티
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

    @Column(name = "record_date", nullable = false)
    private Instant recordDate;

    @Column(name = "skin_score", nullable = false)
    private Integer skinScore;

    @Column(name = "skin_image_url")
    private String skinImageUrl;

    @Column(name = "sleep_time_minutes")
    private Integer sleepTimeMinutes;

    @Column(name = "water_intake_ml")
    private Integer waterIntakeMl;

    @ElementCollection
    @CollectionTable(
            name = "skin_diary_diet",
            joinColumns = @JoinColumn(name = "skin_diary_id")
    )
    @Column(name = "food_name")
    private List<String> diet = new ArrayList<>();

    @OneToMany(mappedBy = "skinDiary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkinDiaryProduct> usedCosmetics = new ArrayList<>();

    @Builder
    public SkinDiary(Member member, Instant recordDate, Integer skinScore, String skinImageUrl,
                     Integer sleepTimeMinutes, Integer waterIntakeMl, List<String> diet) {
        this.member = member;
        this.recordDate = recordDate;
        this.skinScore = skinScore;
        this.skinImageUrl = skinImageUrl;
        this.sleepTimeMinutes = sleepTimeMinutes;
        this.waterIntakeMl = waterIntakeMl;
        if (diet != null) {
            this.diet = diet;
        }
    }

    public void addProduct(SkinDiaryProduct skinDiaryProduct) {
        this.usedCosmetics.add(skinDiaryProduct);
    }

    public void updateDiary(Integer skinScore, String skinImageUrl, Integer sleepTimeMinutes,
                            Integer waterIntakeMl, List<String> newDiet) {
        this.skinScore = skinScore;
        this.skinImageUrl = skinImageUrl;
        this.sleepTimeMinutes = sleepTimeMinutes;
        this.waterIntakeMl = waterIntakeMl;

        this.diet.clear();
        if (newDiet != null) {
            this.diet.addAll(newDiet);
        }
    }

    public void updateCosmetics(List<SkinDiaryProduct> newCosmetics) {
        this.usedCosmetics.clear();
        if (newCosmetics != null) {
            this.usedCosmetics.addAll(newCosmetics);
        }
    }
}