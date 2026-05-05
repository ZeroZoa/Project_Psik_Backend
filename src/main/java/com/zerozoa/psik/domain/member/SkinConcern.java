package com.zerozoa.psik.domain.member;

import lombok.Getter;

import java.util.List;

@Getter
public enum SkinConcern {
    ACNE("여드름", List.of("여드름", "모공", "트러블", "피지", "염증")),
    SCAR("흉터", List.of("흉터", "재생", "피부회복")),
    RECOVERY("피부회복", List.of("재생", "피부회복", "진정")),
    AGING("노화", List.of("노화", "항산화", "탄력", "항노화")),
    WRINKLE("주름", List.of("주름", "탄력", "리프팅")),
    SPOT("잡티/기미/주근깨", List.of("기미", "잡티", "주근깨", "미백")),
    WHITENING("미백", List.of("미백", "기미", "잡티", "자외선")),
    BLACKHEAD("블랙헤드", List.of("블랙헤드", "모공", "피지")),
    WHITEHEAD("좁쌀/화이트헤드", List.of("화이트헤드", "모공", "피지", "좁쌀여드름")),
    SUN_CARE("자외선차단", List.of("선케어", "자외선차단", "광노화", "피부보호", "선크림")),
    KERATIN("각질/피부결", List.of("각질", "피부결", "필링", "스크럽", "오돌토돌"));
    //PORE("모공", List.of("모공", "모공수축", "나비존", "탄력")),
    //MOISTURIZING("보습/건조", List.of("보습", "수분", "속건조", "당김", "유수분밸런스")),
    //SENSITIVITY("민감성/홍조", List.of("민감", "홍조", "붉은기", "알러지", "장벽강화")),
    //OILINESS("과다 피지/유분", List.of("유분", "개기름", "피지조절", "매트", "번들거림")),
    //DULLNESS("칙칙함/다크서클", List.of("다크서클", "칙칙함", "안색개선", "생기"));

    private final String description;
    private final List<String> relatedTags;

    SkinConcern(String description, List<String> relatedTags) {
        this.description = description;
        this.relatedTags = relatedTags;
    }
}