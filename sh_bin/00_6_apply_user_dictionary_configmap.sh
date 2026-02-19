#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-ai-search}"
CONFIGMAP_NAME="${CONFIGMAP_NAME:-es-user-dict}"
DICT_FILE_PATH="${DICT_FILE_PATH:-src/main/resources/es/dictionary/user_dict_ko.txt}"
DICT_KEY="${DICT_KEY:-user_dict_ko.txt}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

if [ ! -f "${DICT_FILE_PATH}" ]; then
  echo "[ERROR] dictionary file not found: ${DICT_FILE_PATH}"
  exit 1
fi

CURRENT_CONTEXT="$(kubectl config current-context 2>/dev/null || true)"
if [ -n "${CURRENT_CONTEXT}" ]; then
  echo "[INFO] kubectl context: ${CURRENT_CONTEXT}"
fi

echo "[INFO] applying ConfigMap"
echo "       namespace=${NAMESPACE}"
echo "       name=${CONFIGMAP_NAME}"
echo "       source=${DICT_FILE_PATH}"
echo "       key=${DICT_KEY}"

set -x
kubectl create configmap "${CONFIGMAP_NAME}" \
  -n "${NAMESPACE}" \
  --from-file="${DICT_KEY}=${DICT_FILE_PATH}" \
  --dry-run=client -o yaml | kubectl apply -f -
set +x

echo "[OK] ConfigMap applied: ${CONFIGMAP_NAME}"
