# rule

## branch 규칙
- `main`: 제출 가능한 안정 버전
- `feature/<기능명>`: 기능 개발
- `fix/<이슈명>`: 버그 수정
- `test/<대상>`: 테스트 작성·보완
- `docs/<문서명>`: 문서 수정

## commit & PR 규칙
- 커밋은 하나의 논리적 변경 단위로 작성합니다.
- 커밋 메시지는 변경 목적이 드러나도록 작성합니다.
  - `feat` : 새로운 기능 추가
  - `fix` : 버그 수정
  - `docs` : 문서 수정
  - `refactor` : 리팩토링
  - `test` :  테스트 코드 작성 및 수정
  - `chore` : 기타 수정 사항
- 커밋 메시지 규칙 - [타입명] : commit msg (ID)
  - ex) feat : add block pattern (FR-00)
- Pull Request에는 변경 내용, 테스트 결과, ID, 검토 요청사항을 포함합니다.
- `main` 병합 전 사람/코파일럿의 코드 리뷰 후 사람의 승인을 거쳐 merge해야 합니다.
