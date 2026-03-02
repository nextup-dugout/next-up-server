package com.nextup.infrastructure.service.search

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SearchServiceImplTest {
    private val playerRepository: PlayerRepositoryPort = mockk()
    private val teamRepository: TeamRepositoryPort = mockk()
    private val competitionRepository: CompetitionRepositoryPort = mockk()

    private lateinit var service: SearchServiceImpl

    @BeforeEach
    fun setUp() {
        service =
            SearchServiceImpl(
                playerRepository,
                teamRepository,
                competitionRepository,
            )
    }

    @Nested
    @DisplayName("search")
    inner class Search {
        @Test
        @DisplayName("키워드로 선수, 팀, 대회를 검색한다")
        fun searchWithValidKeyword() {
            // given
            val player = mockk<Player>()
            every { player.id } returns 1L
            every { player.name } returns "홍길동"
            every { player.primaryPosition } returns Position.PITCHER
            every { player.profileImageUrl } returns null

            val team = mockk<Team>()
            every { team.id } returns 1L
            every { team.name } returns "홍팀"
            every { team.city } returns "서울"
            every { team.logoUrl } returns null
            every { team.isActive } returns true

            val league = mockk<League>()
            every { league.name } returns "테스트리그"

            val competition = mockk<Competition>()
            every { competition.id } returns 1L
            every { competition.name } returns "홍컵"
            every { competition.league } returns league
            every { competition.year } returns 2026

            every { playerRepository.findByNameContaining("홍") } returns listOf(player)
            every {
                teamRepository.findActiveTeamsByFilter(name = "홍", city = null)
            } returns listOf(team)
            every { competitionRepository.findAll() } returns listOf(competition)

            // when
            val result = service.search("홍", 5)

            // then
            assertThat(result.players).hasSize(1)
            assertThat(result.players[0].playerId).isEqualTo(1L)
            assertThat(result.players[0].playerName).isEqualTo("홍길동")
            assertThat(result.players[0].primaryPosition).isEqualTo(Position.PITCHER)
            assertThat(result.teams).hasSize(1)
            assertThat(result.teams[0].teamId).isEqualTo(1L)
            assertThat(result.teams[0].teamName).isEqualTo("홍팀")
            assertThat(result.teams[0].city).isEqualTo("서울")
            assertThat(result.competitions).hasSize(1)
            assertThat(result.competitions[0].competitionId).isEqualTo(1L)
            assertThat(result.competitions[0].competitionName).isEqualTo("홍컵")
            assertThat(result.competitions[0].leagueName).isEqualTo("테스트리그")
            assertThat(result.competitions[0].year).isEqualTo(2026)
        }

        @Test
        @DisplayName("빈 키워드는 IllegalArgumentException을 던진다")
        fun searchWithBlankKeyword() {
            assertThatThrownBy { service.search("   ", 5) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
        fun searchWithNoResults() {
            every { playerRepository.findByNameContaining("xyz") } returns emptyList()
            every {
                teamRepository.findActiveTeamsByFilter(name = "xyz", city = null)
            } returns emptyList()
            every { competitionRepository.findAll() } returns emptyList()

            val result = service.search("xyz", 5)

            assertThat(result.players).isEmpty()
            assertThat(result.teams).isEmpty()
            assertThat(result.competitions).isEmpty()
        }

        @Test
        @DisplayName("limit 파라미터로 각 카테고리별 결과 수를 제한한다")
        fun searchWithLimit() {
            val players =
                (1..10).map { i ->
                    val p = mockk<Player>()
                    every { p.id } returns i.toLong()
                    every { p.name } returns "선수$i"
                    every { p.primaryPosition } returns Position.PITCHER
                    every { p.profileImageUrl } returns null
                    p
                }

            every { playerRepository.findByNameContaining("선수") } returns players
            every {
                teamRepository.findActiveTeamsByFilter(name = "선수", city = null)
            } returns emptyList()
            every { competitionRepository.findAll() } returns emptyList()

            val result = service.search("선수", 3)

            assertThat(result.players).hasSize(3)
        }

        @Test
        @DisplayName("대회 검색은 이름 부분 일치로 필터링한다")
        fun searchFilterCompetitionByName() {
            val league = mockk<League>()
            every { league.name } returns "리그"

            val matchCompetition = mockk<Competition>()
            every { matchCompetition.id } returns 1L
            every { matchCompetition.name } returns "봄시즌컵"
            every { matchCompetition.league } returns league
            every { matchCompetition.year } returns 2026

            val nonMatchCompetition = mockk<Competition>()
            every { nonMatchCompetition.id } returns 2L
            every { nonMatchCompetition.name } returns "가을대회"

            every { playerRepository.findByNameContaining("봄") } returns emptyList()
            every {
                teamRepository.findActiveTeamsByFilter(name = "봄", city = null)
            } returns emptyList()
            every {
                competitionRepository.findAll()
            } returns listOf(matchCompetition, nonMatchCompetition)

            val result = service.search("봄", 5)

            assertThat(result.competitions).hasSize(1)
            assertThat(result.competitions[0].competitionName).isEqualTo("봄시즌컵")
        }

        @Test
        @DisplayName("대회 검색은 대소문자를 구분하지 않는다")
        fun searchCompetitionCaseInsensitive() {
            val league = mockk<League>()
            every { league.name } returns "리그"

            val competition = mockk<Competition>()
            every { competition.id } returns 1L
            every { competition.name } returns "Spring Cup"
            every { competition.league } returns league
            every { competition.year } returns 2026

            every { playerRepository.findByNameContaining("spring") } returns emptyList()
            every {
                teamRepository.findActiveTeamsByFilter(name = "spring", city = null)
            } returns emptyList()
            every { competitionRepository.findAll() } returns listOf(competition)

            val result = service.search("spring", 5)

            assertThat(result.competitions).hasSize(1)
            assertThat(result.competitions[0].competitionName).isEqualTo("Spring Cup")
        }

        @Test
        @DisplayName("teamName은 null로 반환된다")
        fun playerSearchDtoTeamNameIsNull() {
            val player = mockk<Player>()
            every { player.id } returns 1L
            every { player.name } returns "테스트"
            every { player.primaryPosition } returns Position.SHORTSTOP
            every { player.profileImageUrl } returns "http://img.png"

            every { playerRepository.findByNameContaining("테스트") } returns listOf(player)
            every {
                teamRepository.findActiveTeamsByFilter(name = "테스트", city = null)
            } returns emptyList()
            every { competitionRepository.findAll() } returns emptyList()

            val result = service.search("테스트", 5)

            assertThat(result.players[0].teamName).isNull()
            assertThat(result.players[0].profileImageUrl).isEqualTo("http://img.png")
        }
    }
}
