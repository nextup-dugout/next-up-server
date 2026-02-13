package com.nextup.core.domain.player

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 선수 경력 엔티티
 *
 * 선수의 과거 경력(학교, 프로, 사회인 등)을 기록합니다.
 * '선출(선수 출신)' 여부 판별의 기초 데이터로 사용됩니다.
 */
@Entity
@Table(
    name = "player_careers",
    indexes = [
        Index(name = "idx_player_careers_player", columnList = "player_id"),
        Index(name = "idx_player_careers_type", columnList = "career_type"),
        Index(name = "idx_player_careers_dates", columnList = "player_id, start_date, end_date"),
    ],
)
class PlayerCareer(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Enumerated(EnumType.STRING)
    @Column(name = "career_type", nullable = false, length = 20)
    val type: CareerType,
    @Column(nullable = false, length = 100)
    val organization: String,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
    @Column(length = 50)
    val position: String? = null,
    @Column(length = 500)
    var description: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 현재 활동 중인 경력인지 확인합니다.
     */
    val isActive: Boolean
        get() = endDate == null

    /**
     * 프로 경력인지 확인합니다 (선출 판별 기초).
     */
    val isProfessional: Boolean
        get() = type.isProfessional

    /**
     * 경력을 종료합니다.
     */
    fun endCareer(date: LocalDate) {
        require(date >= startDate) { "종료일은 시작일 이후여야 합니다." }
        this.endDate = date
    }

    /**
     * 특정 날짜에 이 경력이 활성 상태였는지 확인합니다.
     */
    fun isActiveAt(date: LocalDate): Boolean =
        !startDate.isAfter(date) && (endDate == null || !endDate!!.isBefore(date))

    /**
     * 경력 기간(년)을 계산합니다.
     */
    fun calculateYears(baseDate: LocalDate = LocalDate.now()): Int {
        val end = endDate ?: baseDate
        return end.year - startDate.year
    }
}
