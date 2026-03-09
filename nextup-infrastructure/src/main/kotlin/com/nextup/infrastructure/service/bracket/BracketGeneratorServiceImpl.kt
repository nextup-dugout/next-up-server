package com.nextup.infrastructure.service.bracket

import com.nextup.common.exception.BracketEntryNotFoundException
import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.game.Game
import com.nextup.core.port.repository.BracketEntryRepositoryPort
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.bracket.BracketGeneratorService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.log2
import kotlin.math.pow

@Service
@Transactional(readOnly = true)
class BracketGeneratorServiceImpl(
    private val bracketEntryRepository: BracketEntryRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val gameRepository: GameRepositoryPort,
) : BracketGeneratorService {
    @Transactional
    override fun generateSingleElimination(
        competitionId: Long,
        seededTeamIds: List<Long>,
    ): List<BracketEntry> {
        val competition = findCompetition(competitionId)
        validateTeamIds(seededTeamIds)

        // 기존 대진표 삭제
        bracketEntryRepository.deleteByCompetitionId(competitionId)

        val teamCount = seededTeamIds.size
        require(teamCount >= 2) { "최소 2개 팀이 필요합니다" }

        // 다음 2의 거듭제곱 계산
        val totalSlots = nextPowerOfTwo(teamCount)
        val byeCount = totalSlots - teamCount

        // 라운드 수 계산
        val totalRounds = log2(totalSlots.toDouble()).toInt()

        // 팀 매칭 생성 (seeding algorithm)
        val bracketEntries = mutableListOf<BracketEntry>()
        val firstRoundMatchCount = totalSlots / 2

        // 첫 라운드 매치 생성
        for (matchNumber in 1..firstRoundMatchCount) {
            val seed1 = matchNumber
            val seed2 = totalSlots - matchNumber + 1

            val team1Id = if (seed1 <= teamCount) seededTeamIds[seed1 - 1] else null
            val team2Id = if (seed2 <= teamCount) seededTeamIds[seed2 - 1] else null

            val team1 = team1Id?.let { teamRepository.findByIdOrNull(it) }
            val team2 = team2Id?.let { teamRepository.findByIdOrNull(it) }

            bracketEntries.add(
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = matchNumber,
                    team1 = team1,
                    team2 = team2,
                    bracketType = "WINNERS",
                    seed1 = seed1,
                    seed2 = seed2,
                ),
            )
        }

        // 이후 라운드 빈 엔트리 생성
        for (round in 2..totalRounds) {
            val matchCount = totalSlots / (2.0.pow(round).toInt())
            for (matchNumber in 1..matchCount) {
                bracketEntries.add(
                    BracketEntry(
                        competition = competition,
                        roundNumber = round,
                        matchNumber = matchNumber,
                        team1 = null,
                        team2 = null,
                        bracketType = "WINNERS",
                    ),
                )
            }
        }

        return bracketEntryRepository.saveAll(bracketEntries)
    }

    @Transactional
    override fun generateDoubleElimination(
        competitionId: Long,
        seededTeamIds: List<Long>,
    ): List<BracketEntry> {
        val competition = findCompetition(competitionId)
        validateTeamIds(seededTeamIds)

        // 기존 대진표 삭제
        bracketEntryRepository.deleteByCompetitionId(competitionId)

        val teamCount = seededTeamIds.size
        require(teamCount >= 2) { "최소 2개 팀이 필요합니다" }

        val totalSlots = nextPowerOfTwo(teamCount)
        val totalRounds = log2(totalSlots.toDouble()).toInt()

        val bracketEntries = mutableListOf<BracketEntry>()

        // Winners Bracket 생성 (Single Elimination과 동일)
        val firstRoundMatchCount = totalSlots / 2

        for (matchNumber in 1..firstRoundMatchCount) {
            val seed1 = matchNumber
            val seed2 = totalSlots - matchNumber + 1

            val team1Id = if (seed1 <= teamCount) seededTeamIds[seed1 - 1] else null
            val team2Id = if (seed2 <= teamCount) seededTeamIds[seed2 - 1] else null

            val team1 = team1Id?.let { teamRepository.findByIdOrNull(it) }
            val team2 = team2Id?.let { teamRepository.findByIdOrNull(it) }

            bracketEntries.add(
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = matchNumber,
                    team1 = team1,
                    team2 = team2,
                    bracketType = "WINNERS",
                    seed1 = seed1,
                    seed2 = seed2,
                ),
            )
        }

        // Winners Bracket 이후 라운드
        for (round in 2..totalRounds) {
            val matchCount = totalSlots / (2.0.pow(round).toInt())
            for (matchNumber in 1..matchCount) {
                bracketEntries.add(
                    BracketEntry(
                        competition = competition,
                        roundNumber = round,
                        matchNumber = matchNumber,
                        team1 = null,
                        team2 = null,
                        bracketType = "WINNERS",
                    ),
                )
            }
        }

        // Losers Bracket 생성 (간소화 버전: Winners Bracket과 동일한 구조)
        for (round in 1..totalRounds) {
            val matchCount = totalSlots / (2.0.pow(round).toInt())
            for (matchNumber in 1..matchCount) {
                bracketEntries.add(
                    BracketEntry(
                        competition = competition,
                        roundNumber = round,
                        matchNumber = matchNumber,
                        team1 = null,
                        team2 = null,
                        bracketType = "LOSERS",
                    ),
                )
            }
        }

        // Grand Final 추가
        bracketEntries.add(
            BracketEntry(
                competition = competition,
                roundNumber = totalRounds + 1,
                matchNumber = 1,
                team1 = null,
                team2 = null,
                bracketType = "FINAL",
            ),
        )

        return bracketEntryRepository.saveAll(bracketEntries)
    }

    override fun getBracket(competitionId: Long): List<BracketEntry> {
        findCompetition(competitionId)
        return bracketEntryRepository.findByCompetitionId(competitionId)
    }

    @Transactional
    override fun advanceWinner(
        bracketEntryId: Long,
        winnerTeamId: Long,
    ): BracketEntry {
        val bracketEntry =
            bracketEntryRepository.findByIdOrNull(bracketEntryId)
                ?: throw BracketEntryNotFoundException(bracketEntryId)

        val winnerTeam =
            teamRepository.findByIdOrNull(winnerTeamId)
                ?: throw InvalidInputException(
                    code = "TEAM_NOT_FOUND",
                    message = "팀을 찾을 수 없습니다: $winnerTeamId",
                )

        bracketEntry.recordWinner(winnerTeam)
        return bracketEntryRepository.save(bracketEntry)
    }

    @Transactional
    override fun createGameForBracketEntry(
        bracketEntryId: Long,
        scheduledAt: LocalDateTime,
        location: String?,
        fieldName: String?,
    ): BracketEntry {
        val bracketEntry =
            bracketEntryRepository.findByIdOrNull(bracketEntryId)
                ?: throw BracketEntryNotFoundException(bracketEntryId)

        val team1 =
            bracketEntry.team1
                ?: throw InvalidInputException(
                    code = "BRACKET_TEAMS_NOT_DECIDED",
                    message = "두 팀이 모두 결정된 경기에만 경기를 생성할 수 있습니다",
                )
        val team2 =
            bracketEntry.team2
                ?: throw InvalidInputException(
                    code = "BRACKET_TEAMS_NOT_DECIDED",
                    message = "두 팀이 모두 결정된 경기에만 경기를 생성할 수 있습니다",
                )

        if (bracketEntry.game != null) {
            throw InvalidInputException(
                code = "BRACKET_GAME_ALREADY_EXISTS",
                message = "이미 경기가 연결된 대진표 엔트리입니다: $bracketEntryId",
            )
        }

        val game =
            Game.create(
                competition = bracketEntry.competition,
                homeTeam = team1,
                awayTeam = team2,
                scheduledAt = scheduledAt,
                location = location,
                fieldName = fieldName,
            )
        val savedGame = gameRepository.save(game)

        bracketEntry.linkGame(savedGame)
        return bracketEntryRepository.save(bracketEntry)
    }

    private fun findCompetition(competitionId: Long): Competition =
        competitionRepository.findByIdOrNull(competitionId)
            ?: throw CompetitionNotFoundException(competitionId)

    private fun validateTeamIds(teamIds: List<Long>) {
        if (teamIds.isEmpty()) {
            throw InvalidInputException(
                code = "EMPTY_TEAM_LIST",
                message = "팀 목록이 비어있습니다",
            )
        }

        if (teamIds.distinct().size != teamIds.size) {
            throw InvalidInputException(
                code = "DUPLICATE_TEAMS",
                message = "중복된 팀이 존재합니다",
            )
        }
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) {
            power *= 2
        }
        return power
    }
}
