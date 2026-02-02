#!/usr/bin/env bash
set -euo pipefail

# ECK 버전은 필요하면 환경 변수로 바꿀 수 있습니다.
# ECK = Elastic Cloud on Kubernetes(ECK) : 엘라스틱의 쿠버네티스용 오퍼레이터 버전
ECK_VERSION="${ECK_VERSION:-3.2.0}"

# 1) Elasticsearch가 있던 네임스페이스를 삭제합니다.
echo "[INFO] deleting ai-search namespace"
kubectl delete namespace ai-search --ignore-not-found=true

# 2) ECK 오퍼레이터 관련 리소스를 삭제합니다.
echo "[INFO] deleting operator namespace"
kubectl delete -f "https://download.elastic.co/downloads/eck/${ECK_VERSION}/operator.yaml" --ignore-not-found=true >/dev/null 2>&1 || true
kubectl delete namespace elastic-system --ignore-not-found=true

echo "[OK] reset complete"
