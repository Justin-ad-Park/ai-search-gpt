#!/usr/bin/env bash
set -euo pipefail

# 확인할 truststore 경로 (00_4에서 생성한 파일)
TRUSTSTORE_PATH="${HOME}/.ai-cert/djl-truststore.p12"

# truststore 비밀번호
TRUSTSTORE_PASSWORD="${AI_SEARCH_TRUSTSTORE_PASSWORD:-changeit}"

# Java keytool이 필요합니다.
if ! command -v keytool >/dev/null 2>&1; then
  echo "[ERROR] keytool not found"
  exit 1
fi

# 파일이 없으면 먼저 00_4를 실행해야 합니다.
if [ ! -f "${TRUSTSTORE_PATH}" ]; then
  echo "[ERROR] truststore not found: ${TRUSTSTORE_PATH}"
  echo "[NEXT] Run: ./sh_bin/00_4_prepare_djl_truststore.sh"
  exit 1
fi

echo "[INFO] checking truststore: ${TRUSTSTORE_PATH}"
keytool -list -v -storetype PKCS12 -keystore "${TRUSTSTORE_PATH}" -storepass "${TRUSTSTORE_PASSWORD}"
