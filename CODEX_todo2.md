## ✅ 요청 결과 요약
- 요청대로 OOP 원칙/리팩토링 관점에서 개선 포인트를 목록으로 정리했습니다.

## 🧭 구조 요약
- 앱 엔트리: `src/main/java/com/example/aisearch/AiSearchGptApplication.java`, 배치 전용 엔트리: `src/main/java/com/example/aisearch/IndexingApplication.java`
- 설정/연결: `src/main/java/com/example/aisearch/config/ElasticsearchConfig.java`, `src/main/java/com/example/aisearch/support/ElasticsearchAutoConnector.java`, `src/main/java/com/example/aisearch/support/ElasticsearchK8sHelper.java`
- 핵심 서비스: 임베딩 `src/main/java/com/example/aisearch/service/DjlEmbeddingService.java`, 인덱스 관리 `src/main/java/com/example/aisearch/service/IndexManagementService.java`, 색인 `src/main/java/com/example/aisearch/service/ProductIndexingService.java`, 검색 `src/main/java/com/example/aisearch/service/VectorSearchService.java`
- 데이터/모델: `src/main/java/com/example/aisearch/service/FoodDataLoader.java`, `src/main/java/com/example/aisearch/model/FoodProduct.java`, `src/main/java/com/example/aisearch/model/SearchHitResult.java`
- API: `src/main/java/com/example/aisearch/controller/SearchController.java`
- 부트스트랩: `src/main/java/com/example/aisearch/service/BootstrapIndexer.java`

## ⚠️ SOLID 위반/개선 후보 리스트

### 1. SRP 위반 가능
- `src/main/java/com/example/aisearch/support/ElasticsearchAutoConnector.java`  
  - 포트포워딩 판단
  - kubectl 검사/호출
  - 비밀번호 로딩
  - 연결 URL 조합까지 한 클래스가 담당
  - 책임이 너무 넓음
- `src/main/java/com/example/aisearch/service/DjlEmbeddingService.java`  
  - 모델 로딩 정책(경로/URL)
  - 예측 수행
  - 정규화
  - 리소스 관리까지 혼재
- `src/main/java/com/example/aisearch/service/ProductIndexingService.java`  
  - 데이터 로딩
  - 임베딩 생성
  - 문서 매핑
  - 벌크 요청/실행이 한 클래스에 결합

### 2. OCP 위반 가능
- `src/main/java/com/example/aisearch/support/ElasticsearchK8sHelper.java`  
  - kubectl 실행 로직이 고정
  - 다른 실행 수단(예: k8s API) 추가 시 수정 필요
- `src/main/java/com/example/aisearch/service/VectorSearchService.java`  
  - 검색 전략이 knn으로 하드코딩
  - 다른 랭킹/필터 전략 확장 시 클래스 수정 필요
- `src/main/java/com/example/aisearch/service/IndexSchemaBuilder.java`  
  - 매핑이 문자열 템플릿으로 고정
  - 스키마 진화 시 잦은 수정 필요

### 3. DIP 위반 가능
- `src/main/java/com/example/aisearch/support/ElasticsearchAutoConnector.java`  
  - 내부에서 ElasticsearchK8sHelper 정적 호출에 직접 의존
  - 대체 구현/테스트 더블 주입 불가
- `src/main/java/com/example/aisearch/service/ProductIndexingService.java`  
  - 문서 구성 맵이 서비스 내부에 고정
  - 매핑 변경 시 서비스 수정 필요

### 4. ISP 관점 개선 여지
- `src/main/java/com/example/aisearch/service/EmbeddingService.java`  
  - 최소 인터페이스라 문제는 크지 않음
  - 모델 로딩/자원 관리 책임이 구현체에 과도하게 집중

## 🧪 리팩토링/디자인 패턴 적용 추천

### 1. ElasticsearchAutoConnector 책임 분리
- 추천: PortForwardManager, SecretPasswordProvider, ConnectionResolver로 분리
- 패턴: Strategy + Adapter
- 효과: 로컬/쿠버네티스/직접 연결 전략 분리로 테스트와 확장 용이
- 대상: `src/main/java/com/example/aisearch/support/ElasticsearchAutoConnector.java`, `src/main/java/com/example/aisearch/support/ElasticsearchK8sHelper.java`

### 2. 임베딩 로딩 정책 분리
- 추천: EmbeddingModelProvider(URL, Path 구현) + EmbeddingNormalizer
- 패턴: Strategy
- 효과: 모델 로딩/정규화 변경이 서비스 수정 없이 가능
- 대상: `src/main/java/com/example/aisearch/service/DjlEmbeddingService.java`

### 3. 색인 파이프라인 분리
- 추천: FoodDataLoader와 DocumentMapper(FoodProduct -> Map) 분리, 색인 실행은 IndexingExecutor
- 패턴: Pipeline / Mapper
- 효과: 문서 스키마 변경 시 mapper만 수정
- 대상: `src/main/java/com/example/aisearch/service/ProductIndexingService.java`

### 4. 검색 전략 분리
- 추천: SearchStrategy 인터페이스 + KnnSearchStrategy 구현
- 패턴: Strategy
- 효과: 필터 기반/하이브리드 검색 도입 시 확장 용이
- 대상: `src/main/java/com/example/aisearch/service/VectorSearchService.java`

### 5. 인덱스 스키마 빌더 개선
- 추천: 스키마를 DSL/객체 빌더로 분리하거나 파일 기반 JSON으로 외부화
- 패턴: Builder
- 효과: 스키마 변경 시 코드 수정 최소화, 가독성 향상
- 대상: `src/main/java/com/example/aisearch/service/IndexSchemaBuilder.java`

### 6. 배치 전용 엔트리 개선
- 추천: IndexingApplication은 SpringApplication 설정을 @Profile 또는 @ConditionalOnProperty 기반으로 통합
- 패턴: Configuration Profile
- 효과: 두 개의 main 클래스 유지 비용 감소
- 대상: `src/main/java/com/example/aisearch/IndexingApplication.java`, `src/main/java/com/example/aisearch/service/BootstrapIndexer.java`

---
원하면 위 항목 중 우선순위 정해서 실제 리팩토링까지 진행할게.


1. 콘솔 출력/사용하지 않는 import 제거

- 파일: src/main/java/com/example/aisearch/service/DjlEmbeddingService.java
- 문제: import java.io.Console; 미사용, System.out.println 직접 출력은 로깅 정책과 혼재.
- 개선: Logger로 교체하거나 제거. (SRP/일관된 로깅)

2. SearchStrategy 확장성 보강

- 파일: src/main/java/com/example/aisearch/service/SearchStrategy.java, src/main/java/com/example/aisearch/service/KnnSearchStrategy.java
- 문제: 현재 단일 구현만 존재 → 실제 확장 요구가 없다면 과설계 가능.
- 개선 선택지:
  - 확장 계획이 있다면 유지 (Strategy 패턴 정당화)
  - 없다면 VectorSearchService로 단순화 (YAGNI)

3. ElasticsearchK8sHelper 정적 의존 최소화

- 파일: src/main/java/com/example/aisearch/support/ElasticsearchK8sHelper.java, src/main/java/com/example/aisearch/support/K8sPortForwarder.java
- 문제: 정적 유틸 사용으로 DIP 위반, 테스트/대체 구현 어려움.
- 개선: K8sClient 인터페이스 도입 + 구현체로 감싸기 (Adapter)

4. IndexSchemaBuilder 템플릿 캐싱

- 파일: src/main/java/com/example/aisearch/service/IndexSchemaBuilder.java
- 문제: 매번 파일 읽기 → 호출 빈도가 늘면 비효율.
- 개선: 템플릿 내용을 캐시하거나 @PostConstruct 로딩.

5. EmbeddingModelSourceResolver 책임 명확화

- 파일: src/main/java/com/example/aisearch/service/EmbeddingModelSourceResolver.java
- 문제: ResourceLoader 전달 방식이 다소 어색 (서비스가 다시 주입받는 구조).
- 개선: ResourceLoader를 필드로 주입해 의존성 정리.

6. IndexManagementService 실패/복구 전략

- 파일: src/main/java/com/example/aisearch/service/IndexManagementService.java
- 문제: 인덱스 삭제/생성 실패 시 롤백 없음.
- 개선: 템플릿 기반 재생성 대신 별도 “create if absent / update mapping” 전략 분리 (Command 패턴 또는 정책 분리)

7. BulkIndexingExecutor 응답 오류 상세화

- 파일: src/main/java/com/example/aisearch/service/BulkIndexingExecutor.java
- 문제: response.errors()만 체크하고 구체 실패 원인 누락.
- 개선: 실패 item 요약 로그/예외 메시지에 포함.

8. FoodProductDocumentMapper 필드명 상수화

- 파일: src/main/java/com/example/aisearch/service/FoodProductDocumentMapper.java
- 문제: 문자열 키 하드코딩 → 매핑/검색과 중복.
- 개선: IndexFields 상수 클래스로 통합 (DRY)

9. ElasticsearchAutoConnector 예외 정책

- 파일: src/main/java/com/example/aisearch/support/ElasticsearchAutoConnector.java
- 문제: 모든 예외를 동일 처리 후 폴백. 실패 원인 구분 어려움.
- 개선: kubectl 미존재/권한/포트 충돌 등 예외 케이스별 로그 분리.

10. application-indexing.yml 사용 가이드 추가

- 파일: src/main/resources/application-indexing.yml
- 문제: 배치 실행 방법이 코드 밖에 있음.
- 개선: README에 실행 커맨드 추가 (문서화)
