package com.nextup.infrastructure.service.competition

import com.nextup.common.exception.CompetitionNotFoundException
import com.nextup.common.exception.InvalidCompetitionStateException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.infrastructure.repository.competition.CompetitionRepository
import com.nextup.infrastructure.repository.league.LeagueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 대회 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class CompetitionService(
    private val competitionRepository: CompetitionRepository,
    private val leagueRepository: LeagueRepository
) {

    /**
     * 대회를 생성합니다.
     */
    @Transactional
    fun create(
        leagueId: Long,
        name: String,
        year: Int,
        season: Int = 1,
        type: CompetitionType = CompetitionType.LEAGUE,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        description: String? = null,
        maxTeams: Int? = null
    ): Competition {
        // 리그 존재 확인
        val league = leagueRepository.findById(leagueId)
            .orElseThrow { LeagueNotFoundException(leagueId) }

        // 동일한 리그, 연도, 시즌의 대회가 이미 존재하는지 확인
        competitionRepository.findByLeagueIdAndYearAndSeason(leagueId, year, season)?.let {
            throw InvalidCompetitionStateException(
                "Competition already exists for league $leagueId, year $year, season $season"
            )
        }

        val competition = Competition(
            league = league,
            name = name,
            year = year,
            season = season,
            type = type,
            startDate = startDate,
            endDate = endDate,
            description = description,
            maxTeams = maxTeams
        )

        return competitionRepository.save(competition)
    }

    /**
     * ID로 대회를 조회합니다.
     */
    fun getById(id: Long): Competition {
        return competitionRepository.findById(id)
            .orElseThrow { CompetitionNotFoundException(id) }
    }

    /**
     * ID로 대회를 조회합니다 (League 함께 조회).
     */
    fun getByIdWithLeague(id: Long): Competition {
        return competitionRepository.findByIdWithLeague(id)
            ?: throw CompetitionNotFoundException(id)
    }

    /**
     * 모든 대회를 조회합니다.
     */
    fun getAll(): List<Competition> {
        return competitionRepository.findAll()
    }

    /**
     * 리그별 대회 목록을 조회합니다.
     */
    fun getByLeagueId(leagueId: Long): List<Competition> {
        return competitionRepository.findByLeagueId(leagueId)
    }

    /**
     * 진행 중인 대회 목록을 조회합니다.
     */
    fun getInProgress(): List<Competition> {
        return competitionRepository.findInProgressCompetitions()
    }

    /**
     * 특정 상태의 대회 목록을 조회합니다.
     */
    fun getByStatus(status: CompetitionStatus): List<Competition> {
        return competitionRepository.findByStatus(status)
    }

    /**
     * 대회 정보를 수정합니다.
     */
    @Transactional
    fun update(
        id: Long,
        description: String?,
        endDate: LocalDate?
    ): Competition {
        val competition = getById(id)

        description?.let { competition.description = it }
        endDate?.let { competition.endDate = it }

        return competition
    }

    /**
     * 대회를 시작합니다.
     */
    @Transactional
    fun start(id: Long): Competition {
        val competition = getById(id)
        try {
            competition.start()
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot start competition")
        }
        return competition
    }

    /**
     * 대회를 완료합니다.
     */
    @Transactional
    fun complete(id: Long, endDate: LocalDate = LocalDate.now()): Competition {
        val competition = getById(id)
        try {
            competition.complete(endDate)
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot complete competition")
        }
        return competition
    }

    /**
     * 대회를 취소합니다.
     */
    @Transactional
    fun cancel(id: Long): Competition {
        val competition = getById(id)
        try {
            competition.cancel()
        } catch (e: IllegalArgumentException) {
            throw InvalidCompetitionStateException(e.message ?: "Cannot cancel competition")
        }
        return competition
    }

    /**
     * 대회를 연기합니다.
     */
    @Transactional
    fun postpone(id: Long): Competition {
        val competition = getById(id)
        if (competition.status != CompetitionStatus.SCHEDULED) {
            throw InvalidCompetitionStateException("Only scheduled competitions can be postponed")
        }
        competition.status = CompetitionStatus.POSTPONED
        return competition
    }
}
