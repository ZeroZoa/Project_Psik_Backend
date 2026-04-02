package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.domain.member.Member;
import com.zerozoa.skinner.dto.member.MemberResponse;
import com.zerozoa.skinner.dto.member.NicknameRequest;
import com.zerozoa.skinner.dto.member.ProfileSetupRequest;
import com.zerozoa.skinner.dto.member.SkinConcernUpdateRequest;
import com.zerozoa.skinner.global.util.SecurityUtils;
import com.zerozoa.skinner.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

//Member 관련 API 컨트롤러
@Slf4j
@Tag(name = "Member API", description = "회원 정보 조회/수정/탈퇴 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    /**
     * 내 정보 조회 (마이페이지)
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 상세 정보를 조회합니다. (Access Token 필수)")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal
    ) {
        UUID uuid = SecurityUtils.extractMemberUuid(principal);

        log.info("[API] 내 정보 조회 요청 - UUID: {}", uuid);

        MemberResponse response = MemberResponse.from(memberService.getByUuid(uuid));
        return ResponseEntity.ok(response);
    }

    /**
     * 닉네임 중복 확인
     * @return true: 중복됨(사용 불가), false: 사용 가능
     */
    @Operation(summary = "닉네임 중복 확인", description = "true: 중복됨 / false: 사용 가능")
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = memberService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }

    /**
     * 닉네임 수정
     */
    @Operation(summary = "닉네임 수정", description = "현재 로그인된 사용자의 닉네임을 변경합니다.")
    @PatchMapping("/me/nickname")
    public ResponseEntity<MemberResponse> updateNickname(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestBody @Valid NicknameRequest request
    ) {
        UUID uuid = SecurityUtils.extractMemberUuid(principal);
        Member member = memberService.updateNickname(uuid, request.nickname());
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    /**
     * 피부 고민 수정
     */
    @Operation(summary = "피부 고민 수정", description = "현재 로그인된 사용자의 피부 고민을 수정합니다. (1~3개)")
    @PatchMapping("/me/skin-concerns")
    public ResponseEntity<MemberResponse> updateSkinConcerns(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestBody @Valid SkinConcernUpdateRequest request
    ) {
        UUID uuid = SecurityUtils.extractMemberUuid(principal);
        Member member = memberService.updateSkinConcerns(uuid, request);
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    /**
     * 최초 프로필 설정 (신규 회원 전용)
     */
    @Operation(summary = "프로필 초기 설정", description = "소셜 로그인 후 최초 1회 프로필을 설정합니다. 이미 설정된 경우 409를 반환합니다.")
    @PostMapping("/me/profile-setup")
    public ResponseEntity<MemberResponse> setupProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestBody @Valid ProfileSetupRequest request
    ) {
        UUID uuid = SecurityUtils.extractMemberUuid(principal);
        log.info("[API] 프로필 초기 설정 요청 - UUID: {}", uuid);
        Member member = memberService.setupProfile(uuid, request);
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    /**
     * 회원 탈퇴
     */
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 계정을 영구 삭제합니다. 연관된 모든 데이터(토큰 등)가 함께 삭제됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawMember(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal
    ) {
        UUID uuid = SecurityUtils.extractMemberUuid(principal);

        //탈퇴는 민감한 작업이므로 WARN 레벨로 로깅하여 모니터링
        log.warn("[API] 회원 탈퇴 요청 - UUID: {}", uuid);

        memberService.deleteMember(uuid);

        return ResponseEntity.noContent().build();
    }
}
