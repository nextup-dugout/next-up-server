package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*

/**
 * 경기 참여 팀 엔티티
 *
 * 경기(Game)에 참여하는 팀(Team)을 나타냅니다.
 * 각 경기에는 홈팀과 원정팀 두 개의 GameTeam이 존재합니다.
 */
@Entity
@Table(
    name = "game_teams",
    indexes = [
        Index(name = "idx_game_teams_game", columnList = "game_id"),
        Index(name = "idx_game_teams_team", columnList = "team_id"),
        Index(name = "idx_game_teams_result", columnList = "result"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_game_teams_game_home_away",
            columnNames = ["game_id", "home_away"],
        ),
        UniqueConstraint(
            name = "uk_game_teams_game_team",
            columnNames = ["game_id", "team_id"],
        ),
    ],
)
class GameTeam(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @Enumerated(EnumType.STRING)
    @Column(name = "home_away", nullable = false, length = 10)
    val homeAway: HomeAway,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Column(name = "total_score", nullable = false)
    var totalScore: Int = 0
        protected set

    @Column(name = "total_hits", nullable = false)
    var totalHits: Int = 0
        protected set

    @Column(name = "total_errors", nullable = false)
    var totalErrors: Int = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var result: GameResult = GameResult.UNDECIDED
        protected set

    /**
     * 이닝별 점수 (JSON 형태로 저장)
     * 예: "0,0,1,0,2,0,0,3,1" (1회부터 9회까지)
     */
    @Column(name = "inning_scores", length = 100)
    var inningScores: String? = null
        protected set

    /**
     * 홈팀인지 확인합니다.
     */
    val isHome: Boolean
        get() = homeAway.isHome

    /**
     * 원정팀인지 확인합니다.
     */
    val isAway: Boolean
        get() = !homeAway.isHome

    /**
     * 점수를 추가합니다.
     */
    fun addScore(runs: Int) {
        require(runs >= 0) { "점수는 0 이상이어야 합니다." }
        totalScore += runs
    }

    /**
     * 안타를 추가합니다.
     */
    fun addHit() {
        totalHits++
    }

    /**
     * 실책을 추가합니다.
     */
    fun addError() {
        totalErrors++
    }

    /**
     * 이닝 점수를 기록합니다.
     */
    fun recordInningScore(
        inning: Int,
        runs: Int,
    ) {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(runs >= 0) { "점수는 0 이상이어야 합니다." }

        val scores = inningScores?.split(",")?.toMutableList() ?: mutableListOf()

        // 필요한 만큼 이닝 확장
        while (scores.size < inning) {
            scores.add("0")
        }

        scores[inning - 1] = runs.toString()
        inningScores = scores.joinToString(",")
    }

    /**
     * 특정 이닝에 득점을 추가합니다 (BoxScore 계산용).
     */
    fun addRunInInning(
        inning: Int,
        runs: Int = 1,
    ) {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(runs >= 0) { "점수는 0 이상이어야 합니다." }

        val scores = inningScores?.split(",")?.toMutableList() ?: mutableListOf()

        // 필요한 만큼 이닝 확장
        while (scores.size < inning) {
            scores.add("0")
        }

        val currentScore = scores[inning - 1].toIntOrNull() ?: 0
        scores[inning - 1] = (currentScore + runs).toString()
        inningScores = scores.joinToString(",")

        totalScore += runs
    }

    /**
     * 특정 이닝에서 득점을 차감합니다 (Undo용).
     */
    fun subtractRunInInning(
        inning: Int,
        runs: Int = 1,
    ) {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(runs >= 0) { "점수는 0 이상이어야 합니다." }

        val scores = inningScores?.split(",")?.toMutableList() ?: mutableListOf()

        if (inning <= scores.size) {
            val currentScore = scores[inning - 1].toIntOrNull() ?: 0
            scores[inning - 1] = maxOf(0, currentScore - runs).toString()
            inningScores = scores.joinToString(",")
        }

        totalScore = maxOf(0, totalScore - runs)
    }

    /**
     * 안타를 차감합니다 (Undo용).
     */
    fun subtractHit() {
        totalHits = maxOf(0, totalHits - 1)
    }

    /**
     * 특정 이닝의 점수를 조회합니다.
     */
    fun getInningScore(inning: Int): Int {
        require(inning >= 1) { "이닝은 1 이상이어야 합니다." }
        val scores = inningScores?.split(",") ?: return 0
        return if (inning <= scores.size) scores[inning - 1].toIntOrNull() ?: 0 else 0
    }

    /**
     * 경기 결과를 업데이트합니다.
     */
    fun updateResult(result: GameResult) {
        this.result = result
    }

    /**
     * 점수를 업데이트합니다.
     */
    fun updateScore(
        totalScore: Int,
        totalHits: Int,
        totalErrors: Int,
    ) {
        require(totalScore >= 0) { "점수는 0 이상이어야 합니다." }
        require(totalHits >= 0) { "안타 수는 0 이상이어야 합니다." }
        require(totalErrors >= 0) { "실책 수는 0 이상이어야 합니다." }

        this.totalScore = totalScore
        this.totalHits = totalHits
        this.totalErrors = totalErrors
    }

    /**
     * 득점을 정정합니다 (기록 정정용, 음수 델타 허용).
     * totalScore가 0 미만이 되지 않도록 보호합니다.
     */
    fun correctScore(delta: Int) {
        totalScore = maxOf(0, totalScore + delta)
    }

    /**
     * 안타 수를 정정합니다 (기록 정정용, 음수 델타 허용).
     * totalHits가 0 미만이 되지 않도록 보호합니다.
     */
    fun correctHits(delta: Int) {
        totalHits = maxOf(0, totalHits + delta)
    }

    /**
     * 경기 결과 표시 문자열을 반환합니다 (예: "5 - 3 승").
     */
    fun getScoreDisplay(opponentScore: Int): String = "$totalScore - $opponentScore ${result.displayName}"
}
