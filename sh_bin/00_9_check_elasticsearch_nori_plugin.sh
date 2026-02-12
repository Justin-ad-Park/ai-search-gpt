#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-ai-search}"
POD_NAME="${POD_NAME:-}"
PLUGIN_NAME="analysis-nori"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

if [ -z "${POD_NAME}" ]; then
  POD_NAME="$(kubectl -n "${NAMESPACE}" get pods -l elasticsearch.k8s.elastic.co/cluster-name=ai-search-es -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
fi

if [ -z "${POD_NAME}" ]; then
  echo "[ERROR] Elasticsearch pod not found in namespace ${NAMESPACE}"
  exit 1
fi

echo "[INFO] checking plugin on pod: ${POD_NAME}"
PLUGINS="$(kubectl -n "${NAMESPACE}" exec "pod/${POD_NAME}" -- bin/elasticsearch-plugin list)"
echo "${PLUGINS}"

if echo "${PLUGINS}" | grep -qx "${PLUGIN_NAME}"; then
  echo "[OK] ${PLUGIN_NAME} plugin is installed"
  exit 0
fi

echo "[ERROR] ${PLUGIN_NAME} plugin not found"
exit 1
