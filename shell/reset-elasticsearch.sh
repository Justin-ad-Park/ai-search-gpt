#!/usr/bin/env bash
set -euo pipefail

ECK_VERSION="${ECK_VERSION:-3.2.0}"

echo "[INFO] deleting ai-search namespace"
kubectl delete namespace ai-search --ignore-not-found=true

echo "[INFO] deleting operator namespace"
kubectl delete -f "https://download.elastic.co/downloads/eck/${ECK_VERSION}/operator.yaml" --ignore-not-found=true >/dev/null 2>&1 || true
kubectl delete namespace elastic-system --ignore-not-found=true

echo "[OK] reset complete"
