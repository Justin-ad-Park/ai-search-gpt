#!/usr/bin/env bash
set -euo pipefail

# 설치할 ECK 버전 (환경 변수로 변경 가능)
ECK_VERSION="${ECK_VERSION:-3.2.0}"

# kubectl이 없으면 Kubernetes를 제어할 수 없어서 종료합니다.
if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

echo "[INFO] Installing ECK operator (version=${ECK_VERSION})"

# ECK CRD와 오퍼레이터를 설치합니다.
# 네트워크 이슈로 hang 되는 경우를 막기 위해 요청 타임아웃을 설정합니다.
kubectl apply --request-timeout=60s -f "https://download.elastic.co/downloads/eck/${ECK_VERSION}/crds.yaml"
kubectl apply --request-timeout=60s -f "https://download.elastic.co/downloads/eck/${ECK_VERSION}/operator.yaml"

# 오퍼레이터 Pod가 Ready가 될 때까지 대기합니다.
kubectl wait --for=condition=ready pod -l control-plane=elastic-operator -n elastic-system --timeout=300s

echo "[OK] ECK operator is ready"
