package com.nextup.infrastructure.repository.notification

import com.nextup.core.domain.notification.DeviceToken
import com.nextup.core.port.repository.DeviceTokenRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceTokenRepository :
    JpaRepository<DeviceToken, Long>,
    DeviceTokenRepositoryPort {
    override fun findByUserId(userId: Long): List<DeviceToken>

    override fun deleteByToken(token: String)

    override fun findByToken(token: String): DeviceToken?
}
