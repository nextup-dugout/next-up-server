package com.nextup.core.domain.schedule

/**
 * 라운드 로빈 대진표 자동 생성기
 *
 * N개 팀이 참가할 때, 모든 팀이 한 번씩 맞붙는 풀리그 대진표를 생성합니다.
 * Circle Method 알고리즘을 사용합니다.
 */
class RoundRobinScheduleGenerator {
    data class MatchPair(
        val homeTeamId: Long,
        val awayTeamId: Long,
        val round: Int,
    )

    /**
     * 라운드 로빈 대진표를 생성합니다.
     *
     * @param teamIds 참가 팀 ID 목록
     * @param doubleRoundRobin true면 홈/원정 교대로 2회전 (기본 false)
     * @return 라운드별 매치 목록
     */
    fun generate(
        teamIds: List<Long>,
        doubleRoundRobin: Boolean = false,
    ): List<MatchPair> {
        require(teamIds.size >= 2) { "최소 2개 팀이 필요합니다" }
        require(teamIds.distinct().size == teamIds.size) { "중복된 팀이 있습니다" }

        val teams = teamIds.toMutableList()
        val isOdd = teams.size % 2 == 1

        // 홀수 팀이면 BYE 추가 (더미 팀 ID = -1L)
        if (isOdd) {
            teams.add(-1L)
        }

        val n = teams.size
        val rounds = n - 1
        val matches = mutableListOf<MatchPair>()

        // Circle Method: 첫 번째 팀 고정, 나머지 회전
        for (round in 1..rounds) {
            val roundMatches = generateRound(teams, round)
            matches.addAll(roundMatches)
        }

        // Double round robin이면 홈/원정 교대로 추가 라운드 생성
        if (doubleRoundRobin) {
            val secondRoundMatches =
                matches.map { match ->
                    MatchPair(
                        homeTeamId = match.awayTeamId,
                        awayTeamId = match.homeTeamId,
                        round = match.round + rounds,
                    )
                }
            matches.addAll(secondRoundMatches)
        }

        // BYE 매치 제거
        return matches.filter { it.homeTeamId != -1L && it.awayTeamId != -1L }
    }

    /**
     * 특정 라운드의 매치를 생성합니다.
     */
    private fun generateRound(
        teams: List<Long>,
        round: Int,
    ): List<MatchPair> {
        val n = teams.size
        val matches = mutableListOf<MatchPair>()
        val rotation = (round - 1) % (n - 1)

        // Circle Method 회전 배치
        val rotated = rotateTeams(teams, rotation)

        // 매치 페어링
        for (i in 0 until n / 2) {
            val home = rotated[i]
            val away = rotated[n - 1 - i]

            // 홈/원정 균형: 라운드별로 교대
            val (homeTeam, awayTeam) =
                if (shouldSwapHomeAway(i, round)) {
                    away to home
                } else {
                    home to away
                }

            matches.add(
                MatchPair(
                    homeTeamId = homeTeam,
                    awayTeamId = awayTeam,
                    round = round,
                ),
            )
        }

        return matches
    }

    /**
     * Circle Method 회전을 수행합니다.
     * 첫 번째 팀(teams[0])은 고정, 나머지 시계 방향 회전
     */
    private fun rotateTeams(
        teams: List<Long>,
        rotation: Int,
    ): List<Long> {
        if (rotation == 0 || teams.size <= 1) return teams

        val fixed = teams[0]
        val rotating = teams.drop(1)

        // 회전: rotation만큼 오른쪽으로 회전
        val rotatedIndex = rotation % rotating.size
        val rotated = rotating.takeLast(rotatedIndex) + rotating.dropLast(rotatedIndex)

        return listOf(fixed) + rotated
    }

    /**
     * 홈/원정 균형을 위해 일부 매치의 홈/원정을 교대합니다.
     */
    private fun shouldSwapHomeAway(
        matchIndex: Int,
        round: Int,
    ): Boolean {
        // 라운드가 짝수일 때 첫 번째 매치는 교대
        // 라운드가 홀수일 때 두 번째 매치는 교대
        // 이렇게 하면 홈/원정이 균형있게 분배됨
        return (round % 2 == 0 && matchIndex % 2 == 0) ||
            (round % 2 == 1 && matchIndex % 2 == 1)
    }
}
