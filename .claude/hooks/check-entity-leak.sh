#!/bin/bash

# NEXT-UP Zero Entity Leak Hook
# Enforces: Controllers must never return domain entities directly
# All responses must be wrapped in DTO/Response types

INPUT=$(cat)

# Extract file_path and content from JSON input
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

# Only check Controller files
if [[ "$FILE_PATH" != *"/controller/"* ]]; then
  exit 0
fi

# Skip non-Kotlin files
if [[ "$FILE_PATH" != *.kt ]]; then
  exit 0
fi

# Skip test files
if [[ "$FILE_PATH" == *"/test/"* ]]; then
  exit 0
fi

# Known domain entity types from nextup-core
ENTITY_TYPES=(
  "Player" "Team" "Game" "League" "Competition"
  "Appeal" "Association" "Certificate" "Discipline"
  "Election" "Candidate" "ElectionVote"
  "BattingRecord" "PitchingRecord" "PitchEvent"
  "GameEvent" "GamePlayer" "GameTeam" "GameState"
  "LineupEntry" "LineupSubmission"
  "MatchRequest" "MatchResponse"
  "Notification" "NotificationPreference" "DeviceToken"
  "Stadium" "StadiumSlot" "StadiumBooking"
  "TeamMember" "TeamJoinRequest" "TeamBlacklist" "TeamRecruitment"
  "User" "OAuthAccount" "RefreshToken"
  "AttendancePoll" "AttendanceVote" "ActivityScore"
  "BracketEntry" "LeagueSchedule"
  "SeasonBattingStats" "SeasonPitchingStats"
  "CareerBattingStats" "CareerPitchingStats"
  "OrganizationAdmin" "PlayerCareer" "PlayerTeamHistory"
)

# Check for direct entity return types in function signatures
for ENTITY in "${ENTITY_TYPES[@]}"; do
  # Match patterns like: fun method(): Entity, fun method(): List<Entity>
  if echo "$CONTENT" | grep -qE "fun\s+\w+\(.*\)\s*:\s*(List<|Set<|Collection<)?${ENTITY}>?\s*(\{|=)"; then
    # Exclude if it's inside a Response/DTO wrapper
    MATCH=$(echo "$CONTENT" | grep -E "fun\s+\w+\(.*\)\s*:\s*(List<|Set<|Collection<)?${ENTITY}>?\s*(\{|=)" | head -1)
    # Allow if already wrapped in ApiResponse
    if echo "$MATCH" | grep -qE "ApiResponse<"; then
      continue
    fi
    # Allow if it's a Response/Dto type containing the entity name
    if echo "$MATCH" | grep -qE "(Response|Dto|DTO)"; then
      continue
    fi
    echo "WARNING: Possible Entity leak detected in Controller." >&2
    echo "Entity type '${ENTITY}' may be directly exposed in: $FILE_PATH" >&2
    echo "Use a Response/DTO wrapper instead. (Zero Entity Leak rule)" >&2
    exit 2
  fi
done

exit 0
