package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.member.MemberResponse;
import com.zerozoa.skinner.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    @GetMapping("/me") // URL에 uuid 노출 안 함
    public ResponseEntity<MemberResponse> getMyInfo(@AuthenticationPrincipal String memberUuid) {
        log.info("[API] Get My Info Request: uuid={}", memberUuid);

        // 토큰에 있는 UUID로 조회
        MemberResponse response = MemberResponse.from(memberService.getByUuid(UUID.fromString(memberUuid)));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "닉네임 중복 확인", description = "true: 중복됨 / false: 사용 가능")
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        // 이건 로그인 안 한 사람도 가입할 때 써야 하므로 SecurityConfig에서 permitAll() 필요
        boolean isDuplicate = memberService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 계정을 탈퇴합니다.")
    @DeleteMapping("/me") // URL에 uuid 노출 안 함
    public ResponseEntity<Void> withdrawMember(@AuthenticationPrincipal String memberUuid) {
        log.warn("[API] Withdraw Member Request: uuid={}", memberUuid);

        // 토큰에 있는 UUID로 삭제
        memberService.deleteMember(UUID.fromString(memberUuid));
        return ResponseEntity.noContent().build();
    }
}
