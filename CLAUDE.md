# Psik Backend

화장품 성분 분석 서비스의 Spring Boot 백엔드. 성분 DB, RAG 챗봇, 피부 이미지 분석(Gemini Vision), 커뮤니티, 스킨 다이어리.

상세 문서 (해당 영역을 수정하거나 설계를 판단할 때 먼저 Read로 읽는다):
- `docs/architecture.md` — 설계 결정과 이유: N+1, 트랜잭션, OAuth2/토큰, RAG, Pub/Sub 피부 분석, 동시성, 이미지, 탈퇴, 디버깅, 알려진 기술 부채
- `docs/claude-tooling.md` — IntelliJ MCP 도구 사용법과 검증 상태 (열람 이상의 작업엔 일반 Read/Bash보다 우선)

## 작업 규칙 (최우선)
- 코드는 직접 수정하지 않고 스니펫만 제공한다. 사용자가 "이번엔 네가 수정해줘"라고 할 때만 예외.
- git add/commit/push는 항상 사용자가 직접 한다. AI는 명령어와 커밋 메시지만 제공한다.
- 수정 전 관련 파일을 먼저 읽고, 변경 계획(파일 목록 + 이유)을 먼저 보여준다.
- 요청 범위 밖의 리팩터링·포맷 변경·의존성 추가 금지. 최소 변경 원칙.
- 커밋 메시지: `type: 설명` (feat, fix, docs, perf, refactor, ci, chore). 본문은 "왜"가 diff로 안 보일 때만.

## 기술 스택
Java 21, Spring Boot 3.5.10, Spring Security 6(JWT + OAuth2), JPA + QueryDSL 5.1.0, PostgreSQL(Cloud SQL) + pgvector, GCS, Scrimage 4.3.10, Gemini API(임베딩 gemini-embedding-2 768차원 / RAG / Vision), jjwt 0.12.7, GCP Secret Manager(prod).
배포: Flutter Web(Firebase) → Cloudflare Worker → Cloud Run(asia-northeast3) → Cloud SQL / GCS.

## 명령
- 컴파일 확인: `./gradlew compileJava`
- 빌드: `./gradlew build -x test`
- 테스트: `./gradlew test` (`PsikApplicationTests`는 로컬 PostgreSQL 필요. DB 없으면 `./gradlew test --tests "패키지.*"`)
- CI와 동일한 검증: `CI=true ./gradlew clean build` (DB가 필요한 PsikApplicationTests 제외)
- 로컬 실행: `./gradlew bootRun` (기본 profile = LocalFileStorageService)

## 패키지 구조 (`com.zerozoa.psik`)
controller / service / dto / repository 모두 도메인별 하위 패키지:
admin, auth, chat, community, contents, diary, inquiry, member.
- `service/ai`: GeminiService, EmbeddingService (여러 도메인 공유)
- `service/storage`: FileStorageService 구현체 (GCS=prod, Local=local)
- `global`: config, exception(GlobalExceptionHandler, BusinessException, ErrorCode), security, scheduler

## 코드 컨벤션
- DTO: Java record + Bean Validation(`@NotBlank`, `@Size`, `@NotNull`).
- 예외: 항상 `BusinessException(ErrorCode.XXX, "메시지")`. 새 예외 클래스 금지, 새 케이스는 ErrorCode에 추가. 메시지는 응답 body에 그대로 노출되므로 사용자에게 보여도 되는 문구만.
- 컨트롤러: `ResponseEntity<T>` 직접 반환. `ApiResponse` 같은 래퍼는 없음 — 만들지 말 것. Swagger `@Operation`, `@Tag` 필수.
- 서비스: `@RequiredArgsConstructor` + 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`.
- 롤백되지 않는 외부 작업(GCS 삭제, Pub/Sub 발행)은 `AfterCommit.run`으로 커밋 후 실행.
- 조회+NOT_FOUND 헬퍼: 3곳 이상 반복되면 repository `default` 메서드(`findByUuidOrThrow`, `findOwnedById`), 1~2곳이면 서비스 private 메서드.
- 로깅: `@Slf4j`, `log.info("[Domain] 액션 - key={}", value)`.

## 절대 바꾸지 말 것
- Pub/Sub 구독 `parallel-pull-count: 8` (줄이면 PENDING 고착 장애 재발)
- `SkinAnalysisTxOps.markFailed`의 `REQUIRES_NEW` (afterCommit에서 호출됨)
- 임베딩 실패 시 예외를 삼키는 동작 (의도된 설계, embed-all로 재실행)
- 좋아요 토글은 `findByIdForUpdate`(비관적 락)로 조회한다. 락 없는 `findById`로 되돌리지 말 것
- 조회수·댓글 수는 `PostRepository`의 `UPDATE ... SET x = x + 1`로 증감한다. 엔티티 `++`로 되돌리지 말 것. `Post`/`Comment`의 `@DynamicUpdate`도 유지

## 테스트
- 새 기능/버그 수정 시 핵심 로직 단위 테스트 최소 1개 함께 작성.
- 우선순위: 인증/토큰 > 트랜잭션 얽힌 로직(탈퇴, RAG) > 단순 CRUD.
- JUnit 5 + AssertJ, `XxxServiceTest`, `@DisplayName` 한글, Given-When-Then 주석.
- repository `default` 메서드는 목에서 null 반환 → 스텁하거나 `mock(Repo.class, CALLS_REAL_METHODS)`.

## 리뷰
리뷰는 `psik-backend-reviewer` 에이전트를 사용한다 (체크리스트는 `.claude/agents/psik-reviewer.md`).
