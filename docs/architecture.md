# Psik Backend 설계 결정 (Why)

## N+1 해결
`Ingredient` 컬렉션(effects, cautions, skinConcerns, products)에 `@BatchSize(size=100)`. fetch join 대신 선택한 이유: 페이징 호환 (컬렉션 fetch join + 페이징은 메모리 페이징으로 빠짐).

## 트랜잭션 전략
클래스 레벨 `@Transactional(readOnly=true)` 기본, 쓰기만 메서드 레벨 `@Transactional`. 읽기 전용은 Hibernate dirty checking 오버헤드 감소.

## OAuth2 + STATELESS
`SessionCreationPolicy.STATELESS`라 세션 기반 `HttpSessionOAuth2AuthorizationRequestRepository` 사용 불가. `HttpCookieOAuth2AuthorizationRequestRepository`를 직접 구현해 `OAuth2AuthorizationRequest`를 Base64 직렬화 후 httpOnly + Secure(항상 true) + SameSite=Lax 쿠키(3분 TTL)에 저장. (역)직렬화는 Spring Security 공식 `SecurityJackson2Modules` 사용.

## 토큰 저장 전략 (2026-09-12 개선)
- AccessToken: 서버 저장 안 함. OAuth 리다이렉트에 토큰을 싣지 않고, 프론트가 로드 직후 `/api/auth/reissue` 호출해 응답 body로 받음.
- RefreshToken: DB(`refresh_tokens`) + httpOnly 쿠키 이중 저장. DB 저장 이유: 로그아웃 즉시 무효화, RTR, 탈퇴 시 일괄 삭제. `token` 컬럼 unique 제약은 의도적으로 없음(JWT 충돌 확률 사실상 0, 팀 결정).
- 타입 구분: `typ` 클레임(ACCESS/REFRESH). `JwtAuthenticationFilter`는 ACCESS만 인증, `AuthService.reissue`는 REFRESH만 수용(Authorization 헤더에 Refresh 넣으면 401).

## RAG 파이프라인
1. 저장 시점: 성분 등록/수정 → `Ingredient.toEmbeddingText()` → Gemini 임베딩 → pgvector 저장. 임베딩 실패해도 예외를 삼켜 롤백 안 함 (`/api/admin/ingredients/embed-all`로 재실행).
2. 질의 시점: `ChatController` → `RagService` — 질문 임베딩 → pgvector 유사도 검색(threshold 0.6, TOP_K 5) → 유사 성분 없으면 LLM 호출 없이 즉시 응답(비용 절감) → Gemini `systemInstruction`에 컨텍스트 + 피부고민 주입.
3. pgvector `<=>` 코사인 거리, `@ColumnTransformer(write = "CAST(? AS vector)")`로 varchar→vector 타입 불일치 해결.

## 피부 이미지 분석 (Pub/Sub 비동기)
1. 접수(동기): `SkinAnalysisService.analyze()` — 소유자/재분석/하루 3회(KST) 검증 → GCS 저장 → `SkinAnalysis` PENDING 저장 → 커밋 후(`AfterCommit.run`) `SkinAnalysisEventPublisher`가 `skin-analysis-requests` 발행. 커밋 전 발행 시 워커가 행을 못 찾아 메시지 소실. 발행 실패 시 `SkinAnalysisFailureHandler`가 FAILED 전환 + 사진 삭제.
2. 처리(비동기): `SkinAnalysisWorker`가 `@PostConstruct`에서 `skin-analysis-requests-sub` 구독. Gemini Vision `responseSchema` structured output(`faceDetected` + 점수 4종 + `summary`), 호출 전 Scrimage 512x512 JPEG 압축. 전용 스레드풀(4 + 대기열 4), 시도당 60초 제한.
   - `parallel-pull-count: 8` 필수: 작으면 스트림 1개라 연속 요청 시 두 번째가 PENDING 고착(과거 무한 로딩 장애 원인).
3. 멱등성: at-least-once라 `SkinAnalysisTxOps.check()`로 `ALREADY_DONE` 스킵, 5분 지난 건 `EXPIRED` → FAILED. `markCompleted`/`markFailed`는 PENDING일 때만 변경(늦게 온 결과 덮어쓰기 방지). ⚠️ 체크와 변경이 한 트랜잭션이 아니라 동시 배달은 못 막음(기술 부채 1).
4. ack/nack: 성공 → `markCompleted()`. 얼굴 미검출/기타 예외 → `markFailed()` + GCS 삭제 후 ack. 깨진 JSON → ack 폐기. 풀 포화·DB 장애 등 일시 실패만 nack. 각 메서드 독립 트랜잭션.
5. 프론트 `SkinAnalysisProvider`가 5초 간격 최대 20회(100초) 폴링.
6. 안전망: `SkinAnalysisSweeper`가 1분마다 10분 넘은 PENDING을 FAILED로 정리. `markFailed`는 afterCommit에서도 호출되므로 `REQUIRES_NEW` (REQUIRED면 끝난 트랜잭션에 참여해 저장 안 됨).

## 동시성 (좋아요 · 카운터 · 커밋 후 작업)
- 좋아요 토글: `PostRepository`/`CommentRepository.findByIdForUpdate`(`@Lock(PESSIMISTIC_WRITE)`, SELECT ... FOR UPDATE)로 행을 잠가 같은 글/댓글의 요청을 순서대로 처리. 락은 트랜잭션 종료(커밋/롤백) 때 풀린다. 낙관적 락(`@Version`)은 충돌이 드문 경우용이라 좋아요에 맞지 않아 `RefreshToken`에만 사용. 락이 실제로 걸리는지는 단위 테스트로 검증 불가(락 조회 메서드 사용 여부까지만 테스트).
- 조회수·댓글 수: `PostRepository.increaseViewCount/increaseCommentCount/decreaseCommentCount`의 `UPDATE ... SET x = x + 1`로 DB가 직접 계산. 엔티티에서 읽고 `++` 하면 커밋 때 모든 컬럼을 다시 써서 좋아요 수 등을 옛날 값으로 덮어쓰고 값도 유실된다. `Post`/`Comment`의 `@DynamicUpdate`로 수정 시 바뀐 컬럼만 UPDATE. `getPost`는 조회수를 먼저 올린 뒤 글을 읽는다(순서가 바뀌면 응답 조회수가 옛날 값).
- 커밋 후 작업: 롤백될 수 있는 DB 작업을 먼저 확정하고 되돌릴 수 없는 외부 작업(GCS 삭제, Pub/Sub 발행)은 `AfterCommit.run`으로 실행. 롤백되면 실행되지 않고, 활성 트랜잭션이 없으면 즉시 실행, 실패는 로그만 남긴다. afterCommit 안에서 DB를 쓰려면 `REQUIRES_NEW`가 필요(`markFailed` 참고).
- unique 제약 동시 요청: 제약 위반을 `DataIntegrityViolationException`으로 잡아 `BusinessException`으로 변환(`MemberProductService.markAsOwned`, 다이어리/분석 생성).
- 알려진 한계: `deletePost`와 `toggleLike`의 락 순서가 달라 삭제 순간 동시 좋아요 시 데드락 가능성이 아주 낮게 있음. 하루 3회 분석 제한은 락이 없어 동시 요청 시 초과 가능.

## 이미지 처리
`FileStorageService` → `@Profile("prod")` GcsFileStorageService / `@Profile("local")` LocalFileStorageService. 업로드 시 600x600 WebP 재인코딩(확장자는 검증만, 악성 파일 방어 겸함).

## 탈퇴 회원 처리
Ghost User(UUID `00000000-0000-0000-0000-000000000000`). 순서: 좋아요 삭제(unique 제약 때문에 교체 불가) → 보유제품 삭제 → 스킨다이어리 삭제 → 커뮤니티 익명화(`@Modifying` 벌크) → 토큰 삭제 → Hard Delete.

## 반정규화
`Post`에 `likeCount`, `commentCount`, `viewCount` 저장 (COUNT 쿼리 제거). 증감 방식은 위 "동시성" 참고.

## 디버깅
- prod 재현은 GCS/Secret Manager 의존이라 어려움 → `gcloud logging read` 또는 Cloud Run 콘솔 로그.
- 응답 body `code` → `global/exception/ErrorCode` 매핑.
- `GEMINI_RATE_LIMIT_EXCEEDED`: 쿼터 초과, 재시도. `INVALID_TOKEN`: `JwtAuthenticationFilter` 로그. `NonUniqueResultException`: refresh token unique 제약 없음 결정 참고.
- 로컬 실행 주의: local 프로필로 켜도 gcloud 인증(ADC)으로 운영 GCP에 붙어 `SkinAnalysisWorker`가 운영 구독을 같이 구독한다. 로컬 DB에는 그 행이 없어 운영 사용자의 분석이 PENDING에 걸릴 수 있다(스위퍼가 10분 뒤 FAILED로 복구). 짧게 켜고 끌 것. 8080 포트는 다른 프로젝트 Docker 컨테이너(`crewer_app`)와 충돌할 수 있다.

## 알려진 기술 부채 (취업시즌 이후)
1. SkinAnalysisWorker 멱등성 체크가 상태 변경과 별도 트랜잭션 → CAS(`UPDATE ... WHERE status='PENDING'`) 필요
2. EmbeddingService WebClient 타임아웃 없음 (`.block()` 무기한 점유 위험)
3. `/api/chat` 요청 빈도 제한 없음
4. `AdminService.embedAll()` 동기 순차 + `Thread.sleep(150)`
5. 공개 페이징 API size 상한 없음
