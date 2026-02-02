#!/usr/bin/env bash
set -euo pipefail

# 기본 네임스페이스/포트 설정
NAMESPACE="ai-search"
SERVICE="${SERVICE:-}"
LOCAL_PORT="9200"

# kubectl이 없으면 실행할 수 없습니다.
if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

# elastic 사용자 비밀번호를 Kubernetes Secret에서 읽어옵니다.
PASSWORD=$(kubectl get secret ai-search-es-es-elastic-user -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')

echo "[INFO] elastic password loaded from secret"

# SERVICE를 직접 주지 않았다면, -es-http 서비스 이름을 자동 탐지합니다.
if [ -z "${SERVICE}" ]; then
  SERVICE=$(kubectl get svc -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' \
    | awk '/-es-http$/ {print; exit}')
fi

# HTTP 서비스가 없으면 상태 확인을 진행할 수 없습니다.
if [ -z "${SERVICE}" ]; then
  echo "[ERROR] Elasticsearch http service not found in namespace ${NAMESPACE}"
  exit 1
fi

echo "[INFO] using service ${SERVICE}"

# curl로 접속하기 위해 잠시 port-forward를 엽니다.
echo "[INFO] starting temporary port-forward"
kubectl port-forward -n "${NAMESPACE}" service/"${SERVICE}" "${LOCAL_PORT}":9200 >/tmp/ai-search-port-forward.log 2>&1 &
PF_PID=$!

# 스크립트가 끝나면 port-forward 프로세스를 정리합니다.
cleanup() {
  kill "${PF_PID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

sleep 3

# 클러스터 상태 확인
echo "[INFO] cluster health"
curl -s -u "elastic:${PASSWORD}" "http://localhost:${LOCAL_PORT}/_cluster/health?pretty"
echo

# 라이선스 타입 확인 (basic이면 무료)
echo "[INFO] license (must be basic for free use)"
curl -s -u "elastic:${PASSWORD}" "http://localhost:${LOCAL_PORT}/_license?pretty"
echo

# 현재 인덱스 목록 확인
echo "[INFO] vector index list"
curl -s -u "elastic:${PASSWORD}" "http://localhost:${LOCAL_PORT}/_cat/indices?v"
echo

echo "[OK] check finished"
