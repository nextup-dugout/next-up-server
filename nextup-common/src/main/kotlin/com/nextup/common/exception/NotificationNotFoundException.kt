package com.nextup.common.exception

class NotificationNotFoundException(
    id: Long,
) : NotFoundException(
        code = "NOTIFICATION_NOT_FOUND",
        message = "알림을 찾을 수 없습니다: $id",
    )
