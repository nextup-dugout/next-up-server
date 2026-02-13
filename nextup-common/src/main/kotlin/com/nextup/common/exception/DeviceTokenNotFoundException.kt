package com.nextup.common.exception

class DeviceTokenNotFoundException(
    id: Long,
) : NotFoundException(
        code = "DEVICE_TOKEN_NOT_FOUND",
        message = "디바이스 토큰을 찾을 수 없습니다: $id",
    )
