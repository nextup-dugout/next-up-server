package com.nextup.core.service.match

import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.MatchRequestNotFoundException
import com.nextup.common.exception.MatchResponseNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchResponse
import com.nextup.core.port.repository.MatchRequestRepositoryPort
import com.nextup.core.port.repository.MatchResponseRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.match.dto.CreateMatchRequestDto
import com.nextup.core.service.match.dto.CreateMatchResponseDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

        return matchRequest
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
