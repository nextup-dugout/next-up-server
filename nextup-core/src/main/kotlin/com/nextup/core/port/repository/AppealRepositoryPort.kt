package com.nextup.core.port.repository

import com.nextup.core.domain.appeal.Appeal
import com.nextup.core.domain.appeal.AppealStatus

interface AppealRepositoryPort {
    fun save(appeal: Appeal): Appeal

    fun findByIdOrNull(id: Long): Appeal?

    fun findByGameId(gameId: Long): List<Appeal>

    fun findByAppealerId(appealerId: Long): List<Appeal>

    fun findByStatus(status: AppealStatus): List<Appeal>

    fun findAll(): List<Appeal>
}
