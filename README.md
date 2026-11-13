# REMAKE
냉장고 속 식재료를 스마트하게 관리하고, AI 맞춤형 레시피와 소비 알림을 통해 식재료 낭비를 줄여주는 AI 기반 냉장고 관리 서비스

## 배포

백엔드 CD 설정과 서버 준비 방법은 [배포 문서](docs/deployment.md)를 참고하세요.

## PR의 AI 도구 출처 문구

`.claude/settings.json`은 Claude Code가 커밋의 공동 작성자 줄과 PR 설명의 자동 출처 문구를 추가하지 않도록 설정합니다. `PR Attribution Check`는 PR 설명과 커밋에 Claude, Codex, Copilot 등 도구의 자동 출처 문구가 남아 있으면 실패합니다. 사람이 작성한 일반적인 AI 기능 설명은 검사 대상이 아닙니다.

병합까지 막으려면 GitHub의 `dev`와 `main` 규칙에 **Check agent attribution** 상태 검사를 필수 검사로 추가해야 합니다. 이 검사는 PR 생성 자체를 막거나 이미 올라간 문구를 자동 삭제하지는 않습니다. 검사에 걸리면 PR 설명을 수정하거나 해당 커밋 메시지를 수정한 뒤 브랜치를 갱신하세요.
