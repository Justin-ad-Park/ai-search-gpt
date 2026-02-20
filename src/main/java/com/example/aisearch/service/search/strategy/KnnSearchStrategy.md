# KnnSearchStrategy 람다 체인 상세 설명

## 1. 대상 코드
`KnnSearchStrategy#vectorScoreSearch(...)` 안의 아래 코드입니다.

```java
SearchResponse<Map> response = client.search(s -> s
                .index(resolveReadAlias())
                .query(q -> q.scriptScore(ss -> ss
                        .query(baseQuery)
                        .script(sc -> sc.inline(i -> i
                                .lang("painless")
                                .source(
                                        "double vectorScore = (cosineSimilarity(params.query_vector, 'product_vector') + 1.0) / 2.0; " +
                                        "double lexicalScore = Math.min(_score, 5.0) / 5.0; " +
                                        "return Math.min(1.0, 0.9 * vectorScore + 0.1 * lexicalScore + 0.1);"
                                )
                                .params("query_vector", JsonData.of(queryVector))
                        ))
                ))
                .sort(request.sortOption().toSortOptions())
                .trackScores(true)
                .from(from)
                .size(size)
                .minScore(properties.minScoreThreshold()),
        Map.class
);
```

---

## 2. 핵심 개념: 왜 람다를 쓰는가?
Elasticsearch Java Client는 요청 객체를 직접 `new`해서 조립하기보다,
`Builder`를 람다로 채우는 DSL 스타일을 제공합니다.

- `s -> s ...` : `SearchRequest.Builder`를 채우는 함수
- `q -> q ...` : `Query.Builder`를 채우는 함수
- `ss -> ss ...` : `ScriptScoreQuery.Builder`를 채우는 함수
- `sc -> sc ...` : `Script.Builder`를 채우는 함수
- `i -> i ...` : `InlineScript.Builder`를 채우는 함수

즉, 람다 체인 전체가 "SearchRequest를 만드는 명세"입니다.

---

## 3. 사용자 질문의 메서드와 연결
질문에 주신 메서드:

```java
public final <TDocument> SearchResponse<TDocument> search(
        Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> fn,
        Class<TDocument> tDocumentClass
) throws IOException, ElasticsearchException {
    return search(fn.apply(new SearchRequest.Builder()).build(), tDocumentClass);
}
```

이 코드에서 실제 흐름은 다음과 같습니다.

1. `client.search(람다, Map.class)` 호출
2. `람다`가 위 메서드의 `fn` 파라미터로 들어감
3. 클라이언트 내부에서 `new SearchRequest.Builder()` 생성
4. `fn.apply(builder)` 실행 -> 우리가 작성한 `s -> s...` 체인 수행
5. 최종 `SearchRequest` 객체 생성(`build()`)
6. 생성된 요청으로 실제 `search(request, Map.class)` 실행
7. 응답 JSON `_source`를 `Map`으로 역직렬화하여 `SearchResponse<Map>` 반환

정리하면,
- 우리가 작성한 람다는 "검색 실행 코드"가 아니라
- **검색 요청 객체를 만드는 함수**이며
- 실제 실행은 내부 오버로드 `search(request, tDocumentClass)`에서 수행됩니다.

---

## 4. 람다 체인 한 줄씩 해석

### 4.1 최상위 `s -> s`
- 타입: `Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>`
- 역할: SearchRequest 전체를 조립

### 4.2 `.index(resolveReadAlias())`
- 검색 대상 인덱스를 alias로 지정
- 롤아웃 시 물리 인덱스 변경과 검색 코드를 분리

### 4.3 `.query(q -> q.scriptScore(...))`
- 기본 쿼리 점수를 스크립트 점수로 재계산하는 `script_score` 쿼리 사용

### 4.4 `.query(baseQuery)`
- `script_score`의 입력 쿼리
- 여기에서 `_score`(lexical score)가 만들어짐

### 4.5 `.script(sc -> sc.inline(i -> i ...))`
- 스크립트 언어: `painless`
- 점수식:
  - `vectorScore = (cosineSimilarity(...) + 1.0) / 2.0`
  - `lexicalScore = min(_score, 5.0) / 5.0`
  - `final = min(1.0, 0.9*vectorScore + 0.1*lexicalScore + 0.1)`

### 4.6 `.params("query_vector", JsonData.of(queryVector))`
- Java `List<Float>`를 painless 파라미터로 전달
- 스크립트 내부 `params.query_vector`로 참조

### 4.7 `.sort(...)`
- 정렬 조건 적용
- 정렬 기준이 `_score`가 아니면 결과 순서는 정렬 필드 우선

### 4.8 `.trackScores(true)`
- `_score` 정렬이 아니더라도 점수 계산/반환 유지

### 4.9 `.from(from).size(size)`
- 페이지 오프셋/사이즈

### 4.10 `.minScore(properties.minScoreThreshold())`
- 임계값 미만 점수 문서를 Elasticsearch 레벨에서 제거

---

## 5. 점수 공식 의도

### 5.1 `vectorScore`
- 코사인 유사도 범위 `[-1,1]`를 `[0,1]`로 정규화

### 5.2 `lexicalScore`
- `_score`를 최대 5로 캡핑 후 0~1 스케일로 축소
- lexical score가 과도하게 지배하지 않게 제어

### 5.3 가중합
- `0.9 * vector + 0.1 * lexical`:
  - 의미 유사도 중심
  - 키워드 일치는 보조 신호
- `+0.1`:
  - 점수 바닥값 상향(필요 시 튜닝 대상)
- `min(1.0, ...)`:
  - 점수 상한 고정

---

## 6. 타입 관점에서 보면

- `client.search(..., Map.class)`
  - `_source`를 `Map<String, Object>` 형태로 받음
- 결과적으로 `SearchResponse<Map>`가 리턴됨
- 이후 `toResults(...)`에서 `SearchHitResult`로 변환

---

## 7. 자주 헷갈리는 포인트

1. 람다가 바로 HTTP 호출을 하는가?
- 아니오. 람다는 요청 빌더를 채우는 함수이고, 실행은 내부 `search(request, ...)`에서 발생합니다.

2. `minScore`가 있는데 `toResults`에서 왜 한 번 더 필터링하나?
- 현재 구현은 방어적 이중 필터 성격입니다.
- 운영 중 임계값 정책이 바뀔 때 코드 레벨 보호막 역할을 합니다.

3. `sort`를 점수 외 필드로 주면 script_score가 무의미한가?
- 무의미하지 않습니다. 필터/임계값에는 여전히 영향이 있습니다.
- 다만 최종 노출 순서는 정렬 필드가 우선합니다.

---

## 8. 디버깅 팁

- 점수 이상 시 확인 순서:
  1. `queryVector` 차원 수가 `product_vector` 매핑 차원과 일치하는지
  2. `baseQuery`가 예상대로 생성됐는지
  3. `minScoreThreshold`가 과도하지 않은지
  4. 정렬 옵션이 `_score`를 덮어쓰고 있지 않은지

- 개발 환경에서 로그로 확인할 항목:
  - `resolveReadAlias()` 결과
  - `from/size`
  - `minScoreThreshold`
  - `sortOption`

