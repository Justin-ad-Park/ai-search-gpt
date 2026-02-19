#!/usr/bin/env bash
set -euo pipefail

# 하위 호환용 래퍼 스크립트
# 기존 오탈자 파일명(veryfy)을 호출하던 환경을 깨지 않기 위해 남긴다.
# 실제 로직은 verify 스크립트로 위임한다.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[안내] 05_2_veryfyUserDictionary.sh는 오탈자 이름입니다."
echo "[안내] 05_2_verifyUserDictionary.sh를 실행합니다."
exec "${SCRIPT_DIR}/05_2_verifyUserDictionary.sh" "$@"
