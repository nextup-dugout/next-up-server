package com.nextup.infrastructure.service.search

import com.nextup.common.exception.InvalidInputException
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.search.SearchService
import com.nextup.core.service.search.dto.CompetitionSearchDto
import com.nextup.core.service.search.dto.PlayerSearchDto
import com.nextup.core.service.search.dto.SearchResultDto
import com.nextup.core.service.search.dto.TeamSearchDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 통합 검색 서비스 구현
 *
 * 선수/팀/대회를 키워드로 동시 검색합니다.
 */
@Service
@Transactional(readOnly = true)
class SearchServiceImpl(
    private val playerRepository: PlayerRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
) : SearchService {
    override fun search(
        keyword: String,
        limit: Int,
    ): SearchResultDto {
        require(keyword.isNotBlank()) {
            "검색 키워드는 필수입니다"
        }
        if (keyword.length < MIN_KEYWORD_LENGTH) {
            throw InvalidInputException(
                "SEARCH_KEYWORD_TOO_SHORT",
                "검색 키워드는 최소 ${MIN_KEYWORD_LENGTH}자 이상이어야 합니다",
            )
        }

        val players =
            playerRepository
                .findByNameContaining(keyword)
                .take(limit)
                .map { player ->
                    PlayerSearchDto(
                        playerId = player.id,
                        playerName = player.name,
                        primaryPosition = player.primaryPosition,
                        profileImageUrl = player.profileImageUrl,
                        teamName = null,
                    )
                }

        val teams =
            teamRepository
                .findActiveTeamsByFilter(name = keyword, city = null)
                .take(limit)
                .map { team ->
                    TeamSearchDto(
                        teamId = team.id,
                        teamName = team.name,
                        city = team.city,
                        logoUrl = team.logoUrl,
                        isActive = team.isActive,
                    )
                }

        val competitions =
            competitionRepository
                .findByNameContaining(keyword, limit)
                .map { competition ->
                    CompetitionSearchDto(
                        competitionId = competition.id,
                        competitionName = competition.name,
                        leagueName = competition.league.name,
                        year = competition.year,
                    )
                }

        return SearchResultDto(
            players = players,
            teams = teams,
            competitions = competitions,
        )
    }

    companion object {
        private const val MIN_KEYWORD_LENGTH = 1
    }
}
