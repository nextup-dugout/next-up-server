package com.nextup.core.port.repository

import com.nextup.core.domain.notification.DeviceToken

interface DeviceTokenRepositoryPort {
    fun save(deviceToken: DeviceToken): DeviceToken

    fun findByIdOrNull(id: Long): DeviceToken?

    fun findByUserId(userId: Long): List<DeviceToken>

    fun deleteByToken(token: String)

    fun findByToken(token: String): DeviceToken?
}
