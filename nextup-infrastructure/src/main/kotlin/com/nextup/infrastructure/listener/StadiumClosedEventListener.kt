package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.StadiumClosedEvent
import com.nextup.core.domain.stadium.BookingStatus
import com.nextup.core.port.repository.StadiumBookingRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * L-12: 구장 폐업 이벤트 리스너
 *
 * 구장이 폐업(비활성화)되었을 때 해당 구장의 CONFIRMED 상태 예약을 일괄 취소합니다.
 * AFTER_COMMIT 단계에서 실행되어 구장 비활성화 트랜잭션 롤백에 영향을 주지 않습니다.
 */
@Component
class StadiumClosedEventListener(
    private val stadiumBookingRepository: StadiumBookingRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(StadiumClosedEventListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleStadiumClosed(event: StadiumClosedEvent) {
        logger.info(
            "구장 폐업 이벤트 수신 - stadiumId={}, stadiumName={}",
            event.stadiumId,
            event.stadiumName,
        )

        val confirmedBookings =
            stadiumBookingRepository.findByStadiumIdAndStatus(
                event.stadiumId,
                BookingStatus.CONFIRMED,
            )

        if (confirmedBookings.isEmpty()) {
            logger.info("취소 대상 예약 없음 - stadiumId={}", event.stadiumId)
            return
        }

        confirmedBookings.forEach { booking ->
            booking.cancel()
            stadiumBookingRepository.save(booking)
            logger.info(
                "구장 폐업으로 예약 취소 - bookingId={}, teamId={}",
                booking.id,
                booking.teamId,
            )
        }

        logger.info(
            "구장 폐업 예약 일괄 취소 완료 - stadiumId={}, 취소건수={}",
            event.stadiumId,
            confirmedBookings.size,
        )
    }
}
