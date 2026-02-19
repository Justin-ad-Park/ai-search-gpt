#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFEST_PATH="${SCRIPT_DIR}/es-cluster-custom-image.yaml"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

CURRENT_CONTEXT="$(kubectl config current-context 2>/dev/null || true)"
if [ -n "${CURRENT_CONTEXT}" ]; then
  echo "[INFO] kubectl context: ${CURRENT_CONTEXT}"
fi

echo "[INFO] using manifest: ${MANIFEST_PATH}"

set -x
kubectl -n ai-search delete elasticsearch ai-search-es --ignore-not-found=true >/dev/null 2>&1 || true
kubectl apply --request-timeout=60s -f "${MANIFEST_PATH}"

echo "[INFO] waiting for Elasticsearch to be Ready (timeout 600s)"
start_ts=$(date +%s)
timeout_sec=600
while true; do
  phase=$(kubectl get elasticsearch ai-search-es -n ai-search -o jsonpath='{.status.phase}' 2>/dev/null || true)
  health=$(kubectl get elasticsearch ai-search-es -n ai-search -o jsonpath='{.status.health}' 2>/dev/null || true)
  nodes=$(kubectl get elasticsearch ai-search-es -n ai-search -o jsonpath='{.status.availableNodes}' 2>/dev/null || true)
  echo "[INFO] phase=${phase:-unknown} health=${health:-unknown} nodes=${nodes:-0}"

  if [ "${phase}" = "Ready" ]; then
    break
  fi

  now_ts=$(date +%s)
  if [ $((now_ts - start_ts)) -ge "${timeout_sec}" ]; then
    echo "[ERROR] Elasticsearch not Ready after ${timeout_sec}s"
    kubectl -n ai-search describe elasticsearch ai-search-es || true
    kubectl -n ai-search get pods || true
    exit 1
  fi
  sleep 10
done
set +x

echo "[OK] Elasticsearch(custom image) is ready"
echo "[NEXT] Verify plugin: ./sh_bin/00_9_check_elasticsearch_nori_plugin.sh"
