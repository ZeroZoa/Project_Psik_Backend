package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.member.MemberResponse;
import com.zerozoa.skinner.global.util.SecurityUtils;
import com.zerozoa.skinner.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
