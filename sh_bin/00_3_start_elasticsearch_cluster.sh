#!/usr/bin/env bash
set -euo pipefail

# 현재 스크립트 위치를 기준으로 YAML 파일 경로를 계산합니다.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# kubectl이 없으면 실행할 수 없습니다.
if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

# 어떤 클러스터 컨텍스트로 실행 중인지 보여줍니다.
CURRENT_CONTEXT="$(kubectl config current-context 2>/dev/null || true)"
if [ -n "${CURRENT_CONTEXT}" ]; then
  echo "[INFO] kubectl context: ${CURRENT_CONTEXT}"
else
  echo "[WARN] kubectl context not available"
fi

# 진행 상황을 보기 위해 디버그 모드를 켭니다.
set -x

# 기존 Elasticsearch 리소스가 있으면 지우고 새로 만듭니다.
echo "[INFO] Deleting old cluster resource (if exists)"
kubectl -n ai-search delete elasticsearch ai-search-es --ignore-not-found=true >/dev/null 2>&1 || true

# Elasticsearch 클러스터 정의를 적용합니다.
# 네트워크 이슈로 hang 되는 경우를 막기 위해 요청 타임아웃을 설정합니다.
kubectl apply --request-timeout=60s -f "${SCRIPT_DIR}/es-cluster.yaml"

# 클러스터가 Ready 상태가 될 때까지 폴링합니다.
# (kubectl wait가 일부 환경에서 응답 없이 멈추는 경우가 있어 폴링 방식으로 변경)
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
    exit 1
  fi

  sleep 10
done

# 디버그 출력 종료
set +x

echo "[OK] Elasticsearch is ready in namespace ai-search"
echo "[NEXT] Run: ./sh_bin/01_check_elasticsearch_status.sh"
