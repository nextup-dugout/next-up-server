package com.nextup.core.service.match

import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.MatchRequestNotFoundException
import com.nextup.common.exception.MatchResponseNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.league.League
import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.domain.match.MatchResponse
import com.nextup.core.domain.match.MatchResponseStatus
import com.nextup.core.domain.match.SkillLevel
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import com.nextup.core.port.repository.MatchResponseRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.match.dto.CreateMatchRequestDto
import com.nextup.core.service.match.dto.CreateMatchResponseDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalTime

class MatchingServiceTest {
    private lateinit var matchRequestRepository: MatchRequestRepositoryPort
    private lateinit var matchResponseRepository: MatchResponseRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var competitionRepository: CompetitionRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var matchingService: MatchingService

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var teamA: Team
    private lateinit var teamB: Team

    @BeforeEach
    fun setUp() {
        matchRequestRepository = mockk()
        matchResponseRepository = mockk()
        teamRepository = mockk()
        competitionRepository = mockk()
        gameRepository = mockk()
        gameTeamRepository = mockk()
        matchingService =
            MatchingService(
                matchRequestRepository = matchRequestRepository,
                matchResponseRepository = matchResponseRepository,
                teamRepository = teamRepository,
                competitionRepository = competitionRepository,
                gameRepository = gameRepository,
                gameTeamRepository = gameTeamRepository,
            )

        // 테스트 픽스처 설정
        association = Association(name = "서울시야구협회", id = 1L)
        league =
            League(
                association = association,
                name = "1부 리그",
                foundedYear = 2020,
                id = 1L,
            )
        teamA =
            Team(
                league = league,
                name = "A팀",
                city = "서울",
                foundedYear = 2020,
                id = 1L,
            )
        teamB =
            Team(
                league = league,
                name = "B팀",
                city = "서울",
                foundedYear = 2021,
                id = 2L,
            )
    }

    // ========== createRequest 테스트 ==========

    @Test
    fun `매칭 요청을 생성할 수 있다`() {
        // given
        val dto =
            CreateMatchRequestDto(
                teamId = 1L,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = "오후 2시",
                preferredLocation = "서울야구장",
                message = "연습 경기 희망합니다",
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        every { teamRepository.findByIdOrNull(1L) } returns teamA
        every { matchRequestRepository.save(any()) } answers { firstArg() }

        // when
        val result = matchingService.createRequest(dto)

        // then
        assertThat(result.team).isEqualTo(teamA)
        assertThat(result.preferredDate).isEqualTo(dto.preferredDate)
        assertThat(result.preferredTime).isEqualTo(dto.preferredTime)
        assertThat(result.preferredLocation).isEqualTo(dto.preferredLocation)
        assertThat(result.message).isEqualTo(dto.message)
        assertThat(result.skillLevel).isEqualTo(dto.skillLevel)
        assertThat(result.status).isEqualTo(MatchRequestStatus.OPEN)

        verify { teamRepository.findByIdOrNull(1L) }
        verify { matchRequestRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 팀으로 매칭 요청 생성 시 예외가 발생한다`() {
        // given
        val dto =
            CreateMatchRequestDto(
                teamId = 999L,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ANY,
            )

        every { teamRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            matchingService.createRequest(dto)
        }

        verify { teamRepository.findByIdOrNull(999L) }
    }

    // ========== getOpenRequests 테스트 ==========

    @Test
    fun `OPEN 상태의 모든 매칭 요청을 조회할 수 있다`() {
        // given
        val request1 =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )
        val request2 =
            MatchRequest.create(
                team = teamB,
                preferredDate = LocalDate.now().plusDays(10),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ADVANCED,
            )

        every { matchRequestRepository.findAllOpen() } returns listOf(request1, request2)

        // when
        val result = matchingService.getOpenRequests()

        // then
        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(request1, request2)

        verify { matchRequestRepository.findAllOpen() }
    }

    @Test
    fun `OPEN 상태의 매칭 요청이 없으면 빈 리스트를 반환한다`() {
        // given
        every { matchRequestRepository.findAllOpen() } returns emptyList()

        // when
        val result = matchingService.getOpenRequests()

        // then
        assertThat(result).isEmpty()

        verify { matchRequestRepository.findAllOpen() }
    }

    // ========== respondToRequest 테스트 ==========

    @Test
    fun `매칭 요청에 응답할 수 있다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val dto =
            CreateMatchResponseDto(
                matchRequestId = 1L,
                respondTeamId = 2L,
                message = "같이 경기하고 싶습니다",
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { teamRepository.findByIdOrNull(2L) } returns teamB
        every { matchResponseRepository.save(any()) } answers { firstArg() }

        // when
        val result = matchingService.respondToRequest(dto)

        // then
        assertThat(result.matchRequest).isEqualTo(matchRequest)
        assertThat(result.respondTeam).isEqualTo(teamB)
        assertThat(result.message).isEqualTo(dto.message)
        assertThat(result.status).isEqualTo(MatchResponseStatus.PENDING)

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { teamRepository.findByIdOrNull(2L) }
        verify { matchResponseRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 매칭 요청에 응답 시 예외가 발생한다`() {
        // given
        val dto =
            CreateMatchResponseDto(
                matchRequestId = 999L,
                respondTeamId = 2L,
                message = null,
            )

        every { matchRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MatchRequestNotFoundException> {
            matchingService.respondToRequest(dto)
        }

        verify { matchRequestRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `존재하지 않는 팀이 응답 시 예외가 발생한다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val dto =
            CreateMatchResponseDto(
                matchRequestId = 1L,
                respondTeamId = 999L,
                message = null,
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { teamRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            matchingService.respondToRequest(dto)
        }

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { teamRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `OPEN 상태가 아닌 매칭 요청에 응답 시 예외가 발생한다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )
        matchRequest.cancel() // CANCELLED 상태로 변경

        val dto =
            CreateMatchResponseDto(
                matchRequestId = 1L,
                respondTeamId = 2L,
                message = null,
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { teamRepository.findByIdOrNull(2L) } returns teamB

        // when & then
        assertThrows<InvalidStateException> {
            matchingService.respondToRequest(dto)
        }

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { teamRepository.findByIdOrNull(2L) }
    }

    // ========== acceptResponse 테스트 ==========

    @Test
    fun `매칭 응답을 수락하고 요청을 MATCHED 상태로 변경할 수 있다`() {
        // given
        val preferredDate = LocalDate.now().plusDays(7)
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = preferredDate,
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = null,
            )

        val competition =
            Competition(
                league = league,
                name = "${preferredDate.year}년 친선 경기",
                year = preferredDate.year,
                type = CompetitionType.FRIENDLY,
                startDate = LocalDate.of(preferredDate.year, 1, 1),
            )
        val game =
            Game(
                competition = competition,
                scheduledAt = preferredDate.atTime(LocalTime.of(0, 0)),
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(2L) } returns matchResponse
        every { competitionRepository.findByLeagueId(league.id) } returns listOf(competition)
        every { gameRepository.save(any()) } returns game
        every { gameTeamRepository.save(any()) } answers { firstArg() }

        // when
        val result = matchingService.acceptResponse(requestId = 1L, responseId = 2L)

        // then
        assertThat(result.status).isEqualTo(MatchRequestStatus.MATCHED)
        assertThat(matchResponse.status).isEqualTo(MatchResponseStatus.ACCEPTED)

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { matchResponseRepository.findByIdOrNull(2L) }
        verify { gameRepository.save(any()) }
        verify(exactly = 2) { gameTeamRepository.save(any()) }
    }

    @Test
    fun `매칭 성사 시 기존 친선 대회가 없으면 새로 생성하고 Game을 만든다`() {
        // given
        val preferredDate = LocalDate.now().plusDays(7)
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = preferredDate,
                preferredTime = "14:30",
                preferredLocation = "서울야구장",
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = null,
            )

        val newCompetition =
            Competition(
                league = league,
                name = "${preferredDate.year}년 친선 경기",
                year = preferredDate.year,
                type = CompetitionType.FRIENDLY,
                startDate = LocalDate.of(preferredDate.year, 1, 1),
            )
        val savedGame =
            Game(
                competition = newCompetition,
                scheduledAt = preferredDate.atTime(LocalTime.of(14, 30)),
                location = "서울야구장",
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(2L) } returns matchResponse
        every { competitionRepository.findByLeagueId(league.id) } returns emptyList()
        every { competitionRepository.save(any()) } returns newCompetition
        every { gameRepository.save(any()) } returns savedGame
        every { gameTeamRepository.save(any()) } answers { firstArg() }

        // when
        val result = matchingService.acceptResponse(requestId = 1L, responseId = 2L)

        // then
        assertThat(result.status).isEqualTo(MatchRequestStatus.MATCHED)
        verify { competitionRepository.save(any()) }
        verify { gameRepository.save(any()) }
        verify(exactly = 2) { gameTeamRepository.save(any()) }
    }

    @Test
    fun `매칭 성사 시 기존 친선 대회가 있으면 재사용하고 Game을 만든다`() {
        // given
        val preferredDate = LocalDate.now().plusDays(7)
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = preferredDate,
                preferredTime = null,
                preferredLocation = "인천구장",
                message = null,
                skillLevel = SkillLevel.ANY,
            )

        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = null,
            )

        val existingCompetition =
            Competition(
                league = league,
                name = "${preferredDate.year}년 친선 경기",
                year = preferredDate.year,
                type = CompetitionType.FRIENDLY,
                startDate = LocalDate.of(preferredDate.year, 1, 1),
            )
        val savedGame =
            Game(
                competition = existingCompetition,
                scheduledAt = preferredDate.atTime(LocalTime.of(0, 0)),
                location = "인천구장",
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(2L) } returns matchResponse
        every { competitionRepository.findByLeagueId(league.id) } returns listOf(existingCompetition)
        every { gameRepository.save(any()) } returns savedGame
        every { gameTeamRepository.save(any()) } answers { firstArg() }

        // when
        val result = matchingService.acceptResponse(requestId = 1L, responseId = 2L)

        // then
        assertThat(result.status).isEqualTo(MatchRequestStatus.MATCHED)
        verify(exactly = 0) { competitionRepository.save(any()) }
        verify { gameRepository.save(any()) }
        verify(exactly = 2) { gameTeamRepository.save(any()) }
    }

    @Test
    fun `매칭 성사 시 홈팀은 요청팀 원정팀은 응답팀으로 GameTeam이 생성된다`() {
        // given
        val preferredDate = LocalDate.now().plusDays(7)
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = preferredDate,
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = null,
            )

        val competition =
            Competition(
                league = league,
                name = "${preferredDate.year}년 친선 경기",
                year = preferredDate.year,
                type = CompetitionType.FRIENDLY,
                startDate = LocalDate.of(preferredDate.year, 1, 1),
            )
        val savedGame =
            Game(
                competition = competition,
                scheduledAt = preferredDate.atTime(LocalTime.of(0, 0)),
            )

        val savedGameTeams = mutableListOf<GameTeam>()

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(2L) } returns matchResponse
        every { competitionRepository.findByLeagueId(league.id) } returns listOf(competition)
        every { gameRepository.save(any()) } returns savedGame
        every { gameTeamRepository.save(any()) } answers {
            val gt = firstArg<GameTeam>()
            savedGameTeams.add(gt)
            gt
        }

        // when
        matchingService.acceptResponse(requestId = 1L, responseId = 2L)

        // then
        assertThat(savedGameTeams).hasSize(2)
        val homeGameTeam = savedGameTeams.first { it.homeAway == HomeAway.HOME }
        val awayGameTeam = savedGameTeams.first { it.homeAway == HomeAway.AWAY }
        assertThat(homeGameTeam.team).isEqualTo(teamA)
        assertThat(awayGameTeam.team).isEqualTo(teamB)
    }

    @Test
    fun `매칭 성사 시 선호 날짜와 시간이 경기 일정에 반영된다`() {
        // given
        val preferredDate = LocalDate.now().plusDays(7)
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = preferredDate,
                preferredTime = "09:00",
                preferredLocation = "잠실구장",
                message = null,
                skillLevel = SkillLevel.BEGINNER,
            )

        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = null,
            )

        val competition =
            Competition(
                league = league,
                name = "${preferredDate.year}년 친선 경기",
                year = preferredDate.year,
                type = CompetitionType.FRIENDLY,
                startDate = LocalDate.of(preferredDate.year, 1, 1),
            )

        val capturedGames = mutableListOf<Game>()
        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(2L) } returns matchResponse
        every { competitionRepository.findByLeagueId(league.id) } returns listOf(competition)
        every { gameRepository.save(any()) } answers {
            val g = firstArg<Game>()
            capturedGames.add(g)
            g
        }
        every { gameTeamRepository.save(any()) } answers { firstArg() }

        // when
        matchingService.acceptResponse(requestId = 1L, responseId = 2L)

        // then
        assertThat(capturedGames).hasSize(1)
        val createdGame = capturedGames.first()
        assertThat(createdGame.scheduledAt).isEqualTo(preferredDate.atTime(LocalTime.of(9, 0)))
        assertThat(createdGame.location).isEqualTo("잠실구장")
    }

    @Test
    fun `존재하지 않는 매칭 요청의 응답 수락 시 예외가 발생한다`() {
        // given
        every { matchRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MatchRequestNotFoundException> {
            matchingService.acceptResponse(requestId = 999L, responseId = 1L)
        }

        verify { matchRequestRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `존재하지 않는 매칭 응답 수락 시 예외가 발생한다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MatchResponseNotFoundException> {
            matchingService.acceptResponse(requestId = 1L, responseId = 999L)
        }

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { matchResponseRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `PENDING 상태가 아닌 응답 수락 시 예외가 발생한다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = null,
            )
        matchResponse.reject() // REJECTED 상태로 변경

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(2L) } returns matchResponse

        // when & then
        assertThrows<InvalidStateException> {
            matchingService.acceptResponse(requestId = 1L, responseId = 2L)
        }

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { matchResponseRepository.findByIdOrNull(2L) }
    }

    @Test
    fun `OPEN 상태가 아닌 요청의 응답 수락 시 예외가 발생한다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        // OPEN 상태에서 응답을 먼저 생성한 후 요청을 취소
        val matchResponse =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = null,
            )
        matchRequest.cancel() // CANCELLED 상태로 변경

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByIdOrNull(2L) } returns matchResponse

        // when & then
        assertThrows<InvalidStateException> {
            matchingService.acceptResponse(requestId = 1L, responseId = 2L)
        }

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { matchResponseRepository.findByIdOrNull(2L) }
    }

    // ========== cancelRequest 테스트 ==========

    @Test
    fun `매칭 요청을 취소할 수 있다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest

        // when
        val result = matchingService.cancelRequest(1L)

        // then
        assertThat(result.status).isEqualTo(MatchRequestStatus.CANCELLED)

        verify { matchRequestRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 매칭 요청 취소 시 예외가 발생한다`() {
        // given
        every { matchRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MatchRequestNotFoundException> {
            matchingService.cancelRequest(999L)
        }

        verify { matchRequestRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `OPEN 상태가 아닌 매칭 요청 취소 시 예외가 발생한다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )
        matchRequest.match() // MATCHED 상태로 변경

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest

        // when & then
        assertThrows<InvalidStateException> {
            matchingService.cancelRequest(1L)
        }

        verify { matchRequestRepository.findByIdOrNull(1L) }
    }

    // ========== getRequestById 테스트 ==========

    @Test
    fun `ID로 매칭 요청을 조회할 수 있다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest

        // when
        val result = matchingService.getRequestById(1L)

        // then
        assertThat(result).isEqualTo(matchRequest)

        verify { matchRequestRepository.findByIdOrNull(1L) }
    }

    @Test
    fun `존재하지 않는 ID로 매칭 요청 조회 시 예외가 발생한다`() {
        // given
        every { matchRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MatchRequestNotFoundException> {
            matchingService.getRequestById(999L)
        }

        verify { matchRequestRepository.findByIdOrNull(999L) }
    }

    // ========== getResponsesByRequest 테스트 ==========

    @Test
    fun `매칭 요청에 대한 응답 목록을 조회할 수 있다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val response1 =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = "응답1",
            )

        val response2 =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = "응답2",
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByMatchRequestId(1L) } returns listOf(response1, response2)

        // when
        val result = matchingService.getResponsesByRequest(1L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(response1, response2)

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { matchResponseRepository.findByMatchRequestId(1L) }
    }

    @Test
    fun `존재하지 않는 매칭 요청의 응답 조회 시 예외가 발생한다`() {
        // given
        every { matchRequestRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<MatchRequestNotFoundException> {
            matchingService.getResponsesByRequest(999L)
        }

        verify { matchRequestRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `매칭 요청에 응답이 없으면 빈 리스트를 반환한다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        every { matchRequestRepository.findByIdOrNull(1L) } returns matchRequest
        every { matchResponseRepository.findByMatchRequestId(1L) } returns emptyList()

        // when
        val result = matchingService.getResponsesByRequest(1L)

        // then
        assertThat(result).isEmpty()

        verify { matchRequestRepository.findByIdOrNull(1L) }
        verify { matchResponseRepository.findByMatchRequestId(1L) }
    }

    // ========== getRequestsByTeam 테스트 ==========

    @Test
    fun `팀의 매칭 요청 목록을 조회할 수 있다`() {
        // given
        val request1 =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val request2 =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(10),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.ADVANCED,
            )

        every { teamRepository.findByIdOrNull(1L) } returns teamA
        every { matchRequestRepository.findByTeamId(1L) } returns listOf(request1, request2)

        // when
        val result = matchingService.getRequestsByTeam(1L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(request1, request2)

        verify { teamRepository.findByIdOrNull(1L) }
        verify { matchRequestRepository.findByTeamId(1L) }
    }

    @Test
    fun `존재하지 않는 팀의 매칭 요청 조회 시 예외가 발생한다`() {
        // given
        every { teamRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            matchingService.getRequestsByTeam(999L)
        }

        verify { teamRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `팀의 매칭 요청이 없으면 빈 리스트를 반환한다`() {
        // given
        every { teamRepository.findByIdOrNull(1L) } returns teamA
        every { matchRequestRepository.findByTeamId(1L) } returns emptyList()

        // when
        val result = matchingService.getRequestsByTeam(1L)

        // then
        assertThat(result).isEmpty()

        verify { teamRepository.findByIdOrNull(1L) }
        verify { matchRequestRepository.findByTeamId(1L) }
    }

    // ========== getResponsesByTeam 테스트 ==========

    @Test
    fun `팀의 매칭 응답 목록을 조회할 수 있다`() {
        // given
        val matchRequest =
            MatchRequest.create(
                team = teamA,
                preferredDate = LocalDate.now().plusDays(7),
                preferredTime = null,
                preferredLocation = null,
                message = null,
                skillLevel = SkillLevel.INTERMEDIATE,
            )

        val response1 =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = "응답1",
            )

        val response2 =
            MatchResponse.create(
                matchRequest = matchRequest,
                respondTeam = teamB,
                message = "응답2",
            )

        every { teamRepository.findByIdOrNull(2L) } returns teamB
        every { matchResponseRepository.findByRespondTeamId(2L) } returns listOf(response1, response2)

        // when
        val result = matchingService.getResponsesByTeam(2L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(response1, response2)

        verify { teamRepository.findByIdOrNull(2L) }
        verify { matchResponseRepository.findByRespondTeamId(2L) }
    }

    @Test
    fun `존재하지 않는 팀의 매칭 응답 조회 시 예외가 발생한다`() {
        // given
        every { teamRepository.findByIdOrNull(999L) } returns null

        // when & then
        assertThrows<TeamNotFoundException> {
            matchingService.getResponsesByTeam(999L)
        }

        verify { teamRepository.findByIdOrNull(999L) }
    }

    @Test
    fun `팀의 매칭 응답이 없으면 빈 리스트를 반환한다`() {
        // given
        every { teamRepository.findByIdOrNull(2L) } returns teamB
        every { matchResponseRepository.findByRespondTeamId(2L) } returns emptyList()

        // when
        val result = matchingService.getResponsesByTeam(2L)

        // then
        assertThat(result).isEmpty()

        verify { teamRepository.findByIdOrNull(2L) }
        verify { matchResponseRepository.findByRespondTeamId(2L) }
    }
}
