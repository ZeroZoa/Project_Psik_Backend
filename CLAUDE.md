# Psik Backend

화장품 성분 분석 서비스의 Spring Boot 백엔드. 성분 DB, RAG 챗봇, 피부 이미지 분석(Gemini Vision), 커뮤니티, 스킨 다이어리 기능을 제공한다.

## 기술 스택

- Java 21, Spring Boot 3.5.10
- Spring Security 6 (JWT + OAuth2 Client)
- JPA + Hibernate, QueryDSL 5.1.0
- PostgreSQL (Cloud SQL) + pgvector 확장
- Google Cloud Storage (google-cloud-storage 2.64.1) — 이미지 저장
- Scrimage 4.3.10 — 이미지 리사이즈/WebP 변환
- Gemini API — 임베딩(gemini-embedding-2, 768차원) + RAG 챗봇 + 피부 분석(Vision)
- jjwt 0.12.7 — JWT 발급/검증
- GCP Secret Manager — prod 환경변수 관리

## Commands
- Build: `./gradlew build -x test`
- Compile check: `./gradlew compileJava`
- Test: `./gradlew test`

## 아키텍처

```
[Flutter Web — Firebase Hosting]
  ↓ https://api.psik.kr (Cloudflare Worker 프록시)
[Cloudflare Worker] → Host 헤더 변환
  ↓
[Spring Boot — Cloud Run (asia-northeast3, min=1, max=3)]
  ↓
[PostgreSQL — Cloud SQL] / [GCS — 이미지]
```

## 패키지 구조

```
com.zerozoa.psik
├── controller     (13개: Admin, Auth, Chat, Comment, Ingredient, Inquiry,
│                   Member, MemberProduct, MyComment, Post, Product,
│                   SkinAnalysis, SkinDiary)
├── domain         (community, contents, member, diary, auth, common, inquiry)
├── dto
├── global
│   ├── config     (SecurityConfig, CorsConfig, GcsConfig, QueryDslConfig 등)
│   ├── exception  (GlobalExceptionHandler + BusinessException/ErrorCode)
│   ├── security   (JwtTokenProvider, JwtAuthenticationFilter, oauth/*)
│   └── scheduler  (TokenCleanupScheduler)
├── repository
└── service        (16개)
```

## 핵심 설계 결정 (Why)

### N+1 해결
`Ingredient`의 컬렉션(effects, cautions, skinConcerns, products)에 `@BatchSize(size=100)` 적용. fetch join 대신 선택한 이유: 페이징과 호환되어야 했기 때문 (fetch join은 컬렉션 페이징 시 메모리 페이징으로 빠짐).

### 트랜잭션 전략
클래스 레벨 `@Transactional(readOnly=true)`를 기본 적용하고, 쓰기 작업에만 메서드 레벨 `@Transactional`을 별도 선언. 읽기 전용 트랜잭션은 Hibernate의 dirty checking 오버헤드를 줄여줌.

### OAuth2 + STATELESS
`SessionCreationPolicy.STATELESS`라 기본 `HttpSessionOAuth2AuthorizationRequestRepository`(세션 기반)를 못 씀. `HttpCookieOAuth2AuthorizationRequestRepository`를 직접 구현해 `OAuth2AuthorizationRequest` 객체 전체를 Base64 직렬화 후 httpOnly+Secure(항상 true)+SameSite=Lax 쿠키(3분 TTL)에 저장. Spring Security 공식 `SecurityJackson2Modules`를 사용해 안전하게 (역)직렬화.

### 토큰 저장 전략 (2026-09-12 개선)
- **AccessToken**: 서버에 저장 안 함(stateless). URL 파라미터로 프론트에 전달하던 방식 제거 — 지금은 OAuth 리다이렉트 시 아무 토큰도 안 실음. 프론트가 로드 직후 `/api/auth/reissue`를 호출해 받아감(응답 body).
- **RefreshToken**: DB(`refresh_tokens` 테이블) + httpOnly 쿠키 이중 저장. DB에 저장하는 이유는 로그아웃 시 즉시 무효화, 재발급 시 Rotation(RTR), 탈퇴 시 일괄 삭제가 필요하기 때문. `token` 컬럼엔 의도적으로 unique 제약 없음(JWT 특성상 충돌 확률 사실상 0 — 팀 논의 후 현상 유지 결정).
- ⚠️ 알려진 리스크: Access/Refresh 토큰에 `typ` 클레임이 없어 RefreshToken을 Authorization 헤더에 넣어도 인증됨. 백로그 처리 예정 (아래 "알려진 기술 부채" 참고).

### RAG 파이프라인 (2가지 별도 흐름)
1. **저장 시점**: 성분 등록/수정 시 `Ingredient.toEmbeddingText()` → Gemini 임베딩 → pgvector 저장. 임베딩 API 실패해도 예외를 삼켜 트랜잭션 롤백 안 시킴 (`/api/admin/ingredients/embed-all`로 재실행 가능).
2. **질의 시점**: `ChatController` → `RagService` — 질문 임베딩 → pgvector 유사도 검색(threshold=0.6, TOP_K=5) → 유사 성분 없으면 LLM 호출 없이 즉시 응답(비용 절감) → Gemini `systemInstruction`으로 컨텍스트+피부고민 주입해 답변 생성.
3. `pgvector`는 `<=>` 코사인 유사도, `@ColumnTransformer(write = "CAST(? AS vector)")`로 Hibernate varchar→vector 타입 불일치 해결.

### 피부 이미지 분석
`SkinAnalysisService` — Chain-of-Thought 프롬프트로 Gemini Vision 호출, 하루 최대 3회 제한(KST 기준), 재분석 방지. 이미지는 Scrimage로 512x512 JPEG 압축 후 전송.

### 이미지 처리
`FileStorageService` 인터페이스 → `@Profile("prod")`: GcsFileStorageService, `@Profile("!prod")`: LocalFileStorageService. 업로드 시 Scrimage로 600x600 WebP 변환 후 저장. 확장자는 검증만 하고 실제로는 항상 WebP로 재인코딩(악성 파일 방어 겸함).

### 탈퇴 회원 처리
Ghost User 패턴(UUID: `00000000-0000-0000-0000-000000000000`). 순서: 좋아요 삭제(unique 제약으로 Ghost 교체 불가) → 보유제품 삭제 → 스킨다이어리 삭제 → 커뮤니티 익명화(`@Modifying` 벌크 업데이트) → 토큰 삭제 → Hard Delete.

### 반정규화
`Post`에 `likeCount`, `commentCount`, `viewCount` 직접 저장 (COUNT 쿼리 제거).

## Core Rules

- DTO는 Java **record** + Bean Validation 사용 (`@NotBlank`, `@Size`, `@NotNull`) — 예: `IngredientCreateRequest`
- 예외는 항상 `BusinessException(ErrorCode.XXX, "선택적 메시지")` — 커스텀 예외 클래스를 새로 만들지 않는다. 새 에러 케이스는 `ErrorCode` enum에 추가.
- 컨트롤러는 `ResponseEntity<T>`를 직접 반환한다. **커스텀 응답 래퍼(`ApiResponse` 등)는 존재하지 않음 — 만들어내지 말 것.**
- 서비스는 `@RequiredArgsConstructor` 생성자 주입 + 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드에만 메서드 레벨 `@Transactional` 추가.
- 조회 헬퍼 메서드는 `findXById`/`findXByUuid` 네이밍으로 private 메서드화하고 `orElseThrow(() -> new BusinessException(ErrorCode.X_NOT_FOUND))` 패턴을 따른다 (`PostService.findPostById` 등 참고).
- 로깅은 `@Slf4j` + `log.info("[Domain] 액션 설명 - key={}", value)` 형식 (예: `log.info("[Admin] 성분 생성 완료 - id={}, name={}", ...)`).
- 컨트롤러엔 Swagger 어노테이션(`@Operation`, `@Tag`) 필수.

## 도구 사용 우선순위

JetBrains(IntelliJ) MCP 도구가 세션에 연결되어 있다면, 단순 열람 이상의 작업엔 일반 Read/Bash/grep보다 이걸 우선 사용한다. IntelliJ는 `/Users/noseungjun/IdeaProjects/Project_Psik`(부모 폴더) 전체를 하나의 프로젝트로 열어둔 상태이므로, `projectPath`는 항상 이 경로로 지정하고 `filePath`는 `psik_backend/...`처럼 그 기준 상대경로로 준다.

- **코드 진단**: `get_file_problems` — 컴파일 통과 이상의 IntelliJ 인스펙션(코드 스멜, 잠재 버그, 미사용 import)까지 확인. 정상 작동 검증됨(2026-09-23).
- **심볼 리네임**: `rename_refactoring` — 텍스트 치환 대신 프로젝트 전체 참조를 안전하게 갱신 (미검증, 시도 후 실패 시 수동 폴백)
- **코드 검색**: `search_in_files_by_regex`/`search_in_files_by_text` — 2026-09-23 기준 `probablyHasMoreMatchingEntries` 스키마 에러로 실패 확인됨(도구 자체 버그로 추정). **당분간 grep으로 폴백**, 이후 세션에서 재시도해서 고쳐졌는지 확인할 것.
- **심볼 이해**: `get_symbol_info` — 정의/시그니처/문서를 grep보다 정확하게 조회 (미검증)

단, CI와 동일한 검증(`./gradlew build`, `./gradlew compileJava`)은 계속 네이티브 명령으로 한다 — IDE 진단은 보완재이지 빌드 검증의 대체재가 아니다. (프론트엔드 Flutter/Dart 파일에도 통하는지는 미검증.)

## 테스트 전략

**현재 상태**: 테스트 커버리지 사실상 없음 (`PsikApplicationTests`만 존재, 컨텍스트 로딩 확인용 스모크 테스트 수준).

**원칙**: 전면적인 테스트 인프라 구축보다, 새 기능/버그 수정 시 해당 핵심 로직에 대한 단위 테스트를 최소 1개는 같이 작성하는 것을 목표로 점진적으로 채워나간다.

**우선순위**: 인증/토큰 로직(`AuthService`, `JwtTokenProvider`) > 트랜잭션이 얽힌 비즈니스 로직(회원 탈퇴, RAG 파이프라인) > 단순 CRUD

**컨벤션** (새로 작성 시 적용):
- 클래스명: `XxxServiceTest`, `XxxControllerTest`
- JUnit 5 + AssertJ, `@DisplayName`은 한글로 작성
- Given-When-Then 주석으로 구간 구분

**실행**: `./gradlew test`

## 코드 리뷰 체크리스트

이 프로젝트에서 실제로 반복 발견된 패턴 기준:
- [ ] 트랜잭션 경계가 명확한가 (읽기전용 vs 쓰기)
- [ ] N+1 발생 가능한 컬렉션 조회에 `@BatchSize`/fetch join을 고려했는가
- [ ] 동시 요청 상황에서 unique 제약 위반을 방어했는가 (`DataIntegrityViolationException` 캐치 패턴 — `MemberProductService.markAsOwned` 참고)
- [ ] 외부 API(Gemini, GCS) 호출 실패 시, 롤백되지 않는 자원(업로드된 파일 등) 정리가 되는가
- [ ] DTO에 입력값 검증(`@Valid`, `@Size`, `@NotBlank`)이 있는가
- PR 단위로 `/code-review` 스킬 활용 권장

## 디버깅 가이드

- 로컬 실행: `./gradlew bootRun` (기본 profile → `LocalFileStorageService` 사용, GCS 불필요)
- prod 재현: `SPRING_PROFILES_ACTIVE=prod`는 GCS/Secret Manager 의존이라 로컬 완전 재현 어려움 — 대신 `gcloud logging read` 또는 Cloud Run 콘솔에서 실제 로그 확인
- 에러 원인 특정: 응답 body의 `code` 필드 → `global/exception/ErrorCode` 매핑 확인
- 자주 보는 에러:
  - `GEMINI_RATE_LIMIT_EXCEEDED` — Gemini API 쿼터 초과, 재시도 필요
  - `INVALID_TOKEN` — JWT 서명/만료 문제, `JwtAuthenticationFilter` 로그 확인
  - `NonUniqueResultException` 관련 — "알려진 기술 부채" 1번(토큰 unique 제약 없음) 참고

## 브랜치 전략 & 배포

- `develop`: push 시 빌드 검증만 (`backend-ci.yml`)
- `main`: push 시 Docker 빌드 → Artifact Registry(git SHA 태그) → Cloud Run 배포 (`backend-deploy.yml`)
- 배포 흐름: `develop`에 커밋 → PR로 `main` 병합 → 자동 배포

## 작업 규칙

- **코드는 직접 수정하지 않고 스니펫만 제공한다.** 사용자가 명시적으로 "이번엔 네가 수정해줘"라고 말할 때만 예외.
- **git add/commit/push도 항상 사용자가 직접 한다.** AI는 실행할 명령어와 커밋 메시지만 제공하고, 별도 지시("커밋까지 해줘" 등) 없으면 절대 직접 커밋/푸시하지 않는다.
- 리팩토링/기능 추가 전에는 관련 파일을 먼저 읽고 확인한 뒤 코드를 제공한다.
- 커밋 메시지는 `type: 설명` 스타일. 타입: `feat`, `fix`, `docs`, `perf`, `refactor`, `ci`, `chore`. 헤더는 한 줄 요약 위주, 본문은 "왜"가 diff로 안 보일 때만 추가.

## 알려진 기술 부채 (우선순위 순, 취업시즌 이후 착수 예정)

1. JWT Access/Refresh 토큰에 타입 클레임 없음 (RefreshToken을 Access처럼 재사용 가능)
2. 좋아요 토글(`PostService`/`CommentService`) 동시성 미처리 — `MemberProductService.markAsOwned`의 `DataIntegrityViolationException` 캐치 패턴 재사용 권장
3. 피부 분석 실패 시 일부 예외 경로에서 GCS 이미지 파일 정리 누락
4. Gemini 호출(`GeminiService`, `EmbeddingService`)에 WebClient 타임아웃 미설정
5. `/api/chat`에 요청 빈도 제한 없음 (SkinAnalysis는 하루 3회 제한 있음)
6. `AdminService.embedAll()` 동기 순차 처리 + `Thread.sleep(150)` — 성분 많아지면 요청 스레드 장시간 점유
7. 공개 페이징 API 최대 size 캡 없음
8. `PostService.deletePost` — GCS 삭제 후 DB 삭제 순서 (update 흐름과 반대)
