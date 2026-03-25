package com.nextup.core.domain.attendance

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class AttendancePollTest {
    private val association =
        Association(
            name = "테스트협회",
            region = "서울",
        )

    private val league =
        League(
            association = association,
            name = "테스트 리그",
            foundedYear = 2024,
        )

    private val team =
        Team(
            league = league,
            name = "테스트 팀",
            city = "서울",
            foundedYear = 2024,
        )

    @Test
    fun `출석 투표를 생성할 수 있다`() {
        // given
        val title = "이번 주 일요일 경기 출석 조사"
        val eventDate = LocalDateTime.now().plusDays(7)
        val deadline = LocalDateTime.now().plusDays(5)

        // when
        val poll =
            AttendancePoll.create(
                team = team,
                title = title,
                eventDate = eventDate,
                deadline = deadline,
            )

        // then
        assertThat(poll.title).isEqualTo(title)
        assertThat(poll.eventDate).isEqualTo(eventDate)
        assertThat(poll.deadline).isEqualTo(deadline)
        assertThat(poll.status).isEqualTo(PollStatus.OPEN)
        assertThat(poll.isOpen()).isTrue()
        assertThat(poll.version).isEqualTo(0L)
    }

    @Test
    fun `제목이 비어있으면 예외가 발생한다`() {
        // given
        val eventDate = LocalDateTime.now().plusDays(7)
        val deadline = LocalDateTime.now().plusDays(5)

        // when & then
        assertThrows<IllegalArgumentException> {
            AttendancePoll.create(
                team = team,
                title = "",
                eventDate = eventDate,
                deadline = deadline,
            )
        }
    }

    @Test
    fun `이벤트 날짜가 과거이면 예외가 발생한다`() {
        // given
        val eventDate = LocalDateTime.now().minusDays(1)
        val deadline = LocalDateTime.now().minusDays(2)

        // when & then
        assertThrows<IllegalArgumentException> {
            AttendancePoll.create(
                team = team,
                title = "테스트 투표",
                eventDate = eventDate,
                deadline = deadline,
            )
        }
    }

    @Test
    fun `마감 시간이 이벤트 날짜 이후이면 예외가 발생한다`() {
        // given
        val eventDate = LocalDateTime.now().plusDays(7)
        val deadline = LocalDateTime.now().plusDays(10)

        // when & then
        assertThrows<IllegalArgumentException> {
            AttendancePoll.create(
                team = team,
                title = "테스트 투표",
                eventDate = eventDate,
                deadline = deadline,
            )
        }
    }

    @Test
    fun `투표를 마감할 수 있다`() {
        // given
        val poll =
            AttendancePoll.create(
                team = team,
                title = "테스트 투표",
                eventDate = LocalDateTime.now().plusDays(7),
                deadline = LocalDateTime.now().plusDays(5),
            )

        // when
        poll.close()

        // then
        assertThat(poll.isClosed()).isTrue()
        assertThat(poll.isOpen()).isFalse()
    }

    @Test
    fun `이미 마감된 투표를 다시 마감하면 예외가 발생한다`() {
        // given
        val poll =
            AttendancePoll.create(
                team = team,
                title = "테스트 투표",
                eventDate = LocalDateTime.now().plusDays(7),
                deadline = LocalDateTime.now().plusDays(5),
            )
        poll.close()

        // when & then
        assertThrows<IllegalStateException> {
            poll.close()
        }
    }

    @Test
    fun `마감 시간이 지나면 투표할 수 없다`() {
        // given
        val poll =
            AttendancePoll.create(
                team = team,
                title = "테스트 투표",
                eventDate = LocalDateTime.now().plusDays(7),
                deadline = LocalDateTime.now().minusMinutes(1),
            )

        // when & then
        assertThat(poll.isExpired()).isTrue()
        assertThat(poll.canVote()).isFalse()
    }

    @Test
    fun `제목을 수정할 수 있다`() {
        // given
        val poll =
            AttendancePoll.create(
                team = team,
                title = "원래 제목",
                eventDate = LocalDateTime.now().plusDays(7),
                deadline = LocalDateTime.now().plusDays(5),
            )

        // when
        poll.updateTitle("변경된 제목")

        // then
        assertThat(poll.title).isEqualTo("변경된 제목")
    }

    @Test
    fun `제목을 빈 문자열로 수정하면 예외가 발생한다`() {
        // given
        val poll =
            AttendancePoll.create(
                team = team,
                title = "원래 제목",
                eventDate = LocalDateTime.now().plusDays(7),
                deadline = LocalDateTime.now().plusDays(5),
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            poll.updateTitle("")
        }
    }

    @Test
    fun `카테고리를 지정하여 투표를 생성할 수 있다`() {
        // given
        val eventDate = LocalDateTime.now().plusDays(7)
        val deadline = LocalDateTime.now().plusDays(5)

        // when
        val poll =
            AttendancePoll.create(
                team = team,
                title = "연습 출석 조사",
                eventDate = eventDate,
                deadline = deadline,
                category = EventCategory.PRACTICE,
            )

        // then
        assertThat(poll.category).isEqualTo(EventCategory.PRACTICE)
        assertThat(poll.gameId).isNull()
        assertThat(poll.isGamePoll()).isFalse()
    }

    @Test
    fun `경기용 투표를 생성할 수 있다`() {
        // given
        val eventDate = LocalDateTime.now().plusDays(7)
        val deadline = LocalDateTime.now().plusDays(6)
        val gameId = 100L

        // when
        val poll =
            AttendancePoll.createForGame(
                team = team,
                gameId = gameId,
                eventDate = eventDate,
                deadline = deadline,
            )

        // then
        assertThat(poll.category).isEqualTo(EventCategory.GAME)
        assertThat(poll.gameId).isEqualTo(gameId)
        assertThat(poll.isGamePoll()).isTrue()
        assertThat(poll.title).isEqualTo("경기 출석 조사")
        assertThat(poll.isOpen()).isTrue()
    }

    @Test
    fun `GAME 카테고리에 gameId가 없으면 예외가 발생한다`() {
        // given
        val eventDate = LocalDateTime.now().plusDays(7)
        val deadline = LocalDateTime.now().plusDays(5)

        // when & then
        assertThrows<IllegalArgumentException> {
            AttendancePoll.create(
                team = team,
                title = "경기 투표",
                eventDate = eventDate,
                deadline = deadline,
                category = EventCategory.GAME,
                gameId = null,
            )
        }
    }

    @Test
    fun `기본 카테고리는 OTHER이다`() {
        // given
        val eventDate = LocalDateTime.now().plusDays(7)
        val deadline = LocalDateTime.now().plusDays(5)

        // when
        val poll =
            AttendancePoll.create(
                team = team,
                title = "일반 투표",
                eventDate = eventDate,
                deadline = deadline,
            )

        // then
        assertThat(poll.category).isEqualTo(EventCategory.OTHER)
    }
}
