# WBS v2와 GitHub 이슈 대조

점검 기준: 사용자 제공 WBS v2(2026-11-22) · GitHub 2026-11-23 17:13 KST 스냅샷 · dev `c7b5542`.

WBS 76개 작업을 전부 대조해 누락 추적 이슈 15개를 등록했다. 기능 API 60개에는 모두 구현 담당 이슈가 연결돼 있지만, 이는 구현 완료나 최신 계약 충족을 뜻하지 않는다. 실제 누락은 공통 계약·디자인·UI 연결, 버린 금액 화면, 지표 SQL·시드, PWA 출시와 QA에 있었다. 어드민 보류와 지표 SQL 보류를 혼동하면 안 된다.

이 문서는 이슈 대응·처리 이력의 시점 기록이다. 노션·구글 시트 원본 수정본이나 코드 검증 보고서가 아니다. 새 구현·QA 이슈와 미결 정책의 완료 여부는 각 이슈에서 갱신한다.

## 확인한 문서와 기준

| 문서 | 점검 범위 |
| --- | --- |
| WBS | 사용자가 제공한 v2 전문·76개 작업·변경 이력. 기존 구글 시트 gid=0은 v1이라 최신 기준으로 쓰지 않음 |
| PRD·정책 | v4 요약과 영역 가이드·PR #142 최종 결정 대조. 별도 노션 원문 전체는 직접 열지 않음 |
| ERD | 26개 테이블 정의와 영역별 이슈 연결. 전체 스키마 구현 완료 판정은 하지 않음 |
| API·오류 | 60개 경로 전부 연결. 오류는 59개, details·공통 계약은 #45/#182 |
| 이벤트·제품 개선 | 79개(45 CLIENT·34 SERVER), 속성 102·지표 21·화면 41 기준. 별도 제품개선 기획 원문은 직접 열지 않음 |
| 피그마 화면·기능 | 사용자 WBS의 노드와 레포 가이드 기준. 실제 프레임·추가/수정 목록 전체는 #183에서 확인 |
| 재료 단위 | #145 결정·#146 구현·PR #154 병합을 반영. 영수증 PACK 필드 계약은 #173 유지 |
| Git 규칙·템플릿 | 레포 컨벤션, 템플릿 변경 PR #180. PR #180은 확인 시점 OPEN |
| 서비스 약관·개인정보 | #95 약관 드로워·#169 결제 약관 UI·#170 환불 미결 연결. 통합 분석서 원문 내용의 완전성은 이 점검으로 보증하지 않음 |

근거: [PR #142 최종 본문·리뷰](https://github.com/likelion-backendschool-9th/REMAKE/pull/142), [단위 결정 병합 PR #154](https://github.com/likelion-backendschool-9th/REMAKE/pull/154), [기존 v1 WBS](https://docs.google.com/spreadsheets/d/1DjqyGJvrB8bcwndAFoTinrkEqPSPvPT1BJmRbZm5J74/edit?gid=0#gid=0), [API 요약](../agent/spec/api-summary.md), [이벤트 정의 요약](../agent/spec/events.md). WBS v2 전문은 사용자가 2026-11-23 이슈 점검 대화에 제공했다.

## 누락해서 이번에 추가한 이슈 15개

| WBS v2 | 이슈 | 작업 | 우선순위 | 담당 |
| --- | --- | --- | --- | --- |
| 0.2 | [#182](https://github.com/likelion-backendschool-9th/REMAKE/issues/182) | OpenAPI·오류 계약과 FE/BE 합의 | P0 | @JUNHORANG, @ggwg1121 |
| 0.4 | [#183](https://github.com/likelion-backendschool-9th/REMAKE/issues/183) | 누락·수정 피그마 화면 확정 | P0 | @JUNHORANG |
| 0.9 | [#184](https://github.com/likelion-backendschool-9th/REMAKE/issues/184) | 공통 토스트·입력 오류·빈 화면·로딩 | P0 | 미배정 |
| 2.6 | [#185](https://github.com/likelion-backendschool-9th/REMAKE/issues/185) | 이번 달 버린 식재료 금액 화면 | P1 | 미배정 |
| 8.4 | [#186](https://github.com/likelion-backendschool-9th/REMAKE/issues/186) | 지표 21개 조회 SQL | P0 | @JUNHORANG |
| 8.5 | [#187](https://github.com/likelion-backendschool-9th/REMAKE/issues/187) | 시드·데모 계정·이벤트·시연 시나리오 | P1 | @JUNHORANG |
| 9.1 | [#188](https://github.com/likelion-backendschool-9th/REMAKE/issues/188) | PWA manifest·서비스 워커 | P0 | 미배정 |
| 9.2 | [#189](https://github.com/likelion-backendschool-9th/REMAKE/issues/189) | 구글 플레이 계정·비공개 테스터 모집 | P0 | @ureeStar |
| 9.3 | [#190](https://github.com/likelion-backendschool-9th/REMAKE/issues/190) | TWA 패키징·비공개 테스트 등록 | P0 | 미배정 |
| 10.1 | [#191](https://github.com/likelion-backendschool-9th/REMAKE/issues/191) | 화면별 테스트 시나리오 | P0 | @jykim78982 |
| 10.2 | [#192](https://github.com/likelion-backendschool-9th/REMAKE/issues/192) | 통합 테스트·결함 수정 | P0 | @jykim78982 |
| 10.3 | [#193](https://github.com/likelion-backendschool-9th/REMAKE/issues/193) | 모바일 실기기·접근성 점검 | P0 | @jykim78982 |
| 10.4 | [#194](https://github.com/likelion-backendschool-9th/REMAKE/issues/194) | 한도·권한·오류 코드 점검 | P0 | @ggwg1121 |
| 10.5 | [#195](https://github.com/likelion-backendschool-9th/REMAKE/issues/195) | README·포트폴리오 정리 | P1 | @ureeStar |
| 10.6 | [#196](https://github.com/likelion-backendschool-9th/REMAKE/issues/196) | 이벤트 누락·중복·속성 검증 | P0 | @jykim78982 |

12개 P0·3개 P1이다. 기존 WBS에 이름이 있는 공통·BE·디자인·QA 담당을 적용했다. 새 기능 FE 4개(#184/#185/#188/#190)는 미배정이며, 구독 #167~#170·기존 FE 배정도 바꾸지 않았다. #185는 화면·조회 계약 대기라 blocked다. #183은 제품 화면 설계, #184는 앱 공통 UI 연결이므로 PR #162/#163의 컴포넌트 구현을 중복하지 않는다.

## 기존 이슈에 보완한 누락·참조

| 이슈 | 보완 |
| --- | --- |
| [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68) | 하루 지난 탈퇴 인증번호 정기 정리·경계 검증 추가. 토큰 정리 #138과 구분 |
| [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75) | 검증 항목의 27/28/29 번호만 있던 설명을 기능명으로 교체 |
| [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116) | E50 결정 #53·BE 계약 #172 연결, 공통 헤더 3개와 최종 이벤트 검증 #196 명시 |
| [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | PR #154 categoryCode 지원이 dev에 병합된 상태로 갱신. 추가·수정 적용은 여전히 미완료 |
| [#53](https://github.com/likelion-backendschool-9th/REMAKE/issues/53) | WBS 화면 작업 P2와 이벤트 E50 우선순위 미결 구분 |
| [#165](https://github.com/likelion-backendschool-9th/REMAKE/issues/165) | PR #179 요청의 frontend README Web Push 설명 정리 추가. PWA #188 연결 |
| [#78](https://github.com/likelion-backendschool-9th/REMAKE/issues/78) | PR #181의 부분 생성·덮어쓰기 정책 병합 반영; 남은 도메인/API 구현과 구분 |
| [#149](https://github.com/likelion-backendschool-9th/REMAKE/issues/149) | 아래 WBS 수치·정책 충돌 정정 체크 추가. 원본 문서 수정 완료로 표시하지 않음 |

## WBS 원문에서 고쳐야 하는 문구

| 위치 | 기존 | 반영 기준 |
| --- | --- | --- |
| 0.2·0.3·10.4·10.6·변경표 | API 56/60, 오류 52/58, ERD 23/25, 이벤트 72 혼재 | API 60·오류 59·테이블 26·이벤트 79, CLIENT 45·SERVER 34·화면 코드 41 |
| 0.5 | orval 타입 생성 | 현재 레포는 openapi-typescript·openapi-fetch. 도구 교체 요구로 해석하지 않음 |
| 0.4·1.7 | 수정 13개인데 수정 17번 참조 | 추가/수정 원목록·실제 개수 확인 #183 |
| 1.8 | 인증번호 오류를 입력칸 아래 표시 | PR #142 최종 결정은 INVALID/EXPIRED/LOCKED 토스트 |
| 3.7·프론트 URL 표 | OCR 품목 빼기 | PR #142에서 빼기 취소. 수정·추가 유지 |
| 4.4 | 요리 기록 | 별도 recipe_cook_logs 신설 없이 ingredient_logs·cook_completed·cooked_at 사용 |
| 4.9·프론트 URL 표 | 추천 위치 미정·E50 보류 | 냉장고 바텀 시트 확정. 이벤트 우선순위만 #53 미결 |
| 5.6·프론트 URL 표 | 식단 생성 대기·실패 전용 화면 | 202 접수·완성/실패 앱 안 알림. 겹침·한도 안내 유지 |
| 8.3 | CLIENT 이벤트 44개 | E50 포함 45개. UI 우선순위와 이벤트 우선순위 구분 |
| 규모·프론트 URL | 프론트 URL 31개 페이지 | 페이지 31개 + 주소만 3개 = 경로 34개. 모달·시트는 별도 URL 없음 |

식단 상태의 달력·겹침 포함(#144), 장바구니 선택 차감·칸 FK(#57), 영수증 PACK 필드(#173), 공유 사진 규격(#174), 구독 실패·해지·환불·키(#170), 기본 양념(#158), 타임아웃(#50/#122), 디자인 값(#150)은 여전히 별도 결정이다. 부분 생성·하루 단위 자르기·생성된 날까지만 덮어쓰기 기준은 PR #181로 병합됐다. #78의 남은 구현과 구분해 WBS·이슈에 반영한다.

## WBS 76개 작업 전체 대응표

번호는 모두 v2 번호다. `연결됨`은 추적할 곳이 있다는 뜻이며 개발 완료를 뜻하지 않는다. 기존 문구·주차·상태가 오래됐다는 이유로 요구사항을 임의 삭제하지 않는다.

| WBS v2 | 작업 | 이전 WBS | 이슈 | 근거 PR | 판정 |
| --- | --- | --- | --- | --- | --- |
| 0.1 | 레포·브랜치·컨벤션·CI | 0.1 | [#16](https://github.com/likelion-backendschool-9th/REMAKE/issues/16), [#43](https://github.com/likelion-backendschool-9th/REMAKE/issues/43) | [#1](https://github.com/likelion-backendschool-9th/REMAKE/pull/1), [#6](https://github.com/likelion-backendschool-9th/REMAKE/pull/6), [#18](https://github.com/likelion-backendschool-9th/REMAKE/pull/18), [#39](https://github.com/likelion-backendschool-9th/REMAKE/pull/39) | 기반 완료·운영 유지 |
| 0.2 | OpenAPI·오류 계약과 FE/BE 합의 | 0.2 | [#45](https://github.com/likelion-backendschool-9th/REMAKE/issues/45), [#182](https://github.com/likelion-backendschool-9th/REMAKE/issues/182) | — | 신규 추적; #45는 오류 응답 일부만 담당 |
| 0.3 | ERD 전체 스키마·엔티티 구성 | 0.3 | [#49](https://github.com/likelion-backendschool-9th/REMAKE/issues/49), [#60](https://github.com/likelion-backendschool-9th/REMAKE/issues/60), [#69](https://github.com/likelion-backendschool-9th/REMAKE/issues/69), [#146](https://github.com/likelion-backendschool-9th/REMAKE/issues/146), [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67), [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68), [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71), [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75), [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76), [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79), [#82](https://github.com/likelion-backendschool-9th/REMAKE/issues/82), [#86](https://github.com/likelion-backendschool-9th/REMAKE/issues/86), [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89), [#92](https://github.com/likelion-backendschool-9th/REMAKE/issues/92), [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167), [#171](https://github.com/likelion-backendschool-9th/REMAKE/issues/171), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | [#31](https://github.com/likelion-backendschool-9th/REMAKE/pull/31), [#128](https://github.com/likelion-backendschool-9th/REMAKE/pull/128), [#132](https://github.com/likelion-backendschool-9th/REMAKE/pull/132), [#140](https://github.com/likelion-backendschool-9th/REMAKE/pull/140), [#151](https://github.com/likelion-backendschool-9th/REMAKE/pull/151), [#154](https://github.com/likelion-backendschool-9th/REMAKE/pull/154) | 영역별 분산 추적; 전체 26개 적용 완료로 간주하지 않음 |
| 0.4 | 누락·수정 피그마 화면 확정 | 0.4 | [#183](https://github.com/likelion-backendschool-9th/REMAKE/issues/183) | [#162](https://github.com/likelion-backendschool-9th/REMAKE/pull/162), [#163](https://github.com/likelion-backendschool-9th/REMAKE/pull/163) | 신규 추적; 컴포넌트 PR은 누락 제품 화면 확정과 별개 |
| 0.5 | Next.js·라우트·API 클라이언트·타입·목 서버 | 0.5 | [#182](https://github.com/likelion-backendschool-9th/REMAKE/issues/182), [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116) | [#7](https://github.com/likelion-backendschool-9th/REMAKE/pull/7), [#147](https://github.com/likelion-backendschool-9th/REMAKE/pull/147), [#166](https://github.com/likelion-backendschool-9th/REMAKE/pull/166) | 기반 있음; 클라이언트 PR·계약/타입·헤더 작업 연결 |
| 0.6 | 서버·DB·이미지 저장소·배포 환경 | 0.6 | [#49](https://github.com/likelion-backendschool-9th/REMAKE/issues/49), [#64](https://github.com/likelion-backendschool-9th/REMAKE/issues/64), [#136](https://github.com/likelion-backendschool-9th/REMAKE/issues/136), [#157](https://github.com/likelion-backendschool-9th/REMAKE/issues/157) | [#11](https://github.com/likelion-backendschool-9th/REMAKE/pull/11), [#17](https://github.com/likelion-backendschool-9th/REMAKE/pull/17), [#20](https://github.com/likelion-backendschool-9th/REMAKE/pull/20), [#159](https://github.com/likelion-backendschool-9th/REMAKE/pull/159), [#179](https://github.com/likelion-backendschool-9th/REMAKE/pull/179) | 배포 기반 있음; 영수증/공유 저장·운영 키는 잔여 작업 |
| 0.7 | 주간 스프린트 회의·진행 점검 | 0.7 | — | — | 운영 항목; 회의 이슈 없음, 회의록/보드 관리 가능 |
| 0.8 | 기획·개발 문서 재작성 | 0.8 | [#143](https://github.com/likelion-backendschool-9th/REMAKE/issues/143), [#149](https://github.com/likelion-backendschool-9th/REMAKE/issues/149) | [#142](https://github.com/likelion-backendschool-9th/REMAKE/pull/142), [#147](https://github.com/likelion-backendschool-9th/REMAKE/pull/147), [#154](https://github.com/likelion-backendschool-9th/REMAKE/pull/154) | PR #142 병합; 남은 문서 충돌은 #149 |
| 0.9 | 공통 토스트·입력 오류·빈 화면·로딩 | 추가 | [#184](https://github.com/likelion-backendschool-9th/REMAKE/issues/184) | [#162](https://github.com/likelion-backendschool-9th/REMAKE/pull/162), [#163](https://github.com/likelion-backendschool-9th/REMAKE/pull/163), [#166](https://github.com/likelion-backendschool-9th/REMAKE/pull/166) | 신규 추적; 공통 토스트 앱 연결 등 잔여 범위 |
| 1.1 | 소셜 로그인·JWT 쿠키·갱신 | 1.1 | [#66](https://github.com/likelion-backendschool-9th/REMAKE/issues/66), [#136](https://github.com/likelion-backendschool-9th/REMAKE/issues/136), [#171](https://github.com/likelion-backendschool-9th/REMAKE/issues/171) | [#132](https://github.com/likelion-backendschool-9th/REMAKE/pull/132) | 기존 구현 완료 + v4 스키마·운영 키 후속 |
| 1.2 | 온보딩·설정·로그아웃 | 1.3 | [#66](https://github.com/likelion-backendschool-9th/REMAKE/issues/66), [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67) | [#132](https://github.com/likelion-backendschool-9th/REMAKE/pull/132) | 연결됨·구현/검증 진행 대상 |
| 1.3 | 계정 삭제·이메일 인증·인증번호 정리 | 1.3 | [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68) | — | 인증번호 하루 보관 정리를 #68에 추가; #138은 토큰 정리 |
| 1.4 | 인증·온보딩·삭제·구독 서버 이벤트 | 11.2 | [#66](https://github.com/likelion-backendschool-9th/REMAKE/issues/66), [#171](https://github.com/likelion-backendschool-9th/REMAKE/issues/171), [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67), [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68), [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167), [#168](https://github.com/likelion-backendschool-9th/REMAKE/issues/168), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | — | 연결됨·구현/검증 진행 대상 |
| 1.5 | 스플래시·로그인·약관·인증 복귀 | 1.2 | [#95](https://github.com/likelion-backendschool-9th/REMAKE/issues/95) | — | 연결됨·구현/검증 진행 대상 |
| 1.6 | 가구원·알레르기 온보딩 화면 | 1.4 | [#96](https://github.com/likelion-backendschool-9th/REMAKE/issues/96) | — | 연결됨·구현/검증 진행 대상 |
| 1.7 | 설정·구독 관리·토스 결제 화면 | 1.5 | [#97](https://github.com/likelion-backendschool-9th/REMAKE/issues/97), [#169](https://github.com/likelion-backendschool-9th/REMAKE/issues/169), [#170](https://github.com/likelion-backendschool-9th/REMAKE/issues/170) | — | 기존 설정과 구독 FE 분리; 구독 미배정 유지 |
| 1.8 | 계정 삭제 인증 화면 | 1.6 | [#98](https://github.com/likelion-backendschool-9th/REMAKE/issues/98) | — | 연결됨·구현/검증 진행 대상 |
| 1.9 | 구독 API·정기 결제·종료·한도 반영 | 추가 | [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68), [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75), [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79), [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167), [#168](https://github.com/likelion-backendschool-9th/REMAKE/issues/168), [#170](https://github.com/likelion-backendschool-9th/REMAKE/issues/170) | — | API·정기 작업·결정으로 분리, 구독 담당 미정 유지 |
| 2.1 | 냉장고 CRUD·조회·차감·단위 결정 | 2.1 | [#59](https://github.com/likelion-backendschool-9th/REMAKE/issues/59), [#69](https://github.com/likelion-backendschool-9th/REMAKE/issues/69), [#70](https://github.com/likelion-backendschool-9th/REMAKE/issues/70), [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71), [#145](https://github.com/likelion-backendschool-9th/REMAKE/issues/145), [#146](https://github.com/likelion-backendschool-9th/REMAKE/issues/146), [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | [#128](https://github.com/likelion-backendschool-9th/REMAKE/pull/128), [#139](https://github.com/likelion-backendschool-9th/REMAKE/pull/139), [#154](https://github.com/likelion-backendschool-9th/REMAKE/pull/154) | 기존 CRUD·목록 및 단위 결정 완료; #71/#153 후속 남음 |
| 2.2 | 카테고리·단위·제외 재료 기준 데이터 | 2.2 | [#60](https://github.com/likelion-backendschool-9th/REMAKE/issues/60) | [#37](https://github.com/likelion-backendschool-9th/REMAKE/pull/37), [#140](https://github.com/likelion-backendschool-9th/REMAKE/pull/140) | 기준 데이터 병합; 카테고리 v4 차이는 #71 |
| 2.3 | 냉장고 서버 이벤트와 호출 경로 검증 | 11.3 | [#70](https://github.com/likelion-backendschool-9th/REMAKE/issues/70), [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | — | 연결됨·구현/검증 진행 대상 |
| 2.4 | 냉장고 목록·필터·차감 시트·상단 알림 | 2.3, 2.4 | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99), [#100](https://github.com/likelion-backendschool-9th/REMAKE/issues/100), [#110](https://github.com/likelion-backendschool-9th/REMAKE/issues/110) | — | 연결됨·구현/검증 진행 대상 |
| 2.5 | 재료 추가·수정·삭제 화면 | 2.3 | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) | — | 연결됨·구현/검증 진행 대상 |
| 2.6 | 이번 달 버린 식재료 금액 화면 | 추가 | [#185](https://github.com/likelion-backendschool-9th/REMAKE/issues/185) | — | 신규 추적·화면 대기; API 경로 미정 |
| 3.1 | 영수증 스캔·실패 분기·등록 시 사진 저장 | 3.1 | [#49](https://github.com/likelion-backendschool-9th/REMAKE/issues/49), [#72](https://github.com/likelion-backendschool-9th/REMAKE/issues/72) | — | 연결됨·구현/검증 진행 대상 |
| 3.2 | OCR·식품 파싱·분류·기한 추정 | 3.2 | [#72](https://github.com/likelion-backendschool-9th/REMAKE/issues/72) | — | 연결됨·구현/검증 진행 대상 |
| 3.3 | 영수증·냉장고 등록·장바구니 정리 | 3.4 | [#49](https://github.com/likelion-backendschool-9th/REMAKE/issues/49), [#57](https://github.com/likelion-backendschool-9th/REMAKE/issues/57), [#102](https://github.com/likelion-backendschool-9th/REMAKE/issues/102), [#173](https://github.com/likelion-backendschool-9th/REMAKE/issues/173) | — | 등록 이슈 있음; 선택 차감 계약은 #57/#173 미결 |
| 3.4 | 영수증 목록·상세·삭제·보관 정리 | 3.5 | [#61](https://github.com/likelion-backendschool-9th/REMAKE/issues/61), [#62](https://github.com/likelion-backendschool-9th/REMAKE/issues/62) | — | 연결됨·구현/검증 진행 대상 |
| 3.5 | 소비 패턴·일별 품목 조회 | 3.6 | [#73](https://github.com/likelion-backendschool-9th/REMAKE/issues/73) | — | 연결됨·구현/검증 진행 대상 |
| 3.6 | 영수증·OCR 서버 이벤트 | 11.4 | [#49](https://github.com/likelion-backendschool-9th/REMAKE/issues/49), [#72](https://github.com/likelion-backendschool-9th/REMAKE/issues/72), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | — | 연결됨·구현/검증 진행 대상 |
| 3.7 | 촬영·OCR 편집·최종 등록 화면 | 3.3, 3.4 | [#101](https://github.com/likelion-backendschool-9th/REMAKE/issues/101), [#102](https://github.com/likelion-backendschool-9th/REMAKE/issues/102), [#173](https://github.com/likelion-backendschool-9th/REMAKE/issues/173) | — | 품목 빼기는 PR #142에서 제거; 수정·추가·최종 등록 유지 |
| 3.8 | 영수증 보관함·상세·삭제 화면 | 3.5 | [#103](https://github.com/likelion-backendschool-9th/REMAKE/issues/103) | — | 연결됨·구현/검증 진행 대상 |
| 3.9 | 나의 소비 화면 | 3.7 | [#104](https://github.com/likelion-backendschool-9th/REMAKE/issues/104) | — | 연결됨·구현/검증 진행 대상 |
| 4.1 | AI 프롬프트·출력 형식·옵션 검증 | 4.1 | [#74](https://github.com/likelion-backendschool-9th/REMAKE/issues/74) | [#156](https://github.com/likelion-backendschool-9th/REMAKE/pull/156) | AI 포트·어댑터 기반 완료; API 연결은 #75 |
| 4.2 | 레시피 생성·재생성·잔여 횟수 | 4.2 | [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75), [#158](https://github.com/likelion-backendschool-9th/REMAKE/issues/158) | — | 연결됨·구현/검증 진행 대상 |
| 4.3 | 레시피 상세·저장·목록·즐겨찾기 | 4.4 | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76), [#57](https://github.com/likelion-backendschool-9th/REMAKE/issues/57) | — | 연결됨·구현/검증 진행 대상 |
| 4.4 | 요리 미리보기·재료 차감·식단 요리 완료 | 4.5 | [#63](https://github.com/likelion-backendschool-9th/REMAKE/issues/63), [#77](https://github.com/likelion-backendschool-9th/REMAKE/issues/77) | — | 요리 기록 전용 테이블 없음; 차감 로그·이벤트·cooked_at |
| 4.5 | 레시피·차감·한도 서버 이벤트 | 11.5 | [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75), [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76), [#77](https://github.com/likelion-backendschool-9th/REMAKE/issues/77), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | — | 연결됨·구현/검증 진행 대상 |
| 4.6 | 레시피 메인·저장 목록·금주 TOP 3 | 4.3, 4.4 | [#105](https://github.com/likelion-backendschool-9th/REMAKE/issues/105), [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106), [#113](https://github.com/likelion-backendschool-9th/REMAKE/issues/113) | — | 연결됨·구현/검증 진행 대상 |
| 4.7 | 커스텀·생성 결과·로딩·실패·한도 화면 | 4.3 | [#105](https://github.com/likelion-backendschool-9th/REMAKE/issues/105), [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) | — | 연결됨·구현/검증 진행 대상 |
| 4.8 | 레시피 상세·음성 읽기·요리 차감 화면 | 4.4, 4.5 | [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106), [#107](https://github.com/likelion-backendschool-9th/REMAKE/issues/107) | — | 연결됨·구현/검증 진행 대상 |
| 4.9 | 요리 후 추천·비추천 피드백 | 4.5 | [#53](https://github.com/likelion-backendschool-9th/REMAKE/issues/53), [#107](https://github.com/likelion-backendschool-9th/REMAKE/issues/107), [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116) | [#142](https://github.com/likelion-backendschool-9th/REMAKE/pull/142) | 위치는 냉장고 바텀 시트로 확정; E50 우선순위만 미결 |
| 5.1 | 식단 재료 배정·중복 방지·부분 생성 설계 | 5.1 | [#78](https://github.com/likelion-backendschool-9th/REMAKE/issues/78), [#158](https://github.com/likelion-backendschool-9th/REMAKE/issues/158) | [#161](https://github.com/likelion-backendschool-9th/REMAKE/pull/161), [#181](https://github.com/likelion-backendschool-9th/REMAKE/pull/181) | PR #181 정책 문서 병합; PR #161 도메인 구현은 아직 OPEN |
| 5.2 | 식단 생성 접수·작업·달력·일별 조회 | 5.2 | [#78](https://github.com/likelion-backendschool-9th/REMAKE/issues/78), [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79), [#80](https://github.com/likelion-backendschool-9th/REMAKE/issues/80), [#144](https://github.com/likelion-backendschool-9th/REMAKE/issues/144) | — | 202 접수·결과 알림; 달력/겹침의 상태 포함 #144 |
| 5.3 | 식단 칸 삭제·레시피 교체 | 추가 | [#175](https://github.com/likelion-backendschool-9th/REMAKE/issues/175), [#176](https://github.com/likelion-backendschool-9th/REMAKE/issues/176) | — | 기존 빈 끼니 추가를 대체하는 신규 삭제/교체 이슈 |
| 5.4 | 식단 생성·삭제·교체 서버 이벤트 | 11.6 | [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79), [#175](https://github.com/likelion-backendschool-9th/REMAKE/issues/175), [#176](https://github.com/likelion-backendschool-9th/REMAKE/issues/176), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | — | 연결됨·구현/검증 진행 대상 |
| 5.5 | 식단 달력·일별 시트·삭제·교체 화면 | 5.3 | [#108](https://github.com/likelion-backendschool-9th/REMAKE/issues/108), [#177](https://github.com/likelion-backendschool-9th/REMAKE/issues/177) | — | 연결됨·구현/검증 진행 대상 |
| 5.6 | 식단 생성 설정·겹침·비동기 안내 | 5.3 | [#108](https://github.com/likelion-backendschool-9th/REMAKE/issues/108), [#115](https://github.com/likelion-backendschool-9th/REMAKE/issues/115), [#144](https://github.com/likelion-backendschool-9th/REMAKE/issues/144) | — | 생성 대기·실패 화면 폐기; 겹침·한도·결과 알림 유지 |
| 6.1 | 앱 안 알림 배치·목록·읽음·보관 정리 | 6.1 | [#44](https://github.com/likelion-backendschool-9th/REMAKE/issues/44), [#82](https://github.com/likelion-backendschool-9th/REMAKE/issues/82) | [#51](https://github.com/likelion-backendschool-9th/REMAKE/pull/51), [#178](https://github.com/likelion-backendschool-9th/REMAKE/pull/178) | PR #178 병합으로 #82 완료; 식단 결과 알림 생성은 #79 |
| 6.2 | 소비기한·알림 서버 이벤트 | 11.7 | [#82](https://github.com/likelion-backendschool-9th/REMAKE/issues/82), [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | — | 연결됨·구현/검증 진행 대상 |
| 6.3 | 알림 목록·상단 띠·결과 이동 | 6.3 | [#110](https://github.com/likelion-backendschool-9th/REMAKE/issues/110), [#115](https://github.com/likelion-backendschool-9th/REMAKE/issues/115) | — | 연결됨·구현/검증 진행 대상 |
| 7.1 | 장바구니 조회·수정·삭제·냉장고 이동 | 6.5 | [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89) | — | 연결됨·구현/검증 진행 대상 |
| 7.2 | 세 경로의 부족 재료 자동 담기 | 5.5 | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76), [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79), [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89), [#90](https://github.com/likelion-backendschool-9th/REMAKE/issues/90), [#91](https://github.com/likelion-backendschool-9th/REMAKE/issues/91), [#115](https://github.com/likelion-backendschool-9th/REMAKE/issues/115), [#57](https://github.com/likelion-backendschool-9th/REMAKE/issues/57) | — | 세 경로 분산 구현; 취소·영수증 정리·칸 FK는 #57 |
| 7.3 | 익명 공유·랭킹·저장·신고 | 7.1 | [#64](https://github.com/likelion-backendschool-9th/REMAKE/issues/64), [#86](https://github.com/likelion-backendschool-9th/REMAKE/issues/86), [#88](https://github.com/likelion-backendschool-9th/REMAKE/issues/88), [#90](https://github.com/likelion-backendschool-9th/REMAKE/issues/90), [#174](https://github.com/likelion-backendschool-9th/REMAKE/issues/174) | [#155](https://github.com/likelion-backendschool-9th/REMAKE/pull/155) | 연결됨·구현/검증 진행 대상 |
| 7.4 | 주간 순위 기록·보상 지급 | 7.3 | [#92](https://github.com/likelion-backendschool-9th/REMAKE/issues/92) | — | 연결됨·구현/검증 진행 대상 |
| 7.5 | 공유·장바구니·보상 서버 이벤트 | 11.8 | [#86](https://github.com/likelion-backendschool-9th/REMAKE/issues/86), [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89), [#90](https://github.com/likelion-backendschool-9th/REMAKE/issues/90), [#91](https://github.com/likelion-backendschool-9th/REMAKE/issues/91), [#92](https://github.com/likelion-backendschool-9th/REMAKE/issues/92), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | — | 연결됨·구현/검증 진행 대상 |
| 7.6 | 장바구니 화면 | 6.6 | [#114](https://github.com/likelion-backendschool-9th/REMAKE/issues/114) | — | 연결됨·구현/검증 진행 대상 |
| 7.7 | 공유 작성·인기 목록·상세·신고 화면 | 7.2 | [#112](https://github.com/likelion-backendschool-9th/REMAKE/issues/112), [#113](https://github.com/likelion-backendschool-9th/REMAKE/issues/113) | — | 연결됨·구현/검증 진행 대상 |
| 8.1 | 이벤트 테이블·수집·공통 헤더 | 0.9 | [#65](https://github.com/likelion-backendschool-9th/REMAKE/issues/65), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | [#126](https://github.com/likelion-backendschool-9th/REMAKE/pull/126) | 기반 #65 완료; v2 카탈로그·검증기·보관은 #172 |
| 8.2 | EventTracker·개인정보 필터·발행 가이드 | 11.1 | [#65](https://github.com/likelion-backendschool-9th/REMAKE/issues/65), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | [#126](https://github.com/likelion-backendschool-9th/REMAKE/pull/126) | 기반 완료; 정의서 변경 반영은 #172 |
| 8.3 | 화면 이벤트 발행 | 추가 | [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116) | — | FE 트래커와 화면별 발행; 45 CLIENT 기준 |
| 8.4 | 지표 21개 조회 SQL | 8.2 | [#186](https://github.com/likelion-backendschool-9th/REMAKE/issues/186) | — | 신규 P0; 어드민 보류에 포함하면 안 됨 |
| 8.5 | 시드·데모 계정·이벤트·시연 시나리오 | 10.5 | [#187](https://github.com/likelion-backendschool-9th/REMAKE/issues/187) | — | 신규 P1; 기준 데이터 #60과 다른 데모 데이터 |
| 9.1 | PWA manifest·서비스 워커 | 9.1 | [#188](https://github.com/likelion-backendschool-9th/REMAKE/issues/188) | [#7](https://github.com/likelion-backendschool-9th/REMAKE/pull/7), [#15](https://github.com/likelion-backendschool-9th/REMAKE/pull/15) | 기반 있음; 아이콘 TODO 등 잔여 범위 신규 추적 |
| 9.2 | 구글 플레이 계정·비공개 테스터 모집 | 9.2 | [#189](https://github.com/likelion-backendschool-9th/REMAKE/issues/189) | — | 신규 P0; 콘솔에서 계정별 실제 요건 확인 |
| 9.3 | TWA 패키징·비공개 테스트 등록 | 9.3 | [#190](https://github.com/likelion-backendschool-9th/REMAKE/issues/190) | — | 신규 P0; Web Push 삭제와 무관하게 유지 |
| 9.4 | 프로덕션 배포·서비스 확인 | 9.4 | [#136](https://github.com/likelion-backendschool-9th/REMAKE/issues/136), [#157](https://github.com/likelion-backendschool-9th/REMAKE/issues/157), [#192](https://github.com/likelion-backendschool-9th/REMAKE/issues/192) | [#11](https://github.com/likelion-backendschool-9th/REMAKE/pull/11), [#20](https://github.com/likelion-backendschool-9th/REMAKE/pull/20), [#159](https://github.com/likelion-backendschool-9th/REMAKE/pull/159), [#160](https://github.com/likelion-backendschool-9th/REMAKE/pull/160), [#179](https://github.com/likelion-backendschool-9th/REMAKE/pull/179) | 배포 기반 병합; 운영 기능 확인은 키·통합 QA와 연결 |
| 10.1 | 화면별 테스트 시나리오 | 10.1 | [#191](https://github.com/likelion-backendschool-9th/REMAKE/issues/191) | — | 신규 추적; 기능별 개발 체크와 별도 인수 작업 |
| 10.2 | 통합 테스트·결함 수정 | 10.2 | [#192](https://github.com/likelion-backendschool-9th/REMAKE/issues/192) | — | 신규 추적; 기능별 개발 체크와 별도 인수 작업 |
| 10.3 | 모바일 실기기·접근성 점검 | 10.3 | [#193](https://github.com/likelion-backendschool-9th/REMAKE/issues/193) | — | 신규 추적; 기능별 개발 체크와 별도 인수 작업 |
| 10.4 | 한도·권한·오류 코드 점검 | 10.4 | [#194](https://github.com/likelion-backendschool-9th/REMAKE/issues/194) | — | 신규 추적; 기능별 개발 체크와 별도 인수 작업 |
| 10.5 | README·포트폴리오 정리 | 10.6 | [#195](https://github.com/likelion-backendschool-9th/REMAKE/issues/195) | — | 신규 추적; 기능별 개발 체크와 별도 인수 작업 |
| 10.6 | 이벤트 누락·중복·속성 검증 | 10.7 | [#196](https://github.com/likelion-backendschool-9th/REMAKE/issues/196) | — | 신규 추적; 기능별 개발 체크와 별도 인수 작업 |
| 11.1 | 어드민 권한 제한 | 8.1 | [#84](https://github.com/likelion-backendschool-9th/REMAKE/issues/84) | — | 보류 유지; 삭제·구현 완료 아님 |
| 11.2 | 어드민 분석 API·화면 | 8.2, 8.3 | [#93](https://github.com/likelion-backendschool-9th/REMAKE/issues/93), [#117](https://github.com/likelion-backendschool-9th/REMAKE/issues/117) | — | 보류 유지; 삭제·구현 완료 아님 |
| 11.3 | 신고·회원·호출·감사 운영 관리 | 8.4 | [#85](https://github.com/likelion-backendschool-9th/REMAKE/issues/85), [#87](https://github.com/likelion-backendschool-9th/REMAKE/issues/87), [#94](https://github.com/likelion-backendschool-9th/REMAKE/issues/94) | — | 보류 유지; 삭제·구현 완료 아님 |

## 처음 열린 74개 이슈의 처리 이력

최초 백업의 열린 이슈 74개를 전부 현재 이슈와 대조했다. 이 중 4개는 범위 제외, 2개는 옛 결정이 해소돼 종료, 4개는 이후 팀 작업으로 완료, 64개는 계속 열려 있다(어드민 보류 6개 포함). 기존 추가 11개와 이번 추가 15개를 더해 현재 열린 이슈는 90개다. 최초 백업 이전부터 닫혀 있던 이슈 18개까지 포함하면 전체 118개 중 28개가 닫혀 있다. 이 정리에서 삭제한 GitHub 이슈는 없다.

### 범위에서 뺀 기능 4개

| 이슈 | 원래 기능 | 종료 이유·대체 |
| --- | --- | --- |
| [#81](https://github.com/likelion-backendschool-9th/REMAKE/issues/81) | 빈 끼니 수동 추가 API | v4에서 폐기; 삭제·교체 [#175](https://github.com/likelion-backendschool-9th/REMAKE/issues/175), [#176](https://github.com/likelion-backendschool-9th/REMAKE/issues/176) |
| [#109](https://github.com/likelion-backendschool-9th/REMAKE/issues/109) | 빈 끼니 수동 추가 FE | v4에서 폐기; ⋮ 삭제·교체 [#177](https://github.com/likelion-backendschool-9th/REMAKE/issues/177) |
| [#83](https://github.com/likelion-backendschool-9th/REMAKE/issues/83) | Web Push 구독·발송 BE | 앱 안 알림만 유지 [#82](https://github.com/likelion-backendschool-9th/REMAKE/issues/82), [#165](https://github.com/likelion-backendschool-9th/REMAKE/issues/165), 배포 [#179](https://github.com/likelion-backendschool-9th/REMAKE/pull/179) |
| [#111](https://github.com/likelion-backendschool-9th/REMAKE/issues/111) | Web Push 권한·수신·iOS 푸시 검증 | 푸시만 제외; 앱 안 알림 [#110](https://github.com/likelion-backendschool-9th/REMAKE/issues/110), PWA·TWA [#188](https://github.com/likelion-backendschool-9th/REMAKE/issues/188), [#190](https://github.com/likelion-backendschool-9th/REMAKE/issues/190), 일반 실기기 [#193](https://github.com/likelion-backendschool-9th/REMAKE/issues/193) 유지 |

위 4개는 CLOSED / not_planned다. WBS 6장의 삭제 4행과 GitHub 이슈 4개는 일대일 관계가 아니다. WBS의 재구매 항목(이전 2.5)은 처음부터 제외라 대응 이슈가 없었고, 빈 끼니 추가는 BE/FE 2개, 푸시와 iOS 푸시 확인은 #83/#111로 나뉘었다.

### 옛 질문을 종료한 2개와 보류한 6개

| 이슈 | 처리·남은 추적 |
| --- | --- |
| [#55](https://github.com/likelion-backendschool-9th/REMAKE/issues/55) | CLOSED / completed: 칸 상세 지연 생성 자체 폐기. 교체의 일일 차감은 #176, 공통 한도는 #75 |
| [#56](https://github.com/likelion-backendschool-9th/REMAKE/issues/56) | CLOSED / completed: 빈 칸 추가·동기 생성 표시 질문 폐기. 비동기 접수 #79, FE #108/#115, 상태 미결 #144 유지 |
| [#84](https://github.com/likelion-backendschool-9th/REMAKE/issues/84) | OPEN + blocked: ADMIN 접근 제한, WBS 11.1 |
| [#93](https://github.com/likelion-backendschool-9th/REMAKE/issues/93), [#117](https://github.com/likelion-backendschool-9th/REMAKE/issues/117) | OPEN + blocked: 어드민 분석 API·FE, WBS 11.2. 지표 SQL #186은 계속 P0 |
| [#85](https://github.com/likelion-backendschool-9th/REMAKE/issues/85), [#87](https://github.com/likelion-backendschool-9th/REMAKE/issues/87), [#94](https://github.com/likelion-backendschool-9th/REMAKE/issues/94) | OPEN + blocked: 사용자·신고·호출·감사 관리, WBS 11.3. 사용자 신고 #86·주간 보상 #92는 유지 |

`completed`는 #55/#56의 결정 정리가 끝났다는 뜻이다. 예전 기능을 구현했다는 뜻이 아니다. 어드민은 화면·범위 확정 후 정의서 갱신 → 이슈 범위 재검토 → blocked 해제로 재개한다.

### 74개 개별 기록

| 이슈 | 최초 제목 | 현재 제목 | 처리 | WBS v2 |
| --- | --- | --- | --- | --- |
| [#45](https://github.com/likelion-backendschool-9th/REMAKE/issues/45) | [feat] 에러 응답에 details 필드 추가 확인 | [feat] 에러 응답의 details 필드와 v4 오류 전달 구현 | 유지·열림 | 0.2, 0.9 |
| [#49](https://github.com/likelion-backendschool-9th/REMAKE/issues/49) | [feat] 영수증 최종 등록 API 구현 | [feat] 영수증 최종 등록 API 구현 | 유지·열림 | 0.3, 3.1, 3.3, 3.6 |
| [#50](https://github.com/likelion-backendschool-9th/REMAKE/issues/50) | [decision] AI 생성 60초 제한과 프록시 60초 제한 맞추기 | [decision] 동기 레시피 60초와 프록시 제한의 충돌 해소 | 유지·열림 | 0.5, 4.2, 5.2 |
| [#53](https://github.com/likelion-backendschool-9th/REMAKE/issues/53) | [decision] 레시피 단계 재생과 요리 피드백 동작 확정 | [decision] 요리 추천 피드백 E50의 우선순위 확정 | 유지·열림 | 4.9 |
| [#55](https://github.com/likelion-backendschool-9th/REMAKE/issues/55) | [decision] 식단표 칸 상세 생성의 횟수 차감 정책 확정 | [decision] 식단표 칸 상세 생성의 횟수 차감 정책 확정 | 옛 결정 종료 | — |
| [#56](https://github.com/likelion-backendschool-9th/REMAKE/issues/56) | [decision] 식단 추가 방식과 생성 중 표시 확정 | [decision] 식단 추가 방식과 생성 중 표시 확정 | 옛 결정 종료 | — |
| [#57](https://github.com/likelion-backendschool-9th/REMAKE/issues/57) | [decision] 장바구니 재저장·합산·재료 등록 후 정리 정책 확정 | [decision] 장바구니 정책·칸별 정리 키의 v4 문서와 댓글 충돌 해소 | 유지·열림 | 3.3, 4.3, 7.2, 7.1 |
| [#61](https://github.com/likelion-backendschool-9th/REMAKE/issues/61) | [feat] 영수증 목록·상세·삭제 API 구현 | [feat] 영수증 목록·상세·삭제 API 구현 | 유지·열림 | 3.4 |
| [#62](https://github.com/likelion-backendschool-9th/REMAKE/issues/62) | [feat] 영수증 6개월 보관 정리와 구매일 경계 검증 | [feat] 영수증 6개월 보관 정리와 구매일 경계 검증 | 유지·열림 | 3.4 |
| [#63](https://github.com/likelion-backendschool-9th/REMAKE/issues/63) | [feat] 단위 코드 검증과 요리 재료 환산 구현 | [feat] 단위 코드 검증과 요리 재료 환산 구현 | 유지·열림 | 4.4 |
| [#64](https://github.com/likelion-backendschool-9th/REMAKE/issues/64) | [feat] 공유 사진 저장과 필수·형식·용량 검증 구현 | [feat] 공유 사진 저장과 필수·형식·용량 검증 구현 | 유지·열림 | 0.6, 7.3 |
| [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67) | [feat] 온보딩·사용자 설정 API 구현 | [feat] 온보딩·사용자 설정 API 구현 | 유지·열림 | 0.3, 1.2, 1.4 |
| [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68) | [feat] 인증번호 기반 계정 삭제 API 구현 | [feat] 이메일 인증·네이버 재인증으로 계정 삭제 API 구현 | 유지·열림 | 0.3, 1.3, 1.4, 1.9 |
| [#70](https://github.com/likelion-backendschool-9th/REMAKE/issues/70) | [feat] 재료 사용·폐기와 잔여 수량·폐기 금액 기록 구현 | [feat] 재료 사용·폐기와 잔여 수량·폐기 금액 기록 구현 | 유지·열림 | 2.1, 2.3 |
| [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71) | [feat] 재료 동시 수정 충돌과 기록 있는 재료 삭제 제약 구현 | [fix] 냉장고 스키마·수정·삭제 규칙을 v4에 맞추기 | 유지·열림 | 0.3, 2.1 |
| [#72](https://github.com/likelion-backendschool-9th/REMAKE/issues/72) | [feat] 영수증 스캔 API와 OCR 품목 정규화 구현 | [feat] 영수증 스캔 API와 OCR 품목 정규화 구현 | 유지·열림 | 3.1, 3.2, 3.6 |
| [#73](https://github.com/likelion-backendschool-9th/REMAKE/issues/73) | [feat] 월별 식비·소비 패턴·일별 품목 집계 API 구현 | [feat] 월별 소비 패턴·일별 품목 조회 API 구현 | 유지·열림 | 3.5 |
| [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75) | [feat] 빠른·커스텀 레시피 생성과 횟수 제어 구현 | [feat] 빠른·커스텀 레시피 생성과 횟수 제어 구현 | 유지·열림 | 0.3, 1.9, 4.2, 4.5 |
| [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76) | [feat] 레시피 상세·저장·목록·즐겨찾기 API 구현 | [feat] 레시피 상세·저장·목록·즐겨찾기 API 구현 | 유지·열림 | 0.3, 4.3, 4.5, 7.2 |
| [#77](https://github.com/likelion-backendschool-9th/REMAKE/issues/77) | [feat] 요리 재료 차감 미리보기·확정과 요리 기록 구현 | [feat] 요리 재료 차감 미리보기·확정과 식단 요리 완료 구현 | 유지·열림 | 4.4, 4.5 |
| [#78](https://github.com/likelion-backendschool-9th/REMAKE/issues/78) | [feat] 식단표 재료 중복 배정 방지와 부분 생성 구현 | [feat] 식단표 재료 중복 배정 방지와 부분 생성 구현 | 유지·열림 | 5.1, 5.2 |
| [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79) | [feat] 식단 목적·끼니·인원과 월 생성 한도 적용 | [feat] 식단표 비동기 생성 접수·완료·실패와 월 한도 구현 | 유지·열림 | 0.3, 1.9, 5.2, 5.4, 6.2, 7.2 |
| [#80](https://github.com/likelion-backendschool-9th/REMAKE/issues/80) | [feat] 식단표 월별·일별 조회와 칸 상세 레시피 생성 구현 | [feat] 식단표 월별 달력·일별 끼니 조회 API 구현 | 유지·열림 | 5.2 |
| [#81](https://github.com/likelion-backendschool-9th/REMAKE/issues/81) | [feat] 빈 끼니에 저장 레시피 추가 API 구현 | [feat] 빈 끼니에 저장 레시피 추가 API 구현 | 범위 제외 | — |
| [#82](https://github.com/likelion-backendschool-9th/REMAKE/issues/82) | [feat] 소비기한 알림 배치와 목록·요약·읽음 API 구현 | [feat] 소비기한 알림 배치와 목록·요약·읽음 API 구현 | 팀 완료: PR #178 | 0.3, 6.1, 6.2 |
| [#83](https://github.com/likelion-backendschool-9th/REMAKE/issues/83) | [feat] 웹 푸시 구독·발송과 실패 구독 정리 구현 | [feat] 웹 푸시 구독·발송과 실패 구독 정리 구현 | 범위 제외 | — |
| [#84](https://github.com/likelion-backendschool-9th/REMAKE/issues/84) | [feat] ADMIN 접근 제어와 일반 사용자 403 검증 | [feat] 보류: ADMIN 접근 제어와 일반 사용자 403 검증 | 보류·열림 | 11.1 |
| [#85](https://github.com/likelion-backendschool-9th/REMAKE/issues/85) | [feat] 사용자 상태·횟수 조정과 사유·감사 로그 구현 | [feat] 보류: 사용자 상태·횟수 조정과 사유·감사 로그 구현 | 보류·열림 | 11.3 |
| [#86](https://github.com/likelion-backendschool-9th/REMAKE/issues/86) | [feat] 익명 레시피 공유 게시·상세·신고 API 구현 | [feat] 익명 레시피 공유 게시·상세·신고 API 구현 | 유지·열림 | 0.3, 7.3, 7.5 |
| [#87](https://github.com/likelion-backendschool-9th/REMAKE/issues/87) | [feat] 신고 처리와 공유 레시피 숨김·삭제 상태 연동 | [feat] 보류: 신고 처리와 공유 레시피 숨김·삭제 상태 연동 | 보류·열림 | 11.3 |
| [#88](https://github.com/likelion-backendschool-9th/REMAKE/issues/88) | [feat] 이번 주 랭킹·TOP 3·지난주 대비 순위 조회 구현 | [feat] 이번 주 랭킹·TOP 3·지난주 대비 순위 조회 구현 | 유지·열림 | 7.3 |
| [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89) | [feat] 장바구니 공통 담기·조회·수량 변경·선택 삭제 구현 | [feat] 장바구니 조회·수량 변경·삭제·냉장고 이동 API 구현 | 유지·열림 | 0.3, 7.1, 7.2, 7.5 |
| [#90](https://github.com/likelion-backendschool-9th/REMAKE/issues/90) | [feat] 공유 레시피 저장 사본 생성과 부족 재료 담기 구현 | [feat] 공유 원본 레시피 저장·취소와 부족 재료 담기 구현 | 유지·열림 | 7.2, 7.3, 7.5 |
| [#91](https://github.com/likelion-backendschool-9th/REMAKE/issues/91) | [feat] 식단표 부족 재료 담기와 덮어쓰기 시 항목 정리 구현 | [feat] 식단표 부족 재료 담기와 덮어쓰기 시 항목 정리 구현 | 유지·열림 | 7.2, 7.5 |
| [#92](https://github.com/likelion-backendschool-9th/REMAKE/issues/92) | [feat] 주간 순위 확정·보상 지급·재실행 안전성 구현 | [feat] 주간 순위 확정·보상 지급·재실행 안전성 구현 | 유지·열림 | 0.3, 7.4, 7.5 |
| [#93](https://github.com/likelion-backendschool-9th/REMAKE/issues/93) | [feat] KPI·퍼널·이벤트 로그 집계 API 구현 | [feat] 보류: KPI·퍼널·이벤트 로그 집계 API 구현 | 보류·열림 | 11.2 |
| [#94](https://github.com/likelion-backendschool-9th/REMAKE/issues/94) | [feat] 호출 현황·감사 로그·주간 보상 조회 API 구현 | [feat] 보류: 호출 현황·감사 로그·주간 보상 조회 API 구현 | 보류·열림 | 11.3 |
| [#95](https://github.com/likelion-backendschool-9th/REMAKE/issues/95) | [feat] 스플래시·로그인과 비로그인 접근 제어 구현 | [feat] 스플래시·로그인과 비로그인 접근 제어 구현 | 유지·열림 | 1.5 |
| [#96](https://github.com/likelion-backendschool-9th/REMAKE/issues/96) | [feat] 온보딩 건너뛰기와 완료 흐름 구현 | [feat] 가구원·알레르기 온보딩 일괄 저장 흐름 구현 | 유지·열림 | 1.6 |
| [#97](https://github.com/likelion-backendschool-9th/REMAKE/issues/97) | [feat] 가구원·제외 재료·알림·재료 보충 설정 화면 구현 | [feat] 가구원·제외 재료·알림·재료 보충 설정 화면 구현 | 유지·열림 | 1.7 |
| [#98](https://github.com/likelion-backendschool-9th/REMAKE/issues/98) | [feat] 계정 삭제 인증번호·타이머·재전송·잠금 화면 구현 | [feat] 계정 삭제 이메일 인증·네이버 재인증 화면 연결 | 유지·열림 | 1.8 |
| [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) | [feat] 냉장고 목록·필터·재료 추가·수정 화면 구현 | [feat] 냉장고 목록·필터·재료 추가·수정 화면 구현 | 유지·열림 | 2.4, 2.5 |
| [#100](https://github.com/likelion-backendschool-9th/REMAKE/issues/100) | [feat] 재료 사용·폐기 시트와 소비기한 상태 표시 구현 | [feat] 재료 사용·폐기 시트와 소비기한 상태 표시 구현 | 유지·열림 | 2.4 |
| [#101](https://github.com/likelion-backendschool-9th/REMAKE/issues/101) | [feat] 영수증 촬영·업로드와 OCR 결과 편집 화면 구현 | [feat] 영수증 촬영·업로드와 OCR 결과 편집 화면 구현 | 유지·열림 | 3.7 |
| [#102](https://github.com/likelion-backendschool-9th/REMAKE/issues/102) | [feat] 영수증 최종 등록 요청과 냉장고 반영 흐름 연결 | [feat] 영수증 최종 등록 요청과 냉장고 반영 흐름 연결 | 유지·열림 | 3.3, 3.7 |
| [#103](https://github.com/likelion-backendschool-9th/REMAKE/issues/103) | [feat] 영수증 보관함·월 선택·상세·삭제 화면 구현 | [feat] 영수증 보관함·월 선택·상세·삭제 화면 구현 | 유지·열림 | 3.8 |
| [#104](https://github.com/likelion-backendschool-9th/REMAKE/issues/104) | [feat] 월별 소비 패턴과 일별 품목 화면 구현 | [feat] 월별 소비 패턴과 일별 품목 화면 구현 | 유지·열림 | 3.9 |
| [#105](https://github.com/likelion-backendschool-9th/REMAKE/issues/105) | [feat] 레시피 생성 설정·로딩·실패 화면 구현 | [feat] 레시피 생성 설정·로딩·실패 화면 구현 | 유지·열림 | 4.6, 4.7 |
| [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) | [feat] 레시피 상세·저장 목록·즐겨찾기 화면 구현 | [feat] 레시피 상세·저장 목록·즐겨찾기 화면 구현 | 유지·열림 | 4.6, 4.7, 4.8 |
| [#107](https://github.com/likelion-backendschool-9th/REMAKE/issues/107) | [feat] 요리 사용 재료 선택·수량 변경·차감 확인 화면 구현 | [feat] 요리 사용 재료 선택·수량 변경·차감 확인 화면 구현 | 유지·열림 | 4.8, 4.9 |
| [#108](https://github.com/likelion-backendschool-9th/REMAKE/issues/108) | [feat] 식단표 생성 설정·달력·상세·부족 안내 화면 구현 | [feat] 식단표 생성 접수·월 달력·일별 식단 화면 연결 | 유지·열림 | 5.5, 5.6 |
| [#109](https://github.com/likelion-backendschool-9th/REMAKE/issues/109) | [feat] 빈 끼니에 저장 레시피 추가 화면 구현 | [feat] 빈 끼니에 저장 레시피 추가 화면 구현 | 범위 제외 | — |
| [#110](https://github.com/likelion-backendschool-9th/REMAKE/issues/110) | [feat] 알림 목록·상단 알림 띠·읽음·이동 구현 | [feat] 알림 목록·상단 알림 띠·읽음·이동 구현 | 유지·열림 | 2.4, 6.3 |
| [#111](https://github.com/likelion-backendschool-9th/REMAKE/issues/111) | [feat] 웹 푸시 권한·수신·설치 안내와 실기기 검증 | [feat] 웹 푸시 권한·수신·설치 안내와 실기기 검증 | 범위 제외 | — |
| [#112](https://github.com/likelion-backendschool-9th/REMAKE/issues/112) | [feat] 공유 작성·상세·저장·신고 화면 구현 | [feat] 공유 작성·상세·저장·신고 화면 구현 | 유지·열림 | 7.7 |
| [#113](https://github.com/likelion-backendschool-9th/REMAKE/issues/113) | [feat] 금주 TOP 3·랭킹·검색·정렬 화면 구현 | [feat] 금주 TOP 3·랭킹·검색·정렬 화면 구현 | 유지·열림 | 4.6, 7.7 |
| [#114](https://github.com/likelion-backendschool-9th/REMAKE/issues/114) | [feat] 장바구니 목록·수량 변경·선택 삭제 화면 구현 | [feat] 장바구니 조회·수량 변경·삭제·냉장고 이동 화면 구현 | 유지·열림 | 7.6 |
| [#115](https://github.com/likelion-backendschool-9th/REMAKE/issues/115) | [feat] 식단표 부족 안내와 장바구니 추가 결과 연결 | [feat] 식단표 부분 생성 안내를 완료 알림·장바구니에 연결 | 유지·열림 | 5.6, 6.3, 7.2 |
| [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116) | [feat] 프론트 공통 이벤트 트래커 구현 | [feat] 프론트 공통 이벤트 트래커 구현 | 유지·열림 | 4.9, 8.3 |
| [#117](https://github.com/likelion-backendschool-9th/REMAKE/issues/117) | [feat] 어드민 분석 대시보드 화면 구현 | [feat] 보류: 어드민 분석 대시보드 화면 구현 | 보류·열림 | 11.2 |
| [#122](https://github.com/likelion-backendschool-9th/REMAKE/issues/122) | [decision] 프론트·백엔드·프록시 요청 타임아웃 최종 값 정하기 | [decision] 요청 타임아웃 적용 순서와 식단 서버 작업 제한 확정 | 유지·열림 | 0.5, 4.2, 5.2 |
| [#136](https://github.com/likelion-backendschool-9th/REMAKE/issues/136) | [chore] #132 머지 전 서버 재료 건수 확인과 JWT·소셜 키 입력 | [chore] 운영 서버 소셜 로그인 키 6개 입력 확인 | 유지·열림 | 0.6, 1.1, 9.4 |
| [#137](https://github.com/likelion-backendschool-9th/REMAKE/issues/137) | [chore] 테스트 로그인 필터를 src/test로 옮기기 | [chore] 테스트 로그인 필터를 src/test로 옮기기 | 유지·열림 | — |
| [#138](https://github.com/likelion-backendschool-9th/REMAKE/issues/138) | [feat] 만료·폐기된 리프레시 토큰 정리 스케줄러 | [feat] 만료 리프레시 토큰 정리 스케줄러 구현 | 유지·열림 | — |
| [#143](https://github.com/likelion-backendschool-9th/REMAKE/issues/143) | [docs] 피그마 v4 문서 기준으로 열린 이슈 정리 | [docs] 피그마 v4 문서 기준으로 열린 이슈 정리 | 유지·열림 | 0.8 |
| [#144](https://github.com/likelion-backendschool-9th/REMAKE/issues/144) | [decision] 식단표 생성 중·실패 상태의 달력·겹침 포함, E50 우선순위, 식단표 작업 시간 제한 | [decision] 식단표 생성 중·실패 상태의 달력·겹침 포함 확정 | 유지·열림 | 5.2, 5.6 |
| [#145](https://github.com/likelion-backendschool-9th/REMAKE/issues/145) | [decision] 재료 사전에 없는 이름의 단위 정하기 | [decision] 재료 단위 결정 순서·영수증 수량 예외 정리 | 팀 완료: PR #154 | 2.1 |
| [#146](https://github.com/likelion-backendschool-9th/REMAKE/issues/146) | [feat] 재료명으로 단위를 정하는 규칙 구현 | [feat] 재료명으로 단위를 정하는 규칙 구현 | 팀 완료: PR #154 | 0.3, 2.1 |
| [#149](https://github.com/likelion-backendschool-9th/REMAKE/issues/149) | [fix] 기획 v4와 어긋난 개발 문서 문구 정리 | [fix] 기획 v4와 어긋난 개발 문서 문구 정리 | 유지·열림 | 0.8 |
| [#150](https://github.com/likelion-backendschool-9th/REMAKE/issues/150) | [decision] 문서끼리 다른 오류 색·구독 폴더·수량 소수 기준 정하기 | [decision] 오류 색·간격·구독 폴더의 미결 기준 확정 | 유지·열림 | 0.4, 0.9, 1.9 |
| [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | [feat] 재료 추가·수정이 서버가 정한 단위를 쓰도록 변경 | [feat] 재료 추가·수정이 서버가 정한 단위를 쓰도록 변경 | 유지·열림 | 2.1, 2.3 |
| [#157](https://github.com/likelion-backendschool-9th/REMAKE/issues/157) | [chore] 운영 서버에 Gemini API 키·모델(gemini-3.5-flash-lite) 설정 추가 | [chore] 운영 서버에 Gemini API 키·모델(gemini-3.5-flash-lite) 설정 추가 | 팀 완료: PR #160 | 0.6, 9.4 |
| [#158](https://github.com/likelion-backendschool-9th/REMAKE/issues/158) | [decision] 냉장고에 없는 기본 양념을 기능 공통으로 가진 재료로 볼지 정하기 | [decision] 냉장고에 없는 기본 양념을 기능 공통으로 가진 재료로 볼지 정하기 | 유지·열림 | 4.2, 5.1, 7.2 |
| [#165](https://github.com/likelion-backendschool-9th/REMAKE/issues/165) | [fix] 프론트에 남은 Web Push 코드와 VAPID 환경 변수 제거 | [fix] 프론트에 남은 Web Push 코드와 VAPID 환경 변수 제거 | 유지·열림 | 9.1 |

### 앞서 추가한 11개와 기존 완료 이슈 18개

| 이슈 | 범위·후속 |
| --- | --- |
| [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167), [#168](https://github.com/likelion-backendschool-9th/REMAKE/issues/168), [#169](https://github.com/likelion-backendschool-9th/REMAKE/issues/169), [#170](https://github.com/likelion-backendschool-9th/REMAKE/issues/170) | 구독 API·정기 결제·FE·정책 결정. 미배정 유지 |
| [#171](https://github.com/likelion-backendschool-9th/REMAKE/issues/171) | users·refresh_tokens v4 스키마 |
| [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | 이벤트 v2 서버 계약·180일 원본 정리 |
| [#173](https://github.com/likelion-backendschool-9th/REMAKE/issues/173) | 영수증 구매 수량·용량·PACK 필드 계약 |
| [#174](https://github.com/likelion-backendschool-9th/REMAKE/issues/174) | 공유 사진 형식·용량·HEIC 계약 |
| [#175](https://github.com/likelion-backendschool-9th/REMAKE/issues/175), [#176](https://github.com/likelion-backendschool-9th/REMAKE/issues/176), [#177](https://github.com/likelion-backendschool-9th/REMAKE/issues/177) | 식단 칸 삭제 BE·교체 BE·FE; 새 FE #177 미배정 |

최초 백업 전부터 닫혀 있던 18개도 대조했다: [#16](https://github.com/likelion-backendschool-9th/REMAKE/issues/16), [#40](https://github.com/likelion-backendschool-9th/REMAKE/issues/40), [#41](https://github.com/likelion-backendschool-9th/REMAKE/issues/41), [#42](https://github.com/likelion-backendschool-9th/REMAKE/issues/42), [#43](https://github.com/likelion-backendschool-9th/REMAKE/issues/43), [#44](https://github.com/likelion-backendschool-9th/REMAKE/issues/44), [#46](https://github.com/likelion-backendschool-9th/REMAKE/issues/46), [#54](https://github.com/likelion-backendschool-9th/REMAKE/issues/54), [#58](https://github.com/likelion-backendschool-9th/REMAKE/issues/58), [#59](https://github.com/likelion-backendschool-9th/REMAKE/issues/59), [#60](https://github.com/likelion-backendschool-9th/REMAKE/issues/60), [#65](https://github.com/likelion-backendschool-9th/REMAKE/issues/65), [#66](https://github.com/likelion-backendschool-9th/REMAKE/issues/66), [#69](https://github.com/likelion-backendschool-9th/REMAKE/issues/69), [#74](https://github.com/likelion-backendschool-9th/REMAKE/issues/74), [#119](https://github.com/likelion-backendschool-9th/REMAKE/issues/119), [#130](https://github.com/likelion-backendschool-9th/REMAKE/issues/130), [#152](https://github.com/likelion-backendschool-9th/REMAKE/issues/152). 완료 기록은 보존했다. 다만 #54의 옛 탈퇴 방식은 #68/#98, #46의 옛 장바구니 계약은 #57, #58의 어드민 선행은 보류 6개, #40의 ERD v3 결정은 v4로 대체됐다. #66/#69/#65/#74가 닫혀 있어도 v4 후속 #171/#71/#153/#172/#75가 남아 있으므로 해당 기능 전체를 최신 구현 완료로 세지 않는다.

## 중복처럼 보여도 유지할 작업

| 이슈 | 서로 다른 범위 |
| --- | --- |
| [#50](https://github.com/likelion-backendschool-9th/REMAKE/issues/50), [#122](https://github.com/likelion-backendschool-9th/REMAKE/issues/122) | 동기 AI 제한 정책 결정 / FE·BE·프록시·비동기 작업 값 적용·실측 |
| [#63](https://github.com/likelion-backendschool-9th/REMAKE/issues/63), [#146](https://github.com/likelion-backendschool-9th/REMAKE/issues/146), [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | 요리 환산 / 재료명 단위 결정(완료) / 추가·수정에 결정 함수 적용 |
| [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71), [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | v4 스키마·기록·수정/삭제 규칙 / 요청 unit 제거·단위 재결정 |
| [#65](https://github.com/likelion-backendschool-9th/REMAKE/issues/65), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172), [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116), [#196](https://github.com/likelion-backendschool-9th/REMAKE/issues/196) | 수집 기반(완료) / BE 계약 / FE 트래커 / 실제 행동별 발행·적재 QA |
| [#74](https://github.com/likelion-backendschool-9th/REMAKE/issues/74), [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75) | AI 포트·어댑터(완료) / 실제 생성·재생성 API·횟수 연결 |
| [#165](https://github.com/likelion-backendschool-9th/REMAKE/issues/165), [#188](https://github.com/likelion-backendschool-9th/REMAKE/issues/188) | 푸시 잔여 코드 제거 / PWA 유지·아이콘 등 완성(배포 제거는 병합된 PR #179) |
| [#138](https://github.com/likelion-backendschool-9th/REMAKE/issues/138), [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68) | 만료 토큰 정리 / 탈퇴 인증번호 하루 보관 정리 |
| [#60](https://github.com/likelion-backendschool-9th/REMAKE/issues/60), [#187](https://github.com/likelion-backendschool-9th/REMAKE/issues/187) | 카테고리·단위 기준 시드 / 시연용 업무·이벤트 시드 |
| [#93](https://github.com/likelion-backendschool-9th/REMAKE/issues/93), [#186](https://github.com/likelion-backendschool-9th/REMAKE/issues/186) | 어드민 API 보류 / SQL로 지표 21개 조회는 P0 |

이번 점검에서 추가 종료한 기존 이슈는 없다. 기능 이슈의 체크리스트가 끝났거나 부분 PR이 있다는 이유로 닫지 않는다. #145/#146은 팀이 PR #154 병합 후 닫았고, #157은 PR #160, #82는 PR #178로 완료된 상태를 그대로 보존했다.

## 기능 API 60개 대응표

기능명과 메서드·전체 경로로 읽는다. 조회·등록 등 구현 이슈가 있다는 것만 검증했으며, 모든 엔드포인트가 현재 dev에서 동작한다는 뜻이 아니다. 폐기 금액 대기 화면 #185·지표 SQL #186·정기 작업은 별도 요구라 이 60개 목록만으로 누락을 판정하지 않는다.

| 기능 | 메서드·경로 | BE 이슈 | FE 이슈 |
| --- | --- | --- | --- |
| 소셜 로그인·가입 | `POST /api/v1/public/auth/oauth/{provider}` | [#66](https://github.com/likelion-backendschool-9th/REMAKE/issues/66), [#171](https://github.com/likelion-backendschool-9th/REMAKE/issues/171), [#136](https://github.com/likelion-backendschool-9th/REMAKE/issues/136) | [#95](https://github.com/likelion-backendschool-9th/REMAKE/issues/95) |
| 로그인 쿠키 재발급 | `POST /api/v1/public/auth/refresh` | [#66](https://github.com/likelion-backendschool-9th/REMAKE/issues/66), [#171](https://github.com/likelion-backendschool-9th/REMAKE/issues/171) | [#95](https://github.com/likelion-backendschool-9th/REMAKE/issues/95) |
| 로그아웃 | `POST /api/v1/user/auth/logout` | [#66](https://github.com/likelion-backendschool-9th/REMAKE/issues/66), [#171](https://github.com/likelion-backendschool-9th/REMAKE/issues/171) | [#97](https://github.com/likelion-backendschool-9th/REMAKE/issues/97) |
| 온보딩 일괄 저장 | `PUT /api/v1/user/onboarding` | [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67) | [#96](https://github.com/likelion-backendschool-9th/REMAKE/issues/96) |
| 내 프로필·설정 조회 | `GET /api/v1/user/profile` | [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67) | [#97](https://github.com/likelion-backendschool-9th/REMAKE/issues/97) |
| 가구원·알림·재료 보충 설정 변경 | `PATCH /api/v1/user/profile` | [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67) | [#97](https://github.com/likelion-backendschool-9th/REMAKE/issues/97) |
| 내 제외 재료 조회 | `GET /api/v1/user/excluded-ingredients` | [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67) | [#97](https://github.com/likelion-backendschool-9th/REMAKE/issues/97) |
| 제외 재료 전체 변경 | `PUT /api/v1/user/excluded-ingredients` | [#67](https://github.com/likelion-backendschool-9th/REMAKE/issues/67) | [#97](https://github.com/likelion-backendschool-9th/REMAKE/issues/97) |
| 계정 삭제 이메일 인증번호 발송·재전송 | `POST /api/v1/user/account/deletion-code` | [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68) | [#98](https://github.com/likelion-backendschool-9th/REMAKE/issues/98) |
| 이메일 인증·네이버 재인증으로 계정 삭제 | `POST /api/v1/user/account/deletion` | [#68](https://github.com/likelion-backendschool-9th/REMAKE/issues/68) | [#98](https://github.com/likelion-backendschool-9th/REMAKE/issues/98) |
| 내 구독 상태·결제일 조회 | `GET /api/v1/user/subscription` | [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167) | [#169](https://github.com/likelion-backendschool-9th/REMAKE/issues/169) |
| 토스 카드 등록 창 초기값 조회 | `POST /api/v1/user/subscription/billing-auth` | [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167) | [#169](https://github.com/likelion-backendschool-9th/REMAKE/issues/169) |
| 구독 첫 결제·시작 | `POST /api/v1/user/subscription` | [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167) | [#169](https://github.com/likelion-backendschool-9th/REMAKE/issues/169) |
| 구독 해지 예약 | `POST /api/v1/user/subscription/cancel` | [#167](https://github.com/likelion-backendschool-9th/REMAKE/issues/167) | [#169](https://github.com/likelion-backendschool-9th/REMAKE/issues/169) |
| 냉장고 전체·소비기한 상태별 개수 조회 | `GET /api/v1/user/ingredients/summary` | [#59](https://github.com/likelion-backendschool-9th/REMAKE/issues/59) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) |
| 냉장고 재료 목록·필터 조회 | `GET /api/v1/user/ingredients` | [#59](https://github.com/likelion-backendschool-9th/REMAKE/issues/59) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) |
| 냉장고 재료 상세 조회 | `GET /api/v1/user/ingredients/{id}` | [#69](https://github.com/likelion-backendschool-9th/REMAKE/issues/69), [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71), [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) |
| 재료명·선택 카테고리에 맞는 단위 조회 | `GET /api/v1/user/ingredient-units` | [#146](https://github.com/likelion-backendschool-9th/REMAKE/issues/146) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99), [#101](https://github.com/likelion-backendschool-9th/REMAKE/issues/101) |
| 냉장고 재료 직접 추가 | `POST /api/v1/user/ingredients` | [#69](https://github.com/likelion-backendschool-9th/REMAKE/issues/69), [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71), [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) |
| 냉장고 재료 부분 수정 | `PATCH /api/v1/user/ingredients/{id}` | [#69](https://github.com/likelion-backendschool-9th/REMAKE/issues/69), [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71), [#153](https://github.com/likelion-backendschool-9th/REMAKE/issues/153) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) |
| 냉장고 재료 삭제 | `DELETE /api/v1/user/ingredients/{id}` | [#69](https://github.com/likelion-backendschool-9th/REMAKE/issues/69), [#71](https://github.com/likelion-backendschool-9th/REMAKE/issues/71) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) |
| 재료 사용·폐기와 수량 차감 | `POST /api/v1/user/ingredients/{id}/consume` | [#70](https://github.com/likelion-backendschool-9th/REMAKE/issues/70) | [#100](https://github.com/likelion-backendschool-9th/REMAKE/issues/100) |
| 재료 카테고리 목록 조회 | `GET /api/v1/user/ingredient-categories` | [#60](https://github.com/likelion-backendschool-9th/REMAKE/issues/60) | [#99](https://github.com/likelion-backendschool-9th/REMAKE/issues/99) |
| 영수증 이미지 OCR 분석 | `POST /api/v1/user/receipts/scan` | [#72](https://github.com/likelion-backendschool-9th/REMAKE/issues/72) | [#101](https://github.com/likelion-backendschool-9th/REMAKE/issues/101) |
| 영수증·품목·냉장고 재료 최종 등록 | `POST /api/v1/user/receipts` | [#49](https://github.com/likelion-backendschool-9th/REMAKE/issues/49) | [#102](https://github.com/likelion-backendschool-9th/REMAKE/issues/102) |
| 월별 영수증·주간 합계 조회 | `GET /api/v1/user/receipts` | [#61](https://github.com/likelion-backendschool-9th/REMAKE/issues/61) | [#103](https://github.com/likelion-backendschool-9th/REMAKE/issues/103) |
| 영수증 상세·원본 이미지 조회 | `GET /api/v1/user/receipts/{id}` | [#61](https://github.com/likelion-backendschool-9th/REMAKE/issues/61) | [#103](https://github.com/likelion-backendschool-9th/REMAKE/issues/103) |
| 영수증 삭제 | `DELETE /api/v1/user/receipts/{id}` | [#61](https://github.com/likelion-backendschool-9th/REMAKE/issues/61) | [#103](https://github.com/likelion-backendschool-9th/REMAKE/issues/103) |
| 월별 소비 패턴·카테고리·일별 합계 조회 | `GET /api/v1/user/expenses/patterns` | [#73](https://github.com/likelion-backendschool-9th/REMAKE/issues/73) | [#104](https://github.com/likelion-backendschool-9th/REMAKE/issues/104) |
| 선택 날짜의 소비 품목 조회 | `GET /api/v1/user/expenses/daily-items` | [#73](https://github.com/likelion-backendschool-9th/REMAKE/issues/73) | [#104](https://github.com/likelion-backendschool-9th/REMAKE/issues/104) |
| 남은 레시피·식단표 생성 횟수 조회 | `GET /api/v1/user/usage-quotas` | [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75) | [#105](https://github.com/likelion-backendschool-9th/REMAKE/issues/105), [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106), [#108](https://github.com/likelion-backendschool-9th/REMAKE/issues/108) |
| 빠른·커스텀 레시피 생성 | `POST /api/v1/user/recipes/generate` | [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75) | [#105](https://github.com/likelion-backendschool-9th/REMAKE/issues/105) |
| 같은 조건으로 레시피 다시 받기 | `POST /api/v1/user/recipes/{id}/regenerate` | [#75](https://github.com/likelion-backendschool-9th/REMAKE/issues/75) | [#105](https://github.com/likelion-backendschool-9th/REMAKE/issues/105), [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) |
| 레시피 상세 조회 | `GET /api/v1/user/recipes/{id}` | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76) | [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) |
| 요리 후 차감할 보유 재료·수량 미리보기 | `GET /api/v1/user/recipes/{id}/cook-preview` | [#63](https://github.com/likelion-backendschool-9th/REMAKE/issues/63), [#77](https://github.com/likelion-backendschool-9th/REMAKE/issues/77) | [#107](https://github.com/likelion-backendschool-9th/REMAKE/issues/107) |
| 선택 재료 차감·요리 완료 | `POST /api/v1/user/recipes/{id}/cook` | [#77](https://github.com/likelion-backendschool-9th/REMAKE/issues/77) | [#107](https://github.com/likelion-backendschool-9th/REMAKE/issues/107) |
| 저장 레시피 목록·필터·정렬 조회 | `GET /api/v1/user/saved-recipes` | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76) | [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) |
| 내 레시피 저장·부족 재료 담기 | `PUT /api/v1/user/saved-recipes/{recipeId}` | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76), [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89) | [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) |
| 내 저장 레시피 취소 | `DELETE /api/v1/user/saved-recipes/{recipeId}` | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76) | [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) |
| 저장 레시피 즐겨찾기 켜기 | `PUT /api/v1/user/saved-recipes/{recipeId}/favorite` | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76) | [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) |
| 저장 레시피 즐겨찾기 끄기 | `DELETE /api/v1/user/saved-recipes/{recipeId}/favorite` | [#76](https://github.com/likelion-backendschool-9th/REMAKE/issues/76) | [#106](https://github.com/likelion-backendschool-9th/REMAKE/issues/106) |
| 식단표 비동기 생성 접수 | `POST /api/v1/user/meal-plans/generate` | [#78](https://github.com/likelion-backendschool-9th/REMAKE/issues/78), [#79](https://github.com/likelion-backendschool-9th/REMAKE/issues/79), [#91](https://github.com/likelion-backendschool-9th/REMAKE/issues/91) | [#108](https://github.com/likelion-backendschool-9th/REMAKE/issues/108), [#115](https://github.com/likelion-backendschool-9th/REMAKE/issues/115) |
| 월별 식단표 달력 조회 | `GET /api/v1/user/meal-plans/calendar` | [#80](https://github.com/likelion-backendschool-9th/REMAKE/issues/80) | [#108](https://github.com/likelion-backendschool-9th/REMAKE/issues/108) |
| 날짜별 식단·끼니 조회 | `GET /api/v1/user/meal-plans/days/{date}` | [#80](https://github.com/likelion-backendschool-9th/REMAKE/issues/80) | [#108](https://github.com/likelion-backendschool-9th/REMAKE/issues/108) |
| 식단표 끼니 삭제 | `DELETE /api/v1/user/meal-plan-slots/{slotId}` | [#175](https://github.com/likelion-backendschool-9th/REMAKE/issues/175) | [#177](https://github.com/likelion-backendschool-9th/REMAKE/issues/177) |
| 식단표 끼니 레시피 교체 | `POST /api/v1/user/meal-plan-slots/{slotId}/replace` | [#176](https://github.com/likelion-backendschool-9th/REMAKE/issues/176) | [#177](https://github.com/likelion-backendschool-9th/REMAKE/issues/177) |
| 최신 알림 문구·안 읽은 수 조회 | `GET /api/v1/user/notifications/summary` | [#82](https://github.com/likelion-backendschool-9th/REMAKE/issues/82) | [#110](https://github.com/likelion-backendschool-9th/REMAKE/issues/110) |
| 알림 목록 조회·전체 읽음 처리 | `GET /api/v1/user/notifications` | [#82](https://github.com/likelion-backendschool-9th/REMAKE/issues/82) | [#110](https://github.com/likelion-backendschool-9th/REMAKE/issues/110) |
| 장바구니 목록 조회 | `GET /api/v1/user/cart-items` | [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89) | [#114](https://github.com/likelion-backendschool-9th/REMAKE/issues/114) |
| 장바구니 수량 변경 | `PATCH /api/v1/user/cart-items/{id}` | [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89) | [#114](https://github.com/likelion-backendschool-9th/REMAKE/issues/114) |
| 선택 장바구니 재료를 냉장고로 이동 | `POST /api/v1/user/cart-items/move-to-fridge` | [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89) | [#114](https://github.com/likelion-backendschool-9th/REMAKE/issues/114) |
| 선택 장바구니 항목 삭제 | `POST /api/v1/user/cart-items/delete` | [#89](https://github.com/likelion-backendschool-9th/REMAKE/issues/89) | [#114](https://github.com/likelion-backendschool-9th/REMAKE/issues/114) |
| 이번 주 인기 공유 레시피 TOP 3 조회 | `GET /api/v1/user/shared-recipes/weekly-top` | [#88](https://github.com/likelion-backendschool-9th/REMAKE/issues/88) | [#113](https://github.com/likelion-backendschool-9th/REMAKE/issues/113) |
| 공유 레시피 인기·최신순·검색·순위 변동 조회 | `GET /api/v1/user/shared-recipes/ranking` | [#88](https://github.com/likelion-backendschool-9th/REMAKE/issues/88) | [#113](https://github.com/likelion-backendschool-9th/REMAKE/issues/113) |
| 레시피 제목·사진과 함께 공유 게시 | `POST /api/v1/user/shared-recipes` | [#64](https://github.com/likelion-backendschool-9th/REMAKE/issues/64), [#86](https://github.com/likelion-backendschool-9th/REMAKE/issues/86) | [#112](https://github.com/likelion-backendschool-9th/REMAKE/issues/112) |
| 익명 공유 레시피 상세·재료 보유 여부 조회 | `GET /api/v1/user/shared-recipes/{id}` | [#86](https://github.com/likelion-backendschool-9th/REMAKE/issues/86) | [#112](https://github.com/likelion-backendschool-9th/REMAKE/issues/112) |
| 공유 원본 레시피 저장·부족 재료 담기 | `PUT /api/v1/user/shared-recipes/{id}/save` | [#90](https://github.com/likelion-backendschool-9th/REMAKE/issues/90) | [#112](https://github.com/likelion-backendschool-9th/REMAKE/issues/112) |
| 공유 레시피 저장 취소 | `DELETE /api/v1/user/shared-recipes/{id}/save` | [#90](https://github.com/likelion-backendschool-9th/REMAKE/issues/90) | [#112](https://github.com/likelion-backendschool-9th/REMAKE/issues/112) |
| 공유 레시피 사유 선택 신고 | `POST /api/v1/user/shared-recipes/{id}/reports` | [#86](https://github.com/likelion-backendschool-9th/REMAKE/issues/86) | [#112](https://github.com/likelion-backendschool-9th/REMAKE/issues/112) |
| 프론트 이벤트 일괄 수집 | `POST /api/v1/user/events` | [#65](https://github.com/likelion-backendschool-9th/REMAKE/issues/65), [#172](https://github.com/likelion-backendschool-9th/REMAKE/issues/172) | [#116](https://github.com/likelion-backendschool-9th/REMAKE/issues/116) |

## 완료 판정과 검증 기록

다음은 이번 이슈 점검에서 확인했다: WBS v2 76개 고유 번호, API 60개 고유 메서드·경로, 최초 74개 이슈 전부 존속, 신규 15개 본문·라벨·담당자, 기존 보완 8개 이슈의 상태·라벨·담당자·제목·완료 체크 보존, 상대 링크와 문서 짝. 런타임 코드를 바꾸지 않아 FE/BE 빌드는 실행하지 않았다. #191~#196의 실제 QA 결과는 이 점검 결과에 포함되지 않는다.
