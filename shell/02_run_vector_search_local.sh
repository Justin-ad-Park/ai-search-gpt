#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="ai-search"
SERVICE="${SERVICE:-}"
LOCAL_PORT="9200"
TEST_CLASS_PATTERN="*VectorSearchIntegrationTest"
TRUSTSTORE_PATH="${PWD}/.gradle/djl-truststore.p12"
TRUSTSTORE_PASSWORD="${AI_SEARCH_TRUSTSTORE_PASSWORD:-changeit}"
DJL_HOSTS=("djl.ai" "resources.djl.ai" "mlrepo.djl.ai")

if ! command -v kubectl >/dev/null 2>&1; then
  echo "[ERROR] kubectl not found"
  exit 1
fi

if [ ! -x "./gradlew" ]; then
  echo "[ERROR] ./gradlew not found or not executable"
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "[ERROR] openssl not found"
  exit 1
fi

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  if JAVA21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null); then
    export JAVA_HOME="${JAVA21_HOME}"
    echo "[INFO] JAVA_HOME set to Java 21 (${JAVA_HOME})"
  fi
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/keytool" ]; then
  echo "[ERROR] keytool not found (JAVA_HOME is required)"
  exit 1
fi

echo "[INFO] preparing custom truststore for DJL hosts"
mkdir -p "$(dirname "${TRUSTSTORE_PATH}")"
rm -f "${TRUSTSTORE_PATH}"

for host in "${DJL_HOSTS[@]}"; do
  cert_tmp=$(mktemp)
  split_prefix=$(mktemp -u)

  openssl s_client -showcerts -servername "${host}" -connect "${host}:443" </dev/null 2>/dev/null \
    | awk '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/' > "${cert_tmp}"

  if [ ! -s "${cert_tmp}" ]; then
    echo "[ERROR] failed to fetch certificates from ${host}"
    exit 1
  fi

  awk -v prefix="${split_prefix}" '
    /-----BEGIN CERTIFICATE-----/ {n++; file=sprintf("%s.%02d.pem", prefix, n)}
    n > 0 {print > file}
    /-----END CERTIFICATE-----/ {close(file)}
  ' "${cert_tmp}"

  idx=0
  for cert_file in "${split_prefix}".*.pem; do
    if [ ! -s "${cert_file}" ]; then
      continue
    fi
    alias="${host}-${idx}"
    "${JAVA_HOME}/bin/keytool" -importcert -noprompt \
      -storetype PKCS12 \
      -keystore "${TRUSTSTORE_PATH}" \
      -storepass "${TRUSTSTORE_PASSWORD}" \
      -alias "${alias}" \
      -file "${cert_file}" >/dev/null
    idx=$((idx + 1))
  done

  if [ "${idx}" -eq 0 ]; then
    echo "[ERROR] no certificates extracted for ${host}"
    exit 1
  fi

  rm -f "${cert_tmp}" "${split_prefix}".*
done

PASSWORD=$(kubectl get secret ai-search-es-es-elastic-user -n "${NAMESPACE}" -o go-template='{{.data.elastic | base64decode}}')
echo "[INFO] elastic password loaded from secret"

if [ -z "${SERVICE}" ]; then
  SERVICE=$(kubectl get svc -n "${NAMESPACE}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' \
    | awk '/-es-http$/ {print; exit}')
fi

if [ -z "${SERVICE}" ]; then
  echo "[ERROR] Elasticsearch http service not found in namespace ${NAMESPACE}"
  exit 1
fi

echo "[INFO] using service ${SERVICE}"

echo "[INFO] starting temporary port-forward"
kubectl port-forward -n "${NAMESPACE}" service/"${SERVICE}" "${LOCAL_PORT}":9200 >/tmp/ai-search-local-test-port-forward.log 2>&1 &
PF_PID=$!

cleanup() {
  kill "${PF_PID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

sleep 4

echo "[INFO] running JUnit test locally (${TEST_CLASS_PATTERN})"
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=${TRUSTSTORE_PATH} -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASSWORD}"
AI_SEARCH_ES_URL="http://localhost:${LOCAL_PORT}" \
AI_SEARCH_ES_USERNAME="elastic" \
AI_SEARCH_ES_PASSWORD="${PASSWORD}" \
./gradlew test --tests "${TEST_CLASS_PATTERN}"

echo "[OK] local test finished"
