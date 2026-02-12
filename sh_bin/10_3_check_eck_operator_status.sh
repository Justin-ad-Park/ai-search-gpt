#!/usr/bin/env bash
set -euo pipefail

# ECK 오퍼레이터가 정상 설치/기동 중인지 확인합니다.
NAMESPACE="elastic-system"
LABEL_SELECTOR="control-plane=elastic-operator"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

echo "[INFO] checking ECK operator namespace: ${NAMESPACE}"
kubectl get ns "${NAMESPACE}" >/dev/null 2>&1 || {
  echo "[ERROR] namespace ${NAMESPACE} not found"
  echo "[NEXT] Run: ./sh_bin/00_2_install_eck_operator.sh"
  exit 1
}

echo "[INFO] operator pods"
kubectl get pods -n "${NAMESPACE}" -l "${LABEL_SELECTOR}" -o wide

echo "[INFO] operator statefulset"
kubectl get statefulset -n "${NAMESPACE}"

echo "[INFO] operator services"
kubectl get svc -n "${NAMESPACE}"

echo "[INFO] waiting for operator pod to be Ready (timeout 300s)"
kubectl wait --for=condition=ready pod -l "${LABEL_SELECTOR}" -n "${NAMESPACE}" --timeout=300s

echo "[OK] ECK operator is ready"
