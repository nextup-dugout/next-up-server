---
name: Discussion / Meeting
about: 팀원들과 논의하거나 의사결정이 필요한 내용이 있나요?
labels: discussion
---

## 💬 논의 주제
<!-- 논의하고자 하는 주제를 명확하게 작성해주세요 -->

**예시:**
> QueryDSL vs JPA Criteria - 복잡한 쿼리 작성에 어떤 기술을 사용할지 결정이 필요합니다.

## 🤔 배경 및 맥락
<!-- 왜 이 논의가 필요한지, 배경 상황을 설명해주세요 -->

**예시:**
> 현재 선수 통계 조회 API에서 복잡한 조건 검색이 필요합니다.
> - 포지션별 필터링
> - 타율/출루율 범위 검색
> - 경기 날짜 범위 검색
> - 정렬 조건 동적 적용
>
> JPA로는 동적 쿼리 작성이 어려워 다른 방안을 검토해야 합니다.

## 🔍 논의 사항
<!-- 구체적으로 논의하고 싶은 내용을 작성해주세요 -->

**예시:**
1. **QueryDSL 도입 시**
   - 장점: 타입 안정성, 가독성, IDE 지원
   - 단점: 학습 곡선, Q 클래스 생성 필요

2. **JPA Criteria 사용 시**
   - 장점: 별도 라이브러리 불필요
   - 단점: 가독성 낮음, 복잡한 쿼리 작성 어려움

3. **Spring Data JPA Specifications**
   - 장점: Spring Data와 자연스러운 통합
   - 단점: 복잡한 쿼리에서 한계

## 📋 결정이 필요한 사항
<!-- 이 논의를 통해 결정해야 할 항목들을 나열해주세요 -->

**예시:**
- [ ] 복잡한 쿼리 작성 기술 스택 결정
- [ ] ADR(Architecture Decision Record) 작성 담당자 지정
- [ ] POC(Proof of Concept) 일정 수립
- [ ] 기술 스택 변경에 따른 영향 범위 분석

## 📅 희망 일정
<!-- 언제까지 논의/결정이 필요한지 작성해주세요 -->

**예시:**
- 논의 희망 일자: 2026-01-25 (금)
- 결정 데드라인: 2026-01-27 (일)
- 이유: Phase 2 개발 시작 전 기술 스택 확정 필요

## 🔗 참고 자료
<!-- 관련 문서, 레퍼런스, 이슈 등을 추가해주세요 -->

**예시:**
- [QueryDSL 공식 문서](https://querydsl.com/)
- [Spring Data JPA Specifications](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications)
- 관련 이슈: #15
- 벤치마크 자료: https://...
