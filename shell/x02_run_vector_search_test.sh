#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="ai-search"
SERVICE="ai-search-es-es-http"
LOCAL_PORT="9200"
MAVEN_IMAGE="maven:3.9.9-eclipse-temurin-21"
TEST_CLASS="VectorSearchIntegrationTest"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker not found"
  exit 1
fi

PASSWORD=$(kubectl get secret ai-search-es-es-elastic-user -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')
echo "[INFO] elastic password loaded from secret"

echo "[INFO] starting temporary port-forward"
kubectl port-forward -n "${NAMESPACE}" service/"${SERVICE}" "${LOCAL_PORT}":9200 >/tmp/ai-search-test-port-forward.log 2>&1 &
PF_PID=$!

cleanup() {
  kill "${PF_PID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

sleep 4

echo "[INFO] running JUnit test in docker (${TEST_CLASS})"
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  -e AI_SEARCH_ES_URL="http://host.docker.internal:${LOCAL_PORT}" \
  -e AI_SEARCH_ES_USERNAME="elastic" \
  -e AI_SEARCH_ES_PASSWORD="${PASSWORD}" \
  "${MAVEN_IMAGE}" \
  mvn -Dtest="${TEST_CLASS}" test

echo "[OK] test finished"
