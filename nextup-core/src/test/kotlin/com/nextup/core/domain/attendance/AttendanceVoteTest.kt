package com.nextup.core.domain.attendance

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("AttendanceVote (Poll) 엔티티 테스트")
class AttendanceVoteTest {
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

    private val poll =
        AttendancePoll.create(
            team = team,
            title = "테스트 투표",
            eventDate = LocalDateTime.now().plusDays(7),
            deadline = LocalDateTime.now().plusDays(5),
        )

    private val player =
        Player(
            name = "홍길동",
            primaryPosition = Position.STARTING_PITCHER,
        )

    @Nested
    @DisplayName("출석 투표 응답 생성")
    inner class Create {
        @Test
        fun `출석 투표 응답을 생성할 수 있다`() {
            // when
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ATTEND,
                )

            // then
            assertThat(vote.poll).isEqualTo(poll)
            assertThat(vote.player).isEqualTo(player)
            assertThat(vote.voteType).isEqualTo(VoteType.ATTEND)
            assertThat(vote.absenceReason).isNull()
            assertThat(vote.reasonDetail).isNull()
        }

        @Test
        fun `불참 투표 시 사유를 입력할 수 있다`() {
            // when
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ABSENT,
                    absenceReason = AbsenceReason.INJURY,
                )

            // then
            assertThat(vote.voteType).isEqualTo(VoteType.ABSENT)
            assertThat(vote.absenceReason).isEqualTo(AbsenceReason.INJURY)
            assertThat(vote.reasonDetail).isNull()
        }

        @Test
        fun `미정 투표 시 사유를 입력할 수 있다`() {
            // when
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.UNDECIDED,
                    absenceReason = AbsenceReason.WORK,
                )

            // then
            assertThat(vote.voteType).isEqualTo(VoteType.UNDECIDED)
            assertThat(vote.absenceReason).isEqualTo(AbsenceReason.WORK)
        }

        @Test
        fun `기타 사유 선택 시 상세 사유를 입력할 수 있다`() {
            // when
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ABSENT,
                    absenceReason = AbsenceReason.OTHER,
                    reasonDetail = "개인 사정으로 불참합니다",
                )

            // then
            assertThat(vote.absenceReason).isEqualTo(AbsenceReason.OTHER)
            assertThat(vote.reasonDetail).isEqualTo("개인 사정으로 불참합니다")
        }

        @Test
        fun `참석 투표 시 불참 사유를 입력하면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ATTEND,
                    absenceReason = AbsenceReason.WORK,
                )
            }
        }

        @Test
        fun `참석 투표 시 상세 사유를 입력하면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ATTEND,
                    reasonDetail = "사유",
                )
            }
        }

        @Test
        fun `기타 외 사유에 상세 사유를 입력하면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ABSENT,
                    absenceReason = AbsenceReason.INJURY,
                    reasonDetail = "상세 사유",
                )
            }
        }

        @Test
        fun `마감된 투표에는 새로운 응답을 생성할 수 없다`() {
            // given
            poll.close()

            // when & then
            assertThrows<IllegalStateException> {
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ATTEND,
                )
            }
        }
    }

    @Nested
    @DisplayName("투표 변경")
    inner class ChangeVoteTest {
        @Test
        fun `투표를 변경할 수 있다`() {
            // given
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.UNDECIDED,
                )

            // when
            vote.changeVote(VoteType.ATTEND)

            // then
            assertThat(vote.voteType).isEqualTo(VoteType.ATTEND)
            assertThat(vote.absenceReason).isNull()
        }

        @Test
        fun `투표 변경 시 불참 사유를 입력할 수 있다`() {
            // given
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ATTEND,
                )

            // when
            vote.changeVote(VoteType.ABSENT, AbsenceReason.WEATHER)

            // then
            assertThat(vote.voteType).isEqualTo(VoteType.ABSENT)
            assertThat(vote.absenceReason).isEqualTo(AbsenceReason.WEATHER)
        }

        @Test
        fun `투표 변경 시 참석으로 변경하면 사유가 초기화된다`() {
            // given
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ABSENT,
                    absenceReason = AbsenceReason.INJURY,
                )

            // when
            vote.changeVote(VoteType.ATTEND)

            // then
            assertThat(vote.voteType).isEqualTo(VoteType.ATTEND)
            assertThat(vote.absenceReason).isNull()
            assertThat(vote.reasonDetail).isNull()
        }

        @Test
        fun `마감된 투표는 변경할 수 없다`() {
            // given - 투표를 먼저 생성 후 마감
            val vote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.UNDECIDED,
                )
            poll.close()

            // when & then
            assertThrows<IllegalStateException> {
                vote.changeVote(VoteType.ATTEND)
            }
        }
    }

    @Nested
    @DisplayName("투표 상태 확인")
    inner class StatusCheck {
        @Test
        fun `참석 의사 확인이 가능하다`() {
            // given
            val attendVote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ATTEND,
                )

            val absentVote =
                AttendanceVote.create(
                    poll = poll,
                    player = player,
                    voteType = VoteType.ABSENT,
                )

            // when & then
            assertThat(attendVote.isAttending()).isTrue()
            assertThat(attendVote.isAbsent()).isFalse()

            assertThat(absentVote.isAttending()).isFalse()
            assertThat(absentVote.isAbsent()).isTrue()
        }
    }
}
