package com.nextup.infrastructure.service.game

import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.GameScheduleService
import com.nextup.core.service.game.GameTimelineService
import com.nextup.core.service.game.ScoresheetService
import com.nextup.core.service.game.dto.BoxScoreDto
import com.nextup.core.service.game.dto.GameDetailDto
import com.nextup.core.service.game.dto.GameTimelineDto
import com.nextup.core.service.game.dto.ScoresheetDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GameAggregateServiceImpl")
class GameAggregateServiceImplTest {
    private lateinit var gameScheduleService: GameScheduleService
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameTimelineService: GameTimelineService
    private lateinit var scoresheetService: ScoresheetService
    private lateinit var service: GameAggregateServiceImpl

    @BeforeEach
    fun setUp() {
        gameScheduleService = mockk()
        boxScoreService = mockk()
        gameTimelineService = mockk()
        scoresheetService = mockk()
        service =
            GameAggregateServiceImpl(
                gameScheduleService,
                boxScoreService,
                gameTimelineService,
                scoresheetService,
            )
    }

    @Nested
    @DisplayName("getGameAggregate")
    inner class GetGameAggregate {
        @Test
        fun `경기 상세 통합 데이터를 반환한다`() {
            val gameDetail = mockk<GameDetailDto>()
            val boxScore = mockk<BoxScoreDto>()
            val timeline = mockk<GameTimelineDto>()
            val scoresheet = mockk<ScoresheetDto>()

            every { gameScheduleService.getGameDetail(1L) } returns gameDetail
            every { boxScoreService.getBoxScore(1L) } returns boxScore
            every { gameTimelineService.getTimeline(1L, null, null) } returns timeline
            every { scoresheetService.getScoresheet(1L) } returns scoresheet

            val result = service.getGameAggregate(1L)

            assertThat(result.gameDetail).isEqualTo(gameDetail)
            assertThat(result.boxScore).isEqualTo(boxScore)
            assertThat(result.timeline).isEqualTo(timeline)
            assertThat(result.scoresheet).isEqualTo(scoresheet)
        }

        @Test
        fun `박스스코어와 기록지가 없으면 null로 처리한다`() {
            val gameDetail = mockk<GameDetailDto>()
            val timeline = mockk<GameTimelineDto>()

            every { gameScheduleService.getGameDetail(1L) } returns gameDetail
            every { boxScoreService.getBoxScore(1L) } throws RuntimeException("없음")
            every { gameTimelineService.getTimeline(1L, null, null) } returns timeline
            every { scoresheetService.getScoresheet(1L) } throws RuntimeException("없음")

            val result = service.getGameAggregate(1L)

            assertThat(result.gameDetail).isEqualTo(gameDetail)
            assertThat(result.boxScore).isNull()
            assertThat(result.timeline).isEqualTo(timeline)
            assertThat(result.scoresheet).isNull()
        }

        @Test
        fun `경기가 존재하지 않으면 예외가 전파된다`() {
            every { gameScheduleService.getGameDetail(999L) } throws RuntimeException("경기를 찾을 수 없습니다")

            assertThatThrownBy { service.getGameAggregate(999L) }
                .isInstanceOf(RuntimeException::class.java)
        }
    }
}
