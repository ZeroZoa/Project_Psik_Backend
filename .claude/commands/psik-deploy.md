---
description: develop → main 병합 및 배포 확인 절차 (Psik 백엔드)
---

Psik 백엔드 배포 절차를 진행한다:

1. `git status`로 현재 develop 브랜치에 커밋 안 된 변경사항이 있는지 확인한다. 있으면 사용자에게 알리고 중단한다.
2. `git fetch origin`으로 원격 최신화 후, `git log --oneline -10`으로 최근 커밋을 확인한다.
3. `git log origin/main..origin/develop --oneline`으로 아직 main에 반영되지 않은 develop 커밋 목록을 보여준다.
4. 사용자에게 main으로 병합(PR 또는 직접 merge)해도 되는지 확인받는다. **push/merge는 절대 직접 실행하지 않고, 실행할 명령어만 제공한다.**
5. 사용자가 병합 완료했다고 알리면, GitHub Actions API로 최신 워크플로우 상태를 확인한다:
   `curl -s "https://api.github.com/repos/ZeroZoa/Project_Psik_Backend/actions/runs?branch=main&per_page=1"`
6. 배포 성공(`conclusion: success`) 확인되면 라이브 헬스체크를 수행한다:
   `curl -s -o /dev/null -w "%{http_code}\n" "https://api.psik.kr/api/ingredients?size=1"`
7. 결과(머지 커밋, CI 상태, 헬스체크 응답 코드)를 요약해서 보고한다.
