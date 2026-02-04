package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("LineupEntry 엔티티 테스트")
class LineupEntryTest {
    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var competition: Competition
    private lateinit var game: Game
    private lateinit var team: Team
    private lateinit var lineupSubmission: LineupSubmission
    private lateinit var player: Player
    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            )
        game =
            Game(
                competition = competition,
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                location = "잠실야구장",
                status = GameStatus.SCHEDULED,
            )
        team = Team(league = league, name = "테스트팀", city = "서울", foundedYear = 2020)
        user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "감독",
            )
        lineupSubmission = LineupSubmission.create(game = game, team = team, submittedBy = user)
        player =
            Player(
                name = "홍길동",
                birthDate = LocalDate.of(1995, 1, 1),
                primaryPosition = Position.FIRST_BASE,
            )
    }

    @Test
    fun `선발 출전 선수로 LineupEntry를 생성할 수 있다`() {
        // when
        val lineupEntry =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.FIRST_BASE,
                battingOrder = 3,
                backNumber = 10,
                isStarter = true,
            )

        // then
        assertThat(lineupEntry.submission).isEqualTo(lineupSubmission)
        assertThat(lineupEntry.player).isEqualTo(player)
        assertThat(lineupEntry.position).isEqualTo(Position.FIRST_BASE)
        assertThat(lineupEntry.battingOrder).isEqualTo(3)
        assertThat(lineupEntry.backNumber).isEqualTo(10)
        assertThat(lineupEntry.isStarter).isTrue()
    }

    @Test
    fun `교체 선수로 LineupEntry를 생성할 수 있다`() {
        // when
        val lineupEntry =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.LEFT_FIELD,
                battingOrder = null,
                backNumber = 25,
                isStarter = false,
            )

        // then
        assertThat(lineupEntry.isStarter).isFalse()
        assertThat(lineupEntry.battingOrder).isNull()
    }

    @Test
    fun `투수는 타순이 없을 수 있다`() {
        // when
        val lineupEntry =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.STARTING_PITCHER,
                battingOrder = null,
                backNumber = 1,
                isStarter = true,
            )

        // then
        assertThat(lineupEntry.position).isEqualTo(Position.STARTING_PITCHER)
        assertThat(lineupEntry.battingOrder).isNull()
        assertThat(lineupEntry.isStarter).isTrue()
    }

    @Test
    fun `등번호가 없을 수 있다`() {
        // when
        val lineupEntry =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.CENTER_FIELD,
                battingOrder = 1,
                backNumber = null,
                isStarter = true,
            )

        // then
        assertThat(lineupEntry.backNumber).isNull()
    }

    @Test
    fun `지명타자로 LineupEntry를 생성할 수 있다`() {
        // when
        val lineupEntry =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.DESIGNATED_HITTER,
                battingOrder = 4,
                backNumber = 44,
                isStarter = true,
            )

        // then
        assertThat(lineupEntry.position).isEqualTo(Position.DESIGNATED_HITTER)
        assertThat(lineupEntry.battingOrder).isEqualTo(4)
        assertThat(lineupEntry.isStarter).isTrue()
    }

    @Test
    fun `타순은 1번부터 9번까지 지정할 수 있다`() {
        // when
        val lineupEntry1 =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.SHORTSTOP,
                battingOrder = 1,
                backNumber = 5,
                isStarter = true,
            )
        val lineupEntry9 =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.RIGHT_FIELD,
                battingOrder = 9,
                backNumber = 9,
                isStarter = true,
            )

        // then
        assertThat(lineupEntry1.battingOrder).isEqualTo(1)
        assertThat(lineupEntry9.battingOrder).isEqualTo(9)
    }

    @Test
    fun `BaseTimeEntity를 상속하여 생성시간과 수정시간을 가진다`() {
        // when
        val lineupEntry =
            LineupEntry(
                submission = lineupSubmission,
                player = player,
                position = Position.CATCHER,
                battingOrder = 2,
                backNumber = 22,
                isStarter = true,
            )

        // then
        assertThat(lineupEntry.createdAt).isNotNull()
        assertThat(lineupEntry.updatedAt).isNotNull()
    }
}
