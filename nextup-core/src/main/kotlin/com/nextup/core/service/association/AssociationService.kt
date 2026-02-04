package com.nextup.core.service.association

import com.nextup.common.exception.AssociationNameDuplicateException
import com.nextup.common.exception.AssociationNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.port.repository.AssociationRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 협회 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class AssociationService(
    private val associationRepository: AssociationRepositoryPort,
) {
    /**
     * 협회를 생성합니다.
     */
    @Transactional
    fun create(
        name: String,
        abbreviation: String? = null,
        region: String? = null,
        description: String? = null,
        logoUrl: String? = null,
        websiteUrl: String? = null,
    ): Association {
        // 이름 중복 체크
        if (associationRepository.existsByName(name)) {
            throw AssociationNameDuplicateException(name)
        }

        val association =
            Association(
                name = name,
                abbreviation = abbreviation,
                region = region,
                description = description,
                logoUrl = logoUrl,
                websiteUrl = websiteUrl,
            )

        return associationRepository.save(association)
    }

    /**
     * ID로 협회를 조회합니다.
     */
    fun getById(id: Long): Association =
        associationRepository.findByIdOrNull(id)
            ?: throw AssociationNotFoundException(id)

    /**
     * 활성화된 모든 협회를 조회합니다.
     */
    fun getAllActive(): List<Association> = associationRepository.findAllActive()

    /**
     * 모든 협회를 조회합니다 (관리자용).
     */
    fun getAll(): List<Association> = associationRepository.findAll()

    /**
     * 지역별 활성화된 협회를 조회합니다.
     */
    fun getActiveByRegion(region: String): List<Association> = associationRepository.findActiveByRegion(region)

    /**
     * 협회 정보를 수정합니다.
     */
    @Transactional
    fun update(
        id: Long,
        description: String? = null,
        logoUrl: String? = null,
        websiteUrl: String? = null,
    ): Association {
        val association = getById(id)
        association.updateInfo(
            description = description,
            logoUrl = logoUrl,
            websiteUrl = websiteUrl,
        )
        return association
    }

    /**
     * 협회를 비활성화합니다.
     */
    @Transactional
    fun deactivate(id: Long): Association {
        val association = getById(id)
        association.deactivate()
        return association
    }

    /**
     * 협회를 활성화합니다.
     */
    @Transactional
    fun activate(id: Long): Association {
        val association = getById(id)
        association.activate()
        return association
    }
}
