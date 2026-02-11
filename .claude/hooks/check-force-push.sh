#!/bin/bash
set -e

# NEXT-UP Dangerous Git Command Hook
# Blocks: force push, hard reset, clean -f, and other destructive operations

INPUT=$(cat)

# Extract command from JSON input
if command -v jq &>/dev/null; then
  COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty' 2>/dev/null)
else
  COMMAND=$(echo "$INPUT" | grep -oP '"command"\s*:\s*"[^"]*"' | head -1 | grep -oP ':\s*"\K[^"]+')
fi

# Skip if no command detected
if [ -z "$COMMAND" ]; then
  exit 0
fi

# Block force push variants
if echo "$COMMAND" | grep -qE 'git\s+push\s+.*(-f|--force)'; then
  echo "BLOCKED: Force push is forbidden. It can overwrite remote history." >&2
  echo "Use 'git push --force-with-lease' if absolutely necessary (requires user approval)." >&2
  exit 2
fi

# Block hard reset
if echo "$COMMAND" | grep -qE 'git\s+reset\s+--hard'; then
  echo "BLOCKED: 'git reset --hard' is forbidden. It discards all local changes." >&2
  echo "Use 'git stash' to save changes before resetting." >&2
  exit 2
fi

# Block git clean -f
if echo "$COMMAND" | grep -qE 'git\s+clean\s+.*-f'; then
  echo "BLOCKED: 'git clean -f' is forbidden. It permanently deletes untracked files." >&2
  exit 2
fi

# Block rm -rf on project directories
if echo "$COMMAND" | grep -qE 'rm\s+(-rf|-fr)\s+'; then
  echo "BLOCKED: 'rm -rf' is forbidden. Use targeted file deletion instead." >&2
  exit 2
fi

exit 0
