package com.nextup.infrastructure.service.league

import com.nextup.common.exception.AssociationNotFoundException
import com.nextup.common.exception.LeagueNameDuplicateException
import com.nextup.common.exception.LeagueNotFoundException
import com.nextup.core.domain.league.League
import com.nextup.infrastructure.repository.association.AssociationRepository
import com.nextup.infrastructure.repository.league.LeagueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 리그 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class LeagueService(
    private val leagueRepository: LeagueRepository,
    private val associationRepository: AssociationRepository
) {

    /**
     * 리그를 생성합니다.
     */
    @Transactional
    fun create(
        associationId: Long,
        name: String,
        abbreviation: String? = null,
        foundedYear: Int,
        divisionLevel: Int? = null,
        description: String? = null,
        logoUrl: String? = null
    ): League {
        val association = associationRepository.findById(associationId)
            .orElseThrow { AssociationNotFoundException(associationId) }

        // 협회 내 이름 중복 체크
        if (leagueRepository.existsByAssociationIdAndName(associationId, name)) {
            throw LeagueNameDuplicateException(associationId, name)
        }

        val league = League(
            association = association,
            name = name,
            abbreviation = abbreviation,
            foundedYear = foundedYear,
            divisionLevel = divisionLevel,
            description = description,
            logoUrl = logoUrl
        )

        return leagueRepository.save(league)
    }

    /**
     * ID로 리그를 조회합니다.
     */
    fun getById(id: Long): League {
        return leagueRepository.findById(id)
            .orElseThrow { LeagueNotFoundException(id) }
    }

    /**
     * 활성화된 모든 리그를 조회합니다.
     */
    fun getAllActive(): List<League> {
        return leagueRepository.findAllActive()
    }

    /**
     * 모든 리그를 조회합니다 (관리자용).
     */
    fun getAll(): List<League> {
        return leagueRepository.findAll()
    }

    /**
     * 협회별 활성화된 리그를 조회합니다.
     */
    fun getActiveByAssociationId(associationId: Long): List<League> {
        return leagueRepository.findActiveByAssociationId(associationId)
    }

    /**
     * 협회별 모든 리그를 조회합니다 (관리자용).
     */
    fun getByAssociationId(associationId: Long): List<League> {
        return leagueRepository.findByAssociationId(associationId)
    }

    /**
     * 리그 정보를 수정합니다.
     */
    @Transactional
    fun update(
        id: Long,
        description: String? = null,
        logoUrl: String? = null
    ): League {
        val league = getById(id)
        league.updateInfo(
            description = description,
            logoUrl = logoUrl
        )
        return league
    }

    /**
     * 리그를 비활성화합니다.
     */
    @Transactional
    fun deactivate(id: Long): League {
        val league = getById(id)
        league.deactivate()
        return league
    }

    /**
     * 리그를 활성화합니다.
     */
    @Transactional
    fun activate(id: Long): League {
        val league = getById(id)
        league.activate()
        return league
    }
}
