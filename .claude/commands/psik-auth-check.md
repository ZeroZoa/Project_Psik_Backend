---
description: 인증/토큰 흐름 점검 체크리스트 (Psik 백엔드)
---

Psik의 인증 흐름을 점검한다. `docs/architecture.md`의 "토큰 저장 전략" 섹션을 기준으로 아래를 순서대로 확인한다:

1. `JwtTokenProvider`, `JwtAuthenticationFilter`, `AuthService`, `OAuth2SuccessHandler`를 Read로 열어, 현재 구현이 `docs/architecture.md`에 적힌 설계와 일치하는지 확인한다.
2. AccessToken이 URL 파라미터나 로그에 노출되는 경로가 새로 생기지 않았는지 확인한다:
   `grep -rn "accessToken=" src/main/java`
3. RefreshToken이 httpOnly/Secure 쿠키로만 발급되는지 `CookieUtils`, `OAuth2SuccessHandler`, `AuthController`를 확인한다.
4. Access/Refresh 토큰이 `typ` 클레임(ACCESS/REFRESH)으로 구분되는지 확인한다. `JwtAuthenticationFilter`가 ACCESS만 인증하고, `AuthService.reissue`가 REFRESH만 받는지 코드로 검증한다. 설계 상세는 `docs/architecture.md`의 "토큰 저장 전략" 참고.
5. 발견한 문제를 실무 기준 심각도(상/중/하)로 정리해서 보고한다. **코드는 직접 수정하지 않고 스니펫만 제공한다.**
