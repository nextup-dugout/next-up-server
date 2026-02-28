package com.nextup.infrastructure.persistence.competition

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

@DisplayName("CompetitionPlayerRepositoryAdapter 테스트")
class CompetitionPlayerRepositoryAdapterTest {
    private lateinit var jpaRepository: CompetitionPlayerJpaRepository
    private lateinit var adapter: CompetitionPlayerRepositoryAdapter

    private lateinit var competition: Competition
    private lateinit var team: Team
    private lateinit var player: Player
    private lateinit var competitionPlayer: CompetitionPlayer

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = CompetitionPlayerRepositoryAdapter(jpaRepository)

        val association = Association(name = "서울시야구협회", abbreviation = "SBA", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                startDate = LocalDate.of(2025, 3, 1),
            )
        team = Team(league = league, name = "Tigers", city = "서울", foundedYear = 2020)
        player = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP, id = 1L)
        competitionPlayer = CompetitionPlayer.register(competition, team, player)
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `should save competition player`() {
            // given
            every { jpaRepository.save(competitionPlayer) } returns competitionPlayer

            // when
            val result = adapter.save(competitionPlayer)

            // then
            assertThat(result).isEqualTo(competitionPlayer)
            verify { jpaRepository.save(competitionPlayer) }
        }
    }

    @Nested
    @DisplayName("saveAll")
    inner class SaveAll {
        @Test
        fun `should save all competition players`() {
            // given
            val player2 = Player(name = "김철수", primaryPosition = Position.CATCHER, id = 2L)
            val competitionPlayer2 = CompetitionPlayer.register(competition, team, player2)
            val players = listOf(competitionPlayer, competitionPlayer2)
            every { jpaRepository.saveAll(players) } returns players

            // when
            val result = adapter.saveAll(players)

            // then
            assertThat(result).hasSize(2)
            verify { jpaRepository.saveAll(players) }
        }

        @Test
        fun `should save empty list`() {
            // given
            every { jpaRepository.saveAll(emptyList<CompetitionPlayer>()) } returns emptyList()

            // when
            val result = adapter.saveAll(emptyList())

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByIdOrNull")
    inner class FindByIdOrNull {
        @Test
        fun `should return competition player when found`() {
            // given
            val id = 1L
            every { jpaRepository.findById(id) } returns Optional.of(competitionPlayer)

            // when
            val result = adapter.findByIdOrNull(id)

            // then
            assertThat(result).isEqualTo(competitionPlayer)
        }

        @Test
        fun `should return null when not found`() {
            // given
            val id = 999L
            every { jpaRepository.findById(id) } returns Optional.empty()

            // when
            val result = adapter.findByIdOrNull(id)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByCompetitionId")
    inner class FindByCompetitionId {
        @Test
        fun `should return players for given competition`() {
            // given
            val competitionId = 1L
            every { jpaRepository.findByCompetitionId(competitionId) } returns listOf(competitionPlayer)

            // when
            val result = adapter.findByCompetitionId(competitionId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(competitionPlayer)
        }

        @Test
        fun `should return empty list when no players found`() {
            // given
            val competitionId = 999L
            every { jpaRepository.findByCompetitionId(competitionId) } returns emptyList()

            // when
            val result = adapter.findByCompetitionId(competitionId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByCompetitionIdAndTeamId")
    inner class FindByCompetitionIdAndTeamId {
        @Test
        fun `should return players for given competition and team`() {
            // given
            val competitionId = 1L
            val teamId = 1L
            every {
                jpaRepository.findByCompetitionIdAndTeamId(competitionId, teamId)
            } returns listOf(competitionPlayer)

            // when
            val result = adapter.findByCompetitionIdAndTeamId(competitionId, teamId)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should return empty list when no players found for team`() {
            // given
            val competitionId = 1L
            val teamId = 999L
            every {
                jpaRepository.findByCompetitionIdAndTeamId(competitionId, teamId)
            } returns emptyList()

            // when
            val result = adapter.findByCompetitionIdAndTeamId(competitionId, teamId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByCompetitionIdAndStatus")
    inner class FindByCompetitionIdAndStatus {
        @Test
        fun `should return active players for given competition`() {
            // given
            val competitionId = 1L
            every {
                jpaRepository.findByCompetitionIdAndStatus(competitionId, CompetitionPlayerStatus.ACTIVE)
            } returns listOf(competitionPlayer)

            // when
            val result = adapter.findByCompetitionIdAndStatus(competitionId, CompetitionPlayerStatus.ACTIVE)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should return suspended players for given competition`() {
            // given
            val competitionId = 1L
            competitionPlayer.suspend()
            every {
                jpaRepository.findByCompetitionIdAndStatus(competitionId, CompetitionPlayerStatus.SUSPENDED)
            } returns listOf(competitionPlayer)

            // when
            val result = adapter.findByCompetitionIdAndStatus(competitionId, CompetitionPlayerStatus.SUSPENDED)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(CompetitionPlayerStatus.SUSPENDED)
        }

        @Test
        fun `should return withdrawn players for given competition`() {
            // given
            val competitionId = 1L
            competitionPlayer.withdraw()
            every {
                jpaRepository.findByCompetitionIdAndStatus(competitionId, CompetitionPlayerStatus.WITHDRAWN)
            } returns listOf(competitionPlayer)

            // when
            val result = adapter.findByCompetitionIdAndStatus(competitionId, CompetitionPlayerStatus.WITHDRAWN)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(CompetitionPlayerStatus.WITHDRAWN)
        }
    }

    @Nested
    @DisplayName("findPlayerIdsByCompetitionId")
    inner class FindPlayerIdsByCompetitionId {
        @Test
        fun `should return player ids for given competition`() {
            // given
            val competitionId = 1L
            val expectedIds = setOf(1L, 2L, 3L)
            every { jpaRepository.findPlayerIdsByCompetitionId(competitionId) } returns expectedIds

            // when
            val result = adapter.findPlayerIdsByCompetitionId(competitionId)

            // then
            assertThat(result).isEqualTo(expectedIds)
        }

        @Test
        fun `should return empty set when no players registered`() {
            // given
            val competitionId = 999L
            every { jpaRepository.findPlayerIdsByCompetitionId(competitionId) } returns emptySet()

            // when
            val result = adapter.findPlayerIdsByCompetitionId(competitionId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findEligiblePlayerIdsByCompetitionId")
    inner class FindEligiblePlayerIdsByCompetitionId {
        @Test
        fun `should return eligible player ids for given competition`() {
            // given
            val competitionId = 1L
            val expectedIds = setOf(1L, 2L)
            every { jpaRepository.findEligiblePlayerIdsByCompetitionId(competitionId) } returns expectedIds

            // when
            val result = adapter.findEligiblePlayerIdsByCompetitionId(competitionId)

            // then
            assertThat(result).isEqualTo(expectedIds)
        }

        @Test
        fun `should return empty set when no eligible players`() {
            // given
            val competitionId = 1L
            every { jpaRepository.findEligiblePlayerIdsByCompetitionId(competitionId) } returns emptySet()

            // when
            val result = adapter.findEligiblePlayerIdsByCompetitionId(competitionId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByCompetitionIdAndPlayerId")
    inner class FindByCompetitionIdAndPlayerId {
        @Test
        fun `should return competition player when found`() {
            // given
            val competitionId = 1L
            val playerId = 1L
            every {
                jpaRepository.findByCompetitionIdAndPlayerId(competitionId, playerId)
            } returns competitionPlayer

            // when
            val result = adapter.findByCompetitionIdAndPlayerId(competitionId, playerId)

            // then
            assertThat(result).isEqualTo(competitionPlayer)
        }

        @Test
        fun `should return null when player not registered in competition`() {
            // given
            val competitionId = 1L
            val playerId = 999L
            every {
                jpaRepository.findByCompetitionIdAndPlayerId(competitionId, playerId)
            } returns null

            // when
            val result = adapter.findByCompetitionIdAndPlayerId(competitionId, playerId)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("existsByCompetitionIdAndPlayerId")
    inner class ExistsByCompetitionIdAndPlayerId {
        @Test
        fun `should return true when player is registered`() {
            // given
            val competitionId = 1L
            val playerId = 1L
            every {
                jpaRepository.existsByCompetitionIdAndPlayerId(competitionId, playerId)
            } returns true

            // when
            val result = adapter.existsByCompetitionIdAndPlayerId(competitionId, playerId)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when player is not registered`() {
            // given
            val competitionId = 1L
            val playerId = 999L
            every {
                jpaRepository.existsByCompetitionIdAndPlayerId(competitionId, playerId)
            } returns false

            // when
            val result = adapter.existsByCompetitionIdAndPlayerId(competitionId, playerId)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("deleteById")
    inner class DeleteById {
        @Test
        fun `should delete competition player by id`() {
            // given
            val id = 1L
            every { jpaRepository.deleteById(id) } returns Unit

            // when
            adapter.deleteById(id)

            // then
            verify { jpaRepository.deleteById(id) }
        }
    }
}
