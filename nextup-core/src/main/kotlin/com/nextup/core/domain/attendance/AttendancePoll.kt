package com.nextup.core.domain.attendance

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 출석 투표 엔티티
 *
 * 팀 이벤트(경기, 연습, 회식 등)에 대한 참석 여부를 조사하는 투표를 관리합니다.
 * Rich Domain Model 원칙에 따라 비즈니스 로직을 Entity 내부에 캡슐화합니다.
 *
 * 경기(GAME) 카테고리의 경우 gameId를 통해 경기와 연결됩니다.
 */
@Entity
@Table(
    name = "attendance_polls",
    indexes = [
        Index(name = "idx_ap_team_id", columnList = "team_id"),
        Index(name = "idx_ap_status", columnList = "status"),
        Index(name = "idx_ap_event_date", columnList = "event_date"),
        Index(name = "idx_ap_category", columnList = "category"),
        Index(name = "idx_ap_game_id", columnList = "game_id"),
    ],
)
class AttendancePoll private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @Column(nullable = false, length = 200)
    var title: String,
    @Column(name = "event_date", nullable = false)
    val eventDate: LocalDateTime,
    @Column(nullable = false)
    val deadline: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var category: EventCategory = EventCategory.OTHER,
    @Column(name = "game_id")
    val gameId: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PollStatus = PollStatus.OPEN,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Version
    var version: Long = 0
        internal set

    /**
     * 투표가 진행 중인지 확인합니다.
     */
    fun isOpen(): Boolean = status == PollStatus.OPEN

    /**
     * 투표가 마감되었는지 확인합니다.
     */
    fun isClosed(): Boolean = status == PollStatus.CLOSED

    /**
     * 마감 시간이 지났는지 확인합니다.
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(deadline)

    /**
     * 투표를 마감합니다.
     */
    fun close() {
        check(isOpen()) { "이미 마감된 투표입니다." }
        this.status = PollStatus.CLOSED
    }

    /**
     * 투표가 가능한 상태인지 확인합니다.
     */
    fun canVote(): Boolean = isOpen() && !isExpired()

    /**
     * 경기용 투표인지 확인합니다.
     */
    fun isGamePoll(): Boolean = category == EventCategory.GAME && gameId != null

    /**
     * 제목을 수정합니다.
     */
    fun updateTitle(newTitle: String) {
        require(newTitle.isNotBlank()) { "제목은 비어있을 수 없습니다." }
        this.title = newTitle
    }

    companion object {
        /**
         * 출석 투표를 생성합니다.
         *
         * @param team 팀
         * @param title 투표 제목
         * @param eventDate 이벤트 날짜
         * @param deadline 투표 마감 시간
         * @param category 이벤트 카테고리
         * @param gameId 경기 ID (GAME 카테고리일 때)
         * @return 생성된 AttendancePoll
         */
        fun create(
            team: Team,
            title: String,
            eventDate: LocalDateTime,
            deadline: LocalDateTime,
            category: EventCategory = EventCategory.OTHER,
            gameId: Long? = null,
        ): AttendancePoll {
            require(title.isNotBlank()) { "제목은 비어있을 수 없습니다." }
            require(eventDate.isAfter(LocalDateTime.now())) {
                "이벤트 날짜는 현재 시간 이후여야 합니다."
            }
            require(deadline.isBefore(eventDate)) {
                "마감 시간은 이벤트 날짜보다 이전이어야 합니다."
            }
            if (category == EventCategory.GAME) {
                requireNotNull(gameId) { "경기 투표에는 gameId가 필수입니다." }
            }

            return AttendancePoll(
                team = team,
                title = title,
                eventDate = eventDate,
                deadline = deadline,
                category = category,
                gameId = gameId,
                status = PollStatus.OPEN,
            )
        }

        /**
         * 경기용 출석 투표를 생성합니다.
         *
         * @param team 팀
         * @param gameId 경기 ID
         * @param eventDate 경기 날짜
         * @param deadline 투표 마감 시간 (경기 1일 전)
         * @return 생성된 AttendancePoll
         */
        fun createForGame(
            team: Team,
            gameId: Long,
            eventDate: LocalDateTime,
            deadline: LocalDateTime,
        ): AttendancePoll {
            return create(
                team = team,
                title = "경기 출석 조사",
                eventDate = eventDate,
                deadline = deadline,
                category = EventCategory.GAME,
                gameId = gameId,
            )
        }
    }
}
