package com.nextup.core.domain.eventgame

import com.nextup.common.exception.InvalidStateException
import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 이벤트 게임 참가자 엔티티
 *
 * 이벤트 게임에 개인으로 참가 신청한 선수를 나타냅니다.
 * 팀 배정(TeamAssignment)은 주최자가 수동 또는 자동으로 배정합니다.
 */
@Entity
@Table(
    name = "event_game_participants",
    indexes = [
        Index(name = "idx_event_game_participants_game", columnList = "event_game_id"),
        Index(name = "idx_event_game_participants_player", columnList = "player_id"),
        Index(name = "idx_event_game_participants_status", columnList = "status"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_event_game_participants_game_player",
            columnNames = ["event_game_id", "player_id"],
        ),
    ],
)
class EventGameParticipant private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_game_id", nullable = false)
    val eventGame: EventGame,
    @Column(name = "player_id", nullable = false)
    val playerId: Long,
    @Column(length = 500)
    var message: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EventGameParticipantStatus = EventGameParticipantStatus.APPLIED
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "team_assignment", length = 10)
    var teamAssignment: TeamAssignment? = null
        protected set

    /**
     * 참가를 확정합니다.
     */
    fun confirm() {
        if (!status.canConfirm()) {
            throw InvalidStateException(
                "INVALID_PARTICIPANT_STATE",
                "신청 상태의 참가자만 확정할 수 있습니다. 현재 상태: ${status.displayName}",
            )
        }
        this.status = EventGameParticipantStatus.CONFIRMED
    }

    /**
     * 참가를 취소합니다.
     */
    fun cancel() {
        if (!status.canCancel()) {
            throw InvalidStateException(
                "INVALID_PARTICIPANT_STATE",
                "이미 취소된 참가자입니다. 현재 상태: ${status.displayName}",
            )
        }
        this.status = EventGameParticipantStatus.CANCELLED
        this.teamAssignment = null
    }

    /**
     * 팀을 배정합니다.
     */
    fun assignTeam(team: TeamAssignment) {
        if (status != EventGameParticipantStatus.CONFIRMED) {
            throw InvalidStateException(
                "INVALID_PARTICIPANT_STATE",
                "확정된 참가자만 팀 배정할 수 있습니다. 현재 상태: ${status.displayName}",
            )
        }
        this.teamAssignment = team
    }

    companion object {
        fun create(
            eventGame: EventGame,
            playerId: Long,
            message: String? = null,
        ): EventGameParticipant =
            EventGameParticipant(
                eventGame = eventGame,
                playerId = playerId,
                message = message,
            )
    }
}
