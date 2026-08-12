# 백엔드 약관 작업 요청사항

FE 이슈 #87에서는 기존 API 계약을 유지하며 `SERVICE(termId: 1)`와 `PRIVACY_POLICY(termId: 2)`를 전송하고 있습니다. 아래 변경은 별도 BE/DB 이슈에서 검토가 필요합니다.

## 필요한 변경

1. 회원가입용 `PRIVACY_CONSENT` 약관 타입을 `TermType`과 DB `terms.term_type`에 추가
2. 개인정보 처리방침(`PRIVACY_POLICY`)과 개인정보 수집·이용 동의(`PRIVACY_CONSENT`)를 별도 문서로 관리
3. 회원가입 필수 약관은 `SERVICE`, `PRIVACY_CONSENT` 두 종류로 검증
4. 개인정보 처리방침은 동의 대상이 아닌 상시 열람 문서로 제공
5. 마케팅 동의는 선택 항목으로 유지하되 이번 FE 회원가입 화면에서는 제외
6. 기존 DB에 적용 가능한 신규 Flyway migration 작성(V1 수정 금지)
7. `GET /api/terms/latest` 응답에 새 약관 타입과 최신 문서가 포함되는지 테스트
8. 회원가입 시 필수 약관 누락·미동의·중복·구버전 ID 검증 테스트

## 문서 및 운영 정보

- 운영주체: KB IT's Your Life 7기 프로젝트팀
- 시행일: 2026년 8월 1일
- 문의: tototoro523@gmail.com
- 확정 필요: 서버/클라우드, 이메일 발송업체, AI API 개인정보 전송 여부, 국외 이전 정보, 개인정보 보호책임자

## FE 전환 시점

BE 배포 후 FE의 임시 `PRIVACY_POLICY(termId: 2)` 매핑을 제거하고 API에서 받은 `PRIVACY_CONSENT`의 실제 `termId`를 전송해야 합니다.
