#!/usr/bin/env bash
set -euo pipefail
kubectl get pods -A | grep -E 'elastic|ai-search' || true
