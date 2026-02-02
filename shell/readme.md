# Elasticsearch 무료 벡터 검색 로컬 구성

## 문제가 있는 기존 설치 삭제
```bash
./shell/reset-elasticsearch.sh
```

## 1) ECK 설치 (문제 시 재설치)
```bash
./shell/install-elasticsearch.sh
```

## 2) Elasticsearch 클러스터 구동
```bash
./shell/run-elasticsearch.sh
```

## 3) 구동/라이선스 확인
```bash
./shell/check-elasticsearch.sh
```

`_license` 결과에서 `"type" : "basic"`이면 무료 라이선스 모드입니다.

## 4) Spring Boot 테스트 실행 준비
비밀번호를 secret에서 추출해 환경 변수로 넣습니다.

```bash
export AI_SEARCH_ES_PASSWORD=$(kubectl get secret ai-search-es-es-elastic-user -n ai-search -o go-template='{{.data.elastic | base64decode}}')
export AI_SEARCH_ES_URL=http://localhost:9200
export AI_SEARCH_ES_USERNAME=elastic
```

포트 포워딩:
```bash
kubectl port-forward -n ai-search service/ai-search-es-es-http 9200:9200
```

다른 터미널에서 테스트:
```bash
./gradlew test
# 또는 gradle test
```

## 5) 샘플 데이터 재생성 (300건)
```bash
python3 shell/generate_food_sample_data.py
```
