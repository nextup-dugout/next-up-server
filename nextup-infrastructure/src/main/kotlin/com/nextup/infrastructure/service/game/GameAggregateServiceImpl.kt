package com.nextup.infrastructure.service.game

import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.GameAggregateService
import com.nextup.core.service.game.GameScheduleService
import com.nextup.core.service.game.GameTimelineService
import com.nextup.core.service.game.ScoresheetService
import com.nextup.core.service.game.dto.GameAggregateDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 경기 상세 통합 서비스 구현체
 *
 * 기존 서비스들을 조합하여 경기 상세 화면에 필요한 모든 데이터를 한 번에 수집합니다.
 * null-safe: 데이터가 없는 부분은 null 반환하여 클라이언트에서 처리.
 */
@Service
@Transactional(readOnly = true)
class GameAggregateServiceImpl(
    private val gameScheduleService: GameScheduleService,
    private val boxScoreService: BoxScoreService,
    private val gameTimelineService: GameTimelineService,
    private val scoresheetService: ScoresheetService,
) : GameAggregateService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getGameAggregate(gameId: Long): GameAggregateDto {
        // 경기 기본 정보 조회 (존재하지 않으면 GameNotFoundException)
        val gameDetail = gameScheduleService.getGameDetail(gameId)

        // 박스스코어 (출전 선수가 없으면 null)
        val boxScore =
            try {
                boxScoreService.getBoxScore(gameId)
            } catch (e: Exception) {
                log.debug("박스스코어 없음: gameId={}, message={}", gameId, e.message)
                null
            }

        // 타임라인 (이벤트가 없어도 빈 목록으로 반환)
        val timeline = gameTimelineService.getTimeline(gameId, null, null)

        // 공식 기록지 (출전 선수가 없으면 null)
        val scoresheet =
            try {
                scoresheetService.getScoresheet(gameId)
            } catch (e: Exception) {
                log.debug("공식 기록지 없음: gameId={}, message={}", gameId, e.message)
                null
            }

        return GameAggregateDto(
            gameDetail = gameDetail,
            boxScore = boxScore,
            timeline = timeline,
            scoresheet = scoresheet,
        )
    }
}
