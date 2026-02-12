#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-${SCRIPT_DIR}/es-nori.Dockerfile}"
ES_VERSION="${ES_VERSION:-8.13.4}"
ES_CUSTOM_IMAGE="${ES_CUSTOM_IMAGE:-ai-search-es:8.13.4-nori}"
PLUGIN_ZIP="analysis-nori-${ES_VERSION}.zip"
PLUGIN_URL="https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-nori/${PLUGIN_ZIP}"
CURL_INSECURE="${CURL_INSECURE:-false}"

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker not found"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "[ERROR] curl not found"
  exit 1
fi

if [ ! -f "${SCRIPT_DIR}/${PLUGIN_ZIP}" ]; then
  echo "[INFO] Downloading plugin zip: ${PLUGIN_URL}"
  CURL_OPTS=(-fL --retry 2 --connect-timeout 15 -o "${SCRIPT_DIR}/${PLUGIN_ZIP}")
  if [ "${CURL_INSECURE}" = "true" ]; then
    CURL_OPTS=(-k "${CURL_OPTS[@]}")
  fi
  if ! curl "${CURL_OPTS[@]}" "${PLUGIN_URL}"; then
    echo "[ERROR] failed to download ${PLUGIN_ZIP}"
    echo "[HINT] Manual download and place file here:"
    echo "       ${SCRIPT_DIR}/${PLUGIN_ZIP}"
    echo "       url: ${PLUGIN_URL}"
    echo "[HINT] If your environment uses a private CA/proxy, try:"
    echo "       CURL_INSECURE=true ./sh_bin/00_3_build_elasticsearch_nori_image.sh"
    exit 1
  fi
else
  echo "[INFO] Using cached plugin zip: ${SCRIPT_DIR}/${PLUGIN_ZIP}"
fi

echo "[INFO] Building custom Elasticsearch image with analysis-nori"
echo "[INFO] image=${ES_CUSTOM_IMAGE}"
echo "[INFO] es_version=${ES_VERSION}"
echo "[INFO] dockerfile=${DOCKERFILE_PATH}"

set -x
docker build --build-arg ES_VERSION="${ES_VERSION}" -f "${DOCKERFILE_PATH}" -t "${ES_CUSTOM_IMAGE}" "${SCRIPT_DIR}"
set +x

echo "[OK] Built image: ${ES_CUSTOM_IMAGE}"
echo "[NEXT] Push it if needed: ./sh_bin/00_4_push_elasticsearch_nori_image.sh"
