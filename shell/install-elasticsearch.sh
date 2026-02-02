#!/usr/bin/env bash
set -euo pipefail

ECK_VERSION="${ECK_VERSION:-3.2.0}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

echo "[INFO] Reinstalling ECK operator (version=${ECK_VERSION})"

echo "[INFO] Deleting existing operator (if exists)"
kubectl delete -f "https://download.elastic.co/downloads/eck/${ECK_VERSION}/operator.yaml" --ignore-not-found=true >/dev/null 2>&1 || true
kubectl delete namespace elastic-system --ignore-not-found=true >/dev/null 2>&1 || true

kubectl apply -f "https://download.elastic.co/downloads/eck/${ECK_VERSION}/crds.yaml"
kubectl apply -f "https://download.elastic.co/downloads/eck/${ECK_VERSION}/operator.yaml"

kubectl wait --for=condition=ready pod -l control-plane=elastic-operator -n elastic-system --timeout=300s

echo "[OK] ECK operator is ready"
