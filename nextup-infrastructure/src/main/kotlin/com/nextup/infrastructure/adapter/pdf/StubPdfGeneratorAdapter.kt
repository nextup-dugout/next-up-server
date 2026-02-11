package com.nextup.infrastructure.adapter.pdf

import com.nextup.core.port.PdfGeneratorPort
import com.nextup.core.service.game.dto.ScoresheetDto
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * PDF 생성 어댑터 Stub 구현
 *
 * 실제 PDF 라이브러리 연동 전까지 사용하는 임시 구현체입니다.
 * 향후 iText, PDFBox 등의 라이브러리로 교체 예정입니다.
 */
@Component
class StubPdfGeneratorAdapter : PdfGeneratorPort {
    override fun generateScoresheetPdf(scoresheet: ScoresheetDto): ByteArray {
        val content = buildScoresheetText(scoresheet)
        return content.toByteArray(StandardCharsets.UTF_8)
    }

    private fun buildScoresheetText(scoresheet: ScoresheetDto): String =
        buildString {
            appendLine("=".repeat(80))
            appendLine("공식 기록지 (Official Scoresheet)")
            appendLine("=".repeat(80))
            appendLine()

            // 경기 정보
            with(scoresheet.gameInfo) {
                appendLine("경기 정보")
                appendLine("-".repeat(80))
                appendLine("대회: $competitionName")
                appendLine("일시: $scheduledAt")
                appendLine("장소: ${location ?: "미정"} ${fieldName?.let { "($it)" } ?: ""}")
                appendLine("상태: $status")
                appendLine()
            }

            // 팀 정보
            appendLine("경기 결과")
            appendLine("-".repeat(80))
            appendLine(
                String.format(
                    "%-20s %3d점 %3d안타 %3d실책 (%s)",
                    scoresheet.teams.away.teamName,
                    scoresheet.teams.away.totalScore,
                    scoresheet.teams.away.totalHits,
                    scoresheet.teams.away.totalErrors,
                    scoresheet.teams.away.result,
                ),
            )
            appendLine(
                String.format(
                    "%-20s %3d점 %3d안타 %3d실책 (%s)",
                    scoresheet.teams.home.teamName,
                    scoresheet.teams.home.totalScore,
                    scoresheet.teams.home.totalHits,
                    scoresheet.teams.home.totalErrors,
                    scoresheet.teams.home.result,
                ),
            )
            appendLine()

            // 이닝별 점수
            appendLine("이닝별 점수")
            appendLine("-".repeat(80))
            append("이닝   ")
            (1..scoresheet.inningScores.innings).forEach { append(String.format("%3d ", it)) }
            appendLine("  R   H   E")

            append("원정   ")
            scoresheet.inningScores.awayScores.forEach { append(String.format("%3d ", it)) }
            appendLine(
                String.format(
                    " %3d %3d %3d",
                    scoresheet.teams.away.totalScore,
                    scoresheet.teams.away.totalHits,
                    scoresheet.teams.away.totalErrors,
                ),
            )

            append("홈     ")
            scoresheet.inningScores.homeScores.forEach { append(String.format("%3d ", it)) }
            appendLine(
                String.format(
                    " %3d %3d %3d",
                    scoresheet.teams.home.totalScore,
                    scoresheet.teams.home.totalHits,
                    scoresheet.teams.home.totalErrors,
                ),
            )
            appendLine()

            // 타격 기록
            appendLine("타격 기록 - ${scoresheet.teams.away.teamName}")
            appendLine("-".repeat(80))
            appendLine("선수명            포지션  타석  타수  득점  안타  2루  3루  홈런  타점  볼넷  삼진  타율")
            scoresheet.battingRecords.away.forEach { batter ->
                appendLine(
                    String.format(
                        "%-15s %4s %4d %4d %4d %4d %3d %3d %4d %4d %4d %4d %5s",
                        batter.name,
                        batter.position,
                        batter.plateAppearances,
                        batter.atBats,
                        batter.runs,
                        batter.hits,
                        batter.doubles,
                        batter.triples,
                        batter.homeRuns,
                        batter.rbis,
                        batter.walks,
                        batter.strikeouts,
                        batter.avg,
                    ),
                )
            }
            appendLine()

            appendLine("타격 기록 - ${scoresheet.teams.home.teamName}")
            appendLine("-".repeat(80))
            appendLine("선수명            포지션  타석  타수  득점  안타  2루  3루  홈런  타점  볼넷  삼진  타율")
            scoresheet.battingRecords.home.forEach { batter ->
                appendLine(
                    String.format(
                        "%-15s %4s %4d %4d %4d %4d %3d %3d %4d %4d %4d %4d %5s",
                        batter.name,
                        batter.position,
                        batter.plateAppearances,
                        batter.atBats,
                        batter.runs,
                        batter.hits,
                        batter.doubles,
                        batter.triples,
                        batter.homeRuns,
                        batter.rbis,
                        batter.walks,
                        batter.strikeouts,
                        batter.avg,
                    ),
                )
            }
            appendLine()

            // 투수 기록
            appendLine("투수 기록 - ${scoresheet.teams.away.teamName}")
            appendLine("-".repeat(80))
            appendLine("선수명            이닝  피안타  실점  자책  볼넷  삼진  홈런  결정  평균자책")
            scoresheet.pitchingRecords.away.forEach { pitcher ->
                appendLine(
                    String.format(
                        "%-15s %5s %6d %4d %4d %4d %4d %4d %4s %8s",
                        pitcher.name,
                        pitcher.inningsPitched,
                        pitcher.hitsAllowed,
                        pitcher.runsAllowed,
                        pitcher.earnedRuns,
                        pitcher.walks,
                        pitcher.strikeouts,
                        pitcher.homeRunsAllowed,
                        pitcher.decision ?: "-",
                        pitcher.era,
                    ),
                )
            }
            appendLine()

            appendLine("투수 기록 - ${scoresheet.teams.home.teamName}")
            appendLine("-".repeat(80))
            appendLine("선수명            이닝  피안타  실점  자책  볼넷  삼진  홈런  결정  평균자책")
            scoresheet.pitchingRecords.home.forEach { pitcher ->
                appendLine(
                    String.format(
                        "%-15s %5s %6d %4d %4d %4d %4d %4d %4s %8s",
                        pitcher.name,
                        pitcher.inningsPitched,
                        pitcher.hitsAllowed,
                        pitcher.runsAllowed,
                        pitcher.earnedRuns,
                        pitcher.walks,
                        pitcher.strikeouts,
                        pitcher.homeRunsAllowed,
                        pitcher.decision ?: "-",
                        pitcher.era,
                    ),
                )
            }
            appendLine()

            // 주요 이벤트
            if (scoresheet.keyEvents.isNotEmpty()) {
                appendLine("주요 이벤트")
                appendLine("-".repeat(80))
                scoresheet.keyEvents.take(20).forEach { event ->
                    appendLine("[${event.inning}] ${event.timestamp} - ${event.description}")
                }
            }

            appendLine()
            appendLine("=".repeat(80))
        }
}
