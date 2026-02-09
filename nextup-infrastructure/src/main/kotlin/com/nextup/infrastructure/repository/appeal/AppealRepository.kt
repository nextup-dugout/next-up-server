package com.nextup.infrastructure.repository.appeal

import com.nextup.core.domain.appeal.Appeal
import com.nextup.core.domain.appeal.AppealStatus
import com.nextup.core.port.repository.AppealRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface AppealRepository :
    JpaRepository<Appeal, Long>,
    AppealRepositoryPort {
    override fun findByGameId(gameId: Long): List<Appeal>

    override fun findByAppealerId(appealerId: Long): List<Appeal>

    override fun findByStatus(status: AppealStatus): List<Appeal>
}
