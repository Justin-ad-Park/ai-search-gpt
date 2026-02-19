#!/usr/bin/env bash
set -euo pipefail

# 이 스크립트의 목적:
# - 커스텀 Elasticsearch 이미지를 원격 레지스트리에 push할 때 사용
# - 로컬 Docker Desktop 단일 노드 환경에서는 push 없이도 00_5에서 바로 사용 가능
#
# 로컬 환경에서의 동작:
# - ES_CUSTOM_IMAGE/TARGET_IMAGE를 주지 않으면 push 단계를 "정상 스킵(exit 0)"
# - 즉, 00_4를 실행해도 에러가 아니라 안내 메시지 후 종료되도록 설계
#
# push가 필요한 경우:
# - 원격 Kubernetes 클러스터 사용 시
# - 멀티 노드/다른 호스트에서 이미지를 pull해야 하는 경우
# - CI/CD 배포에서 이미지 레지스트리를 통해 배포하는 경우
SOURCE_IMAGE="${SOURCE_IMAGE:-ai-search-es:8.13.4-nori}"
TARGET_IMAGE="${ES_CUSTOM_IMAGE:-${TARGET_IMAGE:-}}"

#
#• 예시 값들입니다.
#
# - Docker Hub: yourdockerid/ai-search-es:8.13.4-nori
#  - GHCR: ghcr.io/your-org/ai-search-es:8.13.4-nori
#  - ECR: 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/ai-search-es:8.13.4-nori
#  - GCR/Artifact Registry: asia-northeast3-docker.pkg.dev/your-project/your-repo/ai-search-es:8.13.4-nori
#
#  - GHCR: GitHub Container Registry (ghcr.io)
#  - ECR: Amazon Elastic Container Registry (AWS)
#  - GCR: Google Container Registry / Artifact Registry (GCP)
#
#• 네이버클라우드(NCP) Container Registry는 보통 이렇게 씁니다.
#  - 공용 엔드포인트:
#    <registry-name>.<region-code>.ncr.ntruss.com
#  - 사설 엔드포인트:
#    <random-name>.<region-code>.private-ncr.ntruss.com
#  - 예 :
#   pulmuone-dev-image-registry.kr.ncr.ntruss.com




if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker 명령을 찾을 수 없습니다."
  exit 1
fi

if ! docker image inspect "${SOURCE_IMAGE}" >/dev/null 2>&1; then
  echo "[INFO] 소스 이미지를 찾을 수 없습니다: ${SOURCE_IMAGE}"
  echo "[INFO] push 단계를 건너뜁니다. 필요하면 먼저 빌드하세요: ./sh_bin/00_3_build_elasticsearch_nori_image.sh"
  exit 0
fi

# TARGET_IMAGE 파라미터가 설정되면 이미지를 원격 레지스트리에 push 하기 시도함
if [ -z "${TARGET_IMAGE}" ]; then
  echo "[INFO] ES_CUSTOM_IMAGE가 설정되지 않았습니다. 로컬 Docker Desktop 워크플로우로 push 단계를 건너뜁니다."
  echo "[NEXT] 로컬 이미지를 그대로 사용해 다음 단계를 실행하세요: ./sh_bin/00_6_start_elasticsearch_cluster_custom_image.sh"
  exit 0
fi

echo "[INFO] 소스 이미지: ${SOURCE_IMAGE}"
echo "[INFO] 대상 이미지: ${TARGET_IMAGE}"
set -x
if [ "${SOURCE_IMAGE}" != "${TARGET_IMAGE}" ]; then
  docker tag "${SOURCE_IMAGE}" "${TARGET_IMAGE}"
fi
docker push "${TARGET_IMAGE}"
set +x

echo "[OK] 이미지 push 완료: ${TARGET_IMAGE}"
