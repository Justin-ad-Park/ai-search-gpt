#!/usr/bin/env bash
set -euo pipefail

SOURCE_IMAGE="${SOURCE_IMAGE:-ai-search-es:8.13.4-nori}"
TARGET_IMAGE="${ES_CUSTOM_IMAGE:-${TARGET_IMAGE:-}}"

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker not found"
  exit 1
fi

if ! docker image inspect "${SOURCE_IMAGE}" >/dev/null 2>&1; then
  echo "[INFO] source image not found: ${SOURCE_IMAGE}"
  echo "[INFO] Skip push step. Build first if needed: ./sh_bin/00_3_build_elasticsearch_nori_image.sh"
  exit 0
fi

if [ -z "${TARGET_IMAGE}" ]; then
  echo "[INFO] ES_CUSTOM_IMAGE not set. Skip push step for local Docker Desktop workflow."
  echo "[NEXT] Use local image directly with: ./sh_bin/00_5_start_elasticsearch_cluster_custom_image.sh"
  exit 0
fi

echo "[INFO] source image: ${SOURCE_IMAGE}"
echo "[INFO] target image: ${TARGET_IMAGE}"
set -x
if [ "${SOURCE_IMAGE}" != "${TARGET_IMAGE}" ]; then
  docker tag "${SOURCE_IMAGE}" "${TARGET_IMAGE}"
fi
docker push "${TARGET_IMAGE}"
set +x

echo "[OK] Pushed image: ${TARGET_IMAGE}"
