# Claude 도구 사용 가이드 (Psik Backend)

IntelliJ는 `psik_backend`, `psik_frontend`를 별도 프로젝트로 연다. 백엔드 작업 시 `projectPath`는 `/Users/noseungjun/IdeaProjects/Project_Psik/psik_backend`, 경로는 그 기준 상대경로(`src/main/java/...`). 연결된 프로젝트 목록은 에러 메시지나 `get_project_modules`로 매번 확인.

| 도구 | 용도 | 상태 |
|---|---|---|
| `get_file_problems` | IntelliJ 인스펙션(코드 스멜, 미사용 import) | 정상 (2026-09-23) |
| `rename_refactoring` | 프로젝트 전체 참조 안전 리네임 | 미검증, 실패 시 수동 |
| `search_in_files_by_text` | 텍스트 코드 검색 | 정상 (2026-10-18) |
| `search_in_files_by_regex` | 정규식 코드 검색 | 미검증 (2026-09-23에 text/regex 모두 실패했으나 text는 복구됨). 실패 시 grep 폴백 |
| `get_symbol_info` | 정의/시그니처 조회 | 미검증 |

CI와 동일한 검증(`./gradlew build`, `./gradlew compileJava`)은 항상 네이티브 명령으로. IDE 진단은 보완재.
