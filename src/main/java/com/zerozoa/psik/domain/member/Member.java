package com.zerozoa.psik.domain.member;

import com.github.f4b6a3.uuid.UuidCreator;
import com.zerozoa.psik.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

//회원 엔티티
//물리적 ID는 Long타입 -> DB 성능 및 조인 효율성을 위함
//논리적 ID는 UUID v7을 사용 -> 외부 API 노출용 -> 보안성 강화
@Entity
@Table(name = "members", indexes = {
        //UUID로 조회시 속도 향상
        @Index(name = "idx_member_uuid", columnList = "uuid", unique = true), //uuid를 통해 인덱스 생성
        //소셜 로그인 제공자 + ID 조합으로 복합인덱스 설정
        @Index(name = "idx_member_oauth", columnList = "provider, oauth_id", unique = true) //Provider, oauthId를 통해 복합 인덱스 생성
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    //------------- 회원 -------------

    //내부 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    //외부 식별자 - API 노출용UUID(v7)
    @Column(name = "uuid", columnDefinition = "uuid", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID uuid;

    //소셜 로그인 정보
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private Provider provider;

    //소셜 로그인 아이디
    @Column(name = "oauth_id", length = 100, nullable = false)
    private String oauthId;

    //역할(USER, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    //------------- 회원 프로필 -------------

    @Column(name = "nickname", length = 30, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    private SkinType skinType;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    // 피부 고민 복수 선택
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_skin_concerns")
    @Enumerated(EnumType.STRING)
    private List<SkinConcern> skinConcerns;

    // 프로필 설정 완료 여부
    @Column(name = "profile_complete", nullable = false)
    private boolean profileComplete = false;

    //아래는 OIDC를 인증 후 제공 동의를 통해 받아올 정보들 동의 여부, 정책에 따라 못받을 가능성이 있음 -> Nullable 필수
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    //------------- 빌더 및 매서드 -------------

    @Builder
    public Member(Provider provider, String oauthId, String nickname, String profileImageUrl, String email, String phoneNumber, Role role) {
        this.provider = provider;
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    //엔티티가 DB에 저장되기 전 실행되어 UUID가 없다면 생성
    @PrePersist
    public void generateUuid() {
        if (this.uuid == null) {
            this.uuid = UuidCreator.getTimeOrderedEpoch();
        }
    }

    /**
     * 소셜 로그인 시 이메일·전화번호 동기화
     * 카카오/구글 등에서 정보가 변경되었을 경우에만 갱신 (null이거나 기존 값과 동일하면 유지)
     */
    public void updateSocialInfo(String email, String phoneNumber) {
        // null이 아니고, 기존 값과 다를 때만 갱신 (불필요한 변경 방지)
        if (email != null && !email.equals(this.email)) {
            this.email = email;
        }
        if (phoneNumber != null && !phoneNumber.equals(this.phoneNumber)) {
            this.phoneNumber = phoneNumber;
        }
    }

    /**
     * 피부 고민 목록 수정
     * 기존 목록을 전달된 목록으로 덮어씀 (빈 리스트 허용)
     */
    public void updateSkinConcerns(List<SkinConcern> skinConcerns) {
        this.skinConcerns = skinConcerns;
    }

    /**
     * 닉네임 및 프로필 사진 수정
     * null 또는 빈 값이 들어오면 기존 값 유지
     */
    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    /**
     * 최초 프로필 설정 (소셜 로그인 후 필수 입력)
     * 호출 시 profileComplete = true로 변경되어 이후 재설정 불가
     */
    public void setupProfile(String nickname, Gender gender, Integer birthYear, SkinType skinType, List<SkinConcern> skinConcerns) {

        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        this.nickname = nickname;
        this.gender = gender;
        this.birthYear = birthYear;
        this.skinType = skinType;
        this.skinConcerns = skinConcerns;
        this.profileComplete = true;
    }
}
