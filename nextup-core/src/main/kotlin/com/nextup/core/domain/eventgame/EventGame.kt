package com.nextup.core.domain.eventgame

import com.nextup.common.exception.InvalidStateException
import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime

/**
 * 이벤트 게임 엔티티
 *
 * 팀 상관 없이 야구하고 싶은 개인들이 모여서 하는 ad-hoc 픽업 게임입니다.
 * 공식 대회(Competition/Game)와 독립적으로 운영되며, 시즌 공식 통계에 포함되지 않습니다.
 *
 * 워크플로우:
 * 1. RECRUITING: 주최자가 이벤트 게임을 생성하고 참가자를 모집
 * 2. CLOSED: 모집 마감 (수동 또는 정원 도달 시 자동)
 * 3. TEAM_ASSIGNED: 주최자가 참가자를 팀A/팀B로 배정
 * 4. IN_PROGRESS: 경기 진행
 * 5. FINISHED: 경기 종료
 */
@Entity
@Table(
    name = "event_games",
    indexes = [
        Index(name = "idx_event_games_status", columnList = "status"),
        Index(name = "idx_event_games_scheduled_at", columnList = "scheduled_at"),
        Index(name = "idx_event_games_organizer", columnList = "organizer_id"),
    ],
)
class EventGame private constructor(
    @Column(name = "organizer_id", nullable = false)
    val organizerId: Long,
    @Column(nullable = false, length = 200)
    var title: String,
    @Column(length = 1000)
    var description: String? = null,
    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: LocalDateTime,
    @Column(length = 200)
    var location: String? = null,
    @Column(name = "field_name", length = 100)
    var fieldName: String? = null,
    @Column(name = "max_participants", nullable = false)
    var maxParticipants: Int,
    @Column(name = "innings", nullable = false)
    var innings: Int = 7,
    @Column(name = "team_a_name", nullable = false, length = 50)
    var teamAName: String = "Team A",
    @Column(name = "team_b_name", nullable = false, length = 50)
    var teamBName: String = "Team B",
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EventGameStatus = EventGameStatus.RECRUITING
        protected set

    @Column(name = "team_a_score")
    var teamAScore: Int? = null
        protected set

    @Column(name = "team_b_score")
    var teamBScore: Int? = null
        protected set

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null
        protected set

    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null
        protected set

    @Column(name = "cancel_reason", length = 500)
    var cancelReason: String? = null
        protected set

    @Version
    var version: Long = 0

    @OneToMany(mappedBy = "eventGame", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    private val _participants: MutableList<EventGameParticipant> = mutableListOf()
    val participants: List<EventGameParticipant> get() = _participants.toList()

    /**
     * 모집을 마감합니다.
     */
    fun closeRecruitment() {
        if (!status.canClose()) {
            throw InvalidStateException(
                "INVALID_EVENT_GAME_STATE",
                "모집 중인 이벤트 게임만 마감할 수 있습니다. 현재 상태: ${status.displayName}",
            )
        }
        this.status = EventGameStatus.CLOSED
    }

    /**
     * 팀 배정을 완료합니다.
     */
    fun completeTeamAssignment() {
        if (!status.canAssignTeams()) {
            throw InvalidStateException(
                "INVALID_EVENT_GAME_STATE",
                "모집 마감된 이벤트 게임만 팀 배정할 수 있습니다. 현재 상태: ${status.displayName}",
            )
        }
        val confirmedParticipants =
            _participants.filter { it.status == EventGameParticipantStatus.CONFIRMED }
        val allAssigned = confirmedParticipants.all { it.teamAssignment != null }
        if (!allAssigned) {
            throw InvalidStateException(
                "INVALID_EVENT_GAME_STATE",
                "모든 확정 참가자에게 팀이 배정되어야 합니다.",
            )
        }
        this.status = EventGameStatus.TEAM_ASSIGNED
    }

    /**
     * 경기를 시작합니다.
     */
    fun start() {
        if (!status.canStart()) {
            throw InvalidStateException(
                "INVALID_EVENT_GAME_STATE",
                "팀 배정이 완료된 이벤트 게임만 시작할 수 있습니다. 현재 상태: ${status.displayName}",
            )
        }
        this.status = EventGameStatus.IN_PROGRESS
        this.startedAt = LocalDateTime.now()
    }

    /**
     * 경기를 종료합니다.
     */
    fun finish(
        teamAScore: Int,
        teamBScore: Int,
    ) {
        if (!status.canFinish()) {
            throw InvalidStateException(
                "INVALID_EVENT_GAME_STATE",
                "진행 중인 이벤트 게임만 종료할 수 있습니다. 현재 상태: ${status.displayName}",
            )
        }
        require(teamAScore >= 0) { "점수는 0 이상이어야 합니다." }
        require(teamBScore >= 0) { "점수는 0 이상이어야 합니다." }
        this.status = EventGameStatus.FINISHED
        this.teamAScore = teamAScore
        this.teamBScore = teamBScore
        this.endedAt = LocalDateTime.now()
    }

    /**
     * 이벤트 게임을 취소합니다.
     */
    fun cancel(reason: String) {
        if (!status.canCancel()) {
            throw InvalidStateException(
                "INVALID_EVENT_GAME_STATE",
                "이미 진행 중이거나 종료된 이벤트 게임은 취소할 수 없습니다. 현재 상태: ${status.displayName}",
            )
        }
        require(reason.isNotBlank()) { "취소 사유는 필수입니다." }
        this.status = EventGameStatus.CANCELLED
        this.cancelReason = reason
    }

    /**
     * 참가자를 추가합니다.
     */
    fun addParticipant(participant: EventGameParticipant) {
        if (!status.canJoin()) {
            throw InvalidStateException(
                "INVALID_EVENT_GAME_STATE",
                "모집 중인 이벤트 게임만 참가 신청할 수 있습니다. 현재 상태: ${status.displayName}",
            )
        }
        val activeCount =
            _participants.count { it.status != EventGameParticipantStatus.CANCELLED }
        if (activeCount >= maxParticipants) {
            throw InvalidStateException(
                "EVENT_GAME_FULL",
                "이벤트 게임 정원이 가득 찼습니다. 최대 인원: $maxParticipants",
            )
        }
        val alreadyJoined =
            _participants.any {
                it.playerId == participant.playerId &&
                    it.status != EventGameParticipantStatus.CANCELLED
            }
        if (alreadyJoined) {
            throw InvalidStateException(
                "ALREADY_JOINED_EVENT_GAME",
                "이미 참가 신청한 이벤트 게임입니다.",
            )
        }
        _participants.add(participant)
    }

    /**
     * 현재 활성 참가자 수를 반환합니다.
     */
    val activeParticipantCount: Int
        get() = _participants.count { it.status != EventGameParticipantStatus.CANCELLED }

    companion object {
        fun create(
            organizerId: Long,
            title: String,
            description: String? = null,
            scheduledAt: LocalDateTime,
            location: String? = null,
            fieldName: String? = null,
            maxParticipants: Int,
            innings: Int = 7,
            teamAName: String = "Team A",
            teamBName: String = "Team B",
        ): EventGame {
            require(maxParticipants >= 2) { "최소 참가 인원은 2명 이상이어야 합니다." }
            require(innings in 1..9) { "이닝은 1~9 사이여야 합니다." }
            require(title.isNotBlank()) { "제목은 필수입니다." }
            return EventGame(
                organizerId = organizerId,
                title = title,
                description = description,
                scheduledAt = scheduledAt,
                location = location,
                fieldName = fieldName,
                maxParticipants = maxParticipants,
                innings = innings,
                teamAName = teamAName,
                teamBName = teamBName,
            )
        }
    }
}
