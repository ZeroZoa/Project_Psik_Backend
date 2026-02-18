package com.zerozoa.skinner.domain.member;

import com.github.f4b6a3.uuid.UuidCreator;
import com.zerozoa.skinner.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    //내부 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    //외부 식별자:API 노출용UUID v7
    @Column(name = "uuid", columnDefinition = "uuid", nullable = false, unique = true, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID uuid;

    //소셜 로그인 정보
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private Provider provider;

    @Column(name = "oauth_id", length = 100, nullable = false)
    private String oauthId;

    //회원 정보
    @Column(name = "nickname", length = 30, nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    //아래는 OIDC를 인증 후 제공 동의를 통해 받아올 정보들 동의 여부, 정책에 따라 못받을 가능성이 있음 -> Nullable 필수
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

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

    //소셜 로그인 시 이메일과 전화번호 동기화 -> 카카오/구글 등에서 정보가 바뀌었을 경우를 대비
    public void updateSocialInfo(String email, String phoneNumber) {
        // null이 아니고, 기존 값과 다를 때만 갱신 (불필요한 변경 방지)
        if (email != null && !email.equals(this.email)) {
            this.email = email;
        }
        if (phoneNumber != null && !phoneNumber.equals(this.phoneNumber)) {
            this.phoneNumber = phoneNumber;
        }
    }

    //프로필 정보 수정 -> 닉네임, 프로필 사진
    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
