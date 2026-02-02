package com.nextup.core.domain.player

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 선수의 팀 소속 이력을 관리하는 엔티티.
 *
 * 베스트 프랙티스:
 * 1. M:N 관계를 중간 엔티티로 풀어서 이력 정보(기간, 등번호 등)를 저장
 * 2. endDate가 null이면 현재 소속 중
 * 3. 복합 인덱스로 시간 범위 쿼리 최적화
 * 4. 양방향 관계 시 편의 메서드를 통해 일관성 유지
 */
@Entity
@Table(
    name = "player_team_histories",
    indexes = [
        Index(name = "idx_pth_player_id", columnList = "player_id"),
        Index(name = "idx_pth_team_id", columnList = "team_id"),
        Index(name = "idx_pth_player_dates", columnList = "player_id, start_date, end_date"),
        Index(name = "idx_pth_team_dates", columnList = "team_id, start_date, end_date"),
        Index(name = "idx_pth_current", columnList = "player_id, end_date"),
        Index(name = "idx_pth_status", columnList = "status"),
        Index(name = "idx_pth_player_status", columnList = "player_id, status")
    ]
)
class PlayerTeamHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "uniform_number")
    var uniformNumber: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var position: Position,

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 20)
    var contractType: ContractType = ContractType.REGULAR,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PlayerTeamStatus = PlayerTeamStatus.ACTIVE,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {

    val isCurrentAffiliation: Boolean
        get() = endDate == null

    val durationInDays: Long?
        get() = endDate?.let { ChronoUnit.DAYS.between(startDate, it) }

    /**
     * 현재 소속 상태가 ACTIVE인지 확인합니다.
     */
    val isActive: Boolean
        get() = status == PlayerTeamStatus.ACTIVE

    fun endAffiliation(date: LocalDate) {
        require(date >= startDate) { "종료일은 시작일 이후여야 합니다." }
        this.endDate = date
    }

    /**
     * 선수를 다른 팀으로 이적 처리합니다.
     *
     * @param transferDate 이적 날짜
     * @throws IllegalStateException 이미 이적했거나 비활동 상태인 경우
     * @throws IllegalArgumentException 이적 날짜가 시작일 이전인 경우
     */
    fun transfer(transferDate: LocalDate) {
        check(status == PlayerTeamStatus.ACTIVE) {
            "ACTIVE 상태인 선수만 이적할 수 있습니다. 현재 상태: $status"
        }
        require(transferDate >= startDate) {
            "이적일은 소속 시작일($startDate) 이후여야 합니다."
        }
        this.status = PlayerTeamStatus.TRANSFERRED
        this.endDate = transferDate
    }

    /**
     * 선수를 비활동 상태로 변경합니다.
     *
     * @param deactivateDate 비활동 시작 날짜
     * @throws IllegalStateException 이미 비활동이거나 이적한 상태인 경우
     * @throws IllegalArgumentException 비활동 날짜가 시작일 이전인 경우
     */
    fun deactivate(deactivateDate: LocalDate) {
        check(status == PlayerTeamStatus.ACTIVE) {
            "ACTIVE 상태인 선수만 비활동 처리할 수 있습니다. 현재 상태: $status"
        }
        require(deactivateDate >= startDate) {
            "비활동일은 소속 시작일($startDate) 이후여야 합니다."
        }
        this.status = PlayerTeamStatus.INACTIVE
        this.endDate = deactivateDate
    }

    fun changeUniformNumber(number: Int) {
        this.uniformNumber = number
    }

    fun changePosition(newPosition: Position) {
        this.position = newPosition
    }

    fun isActiveAt(date: LocalDate): Boolean {
        return !startDate.isAfter(date) && (endDate == null || !endDate!!.isBefore(date))
    }

    fun overlaps(other: PlayerTeamHistory): Boolean {
        if (this.player.id != other.player.id) return false

        val thisEnd = this.endDate ?: LocalDate.MAX
        val otherEnd = other.endDate ?: LocalDate.MAX

        return this.startDate <= otherEnd && other.startDate <= thisEnd
    }
}
