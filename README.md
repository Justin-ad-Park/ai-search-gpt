# ai-search-gpt

무료 라이선스(Basic) Elasticsearch + Spring Boot 기반 한글 식품 벡터 검색 토이 프로젝트입니다.

## 핵심 구성
- Elasticsearch: ECK로 Kubernetes에 배포
- 라이선스: Basic(무료)
- 임베딩: DJL + `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` (오픈 모델)
- 검색: Elasticsearch `dense_vector` + `knn` 쿼리
- 데이터: `src/main/resources/data/food-products.json` (300건)

## 빠른 시작
1. `./shell/install-elasticsearch.sh`
2. `./shell/run-elasticsearch.sh`
3. `./shell/check-elasticsearch.sh`
4. `kubectl port-forward -n ai-search service/ai-search-es-es-http 9200:9200`
5. ES 비밀번호 export 후 `./gradlew test`

상세 절차는 `shell/readme.md` 참고.
