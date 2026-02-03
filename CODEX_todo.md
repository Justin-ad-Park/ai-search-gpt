• 요청대로 OOP 원칙/리팩토링 관점에서 개선 포인트를 목록으로 정리했습니다.

1. ~~SRP(단일 책임) 위반~~
   - VectorIndexService가 인덱스 생성 + 색인 + 검색 + 결과 매핑까지 모두 담당
     → 인덱싱과 검색을 분리 필요 ~~

2. 역할 분리 미흡
   - ES 매핑 생성 로직(mapping 문자열)까지 서비스 내부에 포함
     → IndexSchemaBuilder 같은 전용 클래스로 분리
   
3. 검색 결과 모델링 부족
   - SearchHitResult가 Map<String,Object>를 그대로 노출
     → 도메인 DTO로 변환하거나 전용 결과 모델 설계 필요
4. 매직 넘버/상수 하드코딩
   - k, numCandidates, MIN_SCORE_THRESHOLD 등 하드코딩
     → application.yml 또는 설정 클래스로 이동 권장
5. EmbeddingService 책임 혼합
   - DjlEmbeddingService가 모델 로딩 + 추론 + 정규화까지 담당
     → 정규화/후처리를 별도 유틸/전략으로 분리 가능
6. 테스트 코드 재사용성 부족
   - 테스트에서 출력/검증 로직이 길어짐
     → helper 클래스로 분리(일부 개선했지만 더 분리 가능)
7. Controller 반환 구조
   - SearchController가 Map<String,Object> 반환
     → 전용 응답 DTO로 모델링하면 타입 안정성↑
8. 데이터 로더 책임
   - FoodDataLoader가 JSON만 읽게 고정
     → 인터페이스로 분리하면 DB/CSV 확장 쉬움
9. 의존성 방향
   - VectorIndexService가 FoodDataLoader에 직접 의존
     → 인덱싱 파이프라인(예: ProductSource) 추상화 필요
10. 응집도 낮은 설정 방식
