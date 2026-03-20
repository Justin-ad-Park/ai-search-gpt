# Elasticsearch 무료 벡터 검색 로컬 구성

현재 사용 경로:
- `sh_bin/setup`
- `sh_bin/check`
- `sh_bin/model`
- `sh_bin/analyze`
- `sh_bin/deploy`

기존 root shell 원본은 `sh_bin/_archive`에 임시 보관합니다.

## 1회성 스크립트 (필요할 때만)
### Elasticsearch 준비
```bash
./sh_bin/setup/01_delete_elasticsearch_resources.sh
./sh_bin/setup/02_install_eck_operator.sh
./sh_bin/check/01_check_eck_operator_status.sh
./sh_bin/setup/03_build_elasticsearch_nori_image.sh
./sh_bin/setup/04_push_elasticsearch_nori_image.sh
./sh_bin/setup/05_start_elasticsearch_cluster_custom_image.sh
./sh_bin/check/02_check_elasticsearch_nori_plugin.sh
```

### DJL 준비
```bash
./sh_bin/setup/06_prepare_djl_truststore.sh
./sh_bin/check/04_check_djl_truststore.sh
./sh_bin/check/01_check_eck_operator_status.sh
```
`setup/06` 실행 후 truststore는 `~/.ai-cert/djl-truststore.p12`에 생성됩니다.

## Nori 커스텀 이미지 경로 (선택)
`analysis-nori`를 클러스터 내부에서 다운로드하지 않고, 커스텀 이미지에 내장해서 사용합니다.

```bash
./sh_bin/setup/03_build_elasticsearch_nori_image.sh
./sh_bin/setup/04_push_elasticsearch_nori_image.sh
./sh_bin/setup/05_start_elasticsearch_cluster_custom_image.sh
./sh_bin/check/02_check_elasticsearch_nori_plugin.sh
```

기본 동작은 로컬 태그(`ai-search-es:8.13.4-nori`)를 사용합니다.
레지스트리에 push가 필요한 경우에만 `ES_CUSTOM_IMAGE=<registry>/<repo>/ai-search-es:8.13.4-nori`를 지정하세요.
인증서 이슈가 있는 네트워크에서는 `CURL_INSECURE=true ./sh_bin/setup/03_build_elasticsearch_nori_image.sh`를 사용할 수 있습니다.

`setup/03`에서 플러그인 다운로드가 실패하면, 아래 파일을 직접 받아 지정 위치에 넣은 뒤 다시 `setup/03`을 실행하세요.
- URL: `https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-nori/analysis-nori-8.13.4.zip`
- 저장 위치: `sh_bin/setup/resources/analysis-nori-8.13.4.zip`

## 일반 실행 순서 (매번/자주 실행)
### 1) Elasticsearch 상태/라이선스 확인

```bash
./sh_bin/check/03_check_elasticsearch_status.sh
```
`_license` 결과에서 `"type" : "basic"`이면 무료 라이선스 모드입니다.

### 2) 모델별 앱 실행
```bash
./sh_bin/model/02_run_model_web.sh e5-small-ko-v2
./sh_bin/model/03_run_model_indexing_web.sh kure-v1
./sh_bin/model/04_run_model_indexing_only.sh bge-m3
./sh_bin/model/02_run_model_web.sh bge-m3
```

지원 모델 키:
- `e5-small-ko-v2`
- `kure-v1`
- `bge-m3`

전체 모델을 한 번에 실행할 수도 있습니다.
```bash
./sh_bin/model/05_start_all_model_web.sh
./sh_bin/model/06_start_all_model_indexing_web.sh
./sh_bin/model/07_stop_all_models.sh
./sh_bin/deploy/01_deploy_dev_server.sh
```

`model/05_start_all_model_web.sh`, `model/06_start_all_model_indexing_web.sh`, `deploy/01_deploy_dev_server.sh`는 모델 비교용으로 `search-vector-only` 프로필을 함께 사용합니다.
`model/06_start_all_model_indexing_web.sh`는 각 모델별 색인과 웹 실행을 순차적으로 시작합니다.
개발서버 반영은 `deploy/01_deploy_dev_server.sh`를 권장합니다. 이 스크립트는 테스트, 기존 프로세스 정리, 모델별 재색인, 비교용 웹 기동, 상태 확인을 한 번에 수행합니다.

모델별 검색 결과 비교:
```bash
./sh_bin/model/08_compare_model_search.sh "어린이 간식"
./sh_bin/model/09_compare_model_search_queries.sh
./sh_bin/check/05_check_model_status.sh
```

## 운영 상태 빠른 확인
```bash
./sh_bin/check/06_check_k8s_elastic_pods.sh
```
