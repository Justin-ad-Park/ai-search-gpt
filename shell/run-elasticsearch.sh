#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

echo "[INFO] Deleting old cluster resource (if exists)"
kubectl -n ai-search delete elasticsearch ai-search-es --ignore-not-found=true >/dev/null 2>&1 || true

kubectl apply -f "${SCRIPT_DIR}/es-cluster.yaml"

kubectl wait --for=condition=ready elasticsearch/ai-search-es -n ai-search --timeout=600s

echo "[OK] Elasticsearch is ready in namespace ai-search"
echo "[NEXT] Run: ./shell/check-elasticsearch.sh"
