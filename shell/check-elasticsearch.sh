#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="ai-search"
SERVICE="ai-search-es-es-http"
LOCAL_PORT="9200"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

PASSWORD=$(kubectl get secret ai-search-es-es-elastic-user -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')

echo "[INFO] elastic password loaded from secret"

echo "[INFO] starting temporary port-forward"
kubectl port-forward -n "${NAMESPACE}" service/"${SERVICE}" "${LOCAL_PORT}":9200 >/tmp/ai-search-port-forward.log 2>&1 &
PF_PID=$!

cleanup() {
  kill "${PF_PID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

sleep 3

echo "[INFO] cluster health"
curl -s -u "elastic:${PASSWORD}" "http://localhost:${LOCAL_PORT}/_cluster/health?pretty"
echo

echo "[INFO] license (must be basic for free use)"
curl -s -u "elastic:${PASSWORD}" "http://localhost:${LOCAL_PORT}/_license?pretty"
echo

echo "[INFO] vector index list"
curl -s -u "elastic:${PASSWORD}" "http://localhost:${LOCAL_PORT}/_cat/indices?v"
echo

echo "[OK] check finished"
