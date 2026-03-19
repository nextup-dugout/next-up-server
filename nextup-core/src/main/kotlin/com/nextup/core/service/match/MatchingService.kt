package com.nextup.core.service.match

import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.MatchRequestNotFoundException
import com.nextup.common.exception.MatchResponseNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchResponse
import com.nextup.core.port.repository.CompetitionRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import com.nextup.core.port.repository.MatchResponseRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.match.dto.CreateMatchRequestDto
import com.nextup.core.service.match.dto.CreateMatchResponseDto
import com.nextup.core.service.match.dto.MatchRequestFilterDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

/**
 * 매칭 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class MatchingService(
    private val matchRequestRepository: MatchRequestRepositoryPort,
    private val matchResponseRepository: MatchResponseRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
    private val competitionRepository: CompetitionRepositoryPort,
    private val gameRepository: GameRepositoryPort,
) {
    /**
     * 매칭 요청을 생성합니다.
     */
    @Transactional
    fun createRequest(dto: CreateMatchRequestDto): MatchRequest {
        val team =
            teamRepository.findByIdOrNull(dto.teamId)
                ?: throw TeamNotFoundException(dto.teamId)

        val matchRequest =
            MatchRequest.create(
                team = team,
                preferredDate = dto.preferredDate,
                preferredTime = dto.preferredTime,
                preferredLocation = dto.preferredLocation,
                message = dto.message,
                skillLevel = dto.skillLevel,
            )

        return matchRequestRepository.save(matchRequest)
    }

    /**
     * OPEN 상태의 모든 매칭 요청을 조회합니다.
     */
    fun getOpenRequests(): List<MatchRequest> = matchRequestRepository.findAllOpen()

    /**
     * OPEN 상태의 매칭 요청을 필터링하여 조회합니다.
     *
     * @param filter 필터 조건 (지역, 날짜, 실력 수준)
     */
    fun getOpenRequests(filter: MatchRequestFilterDto): List<MatchRequest> {
        if (!filter.hasAnyFilter()) {
            return getOpenRequests()
        }
        return matchRequestRepository.findAllOpenWithFilter(
            area = filter.area,
            date = filter.date,
            skillLevel = filter.skillLevel,
        )
    }

    /**
     * 매칭 요청에 응답합니다.
     */
    @Transactional
    fun respondToRequest(dto: CreateMatchResponseDto): MatchResponse {
        val matchRequest =
            matchRequestRepository.findByIdOrNull(dto.matchRequestId)
                ?: throw MatchRequestNotFoundException(dto.matchRequestId)

        val respondTeam =
            teamRepository.findByIdOrNull(dto.respondTeamId)
                ?: throw TeamNotFoundException(dto.respondTeamId)

        val matchResponse =
            try {
                MatchResponse.create(
                    matchRequest = matchRequest,
                    respondTeam = respondTeam,
                    message = dto.message,
                )
            } catch (e: IllegalArgumentException) {
                throw InvalidStateException(
                    "INVALID_MATCH_REQUEST_STATE",
                    e.message ?: "Cannot respond to match request",
                )
            }

        return matchResponseRepository.save(matchResponse)
    }

    /**
     * 매칭 응답을 수락하고 요청을 MATCHED 상태로 변경합니다.
     * 매칭 성사 후 Game과 GameTeam 2개를 자동 생성합니다.
     */
    @Transactional
    fun acceptResponse(
        requestId: Long,
        responseId: Long,
    ): MatchRequest {
        val matchRequest =
            matchRequestRepository.findByIdOrNull(requestId)
                ?: throw MatchRequestNotFoundException(requestId)

        val matchResponse =
            matchResponseRepository.findByIdOrNull(responseId)
                ?: throw MatchResponseNotFoundException(responseId)

        try {
            matchResponse.accept()
            matchRequest.match()
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_MATCH_STATE",
                e.message ?: "Cannot accept match response",
            )
        }

        createFriendlyGame(matchRequest, matchResponse)

        return matchRequest
    }

    /**
     * 매칭 성사 후 친선 경기(Game)와 참여 팀(GameTeam) 2개를 생성합니다.
     *
     * - 요청 팀(MatchRequest.team)이 홈팀, 응답 팀(MatchResponse.respondTeam)이 원정팀입니다.
     * - 해당 리그의 현재 연도 FRIENDLY 대회를 찾거나 없으면 새로 생성합니다.
     * - preferredDate + preferredTime(String) → LocalDateTime 으로 변환합니다.
     */
    private fun createFriendlyGame(
        matchRequest: MatchRequest,
        matchResponse: MatchResponse,
    ) {
        val homeTeam = matchRequest.team
        val awayTeam = matchResponse.respondTeam
        val league = homeTeam.league
        val year = matchRequest.preferredDate.year

        val competition =
            competitionRepository.findByLeagueId(league.id)
                .firstOrNull { it.type == CompetitionType.FRIENDLY && it.year == year }
                ?: competitionRepository.save(
                    Competition(
                        league = league,
                        name = "${year}년 친선 경기",
                        year = year,
                        type = CompetitionType.FRIENDLY,
                        startDate = LocalDate.of(year, 1, 1),
                    ),
                )

        val scheduledAt =
            matchRequest.preferredDate.atTime(
                matchRequest.preferredTime
                    ?.let { parseTime(it) }
                    ?: LocalTime.of(0, 0),
            )

        gameRepository.save(
            Game.create(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = scheduledAt,
                location = matchRequest.preferredLocation,
            ),
        )
    }

    /**
     * "오후 2시", "14:30" 등 다양한 형식의 시간 문자열을 LocalTime으로 파싱합니다.
     * 파싱 실패 시 00:00(자정)을 반환합니다.
     */
    private fun parseTime(timeStr: String): LocalTime {
        // "HH:mm" 형식 시도
        runCatching {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val hour = parts[0].trim().toInt()
                val minute = parts[1].trim().toInt()
                return LocalTime.of(hour, minute)
            }
        }
        // 파싱 실패 시 자정 반환
        return LocalTime.of(0, 0)
    }

    /**
     * 매칭 요청을 취소합니다.
     */
    @Transactional
    fun cancelRequest(requestId: Long): MatchRequest {
        val matchRequest =
            matchRequestRepository.findByIdOrNull(requestId)
                ?: throw MatchRequestNotFoundException(requestId)

        try {
            matchRequest.cancel()
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_MATCH_REQUEST_STATE",
                e.message ?: "Cannot cancel match request",
            )
        }

        return matchRequest
    }

    /**
     * ID로 매칭 요청을 조회합니다.
     */
    fun getRequestById(id: Long): MatchRequest =
        matchRequestRepository.findByIdOrNull(id)
            ?: throw MatchRequestNotFoundException(id)

    /**
     * 매칭 요청에 대한 응답 목록을 조회합니다.
     */
    fun getResponsesByRequest(requestId: Long): List<MatchResponse> {
        // 요청 존재 여부 확인
        matchRequestRepository.findByIdOrNull(requestId)
            ?: throw MatchRequestNotFoundException(requestId)

        return matchResponseRepository.findByMatchRequestId(requestId)
    }

    /**
     * 팀의 매칭 요청 목록을 조회합니다.
     */
    fun getRequestsByTeam(teamId: Long): List<MatchRequest> {
        // 팀 존재 여부 확인
        teamRepository.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)

        return matchRequestRepository.findByTeamId(teamId)
    }

    /**
     * 팀의 매칭 응답 목록을 조회합니다.
     */
    fun getResponsesByTeam(teamId: Long): List<MatchResponse> {
        // 팀 존재 여부 확인
        teamRepository.findByIdOrNull(teamId)
            ?: throw TeamNotFoundException(teamId)

        return matchResponseRepository.findByRespondTeamId(teamId)
    }
}
