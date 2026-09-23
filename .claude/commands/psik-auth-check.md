---
description: 인증/토큰 흐름 점검 체크리스트 (Psik 백엔드)
---

Psik의 인증 흐름을 점검한다. `CLAUDE.md`의 "핵심 설계 결정 > 토큰 저장 전략" 섹션을 기준으로 아래를 순서대로 확인한다:

1. `JwtTokenProvider`, `JwtAuthenticationFilter`, `AuthService`, `OAuth2SuccessHandler`를 Read로 열어, 현재 구현이 CLAUDE.md에 적힌 설계와 일치하는지 확인한다.
2. AccessToken이 URL 파라미터나 로그에 노출되는 경로가 새로 생기지 않았는지 확인한다:
   `grep -rn "accessToken=" src/main/java`
3. RefreshToken이 httpOnly/Secure 쿠키로만 발급되는지 `CookieUtils`, `OAuth2SuccessHandler`, `AuthController`를 확인한다.
4. CLAUDE.md "알려진 기술 부채" 1번(토큰 typ 클레임 없음)이 아직 해결되지 않았는지 확인한다. 해결됐다면 CLAUDE.md에서 제거를 제안한다.
5. 발견한 문제를 실무 기준 심각도(상/중/하)로 정리해서 보고한다. **코드는 직접 수정하지 않고 스니펫만 제공한다.**
