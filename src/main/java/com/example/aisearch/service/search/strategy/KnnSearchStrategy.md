# KnnSearchStrategy 핵심 검색 코드 이해 가이드

이 문서는 `KnnSearchStrategy#hybridSearch(...)`의 `client.search(...)` 블록을 빠르게 이해하기 위한 설명입니다.

## 1) 현재 대상 코드

```java
        SearchResponse<Map> response
        = client.search(s -> s  /* SearchRequest.Builder */
                .index(getReadAlias())
                .query(
                        /* Query.Builder */
                        q -> q.scriptScore(
                                /* ScriptScoreQuery.Builder */
                                ss -> ss
                                        .query(baseQuery)
                                        .script(
                                                /* Script.Builder */
                                                sc -> sc.inline(
                                                        /* InlineScript.Builder */
                                                        i ->
                                                        {
                                                          i.lang("painless")
                                                                  .source(selectScriptSource(decision))
                                                                  .params("query_vector", JsonData.of(queryVector));
                                                          // boost 적용 케이스에서만 category boost 파라미터를 전달한다.
                                                          if (decision.applyCategoryBoost()) {
                                                            i.params("category_boost_by_id", JsonData.of(decision.categoryBoostById()));
                                                          }
                                                          return i;
                                                        }
                                                )
                                        )
                        )
                )
                .sort(decision.sortOptions())
                .trackScores(true)
                .from(from)
                .size(size)
                .minScore(properties.minScoreThreshold())
        ,
        Map.class
);
);
);
```

## 2) 가장 쉬운 이해 순서 (1~5)

### 1. 람다 DSL을 "요청 JSON 조립"으로 이해하기
`client.search(s -> s ... , Map.class)`에서

- s -> s.index(...)
- .query(...)
- .sort(...)는 결국 SearchRequest 빌더입니다.
- 먼저 이 코드를 “어떤 JSON 쿼리가 나가는지”로 머릿속에서 바꿔보세요.



`SearchRequest`를 만드는 빌더 함수입니다.

- `s -> s` : SearchRequest.Builder
- `q -> q` : Query.Builder
- `ss -> ss` : ScriptScoreQuery.Builder
- `sc -> sc` : Script.Builder
- `i -> i` : InlineScript.Builder

즉, 이 코드는 "검색 요청 명세서"입니다.

### 2. 점수 계산 경로를 분리해서 보기

이 블록의 핵심은 `script_score`입니다.

1. `baseQuery`로 후보 문서를 고름
2. `painless` 스크립트로 최종 점수를 계산
3. `minScore`로 컷오프 적용

점수식(스크립트 내부):
- `vectorScore` = 임베딩 유사도 정규화
- `lexicalScore` = 텍스트 점수 보조 신호
- 필요 시 `categoryBoost` 가산

### 3. `decision`이 바꾸는 것 2가지만 고정해서 보기

`CategoryBoostingDecider` 결과(`decision`)는 아래만 바꿉니다.

1. `selectScriptSource(decision)`  
- 부스팅용 스크립트(`CATEGORY_BOOST_SCRIPT`)를 쓸지
- 기본 스크립트(`BASE_SCRIPT`)를 쓸지

2. `decision.sortOptions()`  
- 최종 정렬 기준(`_score`, `price` 등)

이 두 지점이 결과 순서를 가장 크게 좌우합니다.

### 4. 스크립트 파라미터 매핑 이해하기

항상 전달:
- `query_vector`

조건부 전달:
- `category_boost_by_id` (오직 `decision.applyCategoryBoost() == true`일 때)

즉, 부스팅 비적용 케이스에서는 카테고리 맵 자체를 보내지 않습니다.

### 5. 실제로 확인하면서 학습하기

브레이크포인트 추천 위치:
1. `CategoryBoostingDecider.decide(request)`
2. `selectScriptSource(decision)`
3. `client.search` 직전 (`decision`, `queryVector`, `baseQuery`)

비교 추천:
- 같은 query로 `CATEGORY_BOOSTING_DESC` vs `RELEVANCE_DESC`
- 같은 sort에서 룰 매칭 query vs 불일치 query

## 4) 자주 헷갈리는 포인트

1. 람다 체인이 바로 HTTP를 호출하나?  
- 아니오. 요청 객체를 조립한 뒤 내부 `search(request, ...)`에서 실행됩니다.

2. `trackScores(true)`는 왜 필요한가?  
- 정렬이 `_score`가 아니어도 점수를 유지/반환하려는 의도입니다.

3. `sort`가 `_score` 외 필드면 script_score는 의미 없나?  
- 아닙니다. 후보군 점수 계산과 `minScore` 필터에는 여전히 영향이 있습니다.

## 5) 관련 코드 같이 보기

- `KnnSearchStrategy.java`
- `CategoryBoostingDecider.java`
- `CategoryBoostingResult.java`
- `ProductSearchRequest.java` (query/sort 정규화)

## 6) 쿼리 비교: "사과 + CATEGORY_BOOSTING_DESC" vs "키워드 없음 + categoryId 필터 + RELEVANCE_DESC"

아래는 `KnnSearchRequestSerializationTest`에서 실제 직렬화한 출력입니다.  
주의: `index`는 요청 본문(body)이 아니라 API 경로/파라미터 영역이라 직렬화 JSON에는 나오지 않습니다.

### A. 검색어 `"사과"` + `CATEGORY_BOOSTING_DESC` (룰 매칭됨)

- 경로: `hybridSearch`
- `CategoryBoostingDecider` 결과:
1. `applyCategoryBoost = true`
2. `searchSortOption = CATEGORY_BOOSTING_DESC`
3. `categoryBoostById = {"4": 0.2}` (룰 파일 기준)
- 따라서 `script.source = CATEGORY_BOOST_SCRIPT`
- `script.params`에 `query_vector` + `category_boost_by_id` 둘 다 포함

```json
{
  "from" : 0,
  "min_score" : 0.74,
  "query" : {
    "script_score" : {
      "query" : {
        "bool" : {
          "minimum_should_match" : "0",
          "should" : [ {
            "multi_match" : {
              "fields" : [ "product_name^2", "description" ],
              "query" : "사과"
            }
          } ]
        }
      },
      "script" : {
        "params" : {
          "query_vector" : [ 0.11, 0.22, 0.33 ],
          "category_boost_by_id" : {
            "4" : 0.2
          }
        },
        "lang" : "painless",
        "source" : "<multiline painless script>"
      }
    }
  },
  "size" : 20,
  "sort" : [ {
    "_score" : {
      "order" : "desc"
    }
  }, {
    "id" : {
      "order" : "asc"
    }
  } ],
  "track_scores" : true
}
```

`source` 스크립트 본문(가독성용 줄바꿈):

```painless
double vectorScore = (cosineSimilarity(params.query_vector, 'product_vector') + 1.0) / 2.0;
double lexicalScore = Math.min(_score, 5.0) / 5.0;
double categoryBoost = 0.0;
if (doc['categoryId'].size() != 0) {
  String categoryKey = String.valueOf(doc['categoryId'].value);
  def rawBoost = params.category_boost_by_id.get(categoryKey);
  if (rawBoost != null) {
    categoryBoost = ((Number) rawBoost).doubleValue();
  }
}
return Math.min(1.0, 0.9 * vectorScore + 0.1 * lexicalScore + categoryBoost + 0.1);
```

주의:
- `filter`는 가격/카테고리 조건이 있을 때만 `bool.filter`에 추가됩니다.
- 조건이 없으면 `filter` 필드 자체가 생성되지 않습니다 (`[]`로 들어가지 않음).

### B. 키워드 없음 + `categoryId=[4]` + `RELEVANCE_DESC`

- 경로: `filterOnlySearch`
- `script_score`, 임베딩, `category_boost_by_id`, `min_score` 모두 사용하지 않음
- `buildRootQuery`가 필터 쿼리를 그대로 루트로 사용

```json
{
  "from" : 0,
  "query" : {
    "bool" : {
      "filter" : [ {
        "terms" : {
          "categoryId" : [ 4 ]
        }
      } ]
    }
  },
  "size" : 20,
  "sort" : [ {
    "_score" : {
      "order" : "desc"
    }
  }, {
    "id" : {
      "order" : "asc"
    }
  } ],
  "track_scores" : true
}
```

### 차이 요약 (코드 기준)

1. A는 `script_score`로 점수를 재계산하고, B는 필터+정렬만 수행한다.
2. A는 `min_score` 컷오프가 있고, B는 없다.
3. A는 룰 매칭 시 `category_boost_by_id`를 전달하고 `CATEGORY_BOOST_SCRIPT`를 사용한다.
4. B는 검색어가 없으므로 카테고리 부스팅 판단 자체를 하지 않는다.
