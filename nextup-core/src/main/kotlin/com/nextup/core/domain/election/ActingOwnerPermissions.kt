package com.nextup.core.domain.election

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 임시 구단주(Acting Owner) 권한 Value Object
 *
 * 비상대책위원회 모드에서 임시 구단주에게 부여되는 제한된 권한을 정의합니다.
 * 임시 구단주는 라인업/일정 관리는 가능하나 강퇴/해산/소유권 이전은 불가합니다.
 */
@Embeddable
data class ActingOwnerPermissions(
    @Column(name = "can_manage_lineup", nullable = false)
    val canManageLineup: Boolean = true,
    @Column(name = "can_manage_schedule", nullable = false)
    val canManageSchedule: Boolean = true,
    @Column(name = "can_kick_member", nullable = false)
    val canKickMember: Boolean = false,
    @Column(name = "can_dissolve_team", nullable = false)
    val canDissolveTeam: Boolean = false,
    @Column(name = "can_transfer_ownership", nullable = false)
    val canTransferOwnership: Boolean = false,
) {
    companion object {
        /**
         * 기본 임시 구단주 권한을 생성합니다.
         *
         * 라인업/일정 관리 가능, 강퇴/해산/소유권 이전 불가.
         */
        fun default(): ActingOwnerPermissions = ActingOwnerPermissions()
    }
}
