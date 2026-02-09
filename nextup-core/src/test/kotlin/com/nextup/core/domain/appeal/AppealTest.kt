package com.nextup.core.domain.appeal

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Appeal 엔티티")
class AppealTest {
    private val mockGame = mockk<Game>(relaxed = true)
    private val mockLeague = mockk<League>(relaxed = true)
    private val mockHomeTeam = mockk<Team>(relaxed = true)
    private val mockAwayTeam = mockk<Team>(relaxed = true)

    init {
        every { mockGame.id } returns 1L
        every { mockLeague.id } returns 1L
        every { mockHomeTeam.id } returns 1L
        every { mockAwayTeam.id } returns 2L
    }

    @Test
    fun `should create appeal successfully`() {
        // when
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "득점 오류 정정 요청",
                description = "3회말 득점이 잘못 기록되었습니다",
            )

        // then
        assertThat(appeal.game).isEqualTo(mockGame)
        assertThat(appeal.appealerId).isEqualTo(100L)
        assertThat(appeal.appealerName).isEqualTo("홍길동")
        assertThat(appeal.type).isEqualTo(AppealType.SCORING_ERROR)
        assertThat(appeal.title).isEqualTo("득점 오류 정정 요청")
        assertThat(appeal.description).isEqualTo("3회말 득점이 잘못 기록되었습니다")
        assertThat(appeal.status).isEqualTo(AppealStatus.PENDING)
        assertThat(appeal.reviewerId).isNull()
        assertThat(appeal.reviewerComment).isNull()
        assertThat(appeal.reviewedAt).isNull()
    }

    @Test
    fun `should fail to create appeal with invalid appealer id`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            Appeal.create(
                game = mockGame,
                appealerId = 0L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "제목",
                description = "설명",
            )
        }
    }

    @Test
    fun `should fail to create appeal with blank appealer name`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "  ",
                type = AppealType.SCORING_ERROR,
                title = "제목",
                description = "설명",
            )
        }
    }

    @Test
    fun `should fail to create appeal with blank title`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "  ",
                description = "설명",
            )
        }
    }

    @Test
    fun `should fail to create appeal with blank description`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "제목",
                description = "  ",
            )
        }
    }

    @Test
    fun `should approve appeal successfully`() {
        // given
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "득점 오류",
                description = "설명",
            )

        // when
        appeal.approve(reviewerId = 200L, comment = "검토 완료")

        // then
        assertThat(appeal.status).isEqualTo(AppealStatus.APPROVED)
        assertThat(appeal.reviewerId).isEqualTo(200L)
        assertThat(appeal.reviewerComment).isEqualTo("검토 완료")
        assertThat(appeal.reviewedAt).isNotNull()
    }

    @Test
    fun `should approve appeal without comment`() {
        // given
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.RECORD_CORRECTION,
                title = "기록 정정",
                description = "설명",
            )

        // when
        appeal.approve(reviewerId = 200L, comment = null)

        // then
        assertThat(appeal.status).isEqualTo(AppealStatus.APPROVED)
        assertThat(appeal.reviewerId).isEqualTo(200L)
        assertThat(appeal.reviewerComment).isNull()
        assertThat(appeal.reviewedAt).isNotNull()
    }

    @Test
    fun `should fail to approve already approved appeal`() {
        // given
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "제목",
                description = "설명",
            )
        appeal.approve(reviewerId = 200L, comment = "승인")

        // when & then
        assertThrows<IllegalArgumentException> {
            appeal.approve(reviewerId = 300L, comment = "재승인")
        }
    }

    @Test
    fun `should reject appeal successfully`() {
        // given
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.RULE_VIOLATION,
                title = "규칙 위반 신고",
                description = "설명",
            )

        // when
        appeal.reject(reviewerId = 200L, comment = "증거 불충분")

        // then
        assertThat(appeal.status).isEqualTo(AppealStatus.REJECTED)
        assertThat(appeal.reviewerId).isEqualTo(200L)
        assertThat(appeal.reviewerComment).isEqualTo("증거 불충분")
        assertThat(appeal.reviewedAt).isNotNull()
    }

    @Test
    fun `should fail to reject appeal without comment`() {
        // given
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.OTHER,
                title = "기타 사항",
                description = "설명",
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            appeal.reject(reviewerId = 200L, comment = "  ")
        }
    }

    @Test
    fun `should fail to reject already rejected appeal`() {
        // given
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "제목",
                description = "설명",
            )
        appeal.reject(reviewerId = 200L, comment = "반려 사유")

        // when & then
        assertThrows<IllegalArgumentException> {
            appeal.reject(reviewerId = 300L, comment = "재반려")
        }
    }

    @Test
    fun `should fail to reject already approved appeal`() {
        // given
        val appeal =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "제목",
                description = "설명",
            )
        appeal.approve(reviewerId = 200L, comment = "승인")

        // when & then
        assertThrows<IllegalArgumentException> {
            appeal.reject(reviewerId = 300L, comment = "반려")
        }
    }

    @Test
    fun `should handle different appeal types`() {
        // when
        val scoringError =
            Appeal.create(
                game = mockGame,
                appealerId = 100L,
                appealerName = "선수1",
                type = AppealType.SCORING_ERROR,
                title = "득점 오류",
                description = "설명",
            )

        val recordCorrection =
            Appeal.create(
                game = mockGame,
                appealerId = 101L,
                appealerName = "선수2",
                type = AppealType.RECORD_CORRECTION,
                title = "기록 정정",
                description = "설명",
            )

        val ruleViolation =
            Appeal.create(
                game = mockGame,
                appealerId = 102L,
                appealerName = "감독1",
                type = AppealType.RULE_VIOLATION,
                title = "규칙 위반",
                description = "설명",
            )

        val other =
            Appeal.create(
                game = mockGame,
                appealerId = 103L,
                appealerName = "감독2",
                type = AppealType.OTHER,
                title = "기타",
                description = "설명",
            )

        // then
        assertThat(scoringError.type).isEqualTo(AppealType.SCORING_ERROR)
        assertThat(recordCorrection.type).isEqualTo(AppealType.RECORD_CORRECTION)
        assertThat(ruleViolation.type).isEqualTo(AppealType.RULE_VIOLATION)
        assertThat(other.type).isEqualTo(AppealType.OTHER)
    }
}
