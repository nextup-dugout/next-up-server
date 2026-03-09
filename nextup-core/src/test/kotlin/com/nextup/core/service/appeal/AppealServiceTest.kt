package com.nextup.core.service.appeal

import com.nextup.common.exception.AppealNotFoundException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.appeal.Appeal
import com.nextup.core.domain.appeal.AppealStatus
import com.nextup.core.domain.appeal.AppealType
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.AppealRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.service.appeal.dto.CreateAppealRequest
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
import java.time.LocalDateTime

@DisplayName("AppealService")
class AppealServiceTest {
    private lateinit var appealRepository: AppealRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var appealService: AppealService

    @BeforeEach
    fun setUp() {
        appealRepository = mockk()
        gameRepository = mockk()
        appealService = AppealService(appealRepository, gameRepository)
    }

    @Nested
    @DisplayName("createAppeal")
    inner class CreateAppeal {
        @Test
        fun `이의 제기를 생성할 수 있다`() {
            // given
            val gameId = 1L
            val game = createGame(gameId)
            val request =
                CreateAppealRequest(
                    gameId = gameId,
                    appealerId = 100L,
                    appealerName = "홍길동",
                    type = AppealType.SCORING_ERROR,
                    title = "득점 기록 오류",
                    description = "3회말 득점이 누락되었습니다",
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { appealRepository.save(any()) } answers { firstArg() }

            // when
            val result = appealService.createAppeal(request)

            // then
            assertThat(result.game).isEqualTo(game)
            assertThat(result.appealerId).isEqualTo(100L)
            assertThat(result.appealerName).isEqualTo("홍길동")
            assertThat(result.type).isEqualTo(AppealType.SCORING_ERROR)
            assertThat(result.title).isEqualTo("득점 기록 오류")
            assertThat(result.description).isEqualTo("3회말 득점이 누락되었습니다")
            assertThat(result.status).isEqualTo(AppealStatus.PENDING)
            verify { appealRepository.save(any()) }
        }

        @Test
        fun `경기를 찾을 수 없으면 예외가 발생한다`() {
            // given
            val request =
                CreateAppealRequest(
                    gameId = 999L,
                    appealerId = 100L,
                    appealerName = "홍길동",
                    type = AppealType.SCORING_ERROR,
                    title = "득점 기록 오류",
                    description = "3회말 득점이 누락되었습니다",
                )

            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                appealService.createAppeal(request)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `기록 정정 타입으로 이의 제기를 생성할 수 있다`() {
            // given
            val gameId = 1L
            val game = createGame(gameId)
            val request =
                CreateAppealRequest(
                    gameId = gameId,
                    appealerId = 200L,
                    appealerName = "김철수",
                    type = AppealType.RECORD_CORRECTION,
                    title = "타점 정정 요청",
                    description = "2루타가 1루타로 잘못 기록됨",
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { appealRepository.save(any()) } answers { firstArg() }

            // when
            val result = appealService.createAppeal(request)

            // then
            assertThat(result.type).isEqualTo(AppealType.RECORD_CORRECTION)
            assertThat(result.appealerName).isEqualTo("김철수")
        }

        @Test
        fun `규칙 위반 타입으로 이의 제기를 생성할 수 있다`() {
            // given
            val gameId = 1L
            val game = createGame(gameId)
            val request =
                CreateAppealRequest(
                    gameId = gameId,
                    appealerId = 300L,
                    appealerName = "박영희",
                    type = AppealType.RULE_VIOLATION,
                    title = "DH 규칙 위반",
                    description = "상대팀이 DH 규칙을 위반했습니다",
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { appealRepository.save(any()) } answers { firstArg() }

            // when
            val result = appealService.createAppeal(request)

            // then
            assertThat(result.type).isEqualTo(AppealType.RULE_VIOLATION)
            assertThat(result.title).isEqualTo("DH 규칙 위반")
        }
    }

    @Nested
    @DisplayName("getAppealsByGame")
    inner class GetAppealsByGame {
        @Test
        fun `경기별 이의 제기 목록을 조회할 수 있다`() {
            // given
            val gameId = 1L
            val game = createGame(gameId)
            val appeals =
                listOf(
                    createAppeal(1L, game, 100L, "홍길동"),
                    createAppeal(2L, game, 200L, "김철수"),
                )

            every { appealRepository.findByGameId(gameId) } returns appeals

            // when
            val result = appealService.getAppealsByGame(gameId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.game == game }).isTrue()
        }

        @Test
        fun `해당 경기에 이의 제기가 없으면 빈 목록을 반환한다`() {
            // given
            val gameId = 1L
            every { appealRepository.findByGameId(gameId) } returns emptyList()

            // when
            val result = appealService.getAppealsByGame(gameId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getAppealsByAppealer")
    inner class GetAppealsByAppealer {
        @Test
        fun `신청자별 이의 제기 목록을 조회할 수 있다`() {
            // given
            val appealerId = 100L
            val game1 = createGame(1L)
            val game2 = createGame(2L)
            val appeals =
                listOf(
                    createAppeal(1L, game1, appealerId, "홍길동"),
                    createAppeal(2L, game2, appealerId, "홍길동"),
                )

            every { appealRepository.findByAppealerId(appealerId) } returns appeals

            // when
            val result = appealService.getAppealsByAppealer(appealerId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.appealerId == appealerId }).isTrue()
        }

        @Test
        fun `해당 신청자의 이의 제기가 없으면 빈 목록을 반환한다`() {
            // given
            val appealerId = 999L
            every { appealRepository.findByAppealerId(appealerId) } returns emptyList()

            // when
            val result = appealService.getAppealsByAppealer(appealerId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getAllPendingAppeals")
    inner class GetAllPendingAppeals {
        @Test
        fun `대기 중인 모든 이의 제기를 조회할 수 있다`() {
            // given
            val game = createGame(1L)
            val appeals =
                listOf(
                    createAppeal(1L, game, 100L, "홍길동"),
                    createAppeal(2L, game, 200L, "김철수"),
                )

            every { appealRepository.findByStatus(AppealStatus.PENDING) } returns appeals

            // when
            val result = appealService.getAllPendingAppeals()

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.status == AppealStatus.PENDING }).isTrue()
        }

        @Test
        fun `대기 중인 이의 제기가 없으면 빈 목록을 반환한다`() {
            // given
            every { appealRepository.findByStatus(AppealStatus.PENDING) } returns emptyList()

            // when
            val result = appealService.getAllPendingAppeals()

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getAllAppeals")
    inner class GetAllAppeals {
        @Test
        fun `모든 이의 제기를 조회할 수 있다`() {
            // given
            val game = createGame(1L)
            val appeals =
                listOf(
                    createAppeal(1L, game, 100L, "홍길동"),
                    createAppeal(2L, game, 200L, "김철수"),
                    createAppeal(3L, game, 300L, "박영희"),
                )

            every { appealRepository.findAll() } returns appeals

            // when
            val result = appealService.getAllAppeals()

            // then
            assertThat(result).hasSize(3)
        }
    }

    @Nested
    @DisplayName("getAppealsByStatus")
    inner class GetAppealsByStatus {
        @Test
        fun `승인된 이의 제기를 조회할 수 있다`() {
            // given
            val game = createGame(1L)
            val appeal = createAppeal(1L, game, 100L, "홍길동")
            appeal.approve(500L, "승인합니다")

            every { appealRepository.findByStatus(AppealStatus.APPROVED) } returns listOf(appeal)

            // when
            val result = appealService.getAppealsByStatus(AppealStatus.APPROVED)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(AppealStatus.APPROVED)
        }

        @Test
        fun `반려된 이의 제기를 조회할 수 있다`() {
            // given
            val game = createGame(1L)
            val appeal = createAppeal(1L, game, 100L, "홍길동")
            appeal.reject(500L, "증거 불충분")

            every { appealRepository.findByStatus(AppealStatus.REJECTED) } returns listOf(appeal)

            // when
            val result = appealService.getAppealsByStatus(AppealStatus.REJECTED)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(AppealStatus.REJECTED)
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `ID로 이의 제기를 조회할 수 있다`() {
            // given
            val id = 1L
            val game = createGame(1L)
            val appeal = createAppeal(id, game, 100L, "홍길동")

            every { appealRepository.findByIdOrNull(id) } returns appeal

            // when
            val result = appealService.getById(id)

            // then
            assertThat(result.id).isEqualTo(id)
            assertThat(result.appealerName).isEqualTo("홍길동")
        }

        @Test
        fun `이의 제기를 찾을 수 없으면 예외가 발생한다`() {
            // given
            val id = 999L
            every { appealRepository.findByIdOrNull(id) } returns null

            // when & then
            assertThatThrownBy {
                appealService.getById(id)
            }.isInstanceOf(AppealNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("approveAppeal")
    inner class ApproveAppeal {
        @Test
        fun `이의 제기를 승인할 수 있다`() {
            // given
            val appealId = 1L
            val reviewerId = 500L
            val comment = "증거가 충분하여 승인합니다"
            val game = createGame(1L)
            val appeal = createAppeal(appealId, game, 100L, "홍길동")

            every { appealRepository.findByIdOrNull(appealId) } returns appeal

            // when
            val result = appealService.approveAppeal(appealId, reviewerId, comment)

            // then
            assertThat(result.status).isEqualTo(AppealStatus.APPROVED)
            assertThat(result.reviewerId).isEqualTo(reviewerId)
            assertThat(result.reviewerComment).isEqualTo(comment)
            assertThat(result.reviewedAt).isNotNull()
        }

        @Test
        fun `코멘트 없이 이의 제기를 승인할 수 있다`() {
            // given
            val appealId = 1L
            val reviewerId = 500L
            val game = createGame(1L)
            val appeal = createAppeal(appealId, game, 100L, "홍길동")

            every { appealRepository.findByIdOrNull(appealId) } returns appeal

            // when
            val result = appealService.approveAppeal(appealId, reviewerId, null)

            // then
            assertThat(result.status).isEqualTo(AppealStatus.APPROVED)
            assertThat(result.reviewerId).isEqualTo(reviewerId)
            assertThat(result.reviewerComment).isNull()
            assertThat(result.reviewedAt).isNotNull()
        }

        @Test
        fun `이미 승인된 이의 제기는 다시 승인할 수 없다`() {
            // given
            val appealId = 1L
            val game = createGame(1L)
            val appeal = createAppeal(appealId, game, 100L, "홍길동")
            appeal.approve(500L, "이미 승인됨")

            every { appealRepository.findByIdOrNull(appealId) } returns appeal

            // when & then
            assertThatThrownBy {
                appealService.approveAppeal(appealId, 600L, "재승인 시도")
            }.isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `반려된 이의 제기는 승인할 수 없다`() {
            // given
            val appealId = 1L
            val game = createGame(1L)
            val appeal = createAppeal(appealId, game, 100L, "홍길동")
            appeal.reject(500L, "반려됨")

            every { appealRepository.findByIdOrNull(appealId) } returns appeal

            // when & then
            assertThatThrownBy {
                appealService.approveAppeal(appealId, 600L, "승인 시도")
            }.isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `존재하지 않는 이의 제기를 승인하면 예외가 발생한다`() {
            // given
            val appealId = 999L
            every { appealRepository.findByIdOrNull(appealId) } returns null

            // when & then
            assertThatThrownBy {
                appealService.approveAppeal(appealId, 500L, "승인")
            }.isInstanceOf(AppealNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("rejectAppeal")
    inner class RejectAppeal {
        @Test
        fun `이의 제기를 반려할 수 있다`() {
            // given
            val appealId = 1L
            val reviewerId = 500L
            val comment = "증거가 불충분하여 반려합니다"
            val game = createGame(1L)
            val appeal = createAppeal(appealId, game, 100L, "홍길동")

            every { appealRepository.findByIdOrNull(appealId) } returns appeal

            // when
            val result = appealService.rejectAppeal(appealId, reviewerId, comment)

            // then
            assertThat(result.status).isEqualTo(AppealStatus.REJECTED)
            assertThat(result.reviewerId).isEqualTo(reviewerId)
            assertThat(result.reviewerComment).isEqualTo(comment)
            assertThat(result.reviewedAt).isNotNull()
        }

        @Test
        fun `이미 반려된 이의 제기는 다시 반려할 수 없다`() {
            // given
            val appealId = 1L
            val game = createGame(1L)
            val appeal = createAppeal(appealId, game, 100L, "홍길동")
            appeal.reject(500L, "이미 반려됨")

            every { appealRepository.findByIdOrNull(appealId) } returns appeal

            // when & then
            assertThatThrownBy {
                appealService.rejectAppeal(appealId, 600L, "재반려 시도")
            }.isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `승인된 이의 제기는 반려할 수 없다`() {
            // given
            val appealId = 1L
            val game = createGame(1L)
            val appeal = createAppeal(appealId, game, 100L, "홍길동")
            appeal.approve(500L, "승인됨")

            every { appealRepository.findByIdOrNull(appealId) } returns appeal

            // when & then
            assertThatThrownBy {
                appealService.rejectAppeal(appealId, 600L, "반려 시도")
            }.isInstanceOf(InvalidStateException::class.java)
        }

        @Test
        fun `존재하지 않는 이의 제기를 반려하면 예외가 발생한다`() {
            // given
            val appealId = 999L
            every { appealRepository.findByIdOrNull(appealId) } returns null

            // when & then
            assertThatThrownBy {
                appealService.rejectAppeal(appealId, 500L, "반려")
            }.isInstanceOf(AppealNotFoundException::class.java)
        }
    }

    // Helper methods for creating test entities
    private fun createGame(id: Long): Game {
        val association = createAssociation(1L, "서울시야구협회")
        val league = createLeague(1L, "1부 리그", association)
        val competition = createCompetition(1L, league)

        val homeTeam =
            Team(
                league = league,
                name = "홈팀",
                city = "서울",
                foundedYear = 2020,
                id = 10L,
            )
        val awayTeam =
            Team(
                league = league,
                name = "원정팀",
                city = "부산",
                foundedYear = 2020,
                id = 20L,
            )
        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.now().plusDays(1),
            status = GameStatus.SCHEDULED,
            currentInning = 0,
            id = id,
        )
    }

    private fun createAppeal(
        id: Long,
        game: Game,
        appealerId: Long,
        appealerName: String,
    ): Appeal =
        Appeal.create(
            game = game,
            appealerId = appealerId,
            appealerName = appealerName,
            type = AppealType.SCORING_ERROR,
            title = "테스트 이의 제기",
            description = "테스트 설명",
        ).apply {
            val idField = Appeal::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createAssociation(
        id: Long,
        name: String,
    ): Association =
        Association(
            name = name,
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null,
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createLeague(
        id: Long,
        name: String,
        association: Association,
    ): League =
        League(
            association = association,
            name = name,
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null,
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createCompetition(
        id: Long,
        league: League,
    ): Competition =
        Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.now(),
            status = CompetitionStatus.SCHEDULED,
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
}
