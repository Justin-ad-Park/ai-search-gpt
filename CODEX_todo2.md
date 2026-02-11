아래는 KnnSearchStrategy에 대한 개선 포인트야. (리팩토링/OOP/패턴 관점)

개선 포인트

1. 임베딩 생성 책임 분리

- 현재: KnnSearchStrategy가 임베딩 생성까지 담당
- 개선: QueryVectorProvider 같은 별도 컴포넌트로 분리
  → 검색 전략은 “검색 실행”에 집중 (SRP)

2. 검색 파라미터 캡슐화

- 현재: size, numCandidates 계산이 메서드 내부에 고정
- 개선: SearchOptions/KnnSearchOptions 객체로 캡슐화
  → 테스트와 확장에 유리 (OCP)

3. 결과 변환(Strip Vector) 분리

- 현재: 결과 필터링/변환 로직이 전략 내부
- 개선: SearchHitMapper 또는 ResultSanitizer로 분리
  → 관심사 분리 (SRP)

4. 에러 메시지 정보 보강

- 현재: IllegalStateException("벡터 검색 실패") 단일 메시지
- 개선: indexName, query size 등 최소 컨텍스트 포함
  → 운영 디버깅 쉬움

5. raw Map 사용 축소

- 현재: SearchResponse<Map> / Map<String, Object>
- 개선: 명시적 DTO 또는 JsonData 활용
  → 타입 안정성 향상

6. 전략 객체가 “전략”답게 되려면

- 실제로 다른 전략이 없으면 Strategy 패턴은 과설계일 수 있음
- 방향성 선택:
  - 확장 계획 있음 → 유지
  - 확장 계획 없음 → VectorSearchService로 통합
