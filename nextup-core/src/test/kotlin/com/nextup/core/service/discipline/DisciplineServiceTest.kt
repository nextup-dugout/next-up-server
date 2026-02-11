package com.nextup.core.service.discipline

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.DisciplineNotFoundException
import com.nextup.common.exception.InvalidDisciplineStateException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.discipline.Discipline
import com.nextup.core.domain.discipline.DisciplineStatus
import com.nextup.core.domain.discipline.DisciplineType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.DisciplineRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("DisciplineService")
class DisciplineServiceTest {
    private lateinit var disciplineRepository: DisciplineRepositoryPort
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var disciplineService: DisciplineService

    @BeforeEach
    fun setUp() {
        disciplineRepository = mockk()
        playerRepository = mockk()
        competitionRepository = mockk()
        disciplineService =
            DisciplineService(
                disciplineRepository,
                playerRepository,
                competitionRepository,
            )
    }

    @Nested
    @DisplayName("issueWarning")
    inner class IssueWarning {
        @Test
        fun `should issue warning successfully`() {
            // given
            val playerId = 1L
            val competitionId = 1L
            val player = createPlayer(playerId, "홍길동")
            val competition = createCompetition(competitionId, "2025 춘계대회")

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { competitionRepository.findByIdOrNull(competitionId) } returns competition
            every { disciplineRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                disciplineService.issueWarning(
                    playerId = playerId,
                    competitionId = competitionId,
                    reason = "과도한 항의",
                    issuedBy = "심판장",
                )

            // then
            assertThat(result.type).isEqualTo(DisciplineType.WARNING)
            assertThat(result.reason).isEqualTo("과도한 항의")
            assertThat(result.issuedBy).isEqualTo("심판장")
            verify { disciplineRepository.save(any()) }
        }

        @Test
        fun `should throw exception when player not found`() {
            // given
            val playerId = 999L
            val competitionId = 1L
            every { playerRepository.findByIdOrNull(playerId) } returns null

            // when & then
            assertThatThrownBy {
                disciplineService.issueWarning(
                    playerId = playerId,
                    competitionId = competitionId,
                    reason = "과도한 항의",
                    issuedBy = "심판장",
                )
            }.isInstanceOf(PlayerNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when competition not found`() {
            // given
            val playerId = 1L
            val competitionId = 999L
            val player = createPlayer(playerId, "홍길동")

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { competitionRepository.findByIdOrNull(competitionId) } returns null

            // when & then
            assertThatThrownBy {
                disciplineService.issueWarning(
                    playerId = playerId,
                    competitionId = competitionId,
                    reason = "과도한 항의",
                    issuedBy = "심판장",
                )
            }.isInstanceOf(CompetitionNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("issueSuspension")
    inner class IssueSuspension {
        @Test
        fun `should issue suspension successfully`() {
            // given
            val playerId = 1L
            val competitionId = 1L
            val player = createPlayer(playerId, "홍길동")
            val competition = createCompetition(competitionId, "2025 춘계대회")

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { competitionRepository.findByIdOrNull(competitionId) } returns competition
            every { disciplineRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                disciplineService.issueSuspension(
                    playerId = playerId,
                    competitionId = competitionId,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )

            // then
            assertThat(result.type).isEqualTo(DisciplineType.SUSPENSION)
            assertThat(result.suspensionGames).isEqualTo(3)
            assertThat(result.servedGames).isEqualTo(0)
            verify { disciplineRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("issueBan")
    inner class IssueBan {
        @Test
        fun `should issue ban successfully`() {
            // given
            val playerId = 1L
            val competitionId = 1L
            val player = createPlayer(playerId, "홍길동")
            val competition = createCompetition(competitionId, "2025 춘계대회")

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { competitionRepository.findByIdOrNull(competitionId) } returns competition
            every { disciplineRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                disciplineService.issueBan(
                    playerId = playerId,
                    competitionId = competitionId,
                    reason = "승부 조작",
                    issuedBy = "협회장",
                )

            // then
            assertThat(result.type).isEqualTo(DisciplineType.BAN)
            assertThat(result.reason).isEqualTo("승부 조작")
            verify { disciplineRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("cancelDiscipline")
    inner class CancelDiscipline {
        @Test
        fun `should cancel discipline successfully`() {
            // given
            val disciplineId = 1L
            val player = createPlayer(1L, "홍길동")
            val competition = createCompetition(1L, "2025 춘계대회")
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            every { disciplineRepository.findByIdOrNull(disciplineId) } returns discipline

            // when
            val result = disciplineService.cancelDiscipline(disciplineId)

            // then
            assertThat(result.status).isEqualTo(DisciplineStatus.CANCELLED)
        }

        @Test
        fun `should throw exception when discipline not found`() {
            // given
            val disciplineId = 999L
            every { disciplineRepository.findByIdOrNull(disciplineId) } returns null

            // when & then
            assertThatThrownBy {
                disciplineService.cancelDiscipline(disciplineId)
            }.isInstanceOf(DisciplineNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when discipline is already served`() {
            // given
            val disciplineId = 1L
            val player = createPlayer(1L, "홍길동")
            val competition = createCompetition(1L, "2025 춘계대회")
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 1,
                    issuedBy = "기술위원장",
                )
            discipline.markServed()

            every { disciplineRepository.findByIdOrNull(disciplineId) } returns discipline

            // when & then
            assertThatThrownBy {
                disciplineService.cancelDiscipline(disciplineId)
            }.isInstanceOf(InvalidDisciplineStateException::class.java)
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `should return discipline when found`() {
            // given
            val disciplineId = 1L
            val player = createPlayer(1L, "홍길동")
            val competition = createCompetition(1L, "2025 춘계대회")
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            every { disciplineRepository.findByIdOrNull(disciplineId) } returns discipline

            // when
            val result = disciplineService.getById(disciplineId)

            // then
            assertThat(result).isEqualTo(discipline)
        }

        @Test
        fun `should throw exception when not found`() {
            // given
            val disciplineId = 999L
            every { disciplineRepository.findByIdOrNull(disciplineId) } returns null

            // when & then
            assertThatThrownBy {
                disciplineService.getById(disciplineId)
            }.isInstanceOf(DisciplineNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getActiveDisciplines")
    inner class GetActiveDisciplines {
        @Test
        fun `should return only effective disciplines`() {
            // given
            val playerId = 1L
            val competitionId = 1L
            val player = createPlayer(playerId, "홍길동")
            val competition = createCompetition(competitionId, "2025 춘계대회")

            val activeDiscipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            val cancelledDiscipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고2",
                    issuedBy = "심판장",
                ).apply { cancel() }

            every {
                disciplineRepository.findActiveByPlayerIdAndCompetitionId(playerId, competitionId)
            } returns listOf(activeDiscipline, cancelledDiscipline)

            // when
            val result = disciplineService.getActiveDisciplines(playerId, competitionId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result.first().isEffective()).isTrue()
        }
    }

    @Nested
    @DisplayName("canPlayerPlay")
    inner class CanPlayerPlay {
        @Test
        fun `should return true when no active disciplines`() {
            // given
            val playerId = 1L
            val competitionId = 1L

            every {
                disciplineRepository.findActiveByPlayerIdAndCompetitionId(playerId, competitionId)
            } returns emptyList()

            // when
            val result = disciplineService.canPlayerPlay(playerId, competitionId)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when player has active ban`() {
            // given
            val playerId = 1L
            val competitionId = 1L
            val player = createPlayer(playerId, "홍길동")
            val competition = createCompetition(competitionId, "2025 춘계대회")

            val ban =
                Discipline.createBan(
                    player = player,
                    competition = competition,
                    reason = "승부 조작",
                    issuedBy = "협회장",
                )

            every {
                disciplineRepository.findActiveByPlayerIdAndCompetitionId(playerId, competitionId)
            } returns listOf(ban)

            // when
            val result = disciplineService.canPlayerPlay(playerId, competitionId)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `should return false when player has active suspension`() {
            // given
            val playerId = 1L
            val competitionId = 1L
            val player = createPlayer(playerId, "홍길동")
            val competition = createCompetition(competitionId, "2025 춘계대회")

            val suspension =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )

            every {
                disciplineRepository.findActiveByPlayerIdAndCompetitionId(playerId, competitionId)
            } returns listOf(suspension)

            // when
            val result = disciplineService.canPlayerPlay(playerId, competitionId)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `should return true when only warnings exist`() {
            // given
            val playerId = 1L
            val competitionId = 1L
            val player = createPlayer(playerId, "홍길동")
            val competition = createCompetition(competitionId, "2025 춘계대회")

            val warning =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            every {
                disciplineRepository.findActiveByPlayerIdAndCompetitionId(playerId, competitionId)
            } returns listOf(warning)

            // when
            val result = disciplineService.canPlayerPlay(playerId, competitionId)

            // then
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("incrementServedGames")
    inner class IncrementServedGames {
        @Test
        fun `should increment served games successfully`() {
            // given
            val disciplineId = 1L
            val player = createPlayer(1L, "홍길동")
            val competition = createCompetition(1L, "2025 춘계대회")
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )

            every { disciplineRepository.findByIdOrNull(disciplineId) } returns discipline

            // when
            val result = disciplineService.incrementServedGames(disciplineId)

            // then
            assertThat(result.servedGames).isEqualTo(1)
            assertThat(result.status).isEqualTo(DisciplineStatus.ACTIVE)
        }

        @Test
        fun `should mark as served when all games are completed`() {
            // given
            val disciplineId = 1L
            val player = createPlayer(1L, "홍길동")
            val competition = createCompetition(1L, "2025 춘계대회")
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 1,
                    issuedBy = "기술위원장",
                )

            every { disciplineRepository.findByIdOrNull(disciplineId) } returns discipline

            // when
            val result = disciplineService.incrementServedGames(disciplineId)

            // then
            assertThat(result.servedGames).isEqualTo(1)
            assertThat(result.status).isEqualTo(DisciplineStatus.SERVED)
        }

        @Test
        fun `should throw exception when discipline is not suspension`() {
            // given
            val disciplineId = 1L
            val player = createPlayer(1L, "홍길동")
            val competition = createCompetition(1L, "2025 춘계대회")
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            every { disciplineRepository.findByIdOrNull(disciplineId) } returns discipline

            // when & then
            assertThatThrownBy {
                disciplineService.incrementServedGames(disciplineId)
            }.isInstanceOf(InvalidDisciplineStateException::class.java)
        }
    }

    private fun createPlayer(
        id: Long,
        name: String,
    ): Player =
        Player(
            name = name,
            birthDate = LocalDate.of(1995, 5, 15),
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
        ).apply {
            val idField = Player::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createCompetition(
        id: Long,
        name: String,
    ): Competition {
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = "SBA",
                region = "서울",
            )

        val league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = "1st",
                foundedYear = 2020,
                divisionLevel = 1,
            )

        return Competition(
            league = league,
            name = name,
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
