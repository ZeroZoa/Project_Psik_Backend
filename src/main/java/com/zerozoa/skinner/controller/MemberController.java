package com.zerozoa.skinner.controller;

import com.zerozoa.skinner.dto.member.MemberResponse;
import com.zerozoa.skinner.global.exception.BusinessException;
import com.zerozoa.skinner.global.exception.ErrorCode;
import com.zerozoa.skinner.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

//Member 관련 API 컨트롤러
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    /**
     *내 정보 조회 (마이페이지)
     *@param principal 현재 로그인한 사용자의 식별자 (JwtAuthenticationFilter에서 UUID 객체로 저장함)
     *@return 회원 정보 DTO (이메일, 닉네임, 프로필 사진 등)
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 상세 정보를 조회합니다. (Access Token 필수)")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            //JwtAuthenticationFilter에서 UUID 객체를 받음 -> principal 객체로 삽입
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal
    ) {
        //인증 정보 누락 체크
        if (principal == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "인증 정보가 유효하지 않습니다.");
        }

        //Object -> UUID 캐스팅
        UUID uuid;
        try {
            uuid = (UUID) principal;
        } catch (ClassCastException e) {
            log.error("[MemberController] Principal Type Mismatch! Expected: UUID, Actual: {}", principal.getClass());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "인증 객체 타입 오류");
        }

        log.info("[API] 내 정보 조회 요청 - UUID: {}", uuid);

        MemberResponse response = MemberResponse.from(memberService.getByUuid(uuid));
        return ResponseEntity.ok(response);
    }

    /**
     *닉네임 중복 확인
     *@param nickname 중복 검사할 닉네임
     *@return true: 중복됨(사용 불가), false: 중복 안 됨(사용 가능)
     */
    @Operation(summary = "닉네임 중복 확인", description = "true: 중복됨 / false: 사용 가능")
    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean isDuplicate = memberService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(isDuplicate);
    }

    /**
     *회원 탈퇴
     *주의사항:
     *단순 삭제가 아니라 연관된 리프레시 토큰 등도 함께 정리해야 함
     *탈퇴 후에는 재로그인이 불가능하므로, 프론트엔드에서도 토큰을 삭제하는 후처리가 필요
     */
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 계정을 영구 삭제합니다. 연관된 모든 데이터(토큰 등)가 함께 삭제됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdrawMember(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal
    ) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        UUID uuid;
        try {
            uuid = (UUID) principal;
        } catch (ClassCastException e) {
            log.error("[MemberController] Withdraw Type Mismatch!");
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        //탈퇴는 민감한 작업이므로 WARN 레벨로 로깅하여 모니터링
        log.warn("[API] 회원 탈퇴 요청 - UUID: {}", uuid);

        memberService.deleteMember(uuid);

        return ResponseEntity.noContent().build();
    }
}
