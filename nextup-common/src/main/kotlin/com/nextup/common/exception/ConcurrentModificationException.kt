package com.nextup.common.exception

/**
 * 낙관적 락 충돌로 인한 동시 수정 예외
 *
 * 동시 요청이 같은 엔티티를 수정하려 할 때 발생합니다.
 */
class ConcurrentModificationException(
    code: String,
    message: String,
) : BusinessException(code, message)
