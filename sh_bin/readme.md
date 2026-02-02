# Elasticsearch 무료 벡터 검색 로컬 구성

## 1회성 스크립트 (필요할 때만)
```bash
./sh_bin/00_1_delete_elasticsearch_resources.sh
./sh_bin/00_2_install_eck_operator.sh
./sh_bin/00_3_start_elasticsearch_cluster.sh
./sh_bin/00_4_prepare_djl_truststore.sh
./sh_bin/00_5_check_djl_truststore.sh
./sh_bin/00_6_check_eck_operator_status.sh
```
`00_4` 실행 후 truststore는 `~/.ai-cert/djl-truststore.p12`에 생성됩니다.

## 일반 실행 순서 (매번/자주 실행)
### 1) Elasticsearch 상태/라이선스 확인

```bash
./sh_bin/01_check_elasticsearch_status.sh
```
`_license` 결과에서 `"type" : "basic"`이면 무료 라이선스 모드입니다.

### 2) 샘플 데이터 생성
```bash
./sh_bin/02_generate_sample_data.sh
```

### 3) 샘플 데이터 확인
```bash
./sh_bin/03_check_sample_data.sh
```

### 4) 벡터 검색 테스트 실행
```bash
./sh_bin/04_run_vector_search_test_local.sh
```

## 운영 상태 빠른 확인
```bash
./sh_bin/90_check_k8s_elastic_pods.sh
```
