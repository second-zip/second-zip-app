-- =========================================================
-- 1. 회원
-- =========================================================

CREATE TABLE accounts (
                          account_id BIGINT NOT NULL AUTO_INCREMENT,
                          email VARCHAR(50) NOT NULL,
                          password VARCHAR(100) NOT NULL,
                          nickname VARCHAR(50) NOT NULL,
                          character_type ENUM(
        'MAN',
        'WOMAN',
        'CAT'
    ) NOT NULL DEFAULT 'CAT',

                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

                          PRIMARY KEY (account_id),
                          UNIQUE KEY uk_accounts_email (email),
                          UNIQUE KEY uk_accounts_nickname (nickname)
) ENGINE = InnoDB;


-- =========================================================
-- 2. 약관
-- =========================================================

CREATE TABLE terms (
                       term_id BIGINT NOT NULL AUTO_INCREMENT,
                       title VARCHAR(80) NOT NULL,
                       content TEXT NOT NULL,
                       term_type ENUM(
        'SERVICE',
        'PRIVACY_POLICY',
        'MARKETING'
    ) NOT NULL,
                       is_required BOOLEAN NOT NULL,
                       version VARCHAR(30) NOT NULL,

                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,

                       PRIMARY KEY (term_id),
                       UNIQUE KEY uk_terms_type_version (term_type, version)
) ENGINE = InnoDB;

-- =========================================================
-- 약관 초기 데이터
-- =========================================================

INSERT INTO terms (
    title,
    content,
    term_type,
    is_required,
    version
) VALUES
      (
          '이번집 서비스 이용약관',
          '이번집 서비스 이용약관

      제1조 목적

      본 약관은 운영주체명이 제공하는 전세 계약 위험 분석 및 계약 관리 서비스 “이번집”(이하 “서비스”)의 이용과 관련하여 운영자와 회원 간의 권리, 의무 및 책임사항을 정하는 것을 목적으로 합니다.

      제2조 용어의 정의

      1. “서비스”란 회원이 입력한 주택 주소와 보증금 등의 정보를 바탕으로 주택 관련 정보, 필수 점검 항목, 전세사기 위험 유형 및 계약 관련 참고 정보를 제공하는 서비스를 의미합니다.
      2. “회원”이란 본 약관에 동의하고 서비스에 가입하여 서비스를 이용하는 자를 의미합니다.
      3. “분석 리포트”란 회원이 입력한 정보와 공공데이터, 외부 API 및 내부 판정 기준 등을 바탕으로 생성된 주택 위험 분석 결과를 의미합니다.
      4. “AI 생성 정보”란 인공지능 기술을 활용하여 생성된 특약 추천, 주의사항, 설명 및 기타 참고 정보를 의미합니다.
      5. “공유 링크”란 회원이 분석 리포트를 다른 사람에게 공유하기 위해 생성하는 일정 기간 유효한 접근 링크를 의미합니다.

      제3조 약관의 효력 및 변경

      1. 본 약관은 서비스 화면에 게시하거나 회원에게 알린 시점부터 효력이 발생합니다.
      2. 운영자는 관계 법령을 위반하지 않는 범위에서 본 약관을 변경할 수 있습니다.
      3. 약관이 변경되는 경우 운영자는 적용일과 변경 사유를 서비스 화면 등을 통해 사전에 안내합니다.
      4. 회원에게 불리한 중요한 변경 사항은 적용일 30일 전부터 안내하는 것을 원칙으로 합니다.

      제4조 회원가입

      1. 회원가입은 이용자가 본 약관의 내용에 동의하고 운영자가 정한 가입 절차를 완료함으로써 성립합니다.
      2. 회원은 정확한 정보를 제공해야 하며, 타인의 정보를 사용하거나 허위 정보를 입력해서는 안 됩니다.
      3. 운영자는 다음 각 호에 해당하는 경우 가입을 거절하거나 사후에 이용계약을 해지할 수 있습니다.
         가. 타인의 정보를 도용한 경우
         나. 허위 정보를 입력한 경우
         다. 서비스 운영을 방해할 목적으로 가입한 경우
         라. 관계 법령 또는 본 약관을 위반한 경우

      제5조 계정 관리

      1. 회원은 자신의 계정과 비밀번호를 안전하게 관리해야 합니다.
      2. 계정 관리 소홀 또는 제3자의 부정 사용으로 발생한 피해에 대해 운영자에게 고의 또는 중대한 과실이 없는 경우 운영자는 책임을 부담하지 않습니다.
      3. 회원은 계정이 도용되거나 부정하게 사용된 사실을 알게 된 경우 즉시 운영자에게 알려야 합니다.

      제6조 서비스의 제공

      운영자는 회원에게 다음 각 호의 서비스를 제공할 수 있습니다.

      1. 주택 주소 및 보증금 기반 분석 리포트 제공
      2. 근저당, 위반건축물, 건축물 용도, HUG 보증보험 가입 가능성 및 권리침해 관련 점검 정보 제공
      3. 전세가율, 선순위 채권, 소유관계, 신탁등기 등 전세사기 위험 유형 분석
      4. AI를 활용한 계약 특약 및 주의사항 추천
      5. 분석 리포트 저장, 즐겨찾기 및 공유
      6. 전세 계약 단계별 체크리스트 제공
      7. 전세가격지수 및 전세사기 피해 통계 등 지역 관련 정보 제공
      8. 그 밖에 운영자가 추가로 개발하거나 다른 사업자와의 협력을 통해 제공하는 서비스

      제7조 분석 결과의 성격 및 이용상 주의사항

      1. 서비스에서 제공하는 분석 결과는 회원의 계약 판단을 돕기 위한 참고 정보입니다.
      2. 분석 결과는 법률, 세무, 감정평가, 부동산 중개 또는 금융 상담을 대체하지 않습니다.
      3. HUG 보증보험 관련 결과는 가입 가능성에 대한 사전 점검 정보이며, 실제 가입 승인 여부는 주택도시보증공사, 금융기관 및 관련 기관의 심사 결과에 따라 달라질 수 있습니다.
      4. 공공데이터 또는 외부기관이 제공한 정보는 갱신 시점, 자료 누락 또는 제공기관의 사정에 따라 실제 현황과 차이가 있을 수 있습니다.
      5. AI 생성 정보는 자동으로 생성된 참고 자료로서 부정확하거나 불완전한 내용을 포함할 수 있습니다.
      6. 회원은 실제 계약을 체결하기 전에 등기사항증명서, 건축물대장 및 관련 서류를 직접 확인하고 필요한 경우 공인중개사, 변호사 등 전문가의 도움을 받아야 합니다.

      제8조 회원의 의무

      회원은 다음 각 호의 행위를 해서는 안 됩니다.

      1. 타인의 개인정보 또는 계정을 도용하는 행위
      2. 허위 주소, 허위 보증금 등 사실과 다른 정보를 입력하는 행위
      3. 서비스를 이용하여 다른 사람의 권리 또는 개인정보를 침해하는 행위
      4. 서비스의 정상적인 운영을 방해하거나 시스템에 비정상적으로 접근하는 행위
      5. 서비스의 분석 결과를 조작하거나 허위 사실과 함께 배포하는 행위
      6. 운영자의 허락 없이 서비스를 영리 목적으로 복제, 판매 또는 재제공하는 행위
      7. 관계 법령 또는 공공질서와 미풍양속에 위반되는 행위

      제9조 리포트 공유

      1. 회원은 공유 링크를 통해 분석 리포트를 제3자에게 공유할 수 있습니다.
      2. 공유 링크를 전달받은 사람은 링크의 유효기간 동안 해당 리포트에 접근할 수 있습니다.
      3. 회원은 공유 대상과 범위를 신중하게 결정해야 하며, 공유 링크가 불특정 다수에게 노출되지 않도록 관리해야 합니다.
      4. 회원의 공유 링크 관리 소홀로 발생한 문제에 대해 운영자에게 고의 또는 중대한 과실이 없는 경우 운영자는 책임을 부담하지 않습니다.
      5. 회원은 공유 링크를 만료시키거나 삭제할 수 있습니다.

      제10조 서비스의 변경 및 중단

      1. 운영자는 서비스 개선, 시스템 점검, 외부 API 변경, 공공데이터 제공 중단 또는 기타 운영상 필요한 사유가 있는 경우 서비스의 전부 또는 일부를 변경하거나 중단할 수 있습니다.
      2. 운영자는 예정된 서비스 중단이 있는 경우 사전에 안내하도록 노력합니다.
      3. 천재지변, 시스템 장애, 통신망 장애 등 긴급한 사유가 있는 경우 사전 안내 없이 서비스가 중단될 수 있습니다.

      제11조 이용 제한

      운영자는 회원이 본 약관 또는 관계 법령을 위반한 경우 경고, 서비스 이용 제한 또는 회원 탈퇴 등의 조치를 할 수 있습니다.

      제12조 회원 탈퇴

      1. 회원은 언제든지 서비스에서 회원 탈퇴를 신청할 수 있습니다.
      2. 회원 탈퇴가 완료되면 관계 법령에 따라 보관해야 하는 정보를 제외한 회원의 개인정보와 서비스 이용 정보는 처리방침에 따라 삭제됩니다.
      3. 회원 탈퇴로 삭제된 분석 리포트, 체크리스트 및 기타 정보는 복구되지 않을 수 있습니다.

      제13조 개인정보 보호

      운영자는 관계 법령에 따라 회원의 개인정보를 보호하며, 개인정보의 처리에 관한 구체적인 사항은 개인정보 처리방침에서 정합니다.

      제14조 책임의 제한

      1. 운영자는 고의 또는 중대한 과실이 없는 한 공공기관, 외부 API 또는 제3자가 제공한 정보의 오류나 지연으로 발생한 손해에 대해 책임을 부담하지 않습니다.
      2. 회원이 분석 결과만을 근거로 계약을 체결하거나 별도의 확인 절차를 거치지 않아 발생한 손해에 대해 운영자에게 고의 또는 중대한 과실이 없는 경우 운영자는 책임을 부담하지 않습니다.
      3. 운영자는 회원 간 또는 회원과 제3자 간에 발생한 분쟁에 직접 관여하지 않습니다.
      4. 본 조는 관계 법령에 따라 운영자가 부담해야 하는 책임을 부당하게 배제하는 것으로 해석되지 않습니다.

      제15조 준거법 및 분쟁 해결

      1. 본 약관은 대한민국 법령에 따라 해석됩니다.
      2. 서비스 이용과 관련하여 분쟁이 발생한 경우 운영자와 회원은 원만한 해결을 위해 성실히 협의합니다.
      3. 협의로 해결되지 않는 분쟁은 관계 법령에서 정한 관할법원에 제기할 수 있습니다.

      부칙

      본 약관은 [시행일]부터 시행합니다.',
          'SERVICE',
          TRUE,
          '1.0.0'
      ),
      (
          '이번집 개인정보 처리방침',
          '이번집 개인정보 처리방침

      운영주체명은 개인정보 보호법 등 관계 법령을 준수하며, 이용자의 개인정보를 안전하게 처리하기 위해 다음과 같이 개인정보 처리방침을 수립·공개합니다.

      제1조 개인정보의 처리 목적

      운영자는 다음 목적을 위해 개인정보를 처리합니다.

      1. 회원가입 및 회원 관리
         가. 회원 식별 및 계정 관리
         나. 회원가입 의사 확인
         다. 부정 이용 방지
         라. 서비스 관련 안내 및 문의 대응

      2. 주택 위험 분석 서비스 제공
         가. 회원이 입력한 주소와 보증금을 바탕으로 분석 리포트 생성
         나. 필수 점검 항목 및 전세사기 위험 유형 분석
         다. AI 기반 특약 및 주의사항 제공
         라. 분석 리포트 저장, 즐겨찾기 및 공유 기능 제공

      3. 체크리스트 서비스 제공
         가. 회원별 계약 단계 체크리스트 제공
         나. 체크리스트 완료 상태 저장

      4. 서비스 운영 및 개선
         가. 서비스 오류 확인 및 장애 대응
         나. 서비스 이용현황 분석
         다. 보안 및 부정 이용 방지

      5. 마케팅 정보 제공
         회원이 별도로 동의한 경우 신규 기능, 이벤트 및 혜택 관련 정보를 제공합니다.

      제2조 처리하는 개인정보의 항목

      운영자는 다음과 같은 개인정보를 처리할 수 있습니다.

      1. 회원가입 및 계정 관리
         가. 필수 항목: 이메일 주소, 암호화된 비밀번호, 닉네임
         나. 선택 또는 설정 항목: 캐릭터 유형
         다. 약관 동의 여부 및 동의 일시

      2. 주택 위험 분석 서비스
         가. 도로명주소
         나. 상세주소
         다. 보증금
         라. 분석 결과 및 위험도
         마. 즐겨찾기 여부 및 즐겨찾기 일시
         바. 리포트 공유 토큰 및 공유 만료 일시

      3. 체크리스트 서비스
         가. 체크리스트 항목
         나. 항목별 완료 여부

      4. 서비스 이용 과정에서 자동으로 생성될 수 있는 정보
         가. 접속 일시
         나. IP 주소
         다. 브라우저 및 기기 정보
         라. 서비스 이용기록
         마. 오류 및 접속 로그

      5. 마케팅 정보 수신
         이메일 주소, 닉네임, 마케팅 수신 동의 여부 및 동의 일시

      운영자는 주민등록번호, 계좌번호 등 서비스 제공에 필요하지 않은 개인정보를 입력하도록 요구하지 않습니다. 이용자는 분석 주소 또는 기타 입력란에 불필요한 제3자의 개인정보를 입력하지 않아야 합니다.

      제3조 개인정보의 처리 및 보유기간

      1. 회원가입 및 계정 정보
         회원 탈퇴 시까지 보관한 후 지체 없이 파기합니다.

      2. 분석 리포트 및 체크리스트 정보
         회원이 해당 정보를 삭제하거나 회원 탈퇴를 완료할 때까지 보관합니다.

      3. 공유 링크 정보
         회원이 공유를 취소하거나 공유 유효기간이 만료될 때까지 보관합니다.

      4. 마케팅 수신 동의 정보
         회원이 마케팅 수신 동의를 철회하거나 회원 탈퇴를 완료할 때까지 보관합니다.

      5. 관계 법령에 따라 일정 기간 보관할 의무가 있는 경우
         해당 법령에서 정한 기간 동안 별도로 분리하여 보관할 수 있습니다.

      제4조 개인정보의 제3자 제공

      1. 운영자는 원칙적으로 이용자의 개인정보를 제3자에게 제공하지 않습니다.
      2. 다만 다음의 경우에는 예외로 합니다.
         가. 이용자가 사전에 동의한 경우
         나. 법령에 특별한 규정이 있거나 법령상 의무를 준수하기 위해 필요한 경우
         다. 이용자 또는 제3자의 생명, 신체 또는 재산상 이익을 위해 긴급하게 필요한 경우로서 법에서 정한 요건을 충족한 경우
      3. 개인정보를 제3자에게 제공하게 되는 경우 제공받는 자, 제공 목적, 제공 항목 및 보유기간을 사전에 안내하고 필요한 동의를 받습니다.

      제5조 개인정보 처리업무의 위탁

      운영자는 원활한 서비스 제공을 위해 개인정보 처리업무의 일부를 외부 사업자에게 위탁할 수 있습니다.

      현재 개인정보 처리업무 수탁자는 다음과 같습니다.

      1. 수탁자: [클라우드 또는 서버 운영업체명]
         위탁 업무: 서버 및 데이터베이스 운영
         보유 및 이용기간: 위탁계약 종료 또는 회원 탈퇴 시까지

      2. 수탁자: [이메일 발송업체명]
         위탁 업무: 서비스 및 마케팅 이메일 발송
         보유 및 이용기간: 발송 완료 또는 위탁계약 종료 시까지

      3. 수탁자: [AI API 제공업체명]
         위탁 업무: AI 기반 설명 및 특약 추천 생성
         보유 및 이용기간: [실제 처리방침 및 계약에 따른 기간]

      운영자는 위탁계약 체결 시 개인정보가 안전하게 관리될 수 있도록 수탁자의 처리 목적, 보호조치, 재위탁 및 관리·감독에 관한 사항을 정합니다.

      실제로 이용하지 않는 업체나 업무는 위 목록에서 삭제합니다.

      제6조 개인정보의 국외 이전

      운영자가 해외 사업자의 클라우드 또는 AI API를 이용하여 개인정보를 국외로 이전하는 경우 이전받는 자, 이전 국가, 이전 항목, 이전 목적, 이전 일시 및 방법, 보유기간과 이전 거부 방법을 별도로 공개합니다.

      현재 국외 이전 현황은 다음과 같습니다.

      1. 이전받는 자: [실제 해외 사업자명 또는 해당 없음]
      2. 이전 국가: [국가명]
      3. 이전 항목: [전송되는 실제 정보]
      4. 이전 목적: [AI 결과 생성 또는 서버 운영 등]
      5. 이전 방법: 서비스 이용 과정에서 암호화된 통신망을 통한 전송
      6. 보유기간: [실제 사업자 정책 및 계약에 따른 기간]
      7. 이전 거부 방법 및 효과: [거부 방법과 해당 기능 이용 제한 내용]

      국외 이전이 없는 경우 본 항목에는 “운영자는 개인정보를 국외로 이전하지 않습니다.”라고 기재합니다.

      제7조 개인정보의 파기

      1. 운영자는 개인정보의 보유기간이 경과하거나 처리 목적이 달성된 경우 지체 없이 해당 개인정보를 파기합니다.
      2. 전자적 파일 형태의 개인정보는 복구하거나 재생할 수 없는 방법으로 삭제합니다.
      3. 종이 문서 형태의 개인정보가 있는 경우 분쇄하거나 소각하여 파기합니다.
      4. 관계 법령에 따라 보관해야 하는 개인정보는 다른 개인정보와 분리하여 보관합니다.

      제8조 이용자의 권리와 행사 방법

      1. 이용자는 자신의 개인정보에 대해 다음 권리를 행사할 수 있습니다.
         가. 개인정보 열람 요구
         나. 개인정보 정정 또는 삭제 요구
         다. 개인정보 처리정지 요구
         라. 개인정보 수집·이용 동의 철회
         마. 회원 탈퇴

      2. 이용자는 서비스 내 회원정보 관리 기능 또는 아래 연락처를 통해 권리를 행사할 수 있습니다.
         가. 이메일: [문의 이메일]
         나. 연락처: [문의 연락처]

      3. 운영자는 관계 법령에서 정한 기간과 절차에 따라 이용자의 요구를 처리합니다.

      제9조 개인정보의 안전성 확보조치

      운영자는 개인정보를 안전하게 보호하기 위해 다음 조치를 시행합니다.

      1. 비밀번호의 단방향 암호화 저장
      2. 개인정보 접근 권한의 최소화 및 관리
      3. 개인정보 전송 구간 암호화
      4. 데이터베이스 및 서버 접근 통제
      5. 접속기록 보관 및 위변조 방지
      6. 보안 취약점 점검 및 소프트웨어 업데이트
      7. 개인정보 취급자에 대한 관리·감독

      제10조 개인정보 자동 수집장치

      1. 운영자는 로그인 유지 및 서비스 이용환경 개선을 위해 쿠키를 사용할 수 있습니다.
      2. 이용자는 브라우저 설정을 통해 쿠키 저장을 거부하거나 삭제할 수 있습니다.
      3. 쿠키 저장을 거부할 경우 로그인 유지 등 일부 기능 이용이 제한될 수 있습니다.
      4. 운영자가 맞춤형 광고 등을 위한 행태정보를 수집하게 되는 경우 수집 항목, 목적, 보유기간 및 거부 방법을 별도로 공개합니다.

      제11조 만 14세 미만 이용자

      서비스는 원칙적으로 만 14세 미만 이용자를 대상으로 하지 않습니다. 만 14세 미만 이용자의 개인정보를 처리하게 되는 경우 관계 법령에 따라 법정대리인의 동의를 받는 절차를 마련합니다.

      제12조 개인정보 보호책임자

      운영자는 개인정보 처리에 관한 업무를 총괄하고 이용자의 문의 및 불만을 처리하기 위해 다음과 같이 개인정보 보호책임자를 지정합니다.

      1. 성명: [개인정보 보호책임자 이름]
      2. 직책: [직책]
      3. 이메일: [문의 이메일]
      4. 연락처: [문의 연락처]

      개인정보 침해에 대한 신고 또는 상담이 필요한 경우 개인정보침해 신고센터 등 관계 기관에 문의할 수 있습니다.

      제13조 개인정보 처리방침의 변경

      1. 본 처리방침의 내용이 변경되는 경우 시행일 전에 서비스 화면을 통해 안내합니다.
      2. 이용자의 권리에 중대한 영향을 미치는 변경 사항은 충분한 기간을 두고 안내합니다.

      부칙

      본 개인정보 처리방침은 [시행일]부터 시행합니다.',
          'PRIVACY_POLICY',
          TRUE,
          '1.0.0'
      ),
      (
          '이번집 마케팅 정보 수신 동의',
          '이번집 마케팅 정보 수신 동의

      [운영주체명]은 신규 기능, 이벤트 및 서비스 혜택에 관한 정보를 제공하기 위해 다음과 같이 개인정보를 이용하고 광고성 정보를 전송하고자 합니다.

      1. 이용 목적

      가. 이번집 신규 기능 및 서비스 안내
      나. 이벤트, 프로모션 및 혜택 안내
      다. 이용자 맞춤형 서비스 및 콘텐츠 추천
      라. 서비스 관련 설문조사 및 참여 안내

      2. 이용하는 개인정보

      이메일 주소, 닉네임, 마케팅 정보 수신 동의 여부 및 동의 일시

      3. 광고성 정보 전송 방법

      이메일

      향후 앱 알림, 문자메시지 등 새로운 전송 수단을 추가하는 경우 해당 수단을 이용하기 전에 필요한 안내 및 동의 절차를 진행합니다.

      4. 보유 및 이용기간

      마케팅 정보 수신 동의 철회 또는 회원 탈퇴 시까지 보유하고 이용합니다.

      관계 법령에 따라 보관할 필요가 있는 경우에는 해당 법령에서 정한 기간 동안 보관할 수 있습니다.

      5. 동의 거부 및 철회

      마케팅 정보 수신 동의는 선택 사항이며, 동의하지 않아도 회원가입과 이번집의 기본 서비스를 이용할 수 있습니다.

      이용자는 서비스 설정 또는 [문의 이메일]을 통해 언제든지 마케팅 정보 수신 동의를 철회할 수 있습니다.

      동의를 철회한 이후에는 새로운 광고성 정보가 전송되지 않습니다. 다만 철회 처리 전에 이미 발송된 정보는 수신될 수 있습니다.

      6. 야간 광고성 정보

      운영자는 별도의 야간 수신 동의를 받지 않는 한 오후 9시부터 다음 날 오전 8시까지 광고성 정보를 전송하지 않습니다.

      7. 시행일

      본 마케팅 정보 수신 동의는 [시행일]부터 시행합니다.',
          'MARKETING',
          FALSE,
          '1.0.0'
      );


-- =========================================================
-- 3. 회원 약관 동의
-- =========================================================

CREATE TABLE account_term_consents (
                                       account_term_consent_id BIGINT NOT NULL AUTO_INCREMENT,
                                       account_id BIGINT NOT NULL,
                                       term_id BIGINT NOT NULL,
                                       is_agreed BOOLEAN NOT NULL DEFAULT FALSE,
                                       agreed_at DATETIME NULL,

                                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,

                                       PRIMARY KEY (account_term_consent_id),

    -- 한 회원이 같은 약관에 대한 동의 기록을 중복 생성하는 것 방지
                                       UNIQUE KEY uk_account_term_consents_account_term (
                                           account_id,
                                           term_id
                                           ),

                                       KEY idx_account_term_consents_term_id (term_id),

                                       CONSTRAINT fk_account_term_consents_account
                                           FOREIGN KEY (account_id)
                                               REFERENCES accounts (account_id)
                                               ON DELETE CASCADE
                                               ON UPDATE CASCADE,

                                       CONSTRAINT fk_account_term_consents_term
                                           FOREIGN KEY (term_id)
                                               REFERENCES terms (term_id)
                                               ON DELETE RESTRICT
                                               ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 4. 분석 리포트
-- =========================================================

CREATE TABLE analysis_reports (
                                  analysis_report_id BIGINT NOT NULL AUTO_INCREMENT,
                                  account_id BIGINT NOT NULL,
                                  road_address VARCHAR(100) NOT NULL,
                                  detail_address VARCHAR(100) NULL,
                                  deposit BIGINT NOT NULL,
                                  favorite BOOLEAN NOT NULL DEFAULT FALSE,
                                  favorited_at DATETIME NULL,
                                  share_token VARCHAR(255) NULL,
                                  share_expires_at DATETIME NULL,
                                  risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NULL,

                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  PRIMARY KEY (analysis_report_id),
                                  UNIQUE KEY uk_analysis_reports_share_token (share_token),
                                  KEY idx_analysis_reports_account_id (account_id),
                                  KEY idx_analysis_reports_favorite (
        account_id,
        favorite,
        favorited_at
    ),

                                  CONSTRAINT chk_analysis_reports_deposit
                                      CHECK (deposit >= 0),

                                  CONSTRAINT fk_analysis_reports_account
                                      FOREIGN KEY (account_id)
                                          REFERENCES accounts (account_id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 5. AI 생성 메시지
-- =========================================================

CREATE TABLE ai_generate_messages (
                                      ai_generate_message_id BIGINT NOT NULL AUTO_INCREMENT,
                                      analysis_report_id BIGINT NOT NULL,
                                      title VARCHAR(100) NOT NULL,
                                      content TEXT NOT NULL,
                                      reason TEXT NULL,

                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

                                      PRIMARY KEY (ai_generate_message_id),
                                      KEY idx_ai_generate_messages_report_id (analysis_report_id),

                                      CONSTRAINT fk_ai_generate_messages_report
                                          FOREIGN KEY (analysis_report_id)
                                              REFERENCES analysis_reports (analysis_report_id)
                                              ON DELETE CASCADE
                                              ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 6. 필수 점검 결과
-- =========================================================

CREATE TABLE report_check_results (
                                      report_check_result_id BIGINT NOT NULL AUTO_INCREMENT,
                                      analysis_report_id BIGINT NOT NULL,
                                      check_type ENUM(
		'MORTGAGE_EXISTENCE',
		'ILLEGAL_BUILDING',
		'BUILDING_USE',
		'HUG_GUARANTEE_ELIGIBILITY',
		'RIGHTS_INFRINGEMENT'
    ) NOT NULL,
                                      risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NOT NULL,
                                      evidence JSON NULL,

                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

                                      PRIMARY KEY (report_check_result_id),

    -- 하나의 리포트에서 동일 점검 항목 중복 방지
                                      UNIQUE KEY uk_report_check_results_report_type (
                                          analysis_report_id,
                                          check_type
                                          ),

                                      CONSTRAINT fk_report_check_results_report
                                          FOREIGN KEY (analysis_report_id)
                                              REFERENCES analysis_reports (analysis_report_id)
                                              ON DELETE CASCADE
                                              ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 7. 리포트별 전세사기 유형 결과
-- =========================================================

CREATE TABLE report_fraud_types (
                                    report_fraud_type_id BIGINT NOT NULL AUTO_INCREMENT,
                                    analysis_report_id BIGINT NOT NULL,
                                    fraud_type ENUM(
		'UNDERWATER_JEONSE',
		'FALSE_INFORMATION_RIGHTS_CONCEALMENT',
		'TRUST_PROPERTY_FRAUD'
    ) NOT NULL,
                                    risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NOT NULL,

                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

                                    PRIMARY KEY (report_fraud_type_id),

    -- 하나의 리포트에 같은 사기 유형 중복 저장 방지
                                    UNIQUE KEY uk_report_fraud_types_report_type (
                                        analysis_report_id,
                                        fraud_type
                                        ),

                                    CONSTRAINT fk_report_fraud_types_report
                                        FOREIGN KEY (analysis_report_id)
                                            REFERENCES analysis_reports (analysis_report_id)
                                            ON DELETE CASCADE
                                            ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 8. 전세사기 유형 상세 결과
-- =========================================================

CREATE TABLE report_fraud_detail_results (
                                             report_fraud_detail_result_id BIGINT NOT NULL AUTO_INCREMENT,
                                             report_fraud_type_id BIGINT NOT NULL,
                                             detail_type ENUM(
        'HIGH_JEONSE_RATIO',
        'PRIORITY_DEBT_BURDEN',
        'HUG_GUARANTEE_PRECHECK',

        'LAND_BUILDING_OWNERSHIP_MISMATCH',
        'FALSE_BUILDING_USE_INFORMATION',
        'RIGHTS_INFRINGEMENT_CONCEALMENT',

        'TRUST_REGISTRATION_EXISTENCE',
        'REGISTERED_OWNER_VERIFICATION',
        'POST_TRUST_RIGHTS_INFRINGEMENT'
    ) NOT NULL,
                                             risk_level ENUM(
        'SAFE',
        'CAUTION',
        'DANGER'
    ) NOT NULL,

                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                 ON UPDATE CURRENT_TIMESTAMP,

                                             PRIMARY KEY (report_fraud_detail_result_id),
                                             UNIQUE KEY uk_fraud_detail_results_type (
                                                 report_fraud_type_id,
                                                 detail_type
                                                 ),

                                             CONSTRAINT fk_fraud_type_detail_fraud_type
                                                 FOREIGN KEY (report_fraud_type_id)
                                                     REFERENCES report_fraud_types (report_fraud_type_id)
                                                     ON DELETE CASCADE
                                                     ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 9. 체크리스트 기본 항목
-- =========================================================

CREATE TABLE checklist_items (
                                 checklist_item_id BIGINT NOT NULL AUTO_INCREMENT,
                                 contents VARCHAR(500) NOT NULL,
                                 category ENUM(
        'COMMON',
        'MULTI_FAMILY',
        'SINGLE_FAMILY',
        'APARTMENT',
        'MULTI_HOUSEHOLD',
        'OFFICETEL',
        'TRUST_PROPERTY'
    ) NOT NULL,

                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,

                                 PRIMARY KEY (checklist_item_id),
                                 UNIQUE KEY uk_checklist_items_category_contents (
                                     category,
                                     contents
                                     ),
                                 KEY idx_checklist_items_category (category)
) ENGINE = InnoDB;

-- =========================================================
-- 체크리스트 초기 데이터
-- =========================================================


INSERT INTO checklist_items (contents, category) VALUES
                                                     -- 공통 체크리스트
                                                     ('등기부등본 확인', 'COMMON'),
                                                     ('건축물대장 확인', 'COMMON'),
                                                     ('전세가율 확인', 'COMMON'),
                                                     ('HUG/HF/SGI 보증보험 가능 여부 확인', 'COMMON'),
                                                     ('국세·지방세 체납 확인', 'COMMON'),
                                                     ('잔금 지급 직전 등기부 재확인', 'COMMON'),

                                                     -- 다가구주택
                                                     ('선순위 임차인 보증금', 'MULTI_FAMILY'),
                                                     ('확정일자 부여현황', 'MULTI_FAMILY'),
                                                     ('전입세대확인서', 'MULTI_FAMILY'),
                                                     ('국세·지방세 체납', 'MULTI_FAMILY'),
                                                     ('잔금 전 등기부 재확인', 'MULTI_FAMILY'),

                                                     -- 단독주택
                                                     ('건물·토지 소유자 동일 여부', 'SINGLE_FAMILY'),
                                                     ('위반건축물 여부', 'SINGLE_FAMILY'),
                                                     ('대리계약 여부', 'SINGLE_FAMILY'),
                                                     ('국세·지방세 체납', 'SINGLE_FAMILY'),
                                                     ('잔금 전 등기부 재확인', 'SINGLE_FAMILY'),

                                                     -- 아파트
                                                     ('권리관계 확인', 'APARTMENT'),
                                                     ('전세가율', 'APARTMENT'),
                                                     ('보증보험 가입 가능 여부', 'APARTMENT'),
                                                     ('국세·지방세 체납', 'APARTMENT'),
                                                     ('잔금 전 등기부 재확인', 'APARTMENT'),

                                                     -- 다세대·연립
                                                     ('공동근저당', 'MULTI_HOUSEHOLD'),
                                                     ('위반건축물', 'MULTI_HOUSEHOLD'),
                                                     ('전세가율', 'MULTI_HOUSEHOLD'),
                                                     ('보증보험 가입 가능 여부', 'MULTI_HOUSEHOLD'),
                                                     ('국세·지방세 체납', 'MULTI_HOUSEHOLD'),
                                                     ('잔금 전 등기부 재확인', 'MULTI_HOUSEHOLD'),

                                                     -- 오피스텔
                                                     ('주거용/업무용 여부', 'OFFICETEL'),
                                                     ('신탁등기 여부', 'OFFICETEL'),
                                                     ('신탁회사 동의', 'OFFICETEL'),
                                                     ('보증보험 가입 가능 여부', 'OFFICETEL'),
                                                     ('국세·지방세 체납', 'OFFICETEL'),
                                                     ('잔금 전 등기부 재확인', 'OFFICETEL'),

                                                     -- 신탁주택
                                                     ('신탁등기 여부', 'TRUST_PROPERTY'),
                                                     ('신탁원부', 'TRUST_PROPERTY'),
                                                     ('신탁회사 동의', 'TRUST_PROPERTY'),
                                                     ('보증보험 가입 가능 여부', 'TRUST_PROPERTY'),
                                                     ('국세·지방세 체납', 'TRUST_PROPERTY'),
                                                     ('잔금 전 등기부 재확인', 'TRUST_PROPERTY');



-- =========================================================
-- 10. 회원별 체크리스트 상태
-- =========================================================

CREATE TABLE account_checklist_items (
                                         account_checklist_item_id BIGINT NOT NULL AUTO_INCREMENT,
                                         account_id BIGINT NOT NULL,
                                         checklist_item_id BIGINT NOT NULL,
                                         is_checked BOOLEAN NOT NULL DEFAULT FALSE,

                                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,

                                         PRIMARY KEY (account_checklist_item_id),

    -- 같은 회원에게 같은 체크리스트가 중복 생성되는 것 방지
                                         UNIQUE KEY uk_account_checklist_items_account_item (
                                             account_id,
                                             checklist_item_id
                                             ),

                                         KEY idx_account_checklist_items_item_id (checklist_item_id),

                                         CONSTRAINT fk_account_checklist_items_account
                                             FOREIGN KEY (account_id)
                                                 REFERENCES accounts (account_id)
                                                 ON DELETE CASCADE
                                                 ON UPDATE CASCADE,

                                         CONSTRAINT fk_account_checklist_items_item
                                             FOREIGN KEY (checklist_item_id)
                                                 REFERENCES checklist_items (checklist_item_id)
                                                 ON DELETE CASCADE
                                                 ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 11. 행정구역
-- =========================================================

CREATE TABLE regions (
                         region_id BIGINT NOT NULL AUTO_INCREMENT,
                         region_code VARCHAR(20) NOT NULL,
                         region_name VARCHAR(100) NOT NULL,
                         region_level ENUM(
        'SIDO',
        'SIGUNGU',
        'EUPMYEONDONG'
    ) NOT NULL,
                         parent_region_id BIGINT NULL,

                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP,

                         PRIMARY KEY (region_id),
                         UNIQUE KEY uk_regions_region_code (region_code),
                         KEY idx_regions_parent_region_id (parent_region_id),
                         KEY idx_regions_name_level (region_name, region_level),

                         CONSTRAINT fk_regions_parent
                             FOREIGN KEY (parent_region_id)
                                 REFERENCES regions (region_id)
                                 ON DELETE SET NULL
                                 ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 12. HUG 전세사기 피해주택 통계
-- =========================================================

CREATE TABLE fraud_damage_statistics (
                                         fraud_damage_statistic_id BIGINT NOT NULL AUTO_INCREMENT,
                                         region_id BIGINT NOT NULL,
                                         damage_house_count INT NOT NULL DEFAULT 0,
                                         base_date DATE NOT NULL,

                                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,

                                         PRIMARY KEY (fraud_damage_statistic_id),

    -- 같은 지역과 기준일의 데이터 중복 방지
                                         UNIQUE KEY uk_fraud_damage_statistics_region_date (
                                             region_id,
                                             base_date
                                             ),

                                         CONSTRAINT chk_fraud_damage_statistics_count
                                             CHECK (damage_house_count >= 0),

                                         CONSTRAINT fk_fraud_damage_statistics_region
                                             FOREIGN KEY (region_id)
                                                 REFERENCES regions (region_id)
                                                 ON DELETE RESTRICT
                                                 ON UPDATE CASCADE
) ENGINE = InnoDB;


-- =========================================================
-- 13. 한국부동산원 전세가격지수
-- =========================================================

CREATE TABLE jeonse_price_indices (
                                      jeonse_price_index_id BIGINT NOT NULL AUTO_INCREMENT,
                                      region_id BIGINT NOT NULL,
                                      base_month DATE NOT NULL,
                                      price_index DECIMAL(10, 4) NOT NULL,
                                      change_rate DECIMAL(10, 4) NULL,

                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      modified_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,

                                      PRIMARY KEY (jeonse_price_index_id),

    -- 한 지역에 같은 기준 월 데이터 중복 방지
                                      UNIQUE KEY uk_jeonse_price_indices_region_month (
                                          region_id,
                                          base_month
                                          ),

                                      CONSTRAINT chk_jeonse_price_indices_price
                                          CHECK (price_index >= 0),

                                      CONSTRAINT fk_jeonse_price_indices_region
                                          FOREIGN KEY (region_id)
                                              REFERENCES regions (region_id)
                                              ON DELETE RESTRICT
                                              ON UPDATE CASCADE
) ENGINE = InnoDB;

-- =========================================================
-- 시도 17개
-- =========================================================
INSERT INTO regions (
    region_code,
    region_name,
    region_level,
    parent_region_id
)
VALUES
    ('11', '서울특별시', 'SIDO', NULL),
    ('26', '부산광역시', 'SIDO', NULL),
    ('27', '대구광역시', 'SIDO', NULL),
    ('28', '인천광역시', 'SIDO', NULL),
    ('29', '광주광역시', 'SIDO', NULL),
    ('30', '대전광역시', 'SIDO', NULL),
    ('31', '울산광역시', 'SIDO', NULL),
    ('36', '세종특별자치시', 'SIDO', NULL),
    ('41', '경기도', 'SIDO', NULL),
    ('43', '충청북도', 'SIDO', NULL),
    ('44', '충청남도', 'SIDO', NULL),
    ('46', '전라남도', 'SIDO', NULL),
    ('47', '경상북도', 'SIDO', NULL),
    ('48', '경상남도', 'SIDO', NULL),
    ('50', '제주특별자치도', 'SIDO', NULL),
    ('51', '강원특별자치도', 'SIDO', NULL),
    ('52', '전북특별자치도', 'SIDO', NULL)
    ON DUPLICATE KEY UPDATE
                         region_name = VALUES(region_name),
                         region_level = VALUES(region_level),
                         parent_region_id = VALUES(parent_region_id);
-- =========================================================
-- HUG 통계 대상 시군구 148개
-- =========================================================
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11110', '종로구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11140', '중구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11200', '성동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11215', '광진구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11230', '동대문구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11260', '중랑구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11290', '성북구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11305', '강북구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11320', '도봉구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11350', '노원구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11380', '은평구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11410', '서대문구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11440', '마포구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11470', '양천구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11500', '강서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11530', '구로구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11545', '금천구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11560', '영등포구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11590', '동작구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11620', '관악구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11650', '서초구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11680', '강남구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11710', '송파구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '11740', '강동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '11' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26110', '중구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26140', '서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26170', '동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26200', '영도구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26230', '부산진구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26260', '동래구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26290', '남구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26350', '해운대구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26380', '사하구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26410', '금정구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26440', '강서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26470', '연제구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26500', '수영구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '26530', '사상구', 'SIGUNGU', region_id FROM regions WHERE region_code = '26' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '27110', '중구', 'SIGUNGU', region_id FROM regions WHERE region_code = '27' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '27140', '동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '27' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '27170', '서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '27' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '27230', '북구', 'SIGUNGU', region_id FROM regions WHERE region_code = '27' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '27260', '수성구', 'SIGUNGU', region_id FROM regions WHERE region_code = '27' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '27290', '달서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '27' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '27710', '달성군', 'SIGUNGU', region_id FROM regions WHERE region_code = '27' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28110', '중구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28140', '동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28177', '미추홀구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28185', '연수구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28200', '남동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28237', '부평구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28245', '계양구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28260', '서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '28710', '강화군', 'SIGUNGU', region_id FROM regions WHERE region_code = '28' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '29110', '동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '29' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '29140', '서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '29' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '29155', '남구', 'SIGUNGU', region_id FROM regions WHERE region_code = '29' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '29170', '북구', 'SIGUNGU', region_id FROM regions WHERE region_code = '29' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '29200', '광산구', 'SIGUNGU', region_id FROM regions WHERE region_code = '29' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '30110', '동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '30' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '30140', '중구', 'SIGUNGU', region_id FROM regions WHERE region_code = '30' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '30170', '서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '30' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '30200', '유성구', 'SIGUNGU', region_id FROM regions WHERE region_code = '30' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '30230', '대덕구', 'SIGUNGU', region_id FROM regions WHERE region_code = '30' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '31110', '중구', 'SIGUNGU', region_id FROM regions WHERE region_code = '31' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '31140', '남구', 'SIGUNGU', region_id FROM regions WHERE region_code = '31' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '36110', '세종시', 'SIGUNGU', region_id FROM regions WHERE region_code = '36' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41111', '장안구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41113', '권선구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41115', '팔달구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41117', '영통구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41131', '수정구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41133', '중원구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41135', '분당구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41150', '의정부시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41171', '만안구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41173', '동안구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41192', '원미구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41194', '소사구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41196', '오정구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41220', '평택시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41271', '상록구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41273', '단원구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41281', '덕양구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41285', '일산동구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41287', '일산서구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41310', '구리시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41360', '남양주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41370', '오산시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41390', '시흥시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41410', '군포시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41430', '의왕시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41450', '하남시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41461', '처인구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41463', '기흥구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41465', '수지구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41480', '파주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41500', '이천시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41550', '안성시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41570', '김포시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41593', '효행구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41595', '병점구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41597', '동탄구', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41610', '광주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41630', '양주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41650', '포천시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '41670', '여주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '41' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '43111', '상당구', 'SIGUNGU', region_id FROM regions WHERE region_code = '43' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '43112', '서원구', 'SIGUNGU', region_id FROM regions WHERE region_code = '43' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '43113', '흥덕구', 'SIGUNGU', region_id FROM regions WHERE region_code = '43' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '43114', '청원구', 'SIGUNGU', region_id FROM regions WHERE region_code = '43' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '43130', '충주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '43' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '44133', '서북구', 'SIGUNGU', region_id FROM regions WHERE region_code = '44' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '44200', '아산시', 'SIGUNGU', region_id FROM regions WHERE region_code = '44' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '44210', '서산시', 'SIGUNGU', region_id FROM regions WHERE region_code = '44' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '44230', '논산시', 'SIGUNGU', region_id FROM regions WHERE region_code = '44' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '44270', '당진시', 'SIGUNGU', region_id FROM regions WHERE region_code = '44' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '44810', '예산군', 'SIGUNGU', region_id FROM regions WHERE region_code = '44' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '46150', '순천시', 'SIGUNGU', region_id FROM regions WHERE region_code = '46' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '46170', '나주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '46' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '46230', '광양시', 'SIGUNGU', region_id FROM regions WHERE region_code = '46' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '46720', '곡성군', 'SIGUNGU', region_id FROM regions WHERE region_code = '46' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '46790', '화순군', 'SIGUNGU', region_id FROM regions WHERE region_code = '46' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '46840', '무안군', 'SIGUNGU', region_id FROM regions WHERE region_code = '46' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '46880', '장성군', 'SIGUNGU', region_id FROM regions WHERE region_code = '46' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '47111', '남구', 'SIGUNGU', region_id FROM regions WHERE region_code = '47' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '47113', '북구', 'SIGUNGU', region_id FROM regions WHERE region_code = '47' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '47130', '경주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '47' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '47150', '김천시', 'SIGUNGU', region_id FROM regions WHERE region_code = '47' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '47190', '구미시', 'SIGUNGU', region_id FROM regions WHERE region_code = '47' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '48123', '성산구', 'SIGUNGU', region_id FROM regions WHERE region_code = '48' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '48127', '마산회원구', 'SIGUNGU', region_id FROM regions WHERE region_code = '48' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '48170', '진주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '48' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '48240', '사천시', 'SIGUNGU', region_id FROM regions WHERE region_code = '48' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '48250', '김해시', 'SIGUNGU', region_id FROM regions WHERE region_code = '48' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '48310', '거제시', 'SIGUNGU', region_id FROM regions WHERE region_code = '48' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '50110', '제주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '50' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '51110', '춘천시', 'SIGUNGU', region_id FROM regions WHERE region_code = '51' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '51130', '원주시', 'SIGUNGU', region_id FROM regions WHERE region_code = '51' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '51150', '강릉시', 'SIGUNGU', region_id FROM regions WHERE region_code = '51' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '51720', '홍천군', 'SIGUNGU', region_id FROM regions WHERE region_code = '51' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '51770', '정선군', 'SIGUNGU', region_id FROM regions WHERE region_code = '51' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '51780', '철원군', 'SIGUNGU', region_id FROM regions WHERE region_code = '51' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '52111', '완산구', 'SIGUNGU', region_id FROM regions WHERE region_code = '52' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '52113', '덕진구', 'SIGUNGU', region_id FROM regions WHERE region_code = '52' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '52130', '군산시', 'SIGUNGU', region_id FROM regions WHERE region_code = '52' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '52140', '익산시', 'SIGUNGU', region_id FROM regions WHERE region_code = '52' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
INSERT INTO regions (region_code, region_name, region_level, parent_region_id) SELECT '52710', '완주군', 'SIGUNGU', region_id FROM regions WHERE region_code = '52' AND region_level = 'SIDO' ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), region_level = VALUES(region_level), parent_region_id = VALUES(parent_region_id);
-- =========================================================
-- HUG 피해주택 통계 148개
-- =========================================================
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 5, '2025-12-31' FROM regions WHERE region_code = '11110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 14, '2025-12-31' FROM regions WHERE region_code = '11140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '11200' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 12, '2025-12-31' FROM regions WHERE region_code = '11215' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 30, '2025-12-31' FROM regions WHERE region_code = '11230' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 26, '2025-12-31' FROM regions WHERE region_code = '11260' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 19, '2025-12-31' FROM regions WHERE region_code = '11290' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 22, '2025-12-31' FROM regions WHERE region_code = '11305' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 22, '2025-12-31' FROM regions WHERE region_code = '11320' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '11350' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 35, '2025-12-31' FROM regions WHERE region_code = '11380' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '11410' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 24, '2025-12-31' FROM regions WHERE region_code = '11440' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 34, '2025-12-31' FROM regions WHERE region_code = '11470' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 156, '2025-12-31' FROM regions WHERE region_code = '11500' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 62, '2025-12-31' FROM regions WHERE region_code = '11530' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 75, '2025-12-31' FROM regions WHERE region_code = '11545' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 31, '2025-12-31' FROM regions WHERE region_code = '11560' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 18, '2025-12-31' FROM regions WHERE region_code = '11590' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 63, '2025-12-31' FROM regions WHERE region_code = '11620' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 5, '2025-12-31' FROM regions WHERE region_code = '11650' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '11680' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 19, '2025-12-31' FROM regions WHERE region_code = '11710' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 23, '2025-12-31' FROM regions WHERE region_code = '11740' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '26110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '26140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '26170' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '26200' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 15, '2025-12-31' FROM regions WHERE region_code = '26230' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 8, '2025-12-31' FROM regions WHERE region_code = '26260' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 5, '2025-12-31' FROM regions WHERE region_code = '26290' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '26350' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '26380' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 8, '2025-12-31' FROM regions WHERE region_code = '26410' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '26440' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '26470' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '26500' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '26530' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '27110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '27140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '27170' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '27230' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '27260' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 15, '2025-12-31' FROM regions WHERE region_code = '27290' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '27710' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '28110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '28140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 63, '2025-12-31' FROM regions WHERE region_code = '28177' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '28185' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 38, '2025-12-31' FROM regions WHERE region_code = '28200' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 53, '2025-12-31' FROM regions WHERE region_code = '28237' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 22, '2025-12-31' FROM regions WHERE region_code = '28245' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 29, '2025-12-31' FROM regions WHERE region_code = '28260' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '28710' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '29110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 5, '2025-12-31' FROM regions WHERE region_code = '29140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '29155' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '29170' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 34, '2025-12-31' FROM regions WHERE region_code = '29200' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '30110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '30140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '30170' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 19, '2025-12-31' FROM regions WHERE region_code = '30200' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '30230' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '31110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '31140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 33, '2025-12-31' FROM regions WHERE region_code = '36110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '41111' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 15, '2025-12-31' FROM regions WHERE region_code = '41113' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 16, '2025-12-31' FROM regions WHERE region_code = '41115' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 9, '2025-12-31' FROM regions WHERE region_code = '41117' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '41131' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 15, '2025-12-31' FROM regions WHERE region_code = '41133' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '41135' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 17, '2025-12-31' FROM regions WHERE region_code = '41150' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 15, '2025-12-31' FROM regions WHERE region_code = '41171' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '41173' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 32, '2025-12-31' FROM regions WHERE region_code = '41192' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '41194' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '41196' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 8, '2025-12-31' FROM regions WHERE region_code = '41220' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 21, '2025-12-31' FROM regions WHERE region_code = '41271' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '41273' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 8, '2025-12-31' FROM regions WHERE region_code = '41281' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '41285' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 5, '2025-12-31' FROM regions WHERE region_code = '41287' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '41310' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '41360' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 12, '2025-12-31' FROM regions WHERE region_code = '41370' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 13, '2025-12-31' FROM regions WHERE region_code = '41390' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '41410' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '41430' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 12, '2025-12-31' FROM regions WHERE region_code = '41450' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 14, '2025-12-31' FROM regions WHERE region_code = '41461' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 15, '2025-12-31' FROM regions WHERE region_code = '41463' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '41465' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 18, '2025-12-31' FROM regions WHERE region_code = '41480' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 10, '2025-12-31' FROM regions WHERE region_code = '41500' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '41550' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 16, '2025-12-31' FROM regions WHERE region_code = '41570' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '41593' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 9, '2025-12-31' FROM regions WHERE region_code = '41595' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 61, '2025-12-31' FROM regions WHERE region_code = '41597' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '41610' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '41630' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '41650' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '41670' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '43111' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '43112' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '43113' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '43114' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '43130' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '44133' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '44200' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '44210' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '44230' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '44270' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '44810' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 25, '2025-12-31' FROM regions WHERE region_code = '46150' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 14, '2025-12-31' FROM regions WHERE region_code = '46170' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 38, '2025-12-31' FROM regions WHERE region_code = '46230' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '46720' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '46790' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '46840' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '46880' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '47111' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 5, '2025-12-31' FROM regions WHERE region_code = '47113' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '47130' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '47150' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 6, '2025-12-31' FROM regions WHERE region_code = '47190' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '48123' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '48127' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 5, '2025-12-31' FROM regions WHERE region_code = '48170' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '48240' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 3, '2025-12-31' FROM regions WHERE region_code = '48250' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '48310' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '50110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '51110' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '51130' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '51150' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '51720' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '51770' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '51780' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '52111' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 7, '2025-12-31' FROM regions WHERE region_code = '52113' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 1, '2025-12-31' FROM regions WHERE region_code = '52130' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 4, '2025-12-31' FROM regions WHERE region_code = '52140' AND region_level = 'SIGUNGU';
INSERT INTO fraud_damage_statistics (region_id, damage_house_count, base_date) SELECT region_id, 2, '2025-12-31' FROM regions WHERE region_code = '52710' AND region_level = 'SIGUNGU';
