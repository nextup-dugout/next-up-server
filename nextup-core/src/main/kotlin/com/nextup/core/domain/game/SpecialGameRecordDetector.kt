package com.nextup.core.domain.game

/**
 * 특수 경기 기록 감지기
 *
 * 경기 종료 시 양 팀의 기록을 분석하여 노히트/퍼펙트게임을 감지합니다.
 * 각 팀이 상대 팀에 대해 달성한 특수 기록을 개별적으로 감지합니다.
 *
 * 감지 기준:
 * - 노히트: 상대 팀의 총 안타 수(GameTeam.totalHits) = 0
 * - 퍼펙트게임: 노히트 조건 + 상대 투수 기록에서 볼넷/사구 허용 = 0 + 수비 실책 = 0
 * - 몰수승/취소/중단 경기에서는 감지하지 않음 (정상 종료/콜드게임만 대상)
 */
object SpecialGameRecordDetector {
    /**
     * 특수 기록 감지 결과
     *
     * @property teamId 특수 기록을 달성한 팀 ID
     * @property opponentTeamId 상대 팀 ID
     * @property record 달성한 특수 기록 유형
     */
    data class DetectionResult(
        val teamId: Long,
        val opponentTeamId: Long,
        val record: SpecialGameRecord,
    )

    /**
     * 경기의 특수 기록을 감지합니다.
     *
     * 양 팀 모두 개별적으로 검사하여, 각각 노히트/퍼펙트게임을 달성했는지 확인합니다.
     * 정상 종료(FINISHED) 또는 콜드게임(CALLED) 상태의 경기만 대상으로 합니다.
     *
     * @param game 검사할 경기
     * @param gameTeams 경기에 참여한 팀 목록 (정확히 2개)
     * @param battingRecordsByTeamId 팀 ID별 상대 팀의 타격 기록 맵 (key: 수비 팀 ID, value: 상대 타격 기록)
     * @param pitchingRecordsByTeamId 팀 ID별 투수 기록 맵
     * @param fieldingRecordsByTeamId 팀 ID별 수비 기록 맵
     * @return 감지된 특수 기록 목록 (없으면 빈 리스트)
     */
    fun detect(
        game: Game,
        gameTeams: List<GameTeam>,
        battingRecordsByTeamId: Map<Long, List<BattingRecord>>,
        pitchingRecordsByTeamId: Map<Long, List<PitchingRecord>>,
        fieldingRecordsByTeamId: Map<Long, List<FieldingRecord>>,
    ): List<DetectionResult> {
        if (!isEligibleForDetection(game)) {
            return emptyList()
        }

        if (gameTeams.size != 2) {
            return emptyList()
        }

        val results = mutableListOf<DetectionResult>()

        for (pitchingTeam in gameTeams) {
            val opponentTeam =
                gameTeams.first { it.id != pitchingTeam.id }

            val result =
                detectForTeam(
                    pitchingTeamId = pitchingTeam.team.id,
                    opponentTeam = opponentTeam,
                    pitchingRecords = pitchingRecordsByTeamId[pitchingTeam.team.id] ?: emptyList(),
                    fieldingRecords = fieldingRecordsByTeamId[pitchingTeam.team.id] ?: emptyList(),
                )

            if (result != null) {
                results.add(result)
            }
        }

        return results
    }

    /**
     * 특정 팀의 특수 기록을 감지합니다.
     *
     * @param pitchingTeamId 투수 팀 ID (특수 기록을 달성한 팀)
     * @param opponentTeam 상대 팀 GameTeam (안타 허용 수 확인용)
     * @param pitchingRecords 투수 팀의 투수 기록 목록
     * @param fieldingRecords 투수 팀의 수비 기록 목록
     * @return 감지된 특수 기록 (없으면 null)
     */
    private fun detectForTeam(
        pitchingTeamId: Long,
        opponentTeam: GameTeam,
        pitchingRecords: List<PitchingRecord>,
        fieldingRecords: List<FieldingRecord>,
    ): DetectionResult? {
        // 상대 팀의 총 안타가 0이 아니면 노히트 아님
        if (opponentTeam.totalHits > 0) {
            return null
        }

        // 투수 기록에서 볼넷/사구 합산
        val totalWalksAllowed = pitchingRecords.sumOf { it.walksAllowed }
        val totalHitBatsmen = pitchingRecords.sumOf { it.hitBatsmen }

        // 수비 기록에서 실책 합산
        val totalErrors = fieldingRecords.sumOf { it.errors }

        val record =
            if (totalWalksAllowed == 0 && totalHitBatsmen == 0 && totalErrors == 0) {
                SpecialGameRecord.PERFECT_GAME
            } else {
                SpecialGameRecord.NO_HITTER
            }

        return DetectionResult(
            teamId = pitchingTeamId,
            opponentTeamId = opponentTeam.team.id,
            record = record,
        )
    }

    /**
     * 경기가 특수 기록 감지 대상인지 확인합니다.
     *
     * 정상 종료(FINISHED) 또는 콜드게임(CALLED)만 대상입니다.
     * 몰수승(FORFEITED), 취소(CANCELLED), 중단(SUSPENDED) 등은 제외합니다.
     */
    private fun isEligibleForDetection(game: Game): Boolean =
        game.status == GameStatus.FINISHED || game.status == GameStatus.CALLED
}
