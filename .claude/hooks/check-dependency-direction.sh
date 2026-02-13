#!/bin/bash

# NEXT-UP Dependency Direction Hook
# Enforces: Core → Common ONLY, Common → NONE
# Blocks: Core importing from infra/api/backoffice/scorer
# Blocks: Common importing from any nextup module

INPUT=$(cat)

# Extract file_path from JSON input
if command -v jq &>/dev/null; then
  FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.command // empty' 2>/dev/null)
  CONTENT=$(echo "$INPUT" | jq -r '.tool_input.content // .tool_input.new_string // empty' 2>/dev/null)
else
  FILE_PATH=$(echo "$INPUT" | grep -oP '"file_path"\s*:\s*"[^"]*"' | head -1 | grep -oP ':\s*"\K[^"]+')
  CONTENT=$(echo "$INPUT" | grep -oP '"content"\s*:\s*"[^"]*"' | head -1 | grep -oP ':\s*"\K[^"]+')
  if [ -z "$CONTENT" ]; then
    CONTENT=$(echo "$INPUT" | grep -oP '"new_string"\s*:\s*"[^"]*"' | head -1 | grep -oP ':\s*"\K[^"]+')
  fi
fi

# Skip if no file path detected
if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# Skip non-Kotlin files
if [[ "$FILE_PATH" != *.kt ]]; then
  exit 0
fi

# Rule 1: nextup-core must NOT import from infra, api, backoffice, scorer
if [[ "$FILE_PATH" == *"nextup-core/"* ]]; then
  if echo "$CONTENT" | grep -qE 'import\s+com\.nextup\.(infrastructure|api|backoffice|scorer)'; then
    echo "BLOCKED: nextup-core cannot depend on infrastructure/api/backoffice/scorer modules." >&2
    echo "Dependency direction violation: Core must only depend on Common." >&2
    exit 2
  fi
fi

# Rule 2: nextup-common must NOT import from ANY other nextup module
if [[ "$FILE_PATH" == *"nextup-common/"* ]]; then
  if echo "$CONTENT" | grep -qE 'import\s+com\.nextup\.(core|infrastructure|api|backoffice|scorer)'; then
    echo "BLOCKED: nextup-common is a leaf module and cannot depend on any other nextup module." >&2
    echo "Dependency direction violation: Common must have zero dependencies." >&2
    exit 2
  fi
fi

# Rule 3: nextup-infrastructure must NOT import from api, backoffice, scorer
if [[ "$FILE_PATH" == *"nextup-infrastructure/"* ]]; then
  if echo "$CONTENT" | grep -qE 'import\s+com\.nextup\.(api|backoffice|scorer)'; then
    echo "BLOCKED: nextup-infrastructure cannot depend on api/backoffice/scorer modules." >&2
    echo "Dependency direction violation: Infrastructure must only depend on Core and Common." >&2
    exit 2
  fi
fi

# Rule 4: API layer modules must NOT cross-depend
if [[ "$FILE_PATH" == *"nextup-api/"* ]]; then
  if echo "$CONTENT" | grep -qE 'import\s+com\.nextup\.(backoffice|scorer)'; then
    echo "BLOCKED: nextup-api cannot depend on backoffice or scorer modules." >&2
    echo "API layer isolation violation." >&2
    exit 2
  fi
fi

if [[ "$FILE_PATH" == *"nextup-backoffice/"* ]]; then
  if echo "$CONTENT" | grep -qE 'import\s+com\.nextup\.(api|scorer)'; then
    echo "BLOCKED: nextup-backoffice cannot depend on api or scorer modules." >&2
    echo "API layer isolation violation." >&2
    exit 2
  fi
fi

if [[ "$FILE_PATH" == *"nextup-scorer/"* ]]; then
  if echo "$CONTENT" | grep -qE 'import\s+com\.nextup\.(api|backoffice)'; then
    echo "BLOCKED: nextup-scorer cannot depend on api or backoffice modules." >&2
    echo "API layer isolation violation." >&2
    exit 2
  fi
fi

exit 0
