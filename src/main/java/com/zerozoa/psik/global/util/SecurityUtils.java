package com.zerozoa.psik.global.util;

import com.zerozoa.psik.global.exception.BusinessException;
import com.zerozoa.psik.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * SecurityContext에서 인증 정보를 추출하는 공통 유틸
 * JwtAuthenticationFilter에서 UUID를 Principal로 넣어주기 때문에,
 * 컨트롤러마다 반복되던 null 체크 + 캐스팅 로직을 한 곳으로 통합합니다.
 */
@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * @AuthenticationPrincipal로 받은 Object에서 UUID를 안전하게 추출
     * @param principal 컨트롤러에서 @AuthenticationPrincipal로 주입받은 객체
     * @return 회원 UUID
     * @throws BusinessException 인증 정보가 없거나 타입이 맞지 않을 때
     */
    public static UUID extractMemberUuid(Object principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "인증 정보가 유효하지 않습니다.");
        }

        if (principal instanceof UUID uuid) {
            return uuid;
        }

        log.error("[SecurityUtils] Principal 타입 불일치. Expected: UUID, Actual: {}", principal.getClass().getName());
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "인증 객체 타입 오류");
    }
}
