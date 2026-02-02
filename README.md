# ai-search-gpt

무료 라이선스(Basic) Elasticsearch + Spring Boot 기반 한글 식품 벡터 검색 토이 프로젝트입니다.

## 핵심 구성
- Elasticsearch: ECK로 Kubernetes에 배포
- 라이선스: Basic(무료)
- 임베딩: DJL + `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` (오픈 모델)
- 검색: Elasticsearch `dense_vector` + `knn` 쿼리
- 데이터: `src/main/resources/data/food-products.json` (120건)

## 빠른 시작
1. `./sh_bin/00_2_install_eck_operator.sh` (1회성)
2. `./sh_bin/00_3_start_elasticsearch_cluster.sh` (초기/재구성 시)
3. `./sh_bin/00_4_prepare_djl_truststore.sh` (1회성)
4. `./sh_bin/01_check_elasticsearch_status.sh`
5. `./sh_bin/02_generate_sample_data.sh`
6. `./sh_bin/04_run_vector_search_test_local.sh`

상세 절차는 `sh_bin/readme.md` 참고.
